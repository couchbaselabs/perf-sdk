package com.couchbase.sdk.utils;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.grpc.sdk.protocol.ClusterConnectionCreateRequest;
import com.couchbase.grpc.sdk.protocol.DocLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Creates a connection to the given cluster
 */
public class ClusterConnection {
    private final ClusterConnectionCreateRequest request;
    private static Cluster cluster;
    private static final Logger logger = LoggerFactory.getLogger(ClusterConnection.class);


    public ClusterConnection(ClusterConnectionCreateRequest reqData)  {
        this.request = reqData;
        var hostname = "couchbase://" + reqData.getClusterHostname();
        logger.info("Attempting connection to cluster " + hostname);
        cluster = Cluster.connect(hostname, reqData.getClusterUsername(), reqData.getClusterPassword());
        cluster.waitUntilReady(Duration.ofSeconds(30));
    }

    public Collection collection(DocLocation loc) {
        com.couchbase.grpc.sdk.protocol.Collection coll = null;

        if (loc.hasPool()) {
            coll = loc.getPool().getCollection();
        }
        else if (loc.hasSpecific()) {
            coll = loc.getSpecific().getCollection();
        }
        else if (loc.hasUuid()) {
            coll = loc.getUuid().getCollection();
        }
        else {
            throw new UnsupportedOperationException("Unknown DocLocation type");
        }

        var bucket = cluster.bucket(coll.getBucket());
        return bucket
                .scope(coll.getScope())
                .collection(coll.getCollection());
    }
}

