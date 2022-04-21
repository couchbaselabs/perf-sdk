package com.couchbase.sdk.utils;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.grpc.sdk.protocol.CreateConnectionRequest;
import com.couchbase.sdk.JavaPerformer;
import com.couchbase.sdk.logging.LogUtil;
import org.slf4j.Logger;

import java.time.Duration;


public class ClusterConnection {
    private Cluster cluster;
    private Bucket bucket;
    public String hostname;
    public String userName;
    public String password;
    private static final Logger logger = LogUtil.getLogger(ClusterConnection.class);


    public ClusterConnection( CreateConnectionRequest reqData)  {
        hostname = reqData.getClusterHostname();
        userName = reqData.getClusterUsername();
        password = reqData.getClusterPassword();
        logger.info("The cluster connection command is happening right now");
        try {
            cluster = Cluster.connect(hostname, userName, password);
        }
        catch (Exception e){
            logger.error("Error connecting to cluster", e);
        }
        logger.info("The cluster object is trying to connect to the bucket");
        bucket = cluster.bucket(reqData.getBucketName());
        cluster.waitUntilReady(Duration.ofSeconds(30));
    }

    public Bucket getBucket() {
        return bucket;
    }

    public Bucket getBucket(String bucketName) {
        return  cluster.bucket(bucketName);
    }

    public Cluster getCluster(){
        return cluster;
    }
}

