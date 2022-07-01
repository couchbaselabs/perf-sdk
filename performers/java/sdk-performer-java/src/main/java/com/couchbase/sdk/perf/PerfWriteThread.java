package com.couchbase.sdk.perf;

import com.couchbase.grpc.sdk.protocol.PerfSingleOperationResult;
import com.couchbase.grpc.sdk.protocol.PerfSingleResult;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PerfWriteThread gets given performance data and streams the options back to the driver one by one.
 * This was done because the response observer on the driver is not thread safe so couldn't handle multiple messages
 * at the same time.
 */
public class PerfWriteThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(PerfWriteThread.class);
    private final StreamObserver<PerfSingleResult> responseObserver;
    private final ConcurrentLinkedQueue<PerfSingleOperationResult> writeQueue;
    private final AtomicBoolean done;

    public PerfWriteThread(
            StreamObserver<PerfSingleResult> responseObserver,
            ConcurrentLinkedQueue<PerfSingleOperationResult> writeQueue,
            AtomicBoolean done){
        this.responseObserver = responseObserver;
        this.writeQueue = writeQueue;
        this.done = done;
    }

    public void enqueue(PerfSingleOperationResult result) {
        writeQueue.add(result);
    }

    @Override
    public void run() {
        try {
            while (!done.get()) {
                flush();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException err) {
                    logger.error("Writer thread interrupted whilst waiting for results", err);
                    responseObserver.onError(err);
                    throw new RuntimeException(err);
                }
            }
        } catch (Exception e) {
            logger.error("Error sending performance data to driver", e);
            // Important to tell the driver something has gone badly wrong otherwise it'll hang
            responseObserver.onError(e);
        }

        flush();
    }

    private void flush() {
        int count = 0;
        while (!writeQueue.isEmpty()) {

            var next = writeQueue.poll();
            if (next != null) {
                count += 1;
                responseObserver.onNext(PerfSingleResult.newBuilder()
                        .setOperationResult(next)
                        .build());
            }
            else {
                logger.warn("Got null element from queue");
            }
        }

        logger.info("Flushed {} results", count);
    }
}
