package com.couchbase.client.performer.core.perf;

import com.couchbase.client.performer.grpc.HorizontalScaling;

public record PerPerfThread(int runnerIndex,
                            HorizontalScaling perThread,
                            PerfWriteThread writer,
                            Counters counters) {}
