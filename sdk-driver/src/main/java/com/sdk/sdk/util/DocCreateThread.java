package com.sdk.sdk.util;

import com.couchbase.client.core.error.CollectionExistsException;
import com.couchbase.client.core.error.IndexFailureException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.UpsertOptions;
import com.couchbase.client.java.manager.bucket.BucketManager;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.collection.CollectionManager;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.query.QueryResult;
import com.sdk.constants.Defaults;
import com.sdk.constants.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;


/**
 * Connects to the provided cluster and adds the correct number of documents
 */
public class DocCreateThread extends Thread {
    private final int docNum;
    private Bucket bucket;
    private Scope scope;
    private static final Logger logger = LoggerFactory.getLogger(DocCreateThread.class);


    public DocCreateThread(int docNum, Cluster cluster, String bucketName, String scopeName) throws Exception{
        this.docNum =docNum;
        if (docNum > 0) {
            logger.info("Creating connection to cluster to create document pool");
            this.bucket = cluster.bucket(bucketName);
            this.scope = bucket.scope(scopeName);
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
                    collection.upsert(Defaults.KEY_PREFACE + i, input, UpsertOptions.upsertOptions().timeout(Duration.ofSeconds(30)));

                    if (i % 1000 == 0) {
                        logger.info("Inserted {} documents", i);
                    }
                }
            }
        } catch (Exception e){
            logger.error("Error adding documents to cluster", e);
            System.exit(-1);
        }
    }
}
