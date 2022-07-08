package com.sdk.sdk.util;

import com.couchbase.grpc.sdk.protocol.PerfSingleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Measures the performance of GRPC.
 */
public class GrpcPerformanceMeasureThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(GrpcPerformanceMeasureThread.class);
    private final AtomicInteger received = new AtomicInteger();
    private int receivedTotal = 0;
    private static final double CHECK_EVERY_X_SECONDS = 5.0;
    private static final int SAFETY_GUARD = 50;
    private int receivedZeroInRow = 0;

    @Override
    public void run() {
        logger.info("GRPC performance monitoring thread started");
        long start = System.nanoTime();

        try {
            while (!isInterrupted()) {
                try {
                    Thread.sleep((int) CHECK_EVERY_X_SECONDS * 1000);
                } catch (InterruptedException e) {
                    break;
                }

                double totalTimeSecs = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
                var receivedFrozen = received.get();
                receivedTotal += receivedFrozen;
                if (receivedFrozen == 0) {
                    receivedZeroInRow ++;
                }
                else {
                    receivedZeroInRow = 0;
                }

                logger.info("Driver received throughput over last {} seconds: {} ops/sec, {} ops total, overall throughput: {} ops/sec",
                         CHECK_EVERY_X_SECONDS,
                        (int) (received.get() / CHECK_EVERY_X_SECONDS),
                        receivedTotal,
                        (int) (receivedTotal / totalTimeSecs));

                // Technically we lose a few operations here
                received.set(0);

                if (receivedZeroInRow > SAFETY_GUARD) {
                    logger.error("Received nothing from the performer to often, something must be wrong, bailing");
                    System.exit(-1);
                }
            }
        } finally {
            logger.info("GRPC performance monitoring thread stopped");
        }
    }

    public void register(PerfSingleResult res) {
        received.incrementAndGet();
    }
}
