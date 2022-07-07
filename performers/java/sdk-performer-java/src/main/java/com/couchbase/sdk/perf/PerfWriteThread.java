package com.couchbase.sdk.perf;

import com.couchbase.grpc.sdk.protocol.PerfBatchedResult;
import com.couchbase.grpc.sdk.protocol.PerfRunConfig;
import com.couchbase.grpc.sdk.protocol.PerfSingleOperationResult;
import com.couchbase.grpc.sdk.protocol.PerfSingleResult;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PerfWriteThread gets given performance data and streams the options back to the driver one by one.
 * This was done because the response observer on the driver is not thread safe so couldn't handle multiple messages
 * at the same time.
 */
public class PerfWriteThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(PerfWriteThread.class);
    private final ServerCallStreamObserver<PerfSingleResult> responseObserver;
    private final ConcurrentLinkedQueue<PerfSingleResult> writeQueue = new ConcurrentLinkedQueue<>();
    private int totalFlushed = 0;
    private volatile int enqueued = 0;
    private final PerfRunConfig perfRunConfig;

    public PerfWriteThread(StreamObserver<PerfSingleResult> responseObserver, PerfRunConfig perfRunConfig) {
        this.responseObserver = (ServerCallStreamObserver<PerfSingleResult>) responseObserver;
        this.perfRunConfig = perfRunConfig;
    }

    public void enqueue(PerfSingleResult result) {
        if (isInterrupted()) {
            logger.info("Ignoring queue add {} as stopped", result.getResultCase());
            return;
        }
        writeQueue.add(result);
        if ((enqueued ++) % 10000 == 0) {
            logger.info("Enqueued so far: {}, currently queued to be written: {}", enqueued, writeQueue.size());
        }
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                flush();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException err) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error sending performance data to driver", e);
            // Important to tell the driver something has gone badly wrong otherwise it'll hang
            responseObserver.onError(Status.ABORTED.withDescription(e.toString()).asException());
        }

        logger.info("Writer thread has been stopped, performing final flush");

        flush();
    }

    private void flush() {
        if (perfRunConfig.hasBatchSize()) {
            flushBatch(perfRunConfig.getBatchSize());
        }
        else {
            flushIndividual(!perfRunConfig.getFlowControl());
        }
    }

    private void flushIndividual(boolean asFastAsPossible) {
        int count = 0;

        while (!writeQueue.isEmpty()) {

            if (asFastAsPossible || responseObserver.isReady()) {

                var next = writeQueue.poll();
                if (next != null) {
                    count += 1;
                    try {
                        responseObserver.onNext(next);
                    } catch (RuntimeException err) {
                        logger.warn("Failed to write {}: {}", next, err.toString());
                        // Important to tell the driver something has gone badly wrong otherwise it'll hang
                        responseObserver.onError(Status.ABORTED.withDescription(err.toString()).asException());
                    }

                    if (count % 1000 == 0) {
                        logger.info("Flushed {} results, {} total, response stream readiness = {}, remaining in queue {}", count, totalFlushed, responseObserver.isReady(), writeQueue.size());
                    }
                } else {
                    logger.warn("Got null element from queue");
                }
            }
        }
        totalFlushed += count;

        logger.info("Flushed {} results, {} total, response stream readiness = {}", count, totalFlushed, responseObserver.isReady());
    }

    private void flushBatch(int opsInBatch) {
        int count = 0;
        int batchesSent = 0;

        while (!writeQueue.isEmpty()) {
            if (responseObserver.isReady()) {
                var batch = new ArrayList<PerfSingleResult>(opsInBatch);

                for (int i = 0; i < opsInBatch; i++) {
                    var next = writeQueue.poll();
                    if (next != null) {
                        count += 1;
                        batch.add(next);
                    } else {
                        break;
                    }
                }

                try {
                    responseObserver.onNext(PerfSingleResult.newBuilder()
                            .setBatchedResult(PerfBatchedResult.newBuilder()
                                    .addAllResult(batch))
                            .build());
                    batchesSent++;
                } catch (RuntimeException err) {
                    logger.warn("Failed to write batch: {}", err.toString());
                    // Important to tell the driver something has gone badly wrong otherwise it'll hang
                    responseObserver.onError(Status.ABORTED.withDescription(err.toString()).asException());
                }

                if (count % 1000 == 0) {
                    logger.info("Flushed {} results in {} batches, {} total, response stream readiness = {}, remaining in queue {}",
                            count, batchesSent, totalFlushed, responseObserver.isReady(), writeQueue.size());
                }
            }
        }
        totalFlushed += count;

        logger.info("Flushed {} results in {} batches, {} total, response stream readiness = {}", count, batchesSent, totalFlushed, responseObserver.isReady());
    }
}
