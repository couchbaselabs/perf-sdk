package com.sdk.sdk.util;

import com.couchbase.client.core.deps.org.LatencyUtils.LatencyStats;
import com.couchbase.grpc.sdk.protocol.PerfSingleSdkOpResult;
import com.couchbase.grpc.sdk.protocol.SdkException;
import com.google.protobuf.Timestamp;
import com.sdk.SdkDriver;
import com.sdk.logging.LogUtil;
import org.slf4j.Logger;
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
    private static final Logger logger = LogUtil.getLogger(DbWriteThread.class);
    private static ConcurrentLinkedQueue<PerfSingleSdkOpResult> toWrite = new ConcurrentLinkedQueue<PerfSingleSdkOpResult>();
    private static List<PerfSingleSdkOpResult> nextBucket = new ArrayList<PerfSingleSdkOpResult>();
    private AtomicBoolean done;
    private String uuid;
    private AtomicReference<Tuple2<Timestamp, Long>> first;
    private java.sql.Connection conn;


    public DbWriteThread(java.sql.Connection conn, String uuid, AtomicBoolean done, AtomicReference<Tuple2<Timestamp, Long>> first){
        this.conn = conn;
        this.uuid = uuid;
        this.done = done;
        this.first = first;
    }

    @Override
    public void run() {
        try{
            int partition = 100;
            while(!(toWrite.isEmpty() && done.get())){
                List<PerfSingleSdkOpResult> results = new ArrayList<>();
                // Every 1 million items are written to the database throughout the run to prevent OOM issues
                // 1 million was chosen arbitrarily and can be changed if needed
                if(toWrite.size() >= partition){
                    logger.info("100 docs reached");
                    for (int i=0; i<partition; i++){
                        results.add(toWrite.remove());
                    }
                    sortResults(results);

                    var toBucket = splitIncompleteBucket();

                    var resultsToWrite = processResults(toBucket, first.get());
                    write(resultsToWrite);

                }else if(done.get()){
                    logger.info("writing data");

                    sortResults(toWrite);
                    toWrite.clear();
                    var resultsToWrite = processResults(nextBucket, first.get());
                    write(resultsToWrite);
                }
                else{
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (Exception e){
            logger.error("Error writing data to database",e);
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

    private List<PerfBucketResult> processResults(List<PerfSingleSdkOpResult> result, Tuple2<Timestamp, Long> firstTimes) {
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
                if (finished >= initiated) {
                    stats.recordLatency(finished - initiated);
                }

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

    private void write(List<PerfBucketResult> resultsToWrite){
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

    private void sortResults(Collection<PerfSingleSdkOpResult> results){
        // results may get sent back out of order due to multithreading
        var sortedResults = results.stream()
                .sorted(Comparator.comparingInt(a -> a.getInitiated().getNanos()))
                .collect(Collectors.toList());

        nextBucket.addAll(sortedResults);
    }

    private List<PerfSingleSdkOpResult> splitIncompleteBucket(){
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

            nextBucket = nextBucket.subList(counter, nextBucket.size());
            return completeData;
        }
    }

    public void addToQ(PerfSingleSdkOpResult res){
        toWrite.add(res);
    }
}
