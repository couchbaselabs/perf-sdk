package com.sdk.sdk.util;

import com.couchbase.client.core.error.CollectionExistsException;
import com.couchbase.client.core.error.IndexFailureException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.bucket.BucketManager;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.collection.CollectionManager;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.query.QueryResult;
import com.sdk.constants.Defaults;
import com.sdk.constants.Strings;
import com.sdk.logging.LogUtil;
import org.slf4j.Logger;

import java.time.Duration;


/**
 * Connects to the provided cluster and adds the correct number of documents
 */
public class DocCreateThread extends Thread {
    private final int docNum;
    private Cluster cluster;
    private Bucket bucket;
    private Scope scope;
    private static final Logger logger = LogUtil.getLogger(DocCreateThread.class);


    public DocCreateThread(int docNum, String hostname, String userName, String password, String bucketName, String scopeName) throws Exception{
        this.docNum =docNum;
        if (docNum > 0) {
            logger.info("Creating connection to cluster to create document pool");
            try {
                this.cluster = Cluster.connect("couchbase://"+hostname, userName, password);
                cluster.waitUntilReady(Duration.ofSeconds(30));
                this.bucket = cluster.bucket(bucketName);
                this.scope = bucket.scope(scopeName);
            }
            //TODO Discuss better exception to throw
            catch (Exception err) {
                throw new Exception("Could not connect to cluster for doc pool creation: " + err.getMessage());
            }
        }
    }

    @Override
    public void run() {
        try{
            if (docNum > 0) {
                logger.info("Creating and filling " + Defaults.DOCPOOL_COLLECTION + " collection");
                try {
                    CollectionSpec spec = CollectionSpec.create(Defaults.DOCPOOL_COLLECTION, Defaults.DEFAULT_SCOPE);
                    bucket.collections().createCollection(spec);
                } catch (CollectionExistsException err) {
                    logger.info("Collection named " + Defaults.DOCPOOL_COLLECTION + " already exists, moving on");
                }
                Collection collection = scope.collection(Defaults.DOCPOOL_COLLECTION);
                JsonObject input = JsonObject.create().put(Strings.CONTENT_NAME, Strings.INITIAL_CONTENT_VALUE);
                for (int i = 0; i < this.docNum; i++) {
                    collection.upsert(Defaults.KEY_PREFACE + i, input);
                }
                cluster.disconnect();
            }
        } catch (Exception e){
            logger.error("Error adding documents to cluster", e);
        }
    }
}
