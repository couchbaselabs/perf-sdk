package com.sdk;

import com.couchbase.client.core.error.BucketExistsException;
import com.couchbase.client.core.error.BucketNotFoundException;
import com.couchbase.client.core.error.InvalidArgumentException;
import com.couchbase.client.core.io.CollectionIdentifier;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.grpc.sdk.protocol.Collection;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sdk.constants.Defaults;
import com.sdk.constants.Strings;
import com.couchbase.grpc.sdk.protocol.*;
import com.couchbase.grpc.sdk.protocol.CommandInsert;
import com.sdk.sdk.util.DbWriteThread;
import com.sdk.sdk.util.DocCreateThread;
import com.sdk.sdk.util.Performer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.okhttp3.Credentials;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


record TestSuite(Implementation impl, Variables variables, Connections connections, List<Run> runs) {
//    Duration runtimeAsDuration() {
//        var trimmed = runtime.trim();
//        char suffix = trimmed.charAt(trimmed.length() - 1);
//        var rawNum = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
//        return switch (suffix) {
//            case 's' -> Duration.ofSeconds(rawNum);
//            case 'm' -> Duration.ofMinutes(rawNum);
//            case 'h' -> Duration.ofHours(rawNum);
//            default -> throw new IllegalArgumentException("Could not handle runtime " + runtime);
//        };
//    }
//
//    int runtimeAsInt(){
//        var trimmed = runtime.trim();
//        char suffix = trimmed.charAt(trimmed.length() - 1);
//        var rawNum = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
//        return switch (suffix) {
//            case 's' -> rawNum;
//            case 'm' -> rawNum * Defaults.secPerMin;
//            case 'h' -> rawNum * Defaults.secPerHour;
//            default -> throw new IllegalArgumentException("Could not handle runtime " + runtime);
//        };
//    }

    record Implementation(String language, String version){
    }

    record Variables(List<PredefinedVariable> predefined, List<CustomVariable> custom) {
        Integer getCustomVarAsInt(String varName) {
            if (varName.startsWith("$")) {
                return getCustomVarAsInt(varName.substring(1));
            }

            return Integer.parseInt(varName);
        }

        Integer horizontalScaling() {
            return (Integer) predefinedVar(PredefinedVariable.PredefinedVariableName.HORIZONTAL_SCALING);
        }

        record PredefinedVariable(PredefinedVariableName name, Object value) {
            enum PredefinedVariableName {
                @JsonProperty("horizontal_scaling") HORIZONTAL_SCALING,
            }
        }

        private Object predefinedVar(PredefinedVariable.PredefinedVariableName name) {
            return predefined.stream()
                    .filter(v -> v.name == name)
                    .findFirst()
                    .map(v -> v.value)
                    .orElseThrow(() -> new IllegalArgumentException("Predefined variable " + name + " not found"));
        }

        record CustomVariable(String name, Object value) {
        }
    }

    record Connections(Cluster cluster, PerformerConn performer, Database database) {
        record Cluster(String hostname, String hostname_docker, String username, String password, String type) {
        }

        record PerformerConn(String hostname, String hostname_docker, int port) {
        }

        record Database(String hostname, String hostname_docker, int port, String username, String password, String database) {
        }
    }

    record DocLocation(Method method, String id, String idPreface, String poolSize, PoolSelectionStrategy poolSelectionStrategy) {
        enum Method {
            @JsonProperty("specific") SPECIFIC,
            @JsonProperty("uuid") UUID,
            @JsonProperty("pool") POOL
        }

        enum PoolSelectionStrategy {
            @JsonProperty("random_uniform") RANDOM_UNIFORM,
            @JsonProperty("counter") COUNTER
        }

        public Optional<Long> poolSize(TestSuite.Variables variables) {
            if (method != Method.POOL) {
                return Optional.empty();
            }

            return Optional.of((long) variables.getCustomVarAsInt(poolSize));
        }

        public com.couchbase.grpc.sdk.protocol.DocLocation convert(TestSuite.Variables variables) {
            var out = com.couchbase.grpc.sdk.protocol.DocLocation.newBuilder();
            var collection = Collection.newBuilder()
                    .setBucket(Defaults.DEFAULT_BUCKET)
                    .setScope(CollectionIdentifier.DEFAULT_SCOPE)
                    .setCollection(CollectionIdentifier.DEFAULT_SCOPE)
                    .build();


            switch (method) {
                case SPECIFIC -> {
                    if (id == null) {
                        throw new IllegalArgumentException("Expect id to be set on specific doc location");
                    }

                    out.setSpecific(DocLocationSpecific.newBuilder()
                            .setCollection(collection)
                            .setId(id));
                }

                case UUID -> out.setUuid(DocLocationUuid.newBuilder()
                        .setCollection(collection)
                        .build());

                case POOL -> {
                    var idPrefaceFinal = (idPreface == null) ? Defaults.KEY_PREFACE : idPreface;
                    long poolSizeFinal = variables.getCustomVarAsInt(poolSize);

                    var builder = DocLocationPool.newBuilder()
                            .setCollection(collection)
                            .setIdPreface(idPrefaceFinal)
                            .setPoolSize(poolSizeFinal);

                    switch (poolSelectionStrategy) {
                        case RANDOM_UNIFORM -> builder.setUniform(PoolSelectionStategyRandom.newBuilder()
                                .setDistribution(RandomDistribution.RANDOM_DISTRIBUTION_UNIFORM));
                        case COUNTER -> builder.setCounter(PoolSelectionStrategyCounter.newBuilder());
                        default ->
                                throw new IllegalArgumentException("Unknown pool selection " + poolSelectionStrategy);
                    }

                    out.setPool(builder);
                }
            }

            return out.build();
        }
    }

    record Run(String uuid, String description, List<Operation> operations) {
        record Operation(Op op, String count, DocLocation docLocation) {
            enum Op {
                @JsonProperty("insert") INSERT,
                @JsonProperty("get") GET,
                @JsonProperty("remove") REMOVE,
                @JsonProperty("replace") REPLACE
            }
        }
    }
}

record BuiltSdkCommand(List<Op> sdkCommand, String description) {
}

interface Op {
    void applyTo(PerfRunHorizontalScaling.Builder builder);
}

record OpInsert(JsonObject content, int count, TestSuite.DocLocation location, TestSuite.Variables variables) implements Op {
    @Override
    public void applyTo(PerfRunHorizontalScaling.Builder builder) {
        builder.addWorkloads(Workload.newBuilder()
                .setSdk(SdkWorkload.newBuilder()
                        .setCommand(SdkCommand.newBuilder()
                                .setInsert(CommandInsert.newBuilder()
                                        .setContentJson(content.toString())
                                        .setLocation(location.convert(variables))
                                        .build()))
                        .setCounter(Counter.newBuilder()
                                .setCounterId("counter1")
                                .setGlobal(CounterGlobal.newBuilder()
                                        .setCount(count)))));    }
}

record OpGet(int count, TestSuite.DocLocation location, TestSuite.Variables variables) implements Op {
    @Override
    public void applyTo(PerfRunHorizontalScaling.Builder builder) {
        builder.addWorkloads(Workload.newBuilder()
                .setSdk(SdkWorkload.newBuilder()
                        .setCommand(SdkCommand.newBuilder()
                                .setGet(CommandGet.newBuilder()
                                        .setLocation(location.convert(variables))
                                        .build()))
                        .setCounter(Counter.newBuilder()
                                .setCounterId("counter1")
                                .setGlobal(CounterGlobal.newBuilder()
                                        .setCount(count)))));    }
}

record OpRemove(int count, TestSuite.DocLocation location, TestSuite.Variables variables) implements Op {
    @Override
    public void applyTo(PerfRunHorizontalScaling.Builder builder){
        builder.addWorkloads(Workload.newBuilder()
                .setSdk(SdkWorkload.newBuilder()
                        .setCommand(SdkCommand.newBuilder()
                                .setRemove(CommandRemove.newBuilder()
                                        .setLocation(location.convert(variables))
                                        .build()))
                        .setCounter(Counter.newBuilder()
                                .setCounterId("counter1")
                                .setGlobal(CounterGlobal.newBuilder()
                                        .setCount(count)))));
    }
}

record OpReplace(JsonObject content, int count, TestSuite.DocLocation location, TestSuite.Variables variables) implements Op {
    @Override
    public void applyTo(PerfRunHorizontalScaling.Builder builder){
        builder.addWorkloads(Workload.newBuilder()
                .setSdk(SdkWorkload.newBuilder()
                        .setCommand(SdkCommand.newBuilder()
                                .setReplace(CommandReplace.newBuilder()
                                        .setContentJson(content.toString())
                                        .setLocation(location.convert(variables))
                                        .build()))
                        .setCounter(Counter.newBuilder()
                                .setCounterId("counter1")
                                .setGlobal(CounterGlobal.newBuilder()
                                        .setCount(count)))));    }
}

public class SdkDriver {

    private final static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final static ObjectMapper jsonMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger logger = LoggerFactory.getLogger(SdkDriver.class);

    public static void main(String[] args)  throws Exception {
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
            default:
                throw new IllegalArgumentException("Unknown op " + op);
        }
    }

    static BuiltSdkCommand createSdkCommand(TestSuite.Variables variables, TestSuite.Run run) {
        //TODO Make sure any REPLACE operations are in the YAML before REMOVES
        //StringBuilder sb = new StringBuilder();
        var ops = new ArrayList<Op>();

        run.operations().forEach(op -> {
            int count = evaluateCount(variables, op.count());
            ops.add(addOp(op, count, variables));
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
            e.printStackTrace();
        }
        return null;
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

            var cluster = Cluster.connect(clusterHostname, testSuite.connections().cluster().username(), testSuite.connections().cluster().password());

            logger.info("(Re)creating bucket {}", Defaults.DEFAULT_BUCKET);

            try {
                cluster.buckets().dropBucket(Defaults.DEFAULT_BUCKET);
            }
            catch (BucketNotFoundException ignored) {}

            try {
                cluster.buckets().createBucket(BucketSettings.create(Defaults.DEFAULT_BUCKET));
            }
            catch (BucketExistsException ignored) {}

            var createConnection =
                    ClusterConnectionCreateRequest.newBuilder()
                            .setClusterHostname(clusterHostname)
                            .setClusterUsername(testSuite.connections().cluster().username())
                            .setClusterPassword(testSuite.connections().cluster().password())
                            .setClusterConnectionId(UUID.randomUUID().toString())
                            .build();

            var performerHostname = isRunningInsideDocker() ? testSuite.connections().performer().hostname_docker() : testSuite.connections().performer().hostname();
            logger.info("Connecting to performer on {}:{}", performerHostname, testSuite.connections().performer().port());

            Performer performer = new Performer(
                    performerHostname,
                    testSuite.connections().performer().port(),
                    createConnection);

            for (TestSuite.Run run : testSuite.runs()) {
                logger.info("Running workload " + run);

                run.operations().forEach(op -> {
                    op.docLocation().poolSize(testSuite.variables()).ifPresent(docPoolSize -> {
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
                });



                BuiltSdkCommand command = createSdkCommand(testSuite.variables(), run);

                PerfRunHorizontalScaling.Builder horizontalScalingBuilt = PerfRunHorizontalScaling.newBuilder();

                command.sdkCommand().forEach(op -> {
                    op.applyTo(horizontalScalingBuilt);
                });

                PerfRunRequest.Builder perf = PerfRunRequest.newBuilder()
                        .setClusterConnectionId(createConnection.getClusterConnectionId());

                for (int i=0; i< testSuite.variables().horizontalScaling(); i++){
                    perf.addHorizontalScaling(horizontalScalingBuilt);
                }

                var done = new AtomicBoolean(false);
                DbWriteThread dbWrite = new DbWriteThread(conn, run.uuid(), done);
                dbWrite.start();
                var received = new AtomicInteger(0);

                // CBD-4926: this isn't the slickest way of doing this (would prefer to write it all to database and
                // trim at consumption point), but as a quick fix, discard the first set of data.  This takes care of
                // JVM warmup and other forms of settling.
                var start = System.nanoTime();
                var ignoringInitialResults = new AtomicBoolean(true);

                var responseObserver = new StreamObserver<PerfSingleResult>() {
                    @Override
                    public void onNext(PerfSingleResult perfRunResult) {
                        var got = received.incrementAndGet();
                        if (got % 1000 == 0 || got == 1) {
                            logger.info("Received {}", got);
                        }

                        if (ignoringInitialResults.get()) {
//                            if (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) >= 15) {
                            if (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) >= 0) {
                                logger.info("Warmup period passed, now writing results to database");
                                ignoringInitialResults.set(false);
                            }
                        }

                        if (perfRunResult.hasOperationResult() && !ignoringInitialResults.get()) {
                            dbWrite.addToQ(perfRunResult.getOperationResult());
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        logger.error("Error from performer after receiving {}: {}", received.get(), throwable.toString());
                        done.set(true);
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Performer has finished after receiving {}", received.get());
                        done.set(true);
                    }
                };

                performer.stubBlockFuture().perfRun(perf.build(), responseObserver);
                logger.info("Waiting for run to finish and data to be written to db");
                dbWrite.join();


                // Only insert into the runs table if everything was successful
                var jsonVars = JsonObject.create();
                testSuite.variables().custom().forEach(v -> jsonVars.put(v.name(), v.value()));
                testSuite.variables().predefined().forEach(v -> jsonVars.put(v.name().name().toLowerCase(), v.value()));

                // Bump this whenever anything changes on the driver side that means we can't compare results against previous ones.
                // (Will also need to force a rerun of tests for this language, since jenkins-sdk won't know it's occurred).
                jsonVars.put("driverVersion", 1);
                // todo jsonVars.put("performerVersion", performer.response().getPerformerVersion());

                var clusterJson = produceClusterJson(clusterHostname, testSuite.connections().cluster());

                var json = JsonObject.create()
                        .put("cluster", clusterJson)
                        .put("impl", JsonObject.fromJson(jsonMapper.writeValueAsString(testSuite.impl())))
                        .put("workload", JsonObject.create()
                                .put("description", run.description()))
                        .put("vars", jsonVars);
//                        .put("variables", JsonObject.create()
//                                .put("this is a variable", "test var"));
                logger.info(json.toString());

                try (var st = conn.createStatement()) {
                    String statement = String.format("INSERT INTO runs VALUES ('%s', NOW(), '%s') ON CONFLICT (id) DO UPDATE SET datetime = NOW(), params = '%s'",
                            run.uuid(),
                            json.toString(),
                            json.toString());
                    st.executeUpdate(statement);
                }


                logger.info("Finished!");
            }

        }
    }

    private static JsonObject produceClusterJson(String hostname, TestSuite.Connections.Cluster cluster) {
        var out = JsonObject.create();
        out.put("type", cluster.type());

        try {
            var httpClient = new OkHttpClient().newBuilder().build();

            var adminUsername = cluster.username();
            var adminPassword = cluster.password();

            var resp1 = httpClient.newCall(new Request.Builder()
                            .header("Authorization", Credentials.basic(adminUsername, adminPassword))
                            .url("http://" + hostname + ":8091/pools/default")
                            .build())
                    .execute();

            var resp2 = httpClient.newCall(new Request.Builder()
                            .header("Authorization", Credentials.basic(adminUsername, adminPassword))
                            .url("http://" + hostname + ":8091/pools")
                            .build())
                    .execute();

            var raw1 = JsonObject.fromJson(resp1.body().bytes());
            var raw2 = JsonObject.fromJson(resp2.body().bytes());

            var node1 = ((JsonObject) raw1.getArray("nodes").get(0));

            out.put("nodeCount", raw1.getArray("nodes").size());
            out.put("memory", raw1.getInt("memoryQuota"));
            out.put("cpuCount", node1.getInt("cpuCount"));
            out.put("version", raw2.getString("implementationVersion"));
        } catch (IOException e) {
            logger.warn("Failed to get cluster info {}", e.getMessage());
        }

        return out;
    }

}
