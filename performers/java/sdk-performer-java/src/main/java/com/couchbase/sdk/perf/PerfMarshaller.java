package com.couchbase.sdk.perf;

import com.couchbase.grpc.sdk.protocol.*;
import com.couchbase.sdk.SdkOperation;
import com.couchbase.sdk.utils.ClusterConnection;
import com.google.protobuf.Timestamp;
import com.couchbase.sdk.logging.LogUtil;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class PerfMarshaller {
    private static final Logger logger = LogUtil.getLogger(PerfMarshaller.class);
    // There is a counter for each operation as when a single one was used for all operations,
    // one thread would finish a part of a workload and set the counter to 0 before the other workload was done
    private static AtomicInteger removeCounter = new AtomicInteger();
    private static AtomicInteger replaceCounter = new AtomicInteger();
    private static AtomicInteger getCounter = new AtomicInteger();
    private static ConcurrentLinkedQueue<PerfSingleSdkOpResult> writeQueue = new ConcurrentLinkedQueue<>();
    private static AtomicBoolean done = new AtomicBoolean();

    public static void run(ClusterConnection connection,
                           PerfRunRequest perfRun,
                           StreamObserver<PerfSingleSdkOpResult> responseObserver) throws InterruptedException {
        PerfWriteThread writer = new PerfWriteThread(
                responseObserver,
                writeQueue,
                done);

        List<PerfRunnerThread> runners = new ArrayList<>();
        for (int runnerIndex = 0; runnerIndex < perfRun.getHorizontalScalingCount(); runnerIndex ++) {
            PerfRunHorizontalScaling perThread = perfRun.getHorizontalScaling(runnerIndex);
            runners.add(new PerfRunnerThread(runnerIndex,
                    connection,
                    perfRun,
                    perThread,
                    removeCounter,
                    replaceCounter,
                    getCounter,
                    done,
                    writeQueue));
        }

        logger.info("Starting writer thread");
        writer.start();

        for (PerfRunnerThread perfRunnerThread : runners) {
            perfRunnerThread.start();
        }
        logger.info("Started {} threads", runners.size());

        for (PerfRunnerThread runner : runners) {
            runner.join();
        }
        logger.info("All {} threads completed", runners.size());
        removeCounter.set(0);
        replaceCounter.set(0);
        getCounter.set(0);

        writer.join();
        logger.info("Writer thread completed");
    }

}

class PerfRunnerThread extends Thread {
    private final int runnerIndex;
    private final ClusterConnection connection;
    private final PerfRunRequest perfRun;
    private final PerfRunHorizontalScaling perThread;
    private AtomicInteger removeCounter;
    private AtomicInteger replaceCounter;
    private AtomicInteger getCounter;
    private AtomicBoolean done;
    private static ConcurrentLinkedQueue<PerfSingleSdkOpResult> writeQueue;

    PerfRunnerThread(int runnerIndex,
                     ClusterConnection connection,
                     PerfRunRequest perfRun,
                     PerfRunHorizontalScaling perThread,
                     AtomicInteger removeCounter,
                     AtomicInteger replaceCounter,
                     AtomicInteger getCounter,
                     AtomicBoolean done,
                     ConcurrentLinkedQueue<PerfSingleSdkOpResult> writeQueue) {
        this.runnerIndex = runnerIndex;
        this.connection = connection;
        this.perfRun = perfRun;
        this.perThread = perThread;
        this.removeCounter = removeCounter;
        this.replaceCounter = replaceCounter;
        this.getCounter = getCounter;
        this.done = done;
        this.writeQueue = writeQueue;
    }

    private static Timestamp.Builder getTimeNow() {
        long nowNanos = System.nanoTime();
        long nowSecs = TimeUnit.NANOSECONDS.toSeconds(nowNanos);
        long remainingNanos = nowNanos - TimeUnit.SECONDS.toNanos(nowSecs);
        return Timestamp.newBuilder().setSeconds(nowSecs)
                .setNanos((int) remainingNanos);
    }

    @Override
    public void run() {
        //Instant now = Instant.now();
        //Instant until = now.plus(perfRun.getRunForSeconds(), ChronoUnit.SECONDS);
        //boolean isDone = false;
        SdkOperation operation = new SdkOperation(removeCounter, replaceCounter, getCounter);

        for (SdkCreateRequest command : perThread.getSdkCommandList()){
            for (int i=0; i< command.getCount(); i++) {
                PerfSingleSdkOpResult.Builder singleResult = PerfSingleSdkOpResult.newBuilder();
                singleResult.setInitiated(getTimeNow());
                SdkCommandResult result = operation.run(connection, command);

                singleResult.setFinished(getTimeNow());
                singleResult.setResults(result.toBuilder());

                writeQueue.add(singleResult.build());
                //use if bounding tests by runtime
//            if (Instant.now().isAfter(until)) {
//                isDone = true;
//                break;
//            }
            }
        }
        //Let the writer thread know what there are no more values coming
        done.set(true);
    }
}