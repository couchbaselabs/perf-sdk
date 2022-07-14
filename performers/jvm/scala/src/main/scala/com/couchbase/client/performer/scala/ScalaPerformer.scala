package com.couchbase.client.performer.scala

import com.couchbase.client.performer.core.metrics.MetricsReporter
import com.couchbase.client.performer.core.perf.{PerfMarshaller, PerfRunnerThread, PerfWriteThread}
import com.couchbase.client.performer.grpc._
import com.couchbase.client.performer.scala.util.ClusterConnection
import io.grpc.stub.StreamObserver
import io.grpc.{ServerBuilder, Status}
import org.slf4j.LoggerFactory


object ScalaPerformer {
  private val logger = LoggerFactory.getLogger(classOf[ScalaPerformer])

  def main(args: Array[String]): Unit = {
    var port = 8060
    // ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
    for (parameter <- args) {
      parameter.split("=")(0) match {
        case "port" =>
          port = parameter.split("=")(1).toInt

        case _ =>
          logger.warn("Undefined input: {}. Ignoring it", parameter)
      }
    }
    val builder = ServerBuilder.forPort(port)
    builder.addService(new ScalaPerformer)
    val server = builder.build()
    server.start
    logger.info("Server Started at {}", server.getPort)
    server.awaitTermination()
  }
}

class ScalaPerformer extends PerformerSdkServiceGrpc.PerformerSdkServiceImplBase {
  private val logger = LoggerFactory.getLogger(classOf[ScalaPerformer])
  private val clusterConnections = collection.mutable.Map.empty[String, ClusterConnection]

  override def performerCapsFetch(request: PerformerCapsFetchRequest, responseObserver: StreamObserver[PerformerCapsFetchResponse]): Unit = {
    responseObserver.onNext(PerformerCapsFetchResponse.newBuilder
      .setPerformerUserAgent("scala")
      .setApiCount(1) // blocking only for now
      .addPerformerCaps(PerformerCaps.GRPC_TESTING)
      .build)
    responseObserver.onCompleted()
  }

  override def clusterConnectionCreate(request: ClusterConnectionCreateRequest, responseObserver: StreamObserver[ClusterConnectionCreateResponse]): Unit = {
    try {
      val connection = new ClusterConnection(request)
      clusterConnections.put(request.getClusterConnectionId, connection)
      val response = ClusterConnectionCreateRequest.getDefaultInstance.newBuilderForType
      logger.info("Established connection to cluster at IP: {} with user {} and id {}", request.getClusterHostname, request.getClusterUsername, request.getClusterConnectionId)
      responseObserver.onNext(ClusterConnectionCreateResponse.newBuilder.build)
      responseObserver.onCompleted()
    } catch {
      case err: Exception =>
        logger.error("Operation failed during clusterConnectionCreate due to {}", err.getMessage)
        responseObserver.onError(Status.ABORTED.withDescription(err.toString).asException)
    }
  }

  override def perfRun(request: PerfRunRequest, responseObserver: StreamObserver[PerfRunResult]): Unit = {
    try {
      val connection = clusterConnections(request.getClusterConnectionId)
      logger.info("Beginning PerfRun")
      val writer = new PerfWriteThread(responseObserver, request.getConfig)
      val metrics = new MetricsReporter(writer)
      metrics.start()
      val executor = new ScalaSdkCommandExecutor(connection)
      PerfMarshaller.run(request, writer, (x) => new PerfRunnerThread(x, executor))
      metrics.interrupt()
      metrics.join()
      responseObserver.onCompleted()
    } catch {
      case err: UnsupportedOperationException =>
        responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(err.toString).asException)
      case err@(_: RuntimeException | _: InterruptedException) =>
        responseObserver.onError(Status.ABORTED.withDescription(err.toString).asException)
    }
  }
}