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

import com.couchbase.grpc.sdk.protocol.API;
import com.couchbase.grpc.sdk.protocol.ClusterConnectionCreateRequest;
import com.couchbase.grpc.sdk.protocol.ClusterConnectionCreateResponse;
import com.couchbase.grpc.sdk.protocol.PerfRunRequest;
import com.couchbase.grpc.sdk.protocol.PerfRunResult;
import com.couchbase.grpc.sdk.protocol.PerformerCaps;
import com.couchbase.grpc.sdk.protocol.PerformerCapsFetchRequest;
import com.couchbase.grpc.sdk.protocol.PerformerCapsFetchResponse;
import com.couchbase.grpc.sdk.protocol.PerformerSdkServiceGrpc;
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

import java.io.IOException;
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
                .addPerformerCaps(PerformerCaps.GRPC_TESTING)
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
                        StreamObserver<PerfRunResult> responseObserver) {
        try {
            ClusterConnection connection = clusterConnections.get(request.getClusterConnectionId());

            logger.info("Beginning PerfRun");
            var writer = new PerfWriteThread(responseObserver, request.getConfig());

            var metrics = new MetricsReporter(writer);
            metrics.start();

            PerfMarshaller.run(connection, request, writer);

            metrics.interrupt();
            metrics.join();

            responseObserver.onCompleted();
        }
        catch (UnsupportedOperationException err) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(err.toString()).asException());
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
}