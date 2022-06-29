package com.sdk.sdk.util;

import com.couchbase.client.core.deps.org.LatencyUtils.LatencyStats;
import com.couchbase.grpc.sdk.protocol.PerfSingleOperationResult;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * DbWriteThread dynamically writes performance data sent by the performer to the database
 */
public class DbWriteThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(DbWriteThread.class);
    private final ConcurrentLinkedQueue<PerfSingleOperationResult> toWrite = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean done;
    private final String uuid;
    private final java.sql.Connection conn;

    // We periodically write to the database throughout, to prevent OOM issues.
    // We only want completed one second buckets.  Data can be out of order, unsorted, etc.  So make sure we're never
    // writing the most X recent seconds of data.
    private static final int IGNORE_MOST_RECENT_SECS_OF_DATA = 3;

    public DbWriteThread(java.sql.Connection conn, String uuid, AtomicBoolean done) {
        this.conn = conn;
        this.uuid = uuid;
        this.done = done;
    }

    @Override
    public void run() {
        // This is maintained in time-sorted order
        var bucketisedResults = new TreeMap<Long, List<PerfSingleOperationResult>>();

        try {
            logger.info("Database write thread started");

            while (!done.get()) {
                var next = toWrite.poll();

                if (next == null) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    handleOneResult(next, bucketisedResults);
                    writeResultsIfPossible(bucketisedResults, IGNORE_MOST_RECENT_SECS_OF_DATA);
                }
            }
        } catch (Exception e){
            logger.error("Error writing data to database",e);
        }

        logger.info("Writing remaining {} results at end", toWrite.size());

        // Write what's left.  This may leave a mid-way bucket, but we don't worry about that since all data consumers
        // should be stripping the start and end of the data anyway.
        while (true) {
            var next = toWrite.poll();
            if (next == null) {
                break;
            }
            handleOneResult(next, bucketisedResults);
        }

        writeResultsIfPossible(bucketisedResults, 0);

        logger.info("Database write thread ended");
    }

    private static void handleOneResult(PerfSingleOperationResult next, TreeMap<Long, List<PerfSingleOperationResult>> bucketisedResults) {
        long bucket = TimeUnit.MICROSECONDS.toSeconds(grpcTimestampToMicros(next.getInitiated()));

        bucketisedResults.compute(bucket, (k, v) -> {
            if (v == null) {
                var ret = new ArrayList<PerfSingleOperationResult>();
                ret.add(next);
                return ret;
            } else {
                v.add(next);
                return v;
            }
        });
    }

    private void writeResultsIfPossible(TreeMap<Long, List<PerfSingleOperationResult>> bucketisedResults, int ignoreMostRecentSecs) {
        // See if we've got enough data to do a write
        var first = bucketisedResults.firstEntry().getKey();
        var last = bucketisedResults.lastEntry().getKey();

        while (last - first >= ignoreMostRecentSecs) {
            first = bucketisedResults.firstEntry().getKey();

            // Insert empty buckets for any missing seconds (e.g. where the performer for whatever reason could
            // not complete any operations)
            for (long i = first; i < last; i++) {
                bucketisedResults.compute(i, (k, v) -> {
                    if (v == null) {
                        return new ArrayList<>();
                    }
                    return v;
                });
            }

            var sorted = bucketisedResults.get(first)
                    .stream()
                    .sorted(Comparator.comparingLong(a -> grpcTimestampToMicros(a.getInitiated())))
                    .collect(Collectors.toList());
            var processed = processResults(sorted);
            write(processed);

            bucketisedResults.remove(first);
        }
    }

    private static long grpcTimestampToMicros(Timestamp ts) {
        return TimeUnit.NANOSECONDS.toMicros(TimeUnit.SECONDS.toNanos(ts.getSeconds()) + ts.getNanos());
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
        // No order for SortedMap here, we sort the results before returning
        var groupedBySeconds = result.stream()
                .collect(Collectors.groupingBy(v -> v.getInitiated().getSeconds()));

        var out = new ArrayList<PerfBucketResult>();

        groupedBySeconds.forEach((bySecond, results) -> {
            var stats = new LatencyStats();
            var success = 0;
            var failure = 0;
            var unstagingIncomplete = 0;

            for (PerfSingleOperationResult r : results) {
                long initiatedMicros = grpcTimestampToMicros(r.getInitiated());
                long finishedMicros = grpcTimestampToMicros(r.getFinished());
                if (finishedMicros >= initiatedMicros) {
                    stats.recordLatency(finishedMicros - initiatedMicros);
                }
                else {
                    logger.warn("Got bad values from performer {} {}", initiatedMicros, finishedMicros);
                }

                if (r.getSdkResult().getSuccess()) {
                    success += 1;
                } else {
                    failure += 1;
                }
            }

            var histogram = stats.getIntervalHistogram();
            out.add(new PerfBucketResult(bySecond,
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
                // logger.info("Writing bucket for {}", v.timestamp);

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

    public void addToQ(PerfSingleOperationResult res){
        toWrite.add(res);
    }
}
