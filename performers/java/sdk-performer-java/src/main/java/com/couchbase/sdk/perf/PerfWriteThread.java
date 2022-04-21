package com.couchbase.sdk.perf;

import com.couchbase.grpc.sdk.protocol.PerfSingleSdkOpResult;
import com.couchbase.sdk.logging.LogUtil;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PerfWriteThread extends Thread {
    private final StreamObserver<PerfSingleSdkOpResult> responseObserver;
    private static ConcurrentLinkedQueue<PerfSingleSdkOpResult> writeQueue;
    private static AtomicBoolean done;
    private Logger logger = LogUtil.getLogger(PerfWriteThread.class);

    public PerfWriteThread(
            StreamObserver<PerfSingleSdkOpResult> responseObserver,
            ConcurrentLinkedQueue<PerfSingleSdkOpResult> writeQueue,
            AtomicBoolean done){
        this.responseObserver = responseObserver;
        this.writeQueue = writeQueue;
        this.done = done;
    }

    @Override
    public void run() {
        while(!(writeQueue.isEmpty() && done.get())){
            if (writeQueue.isEmpty()){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException err) {
                    logger.error("Writer thread interrupted whilst waiting for results", err);
                    responseObserver.onError(err);
                }
            }else{
                responseObserver.onNext(writeQueue.remove());
            }
        }
    }
}
