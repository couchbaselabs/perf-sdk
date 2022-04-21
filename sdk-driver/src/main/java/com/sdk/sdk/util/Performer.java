package com.sdk.sdk.util;

import com.couchbase.grpc.sdk.protocol.CreateConnectionResponse;
import com.couchbase.grpc.sdk.protocol.CreateConnectionRequest;

import com.couchbase.grpc.sdk.protocol.PerformerSdkServiceGrpc;
import com.sdk.SdkDriver;
import com.sdk.logging.LogUtil;
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
    private CreateConnectionRequest initialConnectionRequest;
    private PerformerSdkServiceGrpc.PerformerSdkServiceStub stubBlockFuture;
    private PerformerSdkServiceGrpc.PerformerSdkServiceBlockingStub stubBlock;
    private int protocolMajorVersion;
    private String clusterConnectionId;
    private static final Logger logger = LogUtil.getLogger(Performer.class);

    public Performer(String hostname, int port, CreateConnectionRequest createConnection) {
        try {
            ManagedChannel channelBlocking = ManagedChannelBuilder.forAddress(hostname, port).usePlaintext().build();
            stubBlockFuture = PerformerSdkServiceGrpc.newStub(channelBlocking);
            stubBlock = PerformerSdkServiceGrpc.newBlockingStub(channelBlocking);
            initialConnectionRequest = createConnection;
            CreateConnectionResponse response = connectToPerformer(initialConnectionRequest);

            clusterConnectionId = response.getClusterConnectionId();
            logger.info(getClusterConnectionId());
            String protocolVersion = response.getProtocolVersion();
            String[] split = protocolVersion.split("\\.");
            protocolMajorVersion = Integer.parseInt(split[0]);
            int protocolMinorVersion = Integer.parseInt(split[1]);

            if (protocolMajorVersion < 1 || protocolMajorVersion > 2 || protocolMinorVersion < 0
                    || (protocolMajorVersion == 1 && protocolMinorVersion != 0)
                    || (protocolMajorVersion == 2 && protocolMinorVersion > 1)) {
                throw new IllegalArgumentException("Performer has specified an invalid protocol field of " + protocolVersion);
            }

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.ABORTED) {
                logger.error("gRPC Exception from createConnection. Error Message: {} ", e.getMessage());
            }
        }
    }

    public PerformerSdkServiceGrpc.PerformerSdkServiceBlockingStub stubBlock() {
        return stubBlock;
    }

    public PerformerSdkServiceGrpc.PerformerSdkServiceStub stubBlockFuture() {
        return stubBlockFuture;
    }

    public CreateConnectionResponse connectToPerformer(com.couchbase.grpc.sdk.protocol.CreateConnectionRequest createConnectionRequest){
        logger.info("HEY LOOOOOOOOOOK, attempting connection");
        return stubBlock.createConnection(createConnectionRequest);
    }

    public String getClusterConnectionId() {
        return clusterConnectionId;
    }
}

