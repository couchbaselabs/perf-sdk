package com.sdk.config;

import com.couchbase.client.performer.grpc.Bounds;
import com.couchbase.client.performer.grpc.CommandGrpcPing;
import com.couchbase.client.performer.grpc.Counter;
import com.couchbase.client.performer.grpc.CounterGlobal;
import com.couchbase.client.performer.grpc.GrpcCommand;
import com.couchbase.client.performer.grpc.GrpcWorkload;
import com.couchbase.client.performer.grpc.HorizontalScaling;
import com.couchbase.client.performer.grpc.Workload;

public record OpGrpcPing(int count) implements Op {
    @Override
    public void applyTo(HorizontalScaling.Builder builder) {
        builder.addWorkloads(Workload.newBuilder()
                .setGrpc(GrpcWorkload.newBuilder()
                        .setCommand(GrpcCommand.newBuilder()
                                .setPing(CommandGrpcPing.getDefaultInstance()))
                        .setBounds(Bounds.newBuilder()
                                .setCounter(Counter.newBuilder()
                                        .setCounterId("counter1")
                                        .setGlobal(CounterGlobal.newBuilder()
                                                .setCount(count))))));
    }
}
