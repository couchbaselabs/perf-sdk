package com.sdk.config;

import com.couchbase.client.performer.grpc.HorizontalScaling;

public interface Op {
    void applyTo(HorizontalScaling.Builder builder);
}
