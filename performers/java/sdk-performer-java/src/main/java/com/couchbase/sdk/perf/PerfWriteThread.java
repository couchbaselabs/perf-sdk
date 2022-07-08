package com.couchbase.sdk.perf;

import com.couchbase.grpc.sdk.protocol.PerfBatchedResult;
import com.couchbase.grpc.sdk.protocol.PerfRunConfig;
import com.couchbase.grpc.sdk.protocol.PerfSingleResult;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * PerfWriteThread gets given performance data and streams the options back to the driver one by one.
 * This was done because the response observer on the driver is not thread safe so couldn't handle multiple messages
 * at the same time.
 */
public class PerfWriteThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(PerfWriteThread.class);
    private final ServerCallStreamObserver<PerfSingleResult> responseObserver;
    private final ConcurrentLinkedQueue<PerfSingleResult> writeQueue = new ConcurrentLinkedQueue<>();
    private final PerfRunConfig perfRunConfig;
    private final GrpcPerformanceMeasureThread grpcPerformance = new GrpcPerformanceMeasureThread();

    public PerfWriteThread(StreamObserver<PerfSingleResult> responseObserver, PerfRunConfig perfRunConfig) {
        this.responseObserver = (ServerCallStreamObserver<PerfSingleResult>) responseObserver;
        this.perfRunConfig = perfRunConfig;
        this.grpcPerformance.start();
    }

    public void enqueue(PerfSingleResult result) {
        if (isInterrupted()) {
            grpcPerformance.ignored();
            return;
        }
        writeQueue.add(result);
        grpcPerformance.enqueued();
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                flush();
                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException ignored) {
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

        grpcPerformance.interrupt();
    }

    private void flush() {
        if (perfRunConfig.hasStreamingConfig()) {
            var sc = perfRunConfig.getStreamingConfig();
            if (sc.hasBatchSize()) {
                flushBatch(sc.getBatchSize());
            } else {
                flushIndividual(!sc.getFlowControl());
            }
        }
        else {
            flushIndividual(true);
        }
    }

    private void flushIndividual(boolean asFastAsPossible) {
        while (!writeQueue.isEmpty()) {

            if (asFastAsPossible || responseObserver.isReady()) {

                var next = writeQueue.poll();
                if (next != null) {
                    try {
                        responseObserver.onNext(next);
                    } catch (RuntimeException err) {
                        logger.warn("Failed to write {}: {}", next, err.toString());
                        // Important to tell the driver something has gone badly wrong otherwise it'll hang
                        responseObserver.onError(Status.ABORTED.withDescription(err.toString()).asException());
                    }

                    grpcPerformance.sentOne();
                } else {
                    logger.warn("Got null element from queue");
                }
            }
        }
    }

    private void flushBatch(int opsInBatch) {
        while (!writeQueue.isEmpty()) {
            if (responseObserver.isReady()) {
                var batch = new ArrayList<PerfSingleResult>(opsInBatch);

                for (int i = 0; i < opsInBatch; i++) {
                    var next = writeQueue.poll();
                    if (next != null) {
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
                    grpcPerformance.sentBatch(batch.size(), opsInBatch);
                } catch (RuntimeException err) {
                    logger.warn("Failed to write batch: {}", err.toString());
                    // Important to tell the driver something has gone badly wrong otherwise it'll hang
                    responseObserver.onError(Status.ABORTED.withDescription(err.toString()).asException());
                }
            }
        }
    }
}
