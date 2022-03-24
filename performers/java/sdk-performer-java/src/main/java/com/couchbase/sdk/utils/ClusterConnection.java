package com.couchbase.sdk.utils;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.grpc.sdk.protocol.CreateConnectionRequest;

import java.time.Duration;


public class ClusterConnection {
    private Cluster cluster;
    private Bucket bucket;
    public String hostname;
    public String userName;
    public String password;


    public ClusterConnection( CreateConnectionRequest reqData)  {
        hostname = reqData.getClusterHostname();
        userName = reqData.getClusterUsername();
        password = reqData.getClusterPassword();
        cluster = Cluster.connect(hostname,userName,password);
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

