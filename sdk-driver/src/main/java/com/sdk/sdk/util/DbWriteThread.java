package com.sdk.sdk.util;

import com.couchbase.client.core.deps.org.LatencyUtils.LatencyStats;
import com.couchbase.grpc.sdk.protocol.PerfSingleOperationResult;
import com.couchbase.grpc.sdk.protocol.PerfSingleResult;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.util.function.Tuple2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * DbWriteThread dynamically writes performance data sent by the performer to the database
 */
public class DbWriteThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(DbWriteThread.class);
    private final ConcurrentLinkedQueue<PerfSingleOperationResult> toWrite = new ConcurrentLinkedQueue<>();
    // todo make better
//    private final List<PerfSingleOperationResult> nextBucket = new ArrayList<>();
    private final AtomicBoolean done;
    private final String uuid;
    private final java.sql.Connection conn;


    public DbWriteThread(java.sql.Connection conn, String uuid, AtomicBoolean done) {
        this.conn = conn;
        this.uuid = uuid;
        this.done = done;
    }

    @Override
    public void run() {
        try {
            // Every 1 million items are written to the database throughout the run to prevent OOM issues
            // 1 million was chosen arbitrarily and can be changed if needed
            int partition = 1_000_000;

            while (!done.get()) {
                List<PerfSingleOperationResult> results = new ArrayList<>();

                if(toWrite.size() >= partition) {
                    for (int i = 0; i < partition; i++) {
                        results.add(toWrite.remove());
                    }
                    // results may get sent back out of order due to multithreading
                    var sorted = results.stream()
                            .sorted(Comparator.comparingInt(a -> a.getInitiated().getNanos()))
                            .collect(Collectors.toList());

                    var toBucket = splitIncompleteBucket(sorted);

                    var resultsToWrite = processResults(toBucket);
                    write(resultsToWrite);
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (Exception e){
            logger.error("Error writing data to database",e);
        }

        // Write what's left.  This may leave a mid-way bucket, but we don't worry about that since all data consumers
        // should be stripping the start and end of the data anyway.
        var sorted = toWrite.stream()
                .sorted(Comparator.comparingInt(a -> a.getInitiated().getNanos()))
                .collect(Collectors.toList());

        var resultsToWrite = processResults(sorted);
        write(resultsToWrite);

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

    private List<PerfBucketResult> processResults(List<PerfSingleOperationResult> result) {
        var groupedBySeconds = result.stream()
                .collect(Collectors.groupingBy(v -> v.getInitiated().getSeconds()));

        var out = new ArrayList<PerfBucketResult>();

        groupedBySeconds.forEach((bySecond, results) -> {
            var stats = new LatencyStats();
            var success = 0;
            var failure = 0;
            var unstagingIncomplete = 0;

            for (PerfSingleOperationResult r : results) {
                long initiated = TimeUnit.NANOSECONDS.toMicros(grpcTimestampToNanos(r.getInitiated()));
                long finished = TimeUnit.NANOSECONDS.toMicros(grpcTimestampToNanos(r.getFinished()));
                if (finished >= initiated) {
                    stats.recordLatency(finished - initiated);
                }

                if (r.getSdkResult().getSuccess()) {
                    success += 1;
                } else {
                    failure += 1;
                }
            }

            var histogram = stats.getIntervalHistogram();
            out.add(new PerfBucketResult(result.get(0).getInitiated().getSeconds(),
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

    private void write(List<PerfBucketResult> resultsToWrite) {
        if (resultsToWrite.isEmpty()) {
            logger.info("No results to write to database");
        }
        else {
            logger.info("Writing bucket for {}", resultsToWrite.get(0).timestamp);

            resultsToWrite.forEach(v -> {
                try (var st = conn.createStatement()) {

                    st.executeUpdate(String.format("INSERT INTO buckets VALUES (to_timestamp(%d), '%s', %d, %d, %d, %d, %d, %d, %d, %d, %d, %d)",
                            v.timestamp,
                            uuid,
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
                    logger.error("Failed to write performance data to database", throwables);
                    System.exit(-1);
                }
            });
        }
    }

    /**
     * Only want to look at full second buckets of data.  They'll usually be some data leftover in the next bucket.
     */
    private List<PerfSingleOperationResult> splitIncompleteBucket(List<PerfSingleOperationResult> nextBucket) {
        long currentSecond = nextBucket.get(nextBucket.size()-1).getInitiated().getSeconds();
        long firstSecond = nextBucket.get(0).getInitiated().getSeconds();
        if (firstSecond == currentSecond){
            return nextBucket;
        } else {
            long wantedSecond = currentSecond -1;
            int counter = nextBucket.size();
            // Making sure we don't split the bucket
            while(currentSecond != wantedSecond){
                counter -= 1;
                currentSecond = nextBucket.get(counter).getInitiated().getSeconds();
            }
            counter += 1;

            var completeData = nextBucket.subList(0, counter);

            // Put what's left back in the queue
            nextBucket.subList(counter, nextBucket.size())
                    .forEach(leftover -> toWrite.add(leftover));

            return completeData;
        }
    }

    public void addToQ(PerfSingleOperationResult res){
        toWrite.add(res);
    }
}
