package com.couchbase.client.performer.core.perf;

import com.couchbase.client.performer.core.commands.SdkCommandExecutor;
import com.couchbase.client.performer.grpc.Bounds;
import com.couchbase.client.performer.grpc.GrpcWorkload;
import com.couchbase.client.performer.grpc.HorizontalScaling;
import com.couchbase.client.performer.grpc.PerfGrpcResult;
import com.couchbase.client.performer.grpc.PerfRunResult;
import com.couchbase.client.performer.grpc.SdkWorkload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class PerfRunnerThread extends Thread {
    private final Logger logger;
    private final SdkCommandExecutor sdkCommandExecutor;
    private final HorizontalScaling perThread;
    private final PerfWriteThread writeQueue;
    private final Counters counters;
    AtomicInteger operationsSuccessful = new AtomicInteger(0);
    AtomicInteger operationsFailed = new AtomicInteger(0);

    public PerfRunnerThread(PerPerfThread x, SdkCommandExecutor sdkCommandExecutor) {
        logger = LoggerFactory.getLogger("runner-" + x.runnerIndex());
        this.sdkCommandExecutor = sdkCommandExecutor;
        this.perThread = x.perThread();
        this.writeQueue = x.writer();
        this.counters = x.counters();
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
        AtomicInteger counter = getBounds(workload.getBounds());

        long executed = 0;
        while (counter.decrementAndGet() > 0) {
            var nextCommand = workload.getCommand((int) (executed % workload.getCommandCount()));
            ++ executed;
            var result = sdkCommandExecutor.run(nextCommand);
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
