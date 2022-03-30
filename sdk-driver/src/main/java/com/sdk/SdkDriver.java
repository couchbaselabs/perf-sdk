package com.sdk;

import com.couchbase.client.core.deps.org.LatencyUtils.LatencyStats;
import com.couchbase.client.java.json.JsonObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sdk.constants.Defaults;
import com.sdk.constants.Strings;
import com.couchbase.grpc.sdk.protocol.*;
import com.couchbase.grpc.sdk.protocol.CommandInsert;
import com.couchbase.grpc.sdk.protocol.CreateConnectionRequest;
import com.sdk.logging.LogUtil;
import com.sdk.sdk.util.DocCreateThread;
import com.sdk.sdk.util.Performer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


record TestSuite(Implementation implementation, Variables variables, Connections connections, List<Run> runs) {
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
        Integer horizontalScaling() {
            return (Integer) predefinedVar(PredefinedVariable.PredefinedVariableName.HORIZONTAL_SCALING);
        }

        String doc_id() {
            return (String) predefinedVar(PredefinedVariable.PredefinedVariableName.DOC_ID);
        }

        private Object predefinedVar(PredefinedVariable.PredefinedVariableName name) {
            return predefined.stream()
                    .filter(v -> v.name == name)
                    .findFirst()
                    .map(v -> v.value)
                    .orElseThrow(() -> new IllegalArgumentException("Predefined variable " + name + " not found"));
        }

        record PredefinedVariable(PredefinedVariableName name, Object value) {
            enum PredefinedVariableName {
                @JsonProperty("horizontal_scaling") HORIZONTAL_SCALING,
                @JsonProperty("doc_id") DOC_ID
            }
        }

        record CustomVariable(String name, Object value) {
        }
    }

    record Connections(Cluster cluster, PerformerConn performer, Database database) {
        record Cluster(String hostname, String username, String password) {
        }

        record PerformerConn(String hostname, int port) {
        }

        record Database(String hostname, int port, String username, String password, String dbName) {
        }
    }

    record Run(String uuid, String description, List<Operation> operations) {
        record Operation(Op op, int count, Object opConfig) {
            enum Op {
                INSERT,
                GET,
                REMOVE
            }

            int opConfigAsInt(){
                return (int) opConfig();
            }

            String opConfigAsString(){
                return (String) opConfig();
            }
        }
    }
}

record BuiltSdkCommand(List<Op> sdkCommand, String description) {
}

interface Op {
    void applyTo(PerfRunHorizontalScaling.Builder builder);
}

record OpInsert(JsonObject content, int count) implements Op {
    @Override
    public void applyTo(PerfRunHorizontalScaling.Builder builder) {
        builder.addSdkCommand(SdkCreateRequest.newBuilder()
                .setCommand(SdkCommand.newBuilder()
                        .setInsert(CommandInsert.newBuilder()
                                .setContentJson(content.toString())
                                .setBucketInfo(BucketInfo.newBuilder()
                                        .setBucketName(Defaults.DEFAULT_BUCKET)
                                        .setScopeName(Defaults.DEFAULT_SCOPE)
                                        .setCollectionName(Defaults.DEFAULT_COLLECTION)
                                        .build())
                                .build()))
                .setCount(count)
                .setName("INSERT"));
    }
}

record OpGet(String docId, int count) implements Op {
    @Override
    public void applyTo(PerfRunHorizontalScaling.Builder builder) {
        builder.addSdkCommand(SdkCreateRequest.newBuilder()
                .setCommand(SdkCommand.newBuilder()
                        .setGet(CommandGet.newBuilder()
                                .setDocId(docId)
                                .setBucketInfo(BucketInfo.newBuilder()
                                        .setBucketName(Defaults.DEFAULT_BUCKET)
                                        .setScopeName(Defaults.DEFAULT_SCOPE)
                                        .setCollectionName(Defaults.DEFAULT_COLLECTION)
                                        .build())
                                .build()))
                .setCount(count)
                .setName("GET"));
    }
}

record OpRemove(int count) implements Op {
    @Override
    public void applyTo(PerfRunHorizontalScaling.Builder builder){
        builder.addSdkCommand(SdkCreateRequest.newBuilder()
                .setCommand(SdkCommand.newBuilder()
                        .setRemove(CommandRemove.newBuilder()
                                .setBucketInfo(BucketInfo.newBuilder()
                                        .setBucketName(Defaults.DEFAULT_BUCKET)
                                        .setScopeName(Defaults.DEFAULT_SCOPE)
                                        .setCollectionName(Defaults.DOCPOOL_COLLECTION)
                                        .build())
                                .setKeyPreface(Defaults.KEY_PREFACE)
                                .build()))
                .setCount(count)
                .setName("REMOVE"));
    }
}

public class SdkDriver {

    private final static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final static ObjectMapper jsonMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger logger = LogUtil.getLogger(SdkDriver.class);

    public static void main(String[] args)  throws SQLException, IOException, InterruptedException, Exception {
        if (args.length != 1) {
            logger.info("Must provide config.yaml");
            System.exit(-1);
        }
        run(args[0]);
    }

    static Op addOp(TestSuite.Run.Operation op) {
        JsonObject initial = JsonObject.create().put(Strings.CONTENT_NAME, Strings.INITIAL_CONTENT_VALUE);
        JsonObject updated = JsonObject.create().put(Strings.CONTENT_NAME, Strings.UPDATED_CONTENT_VALUE);

        switch (op.op()) {
            case INSERT:
                return new OpInsert(initial, op.count());
            case GET:
                return new OpGet(op.opConfigAsString(), op.count());
            case REMOVE:
                return new OpRemove(op.count());
            default:
                throw new IllegalArgumentException("Unknown op " + op);
        }
    }

    static BuiltSdkCommand createSdkCommand(TestSuite.Run run) {
        //StringBuilder sb = new StringBuilder();
        var ops = new ArrayList<Op>();

        run.operations().forEach(op -> {
            ops.add(addOp(op));
        });
        return new BuiltSdkCommand(ops, "Test");
    }

    static TestSuite readTestSuite(String configFilename) {
        try {
            return yamlMapper.readValue(new File(configFilename), TestSuite.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    static int runRemoveCount(List<TestSuite.Run.Operation> operations){
        return operations.stream()
                .filter(op -> op.op().equals(TestSuite.Run.Operation.Op.REMOVE))
                .mapToInt(count -> count.count())
                .sum();
    }

    static void run(String testSuiteFile) throws IOException, SQLException, InterruptedException, Exception {
        var testSuite = readTestSuite(testSuiteFile);

        var dbUrl = String.format("jdbc:postgresql://%s:%d/%s",
                testSuite.connections().database().hostname(),
                testSuite.connections().database().port(),
                testSuite.connections().database().dbName());

        var props = new Properties();
        props.setProperty("user", testSuite.connections().database().username());
        props.setProperty("password", testSuite.connections().database().password());

        logger.info("Connecting to database " + dbUrl);
        try (var conn = DriverManager.getConnection(dbUrl, props)) {

            // Make sure that the timescaledb database has been created
            CreateConnectionRequest createConnection =
                    CreateConnectionRequest.newBuilder()
                            .setClusterHostname(testSuite.connections().cluster().hostname())
                            .setClusterUsername(testSuite.connections().cluster().username())
                            .setClusterPassword(testSuite.connections().cluster().password())
                            //bucketName is set here rather than in the performer so if it ever needs to be changed it can be done from a single place
                            .setBucketName(Defaults.DEFAULT_BUCKET)
                            .build();

            logger.info("Connecting to performer on {}:{}", testSuite.connections().performer().hostname(), testSuite.connections().performer().port());

            Performer performer = new Performer(
                    testSuite.connections().performer().hostname(),
                    testSuite.connections().performer().port(),
                    createConnection);

            for (TestSuite.Run run : testSuite.runs()) {
                logger.info("Running workload " + run);
                // docThread creates a pool of new documents for certain operations to use
                // it creates enough documents for every operation that needs documents in a run to complete 50 times per thread per second
                // If there are no REMOVE commands in the test suite then workloadMultiplier will be 0 so no docs will be created
                //TODO discuss whether this is the best way to do this
                DocCreateThread docThread = new DocCreateThread(
                        runRemoveCount(run.operations()),
                        testSuite.connections().cluster().hostname(),
                        testSuite.connections().cluster().username(),
                        testSuite.connections().cluster().password(),
                        Defaults.DEFAULT_BUCKET,
                        Defaults.DEFAULT_SCOPE);
                docThread.start();

                var jsonVars = JsonObject.create();
                testSuite.variables().custom().forEach(v -> jsonVars.put(v.name(), v.value()));
                testSuite.variables().predefined().forEach(v -> jsonVars.put(v.name().name().toLowerCase(), v.value()));

                var json = JsonObject.create()
                        .put("cluster", JsonObject.fromJson(jsonMapper.writeValueAsString(testSuite.connections().cluster())))
                        .put("impl", JsonObject.fromJson(jsonMapper.writeValueAsString(testSuite.implementation())))
                        .put("workload", JsonObject.create()
                                .put("description", "test description"))
                        .put("vars", jsonVars)
                        .put("variables", JsonObject.create());
                                //.put("runtime", testSuite.runtime()));

                try (var st = conn.createStatement()) {
                    String statement = String.format("INSERT INTO runs VALUES ('%s', NOW(), '%s') ON CONFLICT (id) DO UPDATE SET datetime = NOW(), params = '%s'",
                            run.uuid(),
                            json.toString(),
                            json.toString());
                    st.executeUpdate(statement);
                }

                BuiltSdkCommand command = createSdkCommand(run);

                PerfRunHorizontalScaling.Builder horizontalScalingBuilt = PerfRunHorizontalScaling.newBuilder();

                command.sdkCommand().forEach(op -> {
                    op.applyTo(horizontalScalingBuilt);
                });

                PerfRunRequest.Builder perf = PerfRunRequest.newBuilder()
                        .setClusterConnectionId(performer.getClusterConnectionId());
                        //.setRunForSeconds((int) testSuite.runtimeAsDuration().toSeconds());

                for (int i=0; i< testSuite.variables().horizontalScaling(); i++){
                    perf.addHorizontalScaling(horizontalScalingBuilt);
                }

                var done = new AtomicBoolean(false);
                var toWrite = new ConcurrentLinkedQueue<PerfSingleSdkOpResult>();
                var first = new AtomicReference<Tuple2<Timestamp, Long>>(null);

                var responseObserver = new StreamObserver<PerfSingleSdkOpResult>() {
                    @Override
                    public void onNext(PerfSingleSdkOpResult perfRunResult) {
                        if (first.get() == null) {
                            first.set(Tuples.of(perfRunResult.getInitiated(), System.currentTimeMillis()));
                        }
                        toWrite.add(perfRunResult);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        logger.error("Error from performer: ", throwable);
                        done.set(true);
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Performer has finished");
                        done.set(true);
                    }
                };

                // wait for all docs to be created
                logger.info("Waiting for docPool to be created");
                docThread.join();

                performer.stubBlockFuture().perfRun(perf.build(), responseObserver);

                // Tests are short enough that we can just buffer everything then write it currently
                while (!done.get()) {
                    Thread.sleep(100);
                }

                // Due to performer threading can't rely on results being streamed in perfect order
                var sortedResults = toWrite.stream()
                        .sorted(Comparator.comparingInt(a -> a.getInitiated().getNanos()))
                        .collect(Collectors.toList());

                var resultsToWrite = processResults(sortedResults, first.get());

                logger.info("Completed workload " + run);

                resultsToWrite.forEach(v -> {
                    try (var st = conn.createStatement()) {

                        st.executeUpdate(String.format("INSERT INTO buckets VALUES (to_timestamp(%d), '%s', %d, %d, %d, %d, %d, %d, %d, %d, %d, %d)",
                                v.timestamp,
                                run.uuid(),
                                v.sdkOpsTotal,
                                v.sdkOpsSuccess,
                                v.sdkOpsFailed,
                                v.sdkOpsIncomplete,
                                v.latencyMin,
                                v.latencyMax,
                                v.latencyAverage,
                                v.latencyP50,
                                v.latencyP95,
                                v.latencyP99
                        ));
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }

                });
            }

        }
    }

    private static long grpcTimestampToNanos(Timestamp ts) {
        return TimeUnit.SECONDS.toNanos(ts.getSeconds()) + ts.getNanos();
    }

    record PerfBucketResult(long timestamp,
                            int sdkOpsTotal,
                            int sdkOpsSuccess,
                            int sdkOpsFailed,
                            int sdkOpsIncomplete,
                            int latencyMin,
                            int latencyMax,
                            int latencyAverage,
                            int latencyP50,
                            int latencyP95,
                            int latencyP99) {
    }

    private static List<PerfBucketResult> processResults(List<PerfSingleSdkOpResult> result, Tuple2<Timestamp, Long> firstTimes) {
        var groupedBySeconds = result.stream()
                .collect(Collectors.groupingBy(v -> v.getInitiated().getSeconds()));

        var out = new ArrayList<PerfBucketResult>();

        groupedBySeconds.forEach((bySecond, results) -> {
            var stats = new LatencyStats();
            var success = 0;
            var failure = 0;
            var unstagingIncomplete = 0;

            for (PerfSingleSdkOpResult r : results) {
                long initiated = TimeUnit.NANOSECONDS.toMicros(grpcTimestampToNanos(r.getInitiated()));
                long finished = TimeUnit.NANOSECONDS.toMicros(grpcTimestampToNanos(r.getFinished()));
                assert(finished >= initiated);
                stats.recordLatency(finished - initiated);

                if (r.getResults().getException() == SdkException.NO_EXCEPTION_THROWN) {
                    success += 1;
                } else {
                    failure += 1;
                }
            }

            var histogram = stats.getIntervalHistogram();
            var timeSinceFirstSecs = bySecond - firstTimes.getT1().getSeconds();
            var timestampMs = TimeUnit.SECONDS.toMillis(timeSinceFirstSecs) + firstTimes.getT2();
            var timestampSec = TimeUnit.MILLISECONDS.toSeconds(timestampMs);
            out.add(new PerfBucketResult(timestampSec,
                    (int) histogram.getTotalCount(),
                    success,
                    failure,
                    unstagingIncomplete,
                    (int) histogram.getMinValue(),
                    (int) histogram.getMaxValue(),
                    (int) histogram.getMean(),
                    (int) histogram.getValueAtPercentile(0.5),
                    (int) histogram.getValueAtPercentile(0.95),
                    (int) histogram.getValueAtPercentile(0.99)));
        });

        return out.stream()
                .sorted(Comparator.comparingLong(a -> a.timestamp))
                .collect(Collectors.toList());

    }
}
