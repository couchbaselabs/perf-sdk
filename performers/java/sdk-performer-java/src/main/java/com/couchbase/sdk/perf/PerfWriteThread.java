package com.couchbase.sdk.perf;

import com.couchbase.grpc.sdk.protocol.PerfSingleOperationResult;
import com.couchbase.grpc.sdk.protocol.PerfSingleResult;
import io.grpc.Status;
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
    private final ConcurrentLinkedQueue<PerfSingleResult> writeQueue = new ConcurrentLinkedQueue<>();

    public PerfWriteThread(StreamObserver<PerfSingleResult> responseObserver) {
        this.responseObserver = responseObserver;
    }

    public void enqueue(PerfSingleResult result) {
        if (isInterrupted()) {
            logger.info("Ignoring queue add {} as stopped", result);
            return;
        }
        writeQueue.add(result);
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
        int count = 0;
        while (!writeQueue.isEmpty()) {

            var next = writeQueue.poll();
            if (next != null) {
                count += 1;
                try {
                    responseObserver.onNext(next);
                }
                catch (RuntimeException err) {
                    logger.warn("Failed to write {}: {}", next, err.toString());
                    // Important to tell the driver something has gone badly wrong otherwise it'll hang
                    responseObserver.onError(Status.ABORTED.withDescription(err.toString()).asException());
                }
            }
            else {
                logger.warn("Got null element from queue");
            }
        }

        logger.info("Flushed {} results", count);
    }
}
