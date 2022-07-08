package com.sdk.config;

import com.couchbase.grpc.sdk.protocol.Bounds;
import com.couchbase.grpc.sdk.protocol.Counter;
import com.couchbase.grpc.sdk.protocol.CounterGlobal;
import com.couchbase.grpc.sdk.protocol.HorizontalScaling;
import com.couchbase.grpc.sdk.protocol.SdkCommand;
import com.couchbase.grpc.sdk.protocol.SdkCommandGet;
import com.couchbase.grpc.sdk.protocol.SdkWorkload;
import com.couchbase.grpc.sdk.protocol.Workload;

public record OpGet(int count, TestSuite.DocLocation location, TestSuite.Variables variables) implements Op {
    @Override
    public void applyTo(HorizontalScaling.Builder builder) {
        builder.addWorkloads(Workload.newBuilder()
                .setSdk(SdkWorkload.newBuilder()
                        .addCommand(SdkCommand.newBuilder()
                                .setGet(SdkCommandGet.newBuilder()
                                        .setLocation(location.convert(variables))
                                        .build()))
                        .setBounds(Bounds.newBuilder()
                                .setCounter(Counter.newBuilder()
                                        .setCounterId("counter1")
                                        .setGlobal(CounterGlobal.newBuilder()
                                                .setCount(count))))));    }
}
