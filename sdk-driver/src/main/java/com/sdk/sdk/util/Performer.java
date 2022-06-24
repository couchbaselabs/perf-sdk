package com.sdk.sdk.util;

import com.couchbase.grpc.sdk.protocol.ClusterConnectionCreateRequest;

import com.couchbase.grpc.sdk.protocol.PerformerSdkServiceGrpc;
import com.sdk.SdkDriver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Stores data and connections for each performer connected to.
 */
public class Performer {
    private PerformerSdkServiceGrpc.PerformerSdkServiceStub stubBlockFuture;
    private PerformerSdkServiceGrpc.PerformerSdkServiceBlockingStub stubBlock;

    public Performer(String hostname, int port, ClusterConnectionCreateRequest createConnection) {
        ManagedChannel channelBlocking = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
        stubBlockFuture = PerformerSdkServiceGrpc.newStub(channelBlocking);
        stubBlock = PerformerSdkServiceGrpc.newBlockingStub(channelBlocking);
        stubBlock.clusterConnectionCreate(createConnection);
    }

    public PerformerSdkServiceGrpc.PerformerSdkServiceBlockingStub stubBlock() {
        return stubBlock;
    }

    public PerformerSdkServiceGrpc.PerformerSdkServiceStub stubBlockFuture() {
        return stubBlockFuture;
    }
}

