package com.sdk.config;

import com.couchbase.grpc.sdk.protocol.PerfRunHorizontalScaling;

public interface Op {
    void applyTo(PerfRunHorizontalScaling.Builder builder);
}
