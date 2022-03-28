package com.couchbase.sdk;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.grpc.sdk.protocol.*;
import com.couchbase.sdk.utils.ClusterConnection;
import com.sdk.logging.LogUtil;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class SdkOperation {
    private String name;
    private Map<Integer, String> docIds;
    private Logger logger;
    private AtomicInteger docPool;

    public SdkOperation(AtomicInteger docPool){
        this.docPool = docPool;
    }

    public com.couchbase.grpc.sdk.protocol.SdkCommandResult run(
            ClusterConnection connection,
            SdkCreateRequest req) {
        this.name = req.getName();
        logger = LogUtil.getLogger(this.name);

        for (int i=0; i< req.getCount(); i++) {
            performOperation(connection, req.getCommand());
        }
        SdkCommandResult.Builder response = SdkCommandResult.getDefaultInstance().newBuilderForType();
        return response.build();
    }

    private void performOperation(ClusterConnection connection,
                                  SdkCommand op) {
        if (op.hasInsert()){
            final CommandInsert request = op.getInsert();
            final Collection collection = connection.getBucket().scope(request.getBucketInfo().getScopeName()).collection(request.getBucketInfo().getCollectionName());
            JsonObject content = JsonObject.fromJson(request.getContentJson());
            logger.info("Performing insert operation on bucket {} on collection {}",
                    request.getBucketInfo().getBucketName(), request.getBucketInfo().getCollectionName());
            collection.insert(UUID.randomUUID().toString(), content);
        }else if(op.hasGet()) {
            final CommandGet request = op.getGet();
            final Collection collection = connection.getBucket().scope(request.getBucketInfo().getScopeName()).collection(request.getBucketInfo().getCollectionName());
            logger.info("Performing get operation on bucket {} on collection {}",
                    request.getBucketInfo().getBucketName(), request.getBucketInfo().getCollectionName());
            collection.get(request.getDocId());
        }else if(op.hasRemove()){
            final CommandRemove request = op.getRemove();
            final Collection collection = connection.getBucket().scope(request.getBucketInfo().getScopeName()).collection(request.getBucketInfo().getCollectionName());
            logger.info("Performing remove operation on bucket {} on collection {}, docId {}",
                    request.getBucketInfo().getBucketName(), request.getBucketInfo().getCollectionName(), docPool.get());
            collection.remove(request.getKeyPreface() + docPool.addAndGet(1));
        }else {
        throw new InternalPerformerFailure(new IllegalArgumentException("Unknown operation"));
    }
    }
}
