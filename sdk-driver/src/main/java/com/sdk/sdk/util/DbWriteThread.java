package com.sdk.sdk.util;

import com.couchbase.client.core.deps.org.LatencyUtils.LatencyStats;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.grpc.sdk.protocol.PerfSingleOperationResult;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DbWriteThread dynamically writes performance data sent by the performer to the database
 */
public class DbWriteThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(DbWriteThread.class);
    private final ConcurrentLinkedQueue<PerfSingleOperationResult> toWrite = new ConcurrentLinkedQueue<>();
    // This is maintained in time-sorted order
    private final SortedMap<Long, List<PerfSingleOperationResult>> bucketisedResults = new TreeMap<>();
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
                    handleOneResult(next);
                    writeResultsIfPossible(IGNORE_MOST_RECENT_SECS_OF_DATA);
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
            handleOneResult(next);
        }

        writeResultsIfPossible(0);

        logger.info("Database write thread ended");
    }

    private void handleOneResult(PerfSingleOperationResult next) {
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

    private void writeResultsIfPossible(int ignoreMostRecentSecs) {
        // See if we've got enough data to do a write
        var first = bucketisedResults.firstKey();
        var last = bucketisedResults.lastKey();

        if (last - first > ignoreMostRecentSecs) {
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

            var wrote = new HashSet<Long>();

            bucketisedResults
                    .forEach((bySecond, results) -> {
                        // Remember we're ignoring the most recent few secs of data
                        if (bySecond < (last - ignoreMostRecentSecs)) {
                            var processed = processResults(bySecond, results);
                            write(processed);
                            wrote.add(bySecond);
                        }
                    });

            wrote.forEach(w -> bucketisedResults.remove(w));
        }
    }

    private static long grpcTimestampToMicros(Timestamp ts) {
        return TimeUnit.NANOSECONDS.toMicros(TimeUnit.SECONDS.toNanos(ts.getSeconds()) + ts.getNanos());
    }

    record PerfBucketResult(long timestamp,
                            int total,
                            int success,
                            int failed,
                            int durationMin,
                            int durationMax,
                            int durationAverage,
                            int durationP50,
                            int durationP95,
                            int durationP99,
                            Map<String, Long> errors) {
    }

    private PerfBucketResult processResults(long timestamp, List<PerfSingleOperationResult> results) {
        var stats = new LatencyStats();
        var success = 0;
        var failure = 0;

        var errors = new HashMap<String, Long>();

        for (PerfSingleOperationResult r : results) {
            long initiatedMicros = grpcTimestampToMicros(r.getInitiated());
            long finishedMicros = grpcTimestampToMicros(r.getFinished());
            if (finishedMicros >= initiatedMicros) {
                stats.recordLatency(finishedMicros - initiatedMicros);
            } else {
                logger.warn("Got bad values from performer {} {}", initiatedMicros, finishedMicros);
            }

            if (r.getSdkResult().getSuccess()) {
                success += 1;
            } else {
                failure += 1;

                if (r.getSdkResult().hasUnknownException()) {
                    var exception = r.getSdkResult().getUnknownException();
                    errors.compute(exception, (k, v) -> v == null ? 1 : v + 1);

                    if (exception.equals("IllegalArgumentException")) {
                        logger.error("Fast failing as performer has indicated a serious error");
                        System.exit(-1);
                    }
                }
            }
        }

        var histogram = stats.getIntervalHistogram();
        return new PerfBucketResult(timestamp,
                (int) histogram.getTotalCount(),
                success,
                failure,
                (int) histogram.getMinValue(),
                (int) histogram.getMaxValue(),
                (int) histogram.getMean(),
                (int) histogram.getValueAtPercentile(0.5),
                (int) histogram.getValueAtPercentile(0.95),
                (int) histogram.getValueAtPercentile(0.99),
                errors);
    }

    private void write(PerfBucketResult v) {
        logger.info("Writing bucket for {} success={} failed={} avg duration={}", v.timestamp, v.success, v.failed, v.durationAverage);

        try (var st = conn.createStatement()) {
            JsonObject errors = JsonObject.create();
            if (!v.errors.isEmpty()) {
                v.errors.forEach((errorName, errorCount) -> errors.put(errorName, errorCount));
            }

            st.executeUpdate(String.format("INSERT INTO buckets VALUES (to_timestamp(%d), '%s', %d, %d, %d, %d, %d, %d, %d, %d, %d, '%s')",
                    v.timestamp,
                    uuid,
                    v.total,
                    v.success,
                    v.failed,
                    v.durationMin,
                    v.durationMax,
                    v.durationAverage,
                    v.durationP50,
                    v.durationP95,
                    v.durationP99,
                    errors.size() == 0 ? null : errors.toString()
            ));
        } catch (SQLException throwables) {
            logger.error("Failed to write performance data to database", throwables);
            System.exit(-1);
        }
    }

    public void addToQ(PerfSingleOperationResult res){
        toWrite.add(res);
    }
}
