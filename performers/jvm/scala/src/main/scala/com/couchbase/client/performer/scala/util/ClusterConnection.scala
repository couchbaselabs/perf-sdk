package com.couchbase.client.performer.scala.util

import com.couchbase.client.performer.grpc.{ClusterConnectionCreateRequest, DocLocation}
import com.couchbase.client.scala.{Cluster, Collection}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

class ClusterConnection(req: ClusterConnectionCreateRequest) {
  private val logger = LoggerFactory.getLogger(classOf[ClusterConnection])
  private val hostname = "couchbase://" + req.getClusterHostname
  logger.info("Attempting connection to cluster " + hostname)

  private val cluster = Cluster.connect(hostname, req.getClusterUsername, req.getClusterPassword).get

  cluster.waitUntilReady(30.seconds)

  def collection(loc: DocLocation): Collection = {
    val coll = {
      if (loc.hasPool) loc.getPool.getCollection
      else if (loc.hasSpecific) loc.getSpecific.getCollection
      else if (loc.hasUuid) loc.getUuid.getCollection
      else throw new UnsupportedOperationException("Unknown DocLocation type")
    }

    val bucket = cluster.bucket(coll.getBucket)
    bucket.scope(coll.getScope).collection(coll.getCollection)
  }
}
