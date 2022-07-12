package com.sdk.config;

import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.performer.grpc.Bounds;
import com.couchbase.client.performer.grpc.Counter;
import com.couchbase.client.performer.grpc.CounterGlobal;
import com.couchbase.client.performer.grpc.HorizontalScaling;
import com.couchbase.client.performer.grpc.SdkCommand;
import com.couchbase.client.performer.grpc.SdkCommandInsert;
import com.couchbase.client.performer.grpc.SdkWorkload;
import com.couchbase.client.performer.grpc.Workload;

public record OpInsert(JsonObject content, int count, TestSuite.DocLocation location,
                       TestSuite.Variables variables) implements Op {
    @Override
    public void applyTo(HorizontalScaling.Builder builder) {
        builder.addWorkloads(Workload.newBuilder()
                .setSdk(SdkWorkload.newBuilder()
                        .addCommand(SdkCommand.newBuilder()
                                .setInsert(SdkCommandInsert.newBuilder()
                                        .setContentJson(content.toString())
                                        .setLocation(location.convert(variables))
                                        .build()))
                        .setBounds(Bounds.newBuilder()
                                .setCounter(Counter.newBuilder()
                                        .setCounterId("counter1")
                                        .setGlobal(CounterGlobal.newBuilder()
                                                .setCount(count))))));
    }
}
