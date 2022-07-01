package com.sdk.config;

import com.couchbase.grpc.sdk.protocol.CommandRemove;
import com.couchbase.grpc.sdk.protocol.Counter;
import com.couchbase.grpc.sdk.protocol.CounterGlobal;
import com.couchbase.grpc.sdk.protocol.PerfRunHorizontalScaling;
import com.couchbase.grpc.sdk.protocol.SdkCommand;
import com.couchbase.grpc.sdk.protocol.SdkWorkload;
import com.couchbase.grpc.sdk.protocol.Workload;

public record OpRemove(int count, TestSuite.DocLocation location, TestSuite.Variables variables) implements Op {
    @Override
    public void applyTo(PerfRunHorizontalScaling.Builder builder) {
        builder.addWorkloads(Workload.newBuilder()
                .setSdk(SdkWorkload.newBuilder()
                        .setCommand(SdkCommand.newBuilder()
                                .setRemove(CommandRemove.newBuilder()
                                        .setLocation(location.convert(variables))
                                        .build()))
                        .setCounter(Counter.newBuilder()
                                .setCounterId("counter1")
                                .setGlobal(CounterGlobal.newBuilder()
                                        .setCount(count)))));
    }
}
