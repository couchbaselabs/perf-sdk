package com.couchbase.client.performer.scala.util

import com.couchbase.client.performer.grpc.{ClusterConnectionCreateRequest, DocLocation}
import com.couchbase.client.scala.{Bucket, Cluster, Collection}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

class ClusterConnection(req: ClusterConnectionCreateRequest) {
  private val logger = LoggerFactory.getLogger(classOf[ClusterConnection])
  private val hostname = "couchbase://" + req.getClusterHostname
  logger.info("Attempting connection to cluster " + hostname)

  private val cluster = Cluster.connect(hostname, req.getClusterUsername, req.getClusterPassword).get
  private val bucketCache = scala.collection.mutable.Map.empty[String, Bucket]

  // SCBC-365: hit performance problems when repeatedly opening a scope or collection.  99% of the time we'll be
  // using the same collection every time, so for performance don't use a map here
  private var lastCollection: Collection = _

  cluster.waitUntilReady(30.seconds)

  def collection(loc: DocLocation): Collection = {
    val coll = {
      if (loc.hasPool) loc.getPool.getCollection
      else if (loc.hasSpecific) loc.getSpecific.getCollection
      else if (loc.hasUuid) loc.getUuid.getCollection
      else throw new UnsupportedOperationException("Unknown DocLocation type")
    }

    if (lastCollection != null
      && lastCollection.bucketName == coll.getBucket
      && lastCollection.scopeName == coll.getScope
      && lastCollection.name == coll.getCollection) {
      lastCollection
    }
    else {
      val bucket = bucketCache.getOrElseUpdate(coll.getBucket, {
        logger.info(s"Opening new bucket ${coll.getBucket}")
        cluster.bucket(coll.getBucket)
      })
      val out = bucket.scope(coll.getScope).collection(coll.getCollection)
      lastCollection = out
      out
    }
  }
}
