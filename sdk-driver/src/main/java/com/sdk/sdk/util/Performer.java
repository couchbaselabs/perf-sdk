package com.sdk.sdk.util;

import com.couchbase.client.performer.grpc.ClusterConnectionCreateRequest;
import com.couchbase.client.performer.grpc.PerformerCapsFetchRequest;
import com.couchbase.client.performer.grpc.PerformerCapsFetchResponse;
import com.couchbase.client.performer.grpc.PerformerSdkServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Stores data and connections for each performer connected to.
 */
public class Performer {
    private static final Logger logger = LoggerFactory.getLogger(Performer.class);
    private final PerformerSdkServiceGrpc.PerformerSdkServiceStub stubBlockFuture;
    private final PerformerSdkServiceGrpc.PerformerSdkServiceBlockingStub stubBlock;
    private final PerformerCapsFetchResponse response;

    public Performer(String hostname, int port, ClusterConnectionCreateRequest createConnection, Optional<Boolean> compression) {
        var builder = ManagedChannelBuilder.forAddress(hostname, port)
                // Using non-TLS channel because presumably will be more performant
                .usePlaintext();
        if (compression.isPresent() && compression.get()) {
            builder.enableFullStreamDecompression();
        }
        var channelBlocking = builder.build();
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

