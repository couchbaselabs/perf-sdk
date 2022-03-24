package com.sdk.sdk.util;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.bucket.BucketManager;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.sdk.constants.Strings;
import com.sdk.logging.LogUtil;
import org.slf4j.Logger;

import java.time.Duration;

public class DocCreateThread extends Thread {
    private final int docNum;
    private Cluster cluster;
    private Bucket bucket;
    private Scope scope;
    private static final Logger logger = LogUtil.getLogger(DocCreateThread.class);


    public DocCreateThread(int docNum, String hostname, String userName, String password, String bucketName, String scopeName) throws Exception{
        this.docNum =docNum;
        logger.info("Creating connection to cluster to create document pool");
        try {
            this.cluster = Cluster.connect(hostname, userName, password);
            this.bucket = cluster.bucket(bucketName);
            this.scope = bucket.scope(scopeName);
            cluster.waitUntilReady(Duration.ofSeconds(30));
        }
        //TODO Discuss better exception to throw
        catch (Exception err){
            throw new Exception("Could not connect to cluster for doc pool creation: " + err.getMessage());
        }
    }

    @Override
    public void run() {
        logger.info("Creating docPool collection");
        // collection called docPool has to exist
        Collection collection = scope.collection("docPool");
        JsonObject input = JsonObject.create().put(Strings.CONTENT_NAME, Strings.INITIAL_CONTENT_VALUE);
        //TODO add awareness if docs pool already exists, if so flush it of try to make use of documents in it
        for (int i=0; i < this.docNum; i++){
            collection.insert("doc_" + i, input);
        }
    }
}
