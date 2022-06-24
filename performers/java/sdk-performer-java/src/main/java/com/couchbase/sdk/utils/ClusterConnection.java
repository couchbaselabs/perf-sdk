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

    public Collection collection(DocLocation location) {
        return cluster.bucket(location.getBucket())
                .scope(location.getScope())
                .collection(location.getCollection());
    }
}

