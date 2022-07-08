package com.couchbase.sdk.metrics;

import com.couchbase.client.java.json.JsonObject;
import com.couchbase.grpc.sdk.protocol.PerfMetricsResult;
import com.couchbase.grpc.sdk.protocol.PerfRunResult;
import com.couchbase.sdk.SdkOperationExecutor;
import com.couchbase.sdk.perf.PerfWriteThread;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;

/**
 * Periodically sends metrics back to the driver
 */
public class MetricsReporter extends Thread {
    private final PerfWriteThread writer;
    private final Logger logger = LoggerFactory.getLogger(MetricsReporter.class);
    private static final double CONVERT = 1073741824;

    public MetricsReporter(PerfWriteThread writer) {
        this.writer = writer;
    }

    @Override
    public void run() {
        logger.info("Metrics thread started");
        boolean done = false;

        while (!isInterrupted() && !done) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                done = true;
            }

            if (!done) {
                var metrics = JsonObject.create();

                try {
                    var memory = ManagementFactory.getMemoryMXBean();
                    // metrics.put("memHeapInitialGB", (float) memory.getHeapMemoryUsage().getInit() / CONVERT);
                    metrics.put("memHeapUsedGB", (float) memory.getHeapMemoryUsage().getUsed() / CONVERT);
                    metrics.put("memHeapMaxGB", (float) memory.getHeapMemoryUsage().getMax() / CONVERT);
                    // metrics.put("memHeapCommittedGB", (float) memory.getHeapMemoryUsage().getCommitted() / CONVERT);
                    // metrics.put("memNonHeapInitialGB", (float) memory.getNonHeapMemoryUsage().getInit() / CONVERT);
                    //metrics.put("memNonHeapUsedGB", (float) memory.getNonHeapMemoryUsage().getUsed() / CONVERT);
                    //metrics.put("memNonHeapMaxGB", (float) memory.getNonHeapMemoryUsage().getMax() / CONVERT);
                    // metrics.put("memNonHeapCommittedGB", (float) memory.getNonHeapMemoryUsage().getCommitted() / CONVERT);
                } catch (Throwable err) {
                    logger.warn("Memory metrics failed: {}", err.toString());
                }

                try {
                    var threadMXBean = ManagementFactory.getThreadMXBean();

                    metrics.put("threadCount", threadMXBean.getAllThreadIds().length);

//                    for (Long threadID : threadMXBean.getAllThreadIds()) {
//                        ThreadInfo info = threadMXBean.getThreadInfo(threadID);
//                        metrics.put("threadCpuNanos_" + info.getThreadName(), threadMXBean.getThreadCpuTime(threadID));
//                    }
                } catch (Throwable err) {
                    logger.warn("Metrics failed: {}", err.toString());
                }

//                try {
//                    var bean = ManagementFactory.getOperatingSystemMXBean();
//                    metrics.put("cpuSystemLoadAverageLastMinute", bean.getSystemLoadAverage());
//                } catch (Throwable err) {
//                    logger.warn("Metrics failed: {}", err.toString());
//                }

                try {
                    var beans = ManagementFactory.getGarbageCollectorMXBeans();
                    // It seems that though multiple GC can be reported, only the first appears to used
                    for (int i = 0; i < 1; i++) {
                        var bean = beans.get(i);

                        metrics.put("gc" + i + "AccTimeMs", bean.getCollectionTime());
                        metrics.put("gc" + i + "Count", bean.getCollectionCount());
                    }
                } catch (Throwable err) {
                    logger.warn("Metrics failed: {}", err.toString());
                }

//                try {
//                    var bean = ManagementFactory.getCompilationMXBean();
//                    metrics.put("compilationTimeMillis", bean.getTotalCompilationTime());
//                } catch (Throwable err) {
//                    logger.warn("Metrics failed: {}", err.toString());
//                }

                try {
                    // todo universal metric names
                    var bean = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
                    metrics.put("processCpu", bean.getProcessCpuLoad() * 100.0);
                    metrics.put("systemCpu", bean.getSystemCpuLoad() * 100.0);
                    // metrics.put("freeMemorySizeGB", bean.getFreeMemorySize() / CONVERT);
                    // metrics.put("freePhysicalMemorySizeGB", bean.getFreePhysicalMemorySize() / CONVERT);
                    // metrics.put("totalPhysicalMemorySizeGB", bean.getTotalPhysicalMemorySize() / CONVERT);
                    // metrics.put("committedVirtualMemorySizeGB", bean.getCommittedVirtualMemorySize() / CONVERT);
                    metrics.put("freeSwapSizeGB", bean.getFreeSwapSpaceSize() / CONVERT);
                    // metrics.put("processCpuTimeNanos", bean.getProcessCpuTime());
                    // metrics.put("totalMemorySizeGB", bean.getTotalMemorySize() / CONVERT);
                    // metrics.put("totalFreeSwapSpaceSizeGB", bean.getFreeSwapSpaceSize() / CONVERT);
                } catch (Throwable err) {
                    logger.warn("Metrics failed: {}", err.toString());
                }

//                try {
//                    var beans = ManagementFactory.getMemoryPoolMXBeans();
//                    for (java.lang.management.MemoryPoolMXBean bean : beans) {
//                        var name = "memoryManager" + bean.getName();
//
//                        metrics.put(name + "Committed", bean.getUsage().getCommitted());
//                        metrics.put(name + "Used", bean.getUsage().getUsed());
//                        metrics.put(name + "Max", bean.getUsage().getMax());
//                        metrics.put(name + "Init", bean.getUsage().getInit());
//                    }
//                } catch (Throwable err) {
//                    logger.warn("Metrics failed: {}", err.toString());
//                }

                try {
                    var pools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
                    for (BufferPoolMXBean pool : pools) {
                        if (pool.getName().equals("direct")) {
                            metrics.put("memDirectUsedGB", pool.getMemoryUsed() / CONVERT);
                            metrics.put("memDirectMaxGB", pool.getTotalCapacity() / CONVERT);
                        }
                    }
                } catch (Throwable err) {
                    logger.warn("Metrics failed: {}", err.toString());
                }

                writer.enqueue(PerfRunResult.newBuilder()
                        .setMetricsResult(PerfMetricsResult.newBuilder()
                                .setInitiated(SdkOperationExecutor.getTimeNow())
                                .setMetrics(metrics.toString()))
                        .build());
            }
        }

        logger.info("Metrics thread finished");
    }
}
