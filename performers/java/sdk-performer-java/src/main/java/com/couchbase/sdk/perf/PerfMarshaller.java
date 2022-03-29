package com.couchbase.sdk.perf;

import com.couchbase.grpc.sdk.protocol.*;
import com.couchbase.sdk.SdkOperation;
import com.couchbase.sdk.utils.ClusterConnection;
import com.google.protobuf.Timestamp;
import com.sdk.logging.LogUtil;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class PerfMarshaller {
    private static final Logger logger = LogUtil.getLogger(PerfMarshaller.class);
    private static AtomicInteger docPool = new AtomicInteger();
    private static ReentrantLock onNextLock = new ReentrantLock();

    public static void run(ClusterConnection connection,
                           PerfRunRequest perfRun,
                           StreamObserver<PerfSingleSdkOpResult> responseObserver) throws InterruptedException {
        List<PerfRunnerThread> runners = new ArrayList<>();

        for (int runnerIndex = 0; runnerIndex < perfRun.getHorizontalScalingCount(); runnerIndex ++) {
            PerfRunHorizontalScaling perThread = perfRun.getHorizontalScaling(runnerIndex);
            runners.add(new PerfRunnerThread(runnerIndex,
                    connection,
                    perfRun,
                    perThread,
                    responseObserver,
                    docPool,
                    onNextLock));
        }

        for (PerfRunnerThread perfRunnerThread : runners) {
            perfRunnerThread.start();
        }
        logger.info("Started {} threads", runners.size());

        for (PerfRunnerThread runner : runners) {
            runner.join();
        }
        logger.info("All {} threads completed", runners.size());

        docPool.set(0);

    }

}

class PerfRunnerThread extends Thread {
    private final int runnerIndex;
    private final ClusterConnection connection;
    private final PerfRunRequest perfRun;
    private final PerfRunHorizontalScaling perThread;
    private final StreamObserver<PerfSingleSdkOpResult> responseObserver;
    private AtomicInteger docPool;
    private ReentrantLock onNextLock;
    private HashMap<String, Integer> count;

    PerfRunnerThread(int runnerIndex,
                     ClusterConnection connection,
                     PerfRunRequest perfRun,
                     PerfRunHorizontalScaling perThread,
                     StreamObserver<PerfSingleSdkOpResult> responseObserver,
                     AtomicInteger docPool,
                     ReentrantLock onNextLock) {
        this.runnerIndex = runnerIndex;
        this.connection = connection;
        this.perfRun = perfRun;
        this.perThread = perThread;
        this.responseObserver = responseObserver;
        this.docPool = docPool;
        this.onNextLock = onNextLock;
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
        SdkOperation operation = new SdkOperation(docPool);
        count = calculateCount();

        //FIXME really don't like the way that this is done
        // uses a map to find out if each count is still active.
        // Has to do a lot of calculation just to do one operation which isn't ideal
        while (Collections.max(count.values()) > 0) {
            for (SdkCreateRequest command : perThread.getSdkCommandList()){
                if (count.get(command.getName()) > 0) {
                    PerfSingleSdkOpResult.Builder singleResult = PerfSingleSdkOpResult.newBuilder();
                    singleResult.setInitiated(getTimeNow());
                    SdkCommandResult result = operation.run(connection, command);

                    singleResult.setFinished(getTimeNow());
                    singleResult.setResults(result.toBuilder());

                    onNextLock.lock();
                    try {
                        responseObserver.onNext(singleResult.build());
                    }
                    finally {
                        onNextLock.unlock();
                    }

                    count.replace(command.getName(), count.get(command.getName()) - 1);
                }
                //use if bounding tests by runtime
//                if (Instant.now().isAfter(until)) {
//                    isDone = true;
//                    break;
//                }
            }
        }
    }

    //calculateCount is run in each thread so that the hashmap does not get shared between them.
    // This just feels bad, I keep going further and further down a rabbit hole of things I don't like...
    private HashMap<String, Integer> calculateCount(){
        HashMap<String, Integer> count = new HashMap<>();
        for (SdkCreateRequest command : perThread.getSdkCommandList()){
            count.put(command.getName(),command.getCount());
        }
        return count;
    }
}