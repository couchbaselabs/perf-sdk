/*
 * Copyright (c) 2020 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.sdk;

import com.couchbase.grpc.sdk.protocol.*;
import com.couchbase.sdk.perf.PerfMarshaller;
import com.couchbase.sdk.utils.ClusterConnection;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JavaPerformer extends PerformerSdkServiceGrpc.PerformerSdkServiceImplBase {

    public static final String DEFAULT_CONFIG_RESOURCE_NAME = "config.toml";
    private final String configResourceName;
    private ClusterConnection defaultConnection= null;
    private static ConcurrentHashMap<String, ClusterConnection> mapIdToClusterConnection = new ConcurrentHashMap<String, ClusterConnection>();
    private static String version;          // "v1_0_0"
    private static String originalVersion;  // "1.0.0"


    public JavaPerformer(String configResourceName) {
        this.configResourceName = configResourceName;
    }

    @Override
    public void createConnection(CreateConnectionRequest request, StreamObserver<CreateConnectionResponse> responseObserver) {
        System.out.println("connection stuff happening");
        try {
            CreateConnectionResponse.Builder response = CreateConnectionResponse.getDefaultInstance().newBuilderForType();
            response.setProtocolVersion("2.0");

            ClusterConnection connection = new ClusterConnection(request);

            String clusterConnectionId = UUID.randomUUID().toString();

            mapIdToClusterConnection.put(clusterConnectionId,connection);

            defaultConnection = connection;

            response.setClusterConnectionId(clusterConnectionId);

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }
        catch (Exception err) {
            responseObserver.onError(Status.ABORTED.withDescription(err.getMessage()).asException());
        }
    }

    @Override
    public void perfRun(PerfRunRequest request,
                        StreamObserver<PerfSingleSdkOpResult> responseObserver) {
        System.out.println("Performer stuff is happening");
        try{
            ClusterConnection connection = getClusterConnection(request.getClusterConnectionId());

            PerfMarshaller.run(connection, request, responseObserver);

            responseObserver.onCompleted();
        } catch (RuntimeException | InterruptedException err) {
            responseObserver.onError(Status.ABORTED.withDescription(err.getMessage()).asException());
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 8060;
        String configResourceName = JavaPerformer.DEFAULT_CONFIG_RESOURCE_NAME;

        for(String parameter : args) {
            switch (parameter.split("=")[0]) {
                case "loglevel":
                    //LogUtil.setLevelFromSpec(parameter.split("=")[1]);
                    break;
                case "port":
                    port= Integer.parseInt(parameter.split("=")[1]);
                    break;
                case "version":
                    originalVersion = parameter.split("=")[1];
                    version = "v"+originalVersion.split("\\.")[0]+"_"+originalVersion.split("\\.")[1]+"_"+originalVersion.split("\\.")[2];
                    break;
                default:
                    //logger.warn("Undefined input: {}. Ignoring it",parameter);
                    System.out.println("Undefined input: {}. Ignoring it");
            }
        }

        Server server = ServerBuilder.forPort(port)
                .addService(new JavaPerformer(configResourceName))
                .build();
        server.start();
        System.out.println("Server Started");
        //logger.info("Server Started at {}", server.getPort());
        server.awaitTermination();

    }

    private ClusterConnection getClusterConnection(@Nullable String clusterConnectionId){
        ClusterConnection connection =null;
        if(clusterConnectionId==null || clusterConnectionId.equals("")) {
            // If test does not send any clusterConnectionId, then use the very first connection i.e sharedTestState connection
            connection = defaultConnection;
        }else {
            if (mapIdToClusterConnection.containsKey(clusterConnectionId)) {
                connection = mapIdToClusterConnection.get(clusterConnectionId);
            } else {
                //We should not be getting here.
                //logger.error("Unknown clusterConnectionId");
                System.exit(-1);
            }
        }
        return connection;
    }

}