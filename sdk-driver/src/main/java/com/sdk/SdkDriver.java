package com.sdk;

import com.couchbase.client.core.deps.org.LatencyUtils.LatencyStats;
import com.couchbase.client.java.json.JsonObject;
import com.sdk.constants.Defaults;
import com.sdk.constants.Strings;
import com.couchbase.grpc.sdk.protocol.*;
import com.couchbase.grpc.sdk.protocol.CommandInsert;
import com.couchbase.grpc.sdk.protocol.CreateConnectionRequest;
import com.couchbase.grpc.sdk.protocol.DocId;
import com.sdk.sdk.util.Performer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

record Workload(String uuid, String description, List<Operation> operations, int horizontalScaling, Integer count){
    enum Operation {
        INSERT,
    }
}

record BuiltSdkCommand(List<Op> sdkCommand, String description) {
}

interface Op {
    void applyTo(SdkAttemptRequest.Builder builder);
}

record OpInsert(String docId, JsonObject content) implements Op {
    @Override
    public void applyTo(SdkAttemptRequest.Builder builder) {
        builder.addCommands(SdkCommand.newBuilder()
                .setInsert(CommandInsert.newBuilder()
                        .setDocId(DocId.newBuilder()
                                .setBucketName(Defaults.DEFAULT_BUCKET)
                                .setScopeName(Defaults.DEFAULT_SCOPE)
                                .setCollectionName(Defaults.DEFAULT_COLLECTION)
                                .setDocId(docId)
                                .build())
                        .setContentJson(content.toString())
                        .build()));
    }
}

public class SdkDriver {
    private final static ObjectMapper jsonMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    //TODO add logger

    //private static final Logger logger = LogUtil.getLogger(PerfRunnerTest.class);
    //private static final Logger logger = LogUtil.getLogger(PerfRunnerTest.class);

    public static void main(String[] args)  throws SQLException, IOException, InterruptedException {
        //Currently doesn't take a YAML file so this isn't needed
        //if (args.length != 1) {
            //logger.info("Must provide config.yaml");
            //System.exit(-1);
        //}
        System.out.println("Runnning run baby");
        run();
    }

    static Op addOp(Workload workload, Workload.Operation op, int repeatIdx) {
        String docId = "__doc_" + repeatIdx;
        JsonObject initial = JsonObject.create().put(Strings.CONTENT_NAME, Strings.INITIAL_CONTENT_VALUE);
        JsonObject updated = JsonObject.create().put(Strings.CONTENT_NAME, Strings.UPDATED_CONTENT_VALUE);

        switch (op) {
            case INSERT:
                return new OpInsert(docId, initial);
            default:
                throw new IllegalArgumentException("Unknown op " + op);
        }
    }

    static BuiltSdkCommand createSdkCommand(Workload workload) {
        //StringBuilder sb = new StringBuilder();
        var ops = new ArrayList<Op>();

        workload.operations().forEach(op -> {
            for (int i=0; i<workload.count(); i++) {
                    ops.add(addOp(workload, op, i));
                }
            }
        );
        return new BuiltSdkCommand(ops, "Test");
    }

    static void run() throws IOException, SQLException, InterruptedException {
        //TODO Add YAML integration

        // Currently all information about a run is stored in variables as such
        // Postgresql db connection information
        String dbHostname = "localhost";
        Integer dbPort = 5432;
        String database = "perf";
        String dbUsername = "postgres";
        String dbPassword = "password";

        //cb cluster connection information
        String cbHostname = "10.112.212.101";
        String cbUsername = "Administrator";
        String cbPassword = "password";

        //performer connection information
        String performerHostname = "localhost";
        Integer performerPort = 8060;

        //workload
        Workload work = new Workload("0","Just a few inserts", Arrays.asList(Workload.Operation.INSERT),1, 50);
        ArrayList<Workload> workList = new ArrayList<>();
        workList.add(work);

        //Performance
        int runtime = 10;

        //TODO Make Driver write results to time series db
        var dbUrl = String.format("jdbc:postgresql://%s:%d/%s",
                dbHostname,
                dbPort,
                database);

        var props = new Properties();
        props.setProperty("user", dbUsername);
        props.setProperty("password", dbPassword);

        //logger.info("Connecting to database " + url);
        System.out.println("Set all wacky variablies");
        try (var conn = DriverManager.getConnection(dbUrl, props)) {

            // Make sure that the timescaledb database is created
            System.out.println("Connection Begin");
            CreateConnectionRequest createConnection =
                    CreateConnectionRequest.newBuilder()
                            .setClusterHostname(cbHostname)
                            .setBucketName("default")
                            .setClusterUsername(cbUsername)
                            .setClusterPassword(cbPassword)
                            .setUseAsDefaultConnection(true)
                            .build();

            //logger.info("Connecting to performer on {}:{}", perfConfig.connections().performer().hostname(), perfConfig.connections().performer().port());
            System.out.println("Connection Done");
            System.out.println("Performer Connect");
            var performer = new Performer(0,
                    performerHostname,
                    performerPort,
                    createConnection);


            for (var workload : workList) {
                //logger.info("Running workload " + workload);
                System.out.println("Running Workload");
                BuiltSdkCommand command = createSdkCommand(workload);

                SdkAttemptRequest.Builder sdkBuilder = SdkAttemptRequest.newBuilder();

                command.sdkCommand().forEach(op -> {
                    op.applyTo(sdkBuilder);
                });

                SdkCreateRequest sdkBuilt = SdkCreateRequest.newBuilder()
                        .addAttempts(sdkBuilder.build())
                .build();

                PerfRunRequest.Builder perf = PerfRunRequest.newBuilder()
                        .setRunForSeconds(runtime);

                for (int i=0; i< workload.horizontalScaling(); i++){
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

                performer.stubBlockFuture().perfRun(perf.build(), responseObserver);

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

                System.out.println(resultsToWrite);
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
