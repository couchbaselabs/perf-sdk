package com.couchbase.sdk.util;

import com.couchbase.grpc.protocol.PerformerTransactionServiceGrpc;
import com.couchbase.grpc.sdk.protocol.CreateConnectionRequest;

import com.couchbase.grpc.sdk.protocol.PerformerSdkServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import static com.couchbase.grpc.sdk.protocol.PerformerSdkServiceGrpc.newBlockingStub;
import static com.couchbase.grpc.sdk.protocol.PerformerSdkServiceGrpc.newStub;

/**
 * Stores data and connections for each performer connected to.
 */
public class Performer {
    private CreateConnectionRequest initialConnectionRequest;
    private PerformerSdkServiceGrpc.PerformerSdkServiceStub stubBlockFuture;
    private PerformerSdkServiceGrpc.PerformerSdkServiceBlockingStub stubBlock;

    public Performer(int performerIdx, String hostname, int port, CreateConnectionRequest createConnection) {
        try {
            ManagedChannel channelBlocking = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
            stubBlockFuture = newStub(channelBlocking);
            stubBlock = newBlockingStub(channelBlocking);
            initialConnectionRequest = createConnection;
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.ABORTED) {
                //logger.error("gRPC Exception from createConnection. Error Message: {} ", e.getMessage());
            }
        }
    }

    public PerformerSdkServiceGrpc.PerformerSdkServiceBlockingStub stubBlock() {
        return stubBlock;
    }
}

