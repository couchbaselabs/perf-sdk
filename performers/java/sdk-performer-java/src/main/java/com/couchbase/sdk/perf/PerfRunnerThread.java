package com.couchbase.sdk.perf;

import com.couchbase.grpc.sdk.protocol.PerfGrpcResult;
import com.couchbase.grpc.sdk.protocol.PerfRunHorizontalScaling;
import com.couchbase.grpc.sdk.protocol.PerfSingleResult;
import com.couchbase.sdk.SdkOperationExecutor;
import com.couchbase.sdk.utils.ClusterConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class PerfRunnerThread extends Thread {
    private final Logger logger;
    private final ClusterConnection connection;
    private final PerfRunHorizontalScaling perThread;
    private final PerfWriteThread writeQueue;
    private final Counters counters;

    PerfRunnerThread(int runnerIndex,
                     ClusterConnection connection,
                     PerfRunHorizontalScaling perThread,
                     PerfWriteThread writer,
                     Counters counters) {
        logger = LoggerFactory.getLogger("runner-" + runnerIndex);
        this.connection = connection;
        this.perThread = perThread;
        this.writeQueue = writer;
        this.counters = counters;
    }

    @Override
    public void run() {
        var sdkExecutor = new SdkOperationExecutor();
        int operationsSuccessful = 0;
        int operationsFailed = 0;

        try {
            logger.info("Runner thread has started, will run {} workloads", perThread.getWorkloadsCount());

            for (var command : perThread.getWorkloadsList()) {
                if (command.hasSdk()) {
                    var sdkWorkload = command.getSdk();

                    AtomicInteger counter;
                    if (sdkWorkload.getCounter().hasGlobal()) {
                        counter = counters.getCounter(sdkWorkload.getCounter().getCounterId(), sdkWorkload.getCounter().getGlobal().getCount());
                        logger.info("Runner thread will SDK commands until counter {} is 0, currently {}",
                                sdkWorkload.getCounter().getCounterId(), counter.get());
                    } else {
                        throw new IllegalArgumentException("Unknown counter type");
                    }

                    while (counter.decrementAndGet() > 0) {
                        var result = sdkExecutor.run(connection, sdkWorkload);
                        writeQueue.enqueue(PerfSingleResult.newBuilder()
                                .setOperationResult(result)
                                .build());
                        if (result.getSdkResult().getSuccess()) {
                            operationsSuccessful += 1;
                        } else {
                            operationsFailed += 1;
                        }
                    }
                } else if (command.hasGrpc()) {
                    var grpcWorkload = command.getGrpc();

                    AtomicInteger counter;
                    if (grpcWorkload.getCounter().hasGlobal()) {
                        counter = counters.getCounter(grpcWorkload.getCounter().getCounterId(), grpcWorkload.getCounter().getGlobal().getCount());
                        logger.info("Runner thread will GRPC commands until counter {} is 0, currently {}",
                                grpcWorkload.getCounter().getCounterId(), counter.get());
                    } else {
                        throw new IllegalArgumentException("Unknown counter type");
                    }

                    while (counter.decrementAndGet() > 0) {
                        if (!grpcWorkload.getCommand().hasPing()) {
                            throw new IllegalArgumentException("Unknown GRPC command type");
                        }

                        writeQueue.enqueue(PerfSingleResult.newBuilder()
                                .setGrpcResult(PerfGrpcResult.getDefaultInstance())
                                .build());
                        operationsSuccessful += 1;
                    }
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
