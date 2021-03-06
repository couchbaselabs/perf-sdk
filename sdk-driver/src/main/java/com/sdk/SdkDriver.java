package com.sdk;

import com.couchbase.client.core.error.BucketExistsException;
import com.couchbase.client.core.error.BucketNotFoundException;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.bucket.StorageBackend;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sdk.config.BuiltSdkCommand;
import com.sdk.config.Op;
import com.sdk.config.OpGet;
import com.sdk.config.OpGrpcPing;
import com.sdk.config.OpInsert;
import com.sdk.config.OpRemove;
import com.sdk.config.OpReplace;
import com.sdk.config.TestSuite;
import com.sdk.constants.Defaults;
import com.sdk.constants.Strings;
import com.couchbase.client.performer.grpc.*;
import com.sdk.sdk.util.DbWriteThread;
import com.sdk.sdk.util.DocCreateThread;
import com.sdk.sdk.util.GrpcPerformanceMeasureThread;
import com.sdk.sdk.util.MetricsWriteThread;
import com.sdk.sdk.util.Performer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.okhttp3.Credentials;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request;
import org.testcontainers.shaded.okhttp3.RequestBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


public class SdkDriver {

    private final static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final static ObjectMapper jsonMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger logger = LoggerFactory.getLogger(SdkDriver.class);

    public static void main(String[] args)  throws Exception {
        // ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        if (args.length != 1) {
            logger.info("Must provide config.yaml");
            System.exit(-1);
        }
        run(args[0]);
    }

    static Op addOp(TestSuite.Run.Operation op, int count, TestSuite.Variables variables) {
        JsonObject initial = JsonObject.create().put(Strings.CONTENT_NAME, Strings.INITIAL_CONTENT_VALUE);
        JsonObject updated = JsonObject.create().put(Strings.CONTENT_NAME, Strings.UPDATED_CONTENT_VALUE);

        switch (op.op()) {
            case INSERT:
                return new OpInsert(initial, count, op.docLocation(), variables);
            case GET:
                return new OpGet(count, op.docLocation(), variables);
            case REMOVE:
                return new OpRemove(count, op.docLocation(), variables);
            case REPLACE:
                return new OpReplace(updated, count, op.docLocation(), variables);
            case PING:
                return new OpGrpcPing(count);
            default:
                throw new IllegalArgumentException("Unknown op " + op);
        }
    }

    static BuiltSdkCommand createSdkCommand(TestSuite.Run run, TestSuite.Variables merged) {
        //TODO Make sure any REPLACE operations are in the YAML before REMOVES
        var ops = new ArrayList<Op>();

        run.operations().forEach(op -> {
            int count = evaluateCount(merged, op.count());
            ops.add(addOp(op, count, merged));
        });
        return new BuiltSdkCommand(ops, "Test");
    }

    private static int evaluateCount(TestSuite.Variables variables, String count){
        try {
            return Integer.parseInt(count);
        } catch (RuntimeException err) {
            if (count.startsWith("$")) {
                return variables.getCustomVarAsInt(count);
            }

            throw new IllegalArgumentException("Don't know how to handle repeated count " + count);
        }
    }

    static TestSuite readTestSuite(String configFilename) {
        try {
            return yamlMapper.readValue(new File(configFilename), TestSuite.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static JsonObject readTestSuiteAsJson(String configFilename) {
        try {
            var object = yamlMapper.readValue(new File(configFilename), Object.class);
            var bytes = jsonMapper.writeValueAsBytes(object);
            return JsonObject.fromJson(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Credit to https://stackoverflow.com/questions/52580008/how-does-java-application-know-it-is-running-within-a-docker-container
    public static Boolean isRunningInsideDocker() {
        try (Stream<String> stream =
                     Files.lines(Paths.get("/proc/1/cgroup"))) {
            return stream.anyMatch(line -> line.contains("/docker"));
        } catch (IOException e) {
            return false;
        }
    }

    static void run(String testSuiteFile) throws Exception {
        logger.info("Reading config file {}", testSuiteFile);
        var testSuite = readTestSuite(testSuiteFile);
        var testSuiteAsJson = readTestSuiteAsJson(testSuiteFile);

        var dbUrl = String.format("jdbc:postgresql://%s:%d/%s",
                isRunningInsideDocker() ? testSuite.connections().database().hostname_docker() : testSuite.connections().database().hostname(),
                testSuite.connections().database().port(),
                testSuite.connections().database().database());

        var props = new Properties();
        props.setProperty("user", testSuite.connections().database().username());
        props.setProperty("password", testSuite.connections().database().password());

        logger.info("Is running inside Docker {}", isRunningInsideDocker());
        logger.info("Connecting to database " + dbUrl);

        try (var conn = DriverManager.getConnection(dbUrl, props)) {

            var clusterHostname = isRunningInsideDocker() ? testSuite.connections().cluster().hostname_docker() : testSuite.connections().cluster().hostname();
            logger.info("Connecting to cluster {}", clusterHostname);

            var cluster = Cluster.connect(clusterHostname, "Administrator", "password");

            var storageBackend = switch (testSuite.connections().cluster().storage()) {
                case "magma" -> StorageBackend.MAGMA;
                default -> StorageBackend.COUCHSTORE;
            };
            var replicas = testSuite.connections().cluster().replicas();

            // Use all the available memory for the bucket
            var memoryMb = testSuite.connections().cluster().memory();

            logger.info("(Re)creating bucket {} with memoryMB {} and backend {}", Defaults.DEFAULT_BUCKET, memoryMb, storageBackend);
            var bucketSettings = BucketSettings.create(Defaults.DEFAULT_BUCKET)
                    .ramQuotaMB(memoryMb)
                    .numReplicas(replicas)
                    .storageBackend(storageBackend);

            try {
                cluster.buckets().dropBucket(Defaults.DEFAULT_BUCKET);
            }
            catch (BucketNotFoundException ignored) {}

            try {
                cluster.buckets().createBucket(bucketSettings);
            }
            catch (BucketExistsException ignored) {}

            // CBD-5001: auto-compaction is disabled for performance.  To prevent disk space growing indefinitely,
            // do a compaction after destroying the bucket.
            forceCompaction(clusterHostname);

            var createConnection =
                    ClusterConnectionCreateRequest.newBuilder()
                            .setClusterHostname(clusterHostname)
                            .setClusterUsername("Administrator")
                            .setClusterPassword("password")
                            .setClusterConnectionId(UUID.randomUUID().toString())
                            .build();

            for (TestSuite.Run run : testSuite.runs()) {
                logger.info("Running workload " + run);

                // Merge the per-run variables with the top-level variables.  Per-run overrides.
                var merged = testSuite.variables() == null
                        ? run.variables()
                        : run.variables().mergeWithTopLevel(testSuite.variables());

                // Connect to the performer for each run, as the GRPC variables may be different
                var performerHostname = isRunningInsideDocker() ? testSuite.connections().performer().hostname_docker() : testSuite.connections().performer().hostname();
                Optional<Boolean> grpcCompression = (merged.grpc() == null) ? Optional.empty() : Optional.ofNullable(merged.grpc().compression());
                logger.info("Connecting to performer on {}:{} compression={}", performerHostname, testSuite.connections().performer().port(), grpcCompression);

                var performer = new Performer(
                        performerHostname,
                        testSuite.connections().performer().port(),
                        createConnection,
                        grpcCompression);

                run.operations().forEach(op -> {
                    if (op.docLocation() != null) {
                        op.docLocation().poolSize(merged).ifPresent(docPoolSize -> {
                            var docThread = new DocCreateThread(docPoolSize,
                                    cluster.bucket(Defaults.DEFAULT_BUCKET).defaultCollection());
                            docThread.start();
                            // wait for all docs to be created
                            logger.info("Waiting for document pool of size {} to be created", docPoolSize);
                            try {
                                docThread.join();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                });

                BuiltSdkCommand command = createSdkCommand(run, merged);

                var horizontalScalingBuilt = HorizontalScaling.newBuilder();

                command.sdkCommand().forEach(op -> {
                    op.applyTo(horizontalScalingBuilt);
                });

                merged.predefined().forEach(p -> {
                            if (p.values() != null && p.values().length > 1) {
                                // Reminder: want to be able to have multiple values for say horizontal_scaling, and for this to generate
                                // multiple runs.
                                throw new UnsupportedOperationException();
                            }
                        });

                PerfRunRequest.Builder perf = PerfRunRequest.newBuilder()
                        .setClusterConnectionId(createConnection.getClusterConnectionId());
                var perfConfigBuilder = PerfRunConfig.newBuilder();
                PerfRunConfigStreaming.Builder streamingConfig = null;
                if (merged.grpc() != null) {
                    if (merged.grpc().batch() != null && merged.grpc().batch() != 0) {
                        if (streamingConfig == null) streamingConfig = PerfRunConfigStreaming.newBuilder();
                        streamingConfig.setBatchSize(merged.grpc().batch());
                    }
                    if (merged.grpc().flowControl() != null) {
                        if (streamingConfig == null) streamingConfig = PerfRunConfigStreaming.newBuilder();
                        streamingConfig.setFlowControl(merged.grpc().flowControl());
                    }
                }
                if (streamingConfig != null) {
                    perfConfigBuilder.setStreamingConfig(streamingConfig);
                }
                perf.setConfig(perfConfigBuilder.build());

                for (int i=0; i< merged.horizontalScaling(); i++){
                    perf.addHorizontalScaling(horizontalScalingBuilt);
                }

                var done = new AtomicBoolean(false);
                var dbWrite = new DbWriteThread(conn, run.uuid(), done);
                var metricsWrite = new MetricsWriteThread(conn, run.uuid());
                var performanceMonitor = new GrpcPerformanceMeasureThread();
                dbWrite.start();
                metricsWrite.start();
                performanceMonitor.start();
                var received = new AtomicInteger(0);
                var start = System.nanoTime();

                var responseObserver = new StreamObserver<PerfRunResult>() {
                    @Override
                    public void onNext(PerfRunResult perfRunResult) {
                        handleResult(perfRunResult, received, dbWrite, metricsWrite, performanceMonitor);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        logger.error("Error from performer after receiving {}: {}", received.get(), throwable.toString());
                        done.set(true);
                        System.exit(-1);
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Performer has finished after receiving {} in {} secs",
                                received.get(), TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start));
                        done.set(true);
                    }
                };

                performer.stubBlockFuture().perfRun(perf.build(), responseObserver);
                logger.info("Waiting for run to finish and data to be written to db");
                dbWrite.join();
                metricsWrite.interrupt();
                metricsWrite.join();
                performanceMonitor.interrupt();
                performanceMonitor.join();

                if (run.shouldWrite()) {
                    writeRun(testSuite, testSuiteAsJson, conn, run, performer, merged);
                }
                else {
                    logger.info("Not writing this run to database");
                }

                logger.info("Finished!");
            }

        }
    }

    private static void forceCompaction(String clusterHostname) throws IOException {
        var httpClient = new OkHttpClient().newBuilder().build();

        var resp = httpClient.newCall(new Request.Builder()
                        .method("POST", RequestBody.create(null, new byte[]{}))
                        .header("Authorization", Credentials.basic("Administrator", "password"))
                        .url("http://" + clusterHostname + ":8091/pools/default/buckets/" + Defaults.DEFAULT_BUCKET + "/controller/compactBucket")
                        .build())
                .execute();
        logger.info("Result of forcing compaction: " + resp.message());
        resp.close();
    }

    private static void writeRun(TestSuite testSuite,
                                 JsonObject testSuiteAsJson,
                                 Connection conn,
                                 TestSuite.Run run,
                                 Performer performer,
                                 TestSuite.Variables merged) throws JsonProcessingException, SQLException {
        // Only insert into the runs table if everything was successful
        var jsonVars = JsonObject.create();
        merged.custom().forEach(v -> jsonVars.put(v.name(), v.value()));
        merged.predefined().forEach(v -> jsonVars.put(v.name().name().toLowerCase(), v.values()[0]));
        if (merged.driverVer() != null) jsonVars.put("driverVer", merged.driverVer());
        if (merged.performerVer() != null) jsonVars.put("performerVer", merged.performerVer());

        var runJson = testSuiteAsJson.getArray("runs")
                .toList().stream()
                .map(v -> JsonObject.from((HashMap) v))
                .filter(v -> v.getString("uuid").equals(run.uuid()))
                .findFirst()
                .get();
        runJson.removeKey("uuid");
        // Don't write this as we currently split it out into a separate "vars" field.  Some discussion on CBD-4979.
        runJson.removeKey("variables");

        var om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        var clusterMap = om.convertValue(testSuite.connections().cluster(), Map.class);
        var clusterJson = JsonObject.from(clusterMap);
        clusterJson.removeKey("hostname");
        clusterJson.removeKey("hostname_docker");


        var json = JsonObject.create()
                .put("cluster", clusterJson)
                .put("impl", JsonObject.fromJson(jsonMapper.writeValueAsString(testSuite.impl())))
                .put("workload", runJson)
                .put("vars", jsonVars);
        logger.info(json.toString());

        try (var st = conn.createStatement()) {
            String statement = String.format("INSERT INTO runs VALUES ('%s', NOW(), '%s') ON CONFLICT (id) DO UPDATE SET datetime = NOW(), params = '%s'",
                    run.uuid(),
                    json.toString(),
                    json.toString());
            st.executeUpdate(statement);
        }
    }

    private static void handleResult(PerfRunResult perfRunResult, AtomicInteger received, DbWriteThread dbWrite, MetricsWriteThread metricsWrite, GrpcPerformanceMeasureThread performanceMonitor) {
        received.incrementAndGet();

        if (perfRunResult.hasOperationResult()) {
            performanceMonitor.register(perfRunResult);
            dbWrite.enqueue(perfRunResult.getOperationResult());
        }
        else if (perfRunResult.hasMetricsResult()) {
            performanceMonitor.register(perfRunResult);
            metricsWrite.enqueue(perfRunResult);
        }
        else if (perfRunResult.hasGrpcResult()) {
            performanceMonitor.register(perfRunResult);
        }
        else if (perfRunResult.hasBatchedResult()) {
            var batched = perfRunResult.getBatchedResult();
            for (int i = 0; i < batched.getResultCount(); i ++) {
                handleResult(batched.getResult(i), received, dbWrite, metricsWrite, performanceMonitor);
            }
        }

    }
}
