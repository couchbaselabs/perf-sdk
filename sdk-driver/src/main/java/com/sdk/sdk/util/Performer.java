package com.sdk.sdk.util;

import com.couchbase.grpc.sdk.protocol.ClusterConnectionCreateRequest;
import com.couchbase.grpc.sdk.protocol.PerformerCapsFetchRequest;
import com.couchbase.grpc.sdk.protocol.PerformerCapsFetchResponse;
import com.couchbase.grpc.sdk.protocol.PerformerSdkServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores data and connections for each performer connected to.
 */
public class Performer {
    private static final Logger logger = LoggerFactory.getLogger(Performer.class);
    private final PerformerSdkServiceGrpc.PerformerSdkServiceStub stubBlockFuture;
    private final PerformerSdkServiceGrpc.PerformerSdkServiceBlockingStub stubBlock;
    private final PerformerCapsFetchResponse response;

    public Performer(String hostname, int port, ClusterConnectionCreateRequest createConnection) {
        ManagedChannel channelBlocking = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
        stubBlockFuture = PerformerSdkServiceGrpc.newStub(channelBlocking);
        stubBlock = PerformerSdkServiceGrpc.newBlockingStub(channelBlocking);

        response = stubBlock.performerCapsFetch(PerformerCapsFetchRequest.newBuilder().build());

        stubBlock.clusterConnectionCreate(createConnection);

        logger.info("Connected to performer: {}", response.toString());
    }

    public PerformerSdkServiceGrpc.PerformerSdkServiceBlockingStub stubBlock() {
        return stubBlock;
    }

    public PerformerSdkServiceGrpc.PerformerSdkServiceStub stubBlockFuture() {
        return stubBlockFuture;
    }

    public PerformerCapsFetchResponse response() {
        return response;
    }
}

