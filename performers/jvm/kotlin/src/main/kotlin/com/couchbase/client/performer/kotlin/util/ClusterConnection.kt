package com.couchbase.client.performer.kotlin.util

import com.couchbase.client.kotlin.Cluster
import com.couchbase.client.performer.grpc.ClusterConnectionCreateRequest
import com.couchbase.client.performer.grpc.DocLocation

class ClusterConnection(req: ClusterConnectionCreateRequest) {
    private val hostname = "couchbase://" + req.clusterHostname
    private val cluster = Cluster.connect(hostname, req.clusterUsername, req.clusterPassword)

    fun collection(loc: DocLocation): com.couchbase.client.kotlin.Collection {
        var coll = {
            if (loc.hasPool()) loc.pool.collection
            else if (loc.hasSpecific()) loc.specific.collection
            else if (loc.hasUuid()) loc.uuid.collection
            else throw UnsupportedOperationException("Unknown DocLocation type")
        }.invoke()

        val bucket = cluster.bucket(coll.bucket)
        return bucket.scope(coll.scope).collection(coll.collection)
    }
}