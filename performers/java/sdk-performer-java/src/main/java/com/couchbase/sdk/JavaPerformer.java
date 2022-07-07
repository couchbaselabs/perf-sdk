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

import com.couchbase.client.core.deps.io.netty.util.ResourceLeakDetector;
import com.couchbase.grpc.sdk.protocol.*;
import com.couchbase.sdk.metrics.MetricsReporter;
import com.couchbase.sdk.perf.PerfMarshaller;
import com.couchbase.sdk.perf.PerfWriteThread;
import com.couchbase.sdk.utils.ClusterConnection;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JavaPerformer extends PerformerSdkServiceGrpc.PerformerSdkServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(JavaPerformer.class);
    private final ConcurrentHashMap<String, ClusterConnection> clusterConnections = new ConcurrentHashMap<>();

    @Override
    public void performerCapsFetch(PerformerCapsFetchRequest request, StreamObserver<PerformerCapsFetchResponse> responseObserver) {
        responseObserver.onNext(PerformerCapsFetchResponse.newBuilder()
                .setPerformerUserAgent("java")
                .addSupportedApis(API.ASYNC)
                .addSupportedApis(API.DEFAULT)
                .setPerformerVersion(0)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void clusterConnectionCreate(ClusterConnectionCreateRequest request, StreamObserver<ClusterConnectionCreateResponse> responseObserver) {
        try {
            ClusterConnection connection = new ClusterConnection(request);
            clusterConnections.put(request.getClusterConnectionId(), connection);

            ClusterConnectionCreateRequest.Builder response = ClusterConnectionCreateRequest.getDefaultInstance().newBuilderForType();

            logger.info("Established connection to cluster at IP: {} with user {} and id {}",request.getClusterHostname(), request.getClusterUsername(), request.getClusterConnectionId());

            responseObserver.onNext(ClusterConnectionCreateResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
        catch (Exception err) {
            logger.error("Operation failed during clusterConnectionCreate due to {}", err.getMessage());
            responseObserver.onError(Status.ABORTED.withDescription(err.toString()).asException());
        }
    }

    @Override
    public void perfRun(PerfRunRequest request,
                        StreamObserver<PerfSingleResult> responseObserver) {
        try{
            ClusterConnection connection = clusterConnections.get(request.getClusterConnectionId());

            logger.info("Beginning PerfRun");
            var writer = new PerfWriteThread(responseObserver);

            var metrics = new MetricsReporter(writer);
            metrics.start();

            PerfMarshaller.run(connection, request, writer);

            metrics.interrupt();
            metrics.join();

            responseObserver.onCompleted();
        } catch (RuntimeException | InterruptedException err) {
            responseObserver.onError(Status.ABORTED.withDescription(err.toString()).asException());
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 8060;

        // ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        for(String parameter : args) {
            switch (parameter.split("=")[0]) {
                case "port":
                    port= Integer.parseInt(parameter.split("=")[1]);
                    break;
                default:
                    logger.warn("Undefined input: {}. Ignoring it",parameter);
            }
        }

        Server server = ServerBuilder.forPort(port)
                .addService(new JavaPerformer())
                .build();
        server.start();
        logger.info("Server Started at {}", server.getPort());
        server.awaitTermination();
    }

    public void exit(com.couchbase.grpc.sdk.protocol.ExitRequest request,
                     io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        logger.info("Been told to exit for reason '{}' with code {}", request.getReason(), request.getExitCode());
        System.exit(request.getExitCode());
    }

}