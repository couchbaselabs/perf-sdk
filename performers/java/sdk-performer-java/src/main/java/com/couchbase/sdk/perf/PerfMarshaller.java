package com.couchbase.sdk.perf;

import com.couchbase.grpc.sdk.protocol.PerfRunHorizontalScaling;
import com.couchbase.grpc.sdk.protocol.PerfRunRequest;
import com.couchbase.grpc.sdk.protocol.PerfSingleSdkOpResult;
import com.couchbase.grpc.sdk.protocol.SdkCommandResult;
import com.couchbase.sdk.SdkOperation;
import com.couchbase.sdk.utils.ClusterConnection;
import com.google.protobuf.Timestamp;
import com.sdk.logging.LogUtil;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PerfMarshaller {
    private static final Logger logger = LogUtil.getLogger(PerfMarshaller.class);

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
                    responseObserver));
        }

        for (PerfRunnerThread perfRunnerThread : runners) {
            perfRunnerThread.start();
        }
        logger.info("Started {} threads", runners.size());

        for (PerfRunnerThread runner : runners) {
            runner.join();
        }
        logger.info("All {} threads completed", runners.size());

    }

}

class PerfRunnerThread extends Thread {
    private final int runnerIndex;
    private final ClusterConnection connection;
    private final PerfRunRequest perfRun;
    private final PerfRunHorizontalScaling perThread;
    private final StreamObserver<PerfSingleSdkOpResult> responseObserver;

    PerfRunnerThread(int runnerIndex,
                     ClusterConnection connection,
                     PerfRunRequest perfRun,
                     PerfRunHorizontalScaling perThread,
                     StreamObserver<PerfSingleSdkOpResult> responseObserver) {
        this.runnerIndex = runnerIndex;
        this.connection = connection;
        this.perfRun = perfRun;
        this.perThread = perThread;
        this.responseObserver = responseObserver;
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
        Instant now = Instant.now();
        Instant until = now.plus(perfRun.getRunForSeconds(), ChronoUnit.SECONDS);
        boolean isDone = false;

        while (!isDone) {
            for (int i=0; i<perThread.getSdkCommandCount(); i++){
                PerfSingleSdkOpResult.Builder singleResult = PerfSingleSdkOpResult.newBuilder();
                singleResult.setInitiated(getTimeNow());
                SdkOperation operation = new SdkOperation();
                SdkCommandResult result = operation.run(connection, perThread.getSdkCommand(i));

                singleResult.setFinished(getTimeNow());
                singleResult.setResults(result.toBuilder());
                //FIXME FIT equivalent gets this data from the driver implement that then remove this
                singleResult.setVersionId("Java:3.1.4");

                responseObserver.onNext(singleResult.build());

                if (Instant.now().isAfter(until)) {
                    isDone = true;
                    break;
                }
            }
        }
    }
}