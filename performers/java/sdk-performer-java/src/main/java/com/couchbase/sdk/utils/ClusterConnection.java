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
        //FIXME: set this in the driver and send it over here
        bucket = cluster.bucket("default");
        cluster.waitUntilReady(Duration.ofSeconds(30));
    }

    public Bucket getBucket() {
        return bucket;
    }

    public Bucket getBucket(String bucketname) {
        return  cluster.bucket(bucketname);
    }

    public Cluster getCluster(){
        return cluster;
    }
}

