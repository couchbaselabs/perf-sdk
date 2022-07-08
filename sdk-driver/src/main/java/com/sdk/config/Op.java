package com.sdk.config;

import com.couchbase.grpc.sdk.protocol.HorizontalScaling;

public interface Op {
    void applyTo(HorizontalScaling.Builder builder);
}
