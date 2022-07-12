package com.couchbase.client.performer.kotlin.util

import com.couchbase.client.java.Bucket
import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.performer.grpc.ClusterConnectionCreateRequest
import com.couchbase.client.performer.grpc.DocLocation
import java.util.concurrent.ConcurrentHashMap

class ClusterConnection(req: ClusterConnectionCreateRequest) {
    private val hostname = "couchbase://" + req.clusterHostname
    private val cluster = Cluster.connect(hostname, req.clusterUsername, req.clusterPassword)
    private val bucketCache: MutableMap<String, com.couchbase.client.kotlin.Bucket> = mutableMapOf()

    fun collection(loc: DocLocation): com.couchbase.client.kotlin.Collection {
        var coll: com.couchbase.client.performer.grpc.Collection? = null
        if (loc.hasPool()) coll = loc.pool.collection
        else if (loc.hasSpecific()) coll = loc.specific.collection
        else if (loc.hasUuid()) coll = loc.uuid.collection
        else throw UnsupportedOperationException("Unknown DocLocation type")

        // KCBC-98: no SDK bucket caching
        val bucket = bucketCache.getOrPut(coll.bucket) { cluster.bucket(coll.bucket) }
        return bucket.scope(coll.scope).collection(coll.collection)
    }
}