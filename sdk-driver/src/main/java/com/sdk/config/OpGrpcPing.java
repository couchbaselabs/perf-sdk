package com.sdk.config;

import com.couchbase.grpc.sdk.protocol.CommandGrpcPing;
import com.couchbase.grpc.sdk.protocol.Counter;
import com.couchbase.grpc.sdk.protocol.CounterGlobal;
import com.couchbase.grpc.sdk.protocol.GrpcCommand;
import com.couchbase.grpc.sdk.protocol.GrpcWorkload;
import com.couchbase.grpc.sdk.protocol.PerfRunHorizontalScaling;
import com.couchbase.grpc.sdk.protocol.Workload;

public record OpGrpcPing(int count) implements Op {
    @Override
    public void applyTo(PerfRunHorizontalScaling.Builder builder) {
        builder.addWorkloads(Workload.newBuilder()
                .setGrpc(GrpcWorkload.newBuilder()
                        .setCommand(GrpcCommand.newBuilder()
                                .setPing(CommandGrpcPing.getDefaultInstance()))
                        .setCounter(Counter.newBuilder()
                                .setCounterId("counter1")
                                .setGlobal(CounterGlobal.newBuilder()
                                        .setCount(count)))));
    }
}
