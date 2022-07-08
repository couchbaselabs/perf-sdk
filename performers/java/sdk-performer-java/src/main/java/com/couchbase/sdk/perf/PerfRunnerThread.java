package com.couchbase.sdk.perf;

import com.couchbase.grpc.sdk.protocol.Bounds;
import com.couchbase.grpc.sdk.protocol.GrpcWorkload;
import com.couchbase.grpc.sdk.protocol.PerfGrpcResult;
import com.couchbase.grpc.sdk.protocol.HorizontalScaling;
import com.couchbase.grpc.sdk.protocol.PerfRunResult;
import com.couchbase.grpc.sdk.protocol.SdkWorkload;
import com.couchbase.grpc.sdk.protocol.Workload;
import com.couchbase.sdk.SdkOperationExecutor;
import com.couchbase.sdk.utils.ClusterConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class PerfRunnerThread extends Thread {
    private final Logger logger;
    private final ClusterConnection connection;
    private final HorizontalScaling perThread;
    private final PerfWriteThread writeQueue;
    private final Counters counters;
    AtomicInteger operationsSuccessful = new AtomicInteger(0);
    AtomicInteger operationsFailed = new AtomicInteger(0);

    PerfRunnerThread(int runnerIndex,
                     ClusterConnection connection,
                     HorizontalScaling perThread,
                     PerfWriteThread writer,
                     Counters counters) {
        logger = LoggerFactory.getLogger("runner-" + runnerIndex);
        this.connection = connection;
        this.perThread = perThread;
        this.writeQueue = writer;
        this.counters = counters;
    }

    private AtomicInteger getBounds(Bounds bounds) {
        if (!bounds.hasCounter()) {
            throw new UnsupportedOperationException("Unknown bounds type");
        }

        if (bounds.getCounter().hasGlobal()) {
            var counter = counters.getCounter(bounds.getCounter().getCounterId(), bounds.getCounter().getGlobal().getCount());
            logger.info("Runner thread will commands until counter {} is 0, currently {}",
                    bounds.getCounter().getCounterId(), counter.get());
            return counter;
        } else {
            throw new UnsupportedOperationException("Unknown counter type");
        }
    }

    private void executeSdkWorkload(SdkWorkload workload) {
        var sdkExecutor = new SdkOperationExecutor();

        AtomicInteger counter = getBounds(workload.getBounds());

        long executed = 0;
        while (counter.decrementAndGet() > 0) {
            var nextCommand = workload.getCommand((int) (executed % workload.getCommandCount()));
            ++ executed;
            var result = sdkExecutor.run(connection, nextCommand);
            writeQueue.enqueue(PerfRunResult.newBuilder()
                    .setOperationResult(result)
                    .build());
            if (result.getSdkResult().getSuccess()) {
                operationsSuccessful.incrementAndGet();
            } else {
                operationsFailed.incrementAndGet();
            }
        }
    }

    private void executeGrpcWorkload(GrpcWorkload workload) {
        AtomicInteger counter = getBounds(workload.getBounds());

        while (counter.decrementAndGet() > 0) {
            if (!workload.getCommand().hasPing()) {
                throw new UnsupportedOperationException("Unknown GRPC command type");
            }

            writeQueue.enqueue(PerfRunResult.newBuilder()
                    .setGrpcResult(PerfGrpcResult.getDefaultInstance())
                    .build());
            operationsSuccessful.incrementAndGet();
        }
    }

    @Override
    public void run() {

        try {
            logger.info("Runner thread has started, will run {} workloads", perThread.getWorkloadsCount());

            for (var workload : perThread.getWorkloadsList()) {
                if (workload.hasSdk()) {
                    executeSdkWorkload(workload.getSdk());
                } else if (workload.hasGrpc()) {
                    executeGrpcWorkload(workload.getGrpc());
                }
            }
        }
        catch (Throwable err) {
            logger.error("Runner thread died with {}", err.toString());
            System.exit(-1);
        }

        logger.info("Finished after {} successful operations and {} failed",
                operationsSuccessful, operationsFailed);
    }
}
