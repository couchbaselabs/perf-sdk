package com.couchbase.client.performer.kotlin

import com.couchbase.client.performer.core.metrics.MetricsReporter
import com.couchbase.client.performer.core.perf.PerPerfThread
import com.couchbase.client.performer.core.perf.PerfMarshaller
import com.couchbase.client.performer.core.perf.PerfRunnerThread
import com.couchbase.client.performer.core.perf.PerfWriteThread
import com.couchbase.client.performer.grpc.*
import com.couchbase.client.performer.grpc.PerformerSdkServiceGrpc.PerformerSdkServiceImplBase
import com.couchbase.client.performer.kotlin.util.ClusterConnection
import io.grpc.ServerBuilder
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory

class KotlinPerformer : PerformerSdkServiceImplBase() {
    private val clusterConnections: MutableMap<String, ClusterConnection> = mutableMapOf()
    private val logger = LoggerFactory.getLogger(KotlinPerformer::class.java)

    override fun performerCapsFetch(request: PerformerCapsFetchRequest, responseObserver: StreamObserver<PerformerCapsFetchResponse>) {
        responseObserver.onNext(PerformerCapsFetchResponse.newBuilder()
                .setPerformerUserAgent("kotlin")
                .setApiCount(1)
                .addPerformerCaps(PerformerCaps.GRPC_TESTING)
                .build())
        responseObserver.onCompleted()
    }

    override fun clusterConnectionCreate(request: ClusterConnectionCreateRequest, responseObserver: StreamObserver<ClusterConnectionCreateResponse>) {
        try {
            val connection = ClusterConnection(request)
            clusterConnections[request.clusterConnectionId] = connection
            logger.info("Established connection to cluster at IP: {} with user {} and id {}", request.clusterHostname, request.clusterUsername, request.clusterConnectionId)
            responseObserver.onNext(ClusterConnectionCreateResponse.newBuilder().build())
            responseObserver.onCompleted()
        } catch (err: Exception) {
            logger.error("Operation failed during clusterConnectionCreate due to {}", err.message)
            responseObserver.onError(Status.ABORTED.withDescription(err.toString()).asException())
        }
    }

    override fun perfRun(request: PerfRunRequest,
                         responseObserver: StreamObserver<PerfRunResult>) {
        try {
            val connection = clusterConnections[request.clusterConnectionId]!!
            logger.info("Beginning PerfRun")
            val writer = PerfWriteThread(responseObserver, request.config)
            val metrics = MetricsReporter(writer)
            metrics.start()
            val executor = KotlinSdkCommandExecutor(connection)
            PerfMarshaller.run(request, writer) { x: PerPerfThread? -> PerfRunnerThread(x, executor) }
            metrics.interrupt()
            metrics.join()
            responseObserver.onCompleted()
        } catch (err: UnsupportedOperationException) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(err.toString()).asException())
        } catch (err: RuntimeException) {
            responseObserver.onError(Status.ABORTED.withDescription(err.toString()).asException())
        } catch (err: InterruptedException) {
            responseObserver.onError(Status.ABORTED.withDescription(err.toString()).asException())
        }
    }
}


fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger(KotlinPerformer::class.java)
    val port = 8060

    // ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    val server = ServerBuilder.forPort(port)
        .addService(KotlinPerformer())
        .build()
    server.start()
    logger.info("Server Started at {}", server.port)
    server.awaitTermination()
}
