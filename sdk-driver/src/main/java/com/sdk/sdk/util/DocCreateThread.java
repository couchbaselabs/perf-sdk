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
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;


/**
 * Connects to the provided cluster and adds the correct number of documents
 */
public class DocCreateThread extends Thread {
    private final long docNum;
    private static final Logger logger = LoggerFactory.getLogger(DocCreateThread.class);
    private final Collection collection;


    public DocCreateThread(long docNum, Collection collection) {
        this.docNum = docNum;
        this.collection = collection;
    }

    @Override
    public void run() {
        try{
            if (docNum > 0) {

                JsonObject input = JsonObject.create().put(Strings.CONTENT_NAME, Strings.INITIAL_CONTENT_VALUE);

                var docIds = new ArrayList<String>();

                for (int i = 0; i < this.docNum; i++) {
                    docIds.add(Defaults.KEY_PREFACE + i);
                }

                var concurrentWrites = 50;

                Flux.fromIterable(docIds)
                        .index()
                        .parallel(concurrentWrites)
                        .runOn(Schedulers.boundedElastic())
                        .concatMap(docIdAndIndex -> {
                            if (docIdAndIndex.getT1() % 1000 == 0) {
                                logger.info("Inserted {} documents", docIdAndIndex.getT1());
                            }

                            return collection.reactive().upsert(docIdAndIndex.getT2(), input, UpsertOptions.upsertOptions().timeout(Duration.ofSeconds(30)));
                        })
                        .sequential()
                        .blockLast();
            }
        } catch (Exception e){
            logger.error("Error adding documents to cluster", e);
            System.exit(-1);
        }
    }
}
