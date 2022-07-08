package com.sdk.sdk.util;

import com.couchbase.grpc.sdk.protocol.PerfRunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static com.sdk.sdk.util.DbWriteThread.grpcTimestampToMicros;

 public class MetricsWriteThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(MetricsWriteThread.class);
    private final ConcurrentLinkedQueue<PerfRunResult> toWrite = new ConcurrentLinkedQueue<>();
    private final java.sql.Connection conn;
    private final String runUuid;

    public MetricsWriteThread(java.sql.Connection conn, String runUuid) {
        this.conn = conn;
        this.runUuid = runUuid;
    }

    @Override
    public void run() {
        try {
            logger.info("Database metrics write thread started");
            var start = System.nanoTime();

            while (!isInterrupted()) {
                var next = toWrite.poll();

                if (next == null) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                else {
                    var metrics = next.getMetricsResult();

                    try (var st = conn.createStatement()) {
                        var timeSinceStartSecs = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);

                        var statement = String.format("INSERT INTO metrics VALUES (to_timestamp(%d), '%s', '%s', %d)",
                                TimeUnit.MICROSECONDS.toSeconds(grpcTimestampToMicros(metrics.getInitiated())),
                                runUuid,
                                metrics.getMetrics(),
                                timeSinceStartSecs);
                        st.executeUpdate(statement);

                        // Log metrics to stdout as it helps diagnosis random issues like performer OOM-ing
                        logger.info("Writing metrics {}", metrics.getMetrics());
                    } catch (SQLException err) {
                        logger.error("Failed to write metrics data to database", err);
                        // Ignore the error, metrics aren't vital
                    }
                }
            }
        } catch (Exception e){
            logger.error("Error writing data to database",e);
        }
    }

    public void enqueue(PerfRunResult res){
        toWrite.add(res);
    }
}
