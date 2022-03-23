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
import com.sdk.sdk.util.Performer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
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


record TestSuite(String runtime, Implementation implementation, Variables variables, Connections connections, List<Run> runs) {
    Duration runtimeAsDuration() {
        var trimmed = runtime.trim();
        char suffix = trimmed.charAt(trimmed.length() - 1);
        var rawNum = Integer.parseInt(trimmed.substring(0, trimmed.length() - 1));
        return switch (suffix) {
            case 's' -> Duration.ofSeconds(rawNum);
            case 'm' -> Duration.ofMinutes(rawNum);
            case 'h' -> Duration.ofHours(rawNum);
            default -> throw new IllegalArgumentException("Could not handle runtime " + runtime);
        };
    }

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

    record Connections(Cluster cluster, List<PerformerConn> performers, Database database) {
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
                GET
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
    void applyTo(SdkCreateRequest.Builder builder);
}

record OpInsert(JsonObject content, int count) implements Op {
    @Override
    public void applyTo(SdkCreateRequest.Builder builder) {
        builder.setCommands(SdkCommand.newBuilder()
                .setInsert(CommandInsert.newBuilder()
                        .setContentJson(content.toString())
                        .setBucketInfo(BucketInfo.newBuilder()
                                .setBucketName(Defaults.DEFAULT_BUCKET)
                                .setScopeName(Defaults.DEFAULT_SCOPE)
                                .setCollectionName(Defaults.DEFAULT_COLLECTION)
                                .build())
                        .build()))
                .setCount(count);
    }
}

record OpGet(String docId, int count) implements Op {
    @Override
    public void applyTo(SdkCreateRequest.Builder builder) {
        builder.setCommands(SdkCommand.newBuilder()
                .setGet(CommandGet.newBuilder()
                        .setDocId(docId)
                        .setBucketInfo(BucketInfo.newBuilder()
                                .setBucketName(Defaults.DEFAULT_BUCKET)
                                .setScopeName(Defaults.DEFAULT_SCOPE)
                                .setCollectionName(Defaults.DEFAULT_COLLECTION)
                                .build())
                        .build()))
                .setCount(count);
    }
}

public class SdkDriver {

    private final static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final static ObjectMapper jsonMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    //TODO: add logger

    //private static final Logger logger = LogUtil.getLogger(PerfRunnerTest.class);
    //private static final Logger logger = LogUtil.getLogger(PerfRunnerTest.class);

    public static void main(String[] args)  throws SQLException, IOException, InterruptedException {
        if (args.length != 1) {
            //logger.info("Must provide config.yaml");
            System.exit(-1);
        }
        System.out.println("Beginning run");
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

    static void run(String testSuiteFile) throws IOException, SQLException, InterruptedException {
        var testSuite = readTestSuite(testSuiteFile);

        var dbUrl = String.format("jdbc:postgresql://%s:%d/%s",
                testSuite.connections().database().hostname(),
                testSuite.connections().database().port(),
                testSuite.connections().database().dbName());

        var props = new Properties();
        props.setProperty("user", testSuite.connections().database().username());
        props.setProperty("password", testSuite.connections().database().password());

        //logger.info("Connecting to database " + url);
        try (var conn = DriverManager.getConnection(dbUrl, props)) {

            // Make sure that the timescaledb database has been created
            System.out.println("Connection Begin");
            CreateConnectionRequest createConnection =
                    CreateConnectionRequest.newBuilder()
                            .setClusterHostname(testSuite.connections().cluster().hostname())
                            .setClusterUsername(testSuite.connections().cluster().username())
                            .setClusterPassword(testSuite.connections().cluster().password())
                            .build();

            //logger.info("Connecting to performer on {}:{}", perfConfig.connections().performer().hostname(), perfConfig.connections().performer().port());
            System.out.println("Connection Done");
            System.out.println("Performer Connect");

            List<Performer> performers = new ArrayList<Performer>();

            for (int i=0; i < testSuite.connections().performers().size(); i++){
                performers.add(new Performer(i,
                        testSuite.connections().performers().get(i).hostname(),
                        testSuite.connections().performers().get(i).port(),
                        createConnection));
            }

            for (TestSuite.Run run : testSuite.runs()) {
                //logger.info("Running workload " + workload);
                System.out.println("Running Workload");

                var jsonVars = JsonObject.create();
                testSuite.variables().custom().forEach(v -> jsonVars.put(v.name(), v.value()));
                testSuite.variables().predefined().forEach(v -> jsonVars.put(v.name().name().toLowerCase(), v.value()));

                var json = JsonObject.create()
                        .put("cluster", JsonObject.fromJson(jsonMapper.writeValueAsString(testSuite.connections().cluster())))
                        .put("impl", JsonObject.fromJson(jsonMapper.writeValueAsString(testSuite.implementation())))
                        .put("workload", JsonObject.create()
                                .put("description", "test description"))
                        .put("vars", jsonVars)
                        .put("variables", JsonObject.create()
                                .put("runtime", testSuite.runtime()));

                try (var st = conn.createStatement()) {
                    String statement = String.format("INSERT INTO runs VALUES ('%s', NOW(), '%s') ON CONFLICT (id) DO UPDATE SET datetime = NOW(), params = '%s'",
                            run.uuid(),
                            json.toString(),
                            json.toString());
                    st.executeUpdate(statement);
                }

                BuiltSdkCommand command = createSdkCommand(run);

                SdkCreateRequest.Builder sdkBuilt = SdkCreateRequest.newBuilder();

                command.sdkCommand().forEach(op -> {
                    op.applyTo(sdkBuilt);
                });

                PerfRunRequest.Builder perf = PerfRunRequest.newBuilder()
                        //TODO refactor the multiple performers bit
                        .setClusterConnectionId(performers.get(0).getClusterConnectionId())
                        .setRunForSeconds((int) testSuite.runtimeAsDuration().toSeconds());

                for (int i=0; i< testSuite.variables().horizontalScaling(); i++){
                    perf.addHorizontalScaling(com.couchbase.grpc.sdk.protocol.PerfRunHorizontalScaling.newBuilder()
                            .addSdkCommand(sdkBuilt));
                }

                long startedAll = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
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
                        //logger.error("Error from performer: ", throwable);
                        done.set(true);
                    }

                    @Override
                    public void onCompleted() {
                        //logger.info("Performer has finished");
                        done.set(true);
                    }
                };

                for (Performer performer: performers) {
                    performer.stubBlockFuture().perfRun(perf.build(), responseObserver);
                }

                // Tests are short enough that we can just buffer everything then write it currently
                while (!done.get()) {
                    Thread.sleep(100);
                }

                // Due to performer threading can't rely on results being streamed in perfect order
                var sortedResults = toWrite.stream()
                        .sorted(Comparator.comparingInt(a -> a.getInitiated().getNanos()))
                        .collect(Collectors.toList());

                long finishedAll = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());

                var resultsToWrite = processResults(sortedResults, first.get());

                resultsToWrite.forEach(v -> {
                    try (var st = conn.createStatement()) {

                        st.executeUpdate(String.format("INSERT INTO buckets VALUES (to_timestamp(%d), '%s', %d, %d, %d, %d, %d, %d, %d, %d, %d, %d)",
                                v.timestamp,
                                //FIXME refactor with FIT-esque version id creation
                                //sortedResults.get(0).getVersionId(),
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
