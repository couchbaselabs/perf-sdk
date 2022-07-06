package com.couchbase.sdk.metrics;

import com.couchbase.client.java.json.JsonObject;
import com.couchbase.grpc.sdk.protocol.PerfMetricsResult;
import com.couchbase.grpc.sdk.protocol.PerfSingleResult;
import com.couchbase.sdk.SdkOperationExecutor;
import com.couchbase.sdk.perf.PerfWriteThread;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;

/**
 * Periodically sends metrics back to the driver
 */
public class MetricsReporter extends Thread {
    private final PerfWriteThread writer;
    private final Logger logger = LoggerFactory.getLogger(MetricsReporter.class);
    private static final int CONVERT = 1073741824;

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
                    // metrics.put("memoryHeapInitialGB", (float) memory.getHeapMemoryUsage().getInit() / CONVERT);
                    metrics.put("memoryHeapUsedGB", (float) memory.getHeapMemoryUsage().getUsed() / CONVERT);
                    metrics.put("memoryHeapMaxGB", (float) memory.getHeapMemoryUsage().getMax() / CONVERT);
                    // metrics.put("memoryHeapCommittedGB", (float) memory.getHeapMemoryUsage().getCommitted() / CONVERT);
                    // metrics.put("memoryNonHeapInitialGB", (float) memory.getNonHeapMemoryUsage().getInit() / CONVERT);
                    metrics.put("memoryNonHeapUsedGB", (float) memory.getNonHeapMemoryUsage().getUsed() / CONVERT);
                    metrics.put("memoryNonHeapMaxGB", (float) memory.getNonHeapMemoryUsage().getMax() / CONVERT);
                    // metrics.put("memoryNonHeapCommittedGB", (float) memory.getNonHeapMemoryUsage().getCommitted() / CONVERT);
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
                    for (int i = 0; i < beans.size(); i++) {
                        var bean = beans.get(i);

                        metrics.put("garbageCollector" + i + "AccCollectionTimeMillis", bean.getCollectionTime());
                        metrics.put("garbageCollector" + i + "AccCollectionCount", bean.getCollectionCount());
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
                    var bean = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
                    metrics.put("processCpuLoad", bean.getProcessCpuLoad() * 100.0);
                    metrics.put("systemCpuLoad", bean.getSystemCpuLoad() * 100.0);
                    // metrics.put("freeMemorySizeGB", bean.getFreeMemorySize() / CONVERT);
                    // metrics.put("freePhysicalMemorySizeGB", bean.getFreePhysicalMemorySize() / CONVERT);
                    // metrics.put("totalPhysicalMemorySizeGB", bean.getTotalPhysicalMemorySize() / CONVERT);
                    // metrics.put("committedVirtualMemorySizeGB", bean.getCommittedVirtualMemorySize() / CONVERT);
                    metrics.put("freeSwapSpaceSizeGB", bean.getFreeSwapSpaceSize() / CONVERT);
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

                writer.enqueue(PerfSingleResult.newBuilder()
                        .setMetricsResult(PerfMetricsResult.newBuilder()
                                .setInitiated(SdkOperationExecutor.getTimeNow())
                                .setMetrics(metrics.toString()))
                        .build());
            }
        }

        logger.info("Metrics thread finished");
    }
}
