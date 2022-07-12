package com.couchbase.client.performer.core.perf;

import com.couchbase.client.performer.grpc.PerfRunRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * PerfMarshaller creates however many threads need to run in tandem based on the given horizontal scaling value
 */
public class PerfMarshaller {
    private static final Logger logger = LoggerFactory.getLogger(PerfMarshaller.class);

    public static void run(PerfRunRequest perfRun,
                           PerfWriteThread writer,
                           Function<PerPerfThread, Thread> createThread) throws InterruptedException {
        try{
            var counters = new Counters();

            var runners = new ArrayList<Thread>();
            for (int runnerIndex = 0; runnerIndex < perfRun.getHorizontalScalingCount(); runnerIndex ++) {
                var perThread = perfRun.getHorizontalScaling(runnerIndex);
                runners.add(createThread.apply(new PerPerfThread(runnerIndex,
                        perThread,
                        writer,
                        counters)));
            }

            logger.info("Starting writer thread");
            writer.start();

            for (Thread perfRunnerThread : runners) {
                perfRunnerThread.start();
            }
            logger.info("Started {} threads", runners.size());

            for (Thread runner : runners) {
                runner.join();
            }
            logger.info("All {} threads completed", runners.size());
            writer.interrupt();
            writer.join();
            logger.info("Writer thread completed");
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

}

