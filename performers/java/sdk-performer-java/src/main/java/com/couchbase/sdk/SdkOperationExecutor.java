package com.couchbase.sdk;

import com.couchbase.client.java.json.JsonObject;
import com.couchbase.grpc.sdk.protocol.*;
import com.couchbase.sdk.utils.ClusterConnection;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SdkOperation performs each requested SDK operation
 */
public class SdkOperationExecutor {
    private Logger logger = LoggerFactory.getLogger(SdkOperationExecutor.class);
    private final AtomicInteger counter = new AtomicInteger(0);
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public com.couchbase.grpc.sdk.protocol.PerfSingleOperationResult run(ClusterConnection connection, SdkWorkload req) {
        PerfSingleOperationResult.Builder singleResult = PerfSingleOperationResult.newBuilder()
                .setInitiated(getTimeNow());

        try {
            performOperation(connection, req.getCommand());
            singleResult.setSdkResult(SdkOperationResult.newBuilder()
                    .setSuccess(true));
        }
        catch (RuntimeException err) {
            singleResult.setSdkResult(SdkOperationResult.newBuilder()
                    .setUnknownException(err.getClass().getSimpleName()));
            logger.warn("Operation failed with {}", err.toString());
        }

        singleResult.setFinished(getTimeNow());
        return singleResult.build();
    }

    private static Timestamp getTimeNow() {
        long millis = System.currentTimeMillis();

        return Timestamp.newBuilder().setSeconds(millis / 1000)
                .setNanos((int) ((millis % 1000) * 1000000)).build();
    }

    private String getDocId(DocLocation location) {
        if (location.hasSpecific()) {
            return location.getSpecific().getId();
        }
        else if (location.hasUuid()) {
            return UUID.randomUUID().toString();
        }
        else if (location.hasPool()) {
            var pool = location.getPool();

            int next;
            if (pool.hasUniform()) {
                next = random.nextInt((int) pool.getPoolSize());
            }
            else if (pool.hasCounter()) {
                next = counter.getAndIncrement() % (int) pool.getPoolSize();
            }
            else {
                throw new IllegalArgumentException("Unrecognised pool selection strategy");
            }

            return pool.getIdPreface() + next;
        }
        else {
            throw new IllegalArgumentException("Unknown doc location type");
        }
    }


    private void performOperation(ClusterConnection connection,
                                  SdkCommand op) {
        if (op.hasInsert()){
            var request = op.getInsert();
            var collection = connection.collection(request.getLocation());
            var content = JsonObject.fromJson(request.getContentJson());
            var docId = getDocId(request.getLocation());

            collection.insert(docId, content);
        } else if(op.hasGet()) {
            var request = op.getGet();
            var collection = connection.collection(request.getLocation());
            var docId = getDocId(request.getLocation());

            collection.get(docId);
        } else if(op.hasRemove()){
            var request = op.getRemove();
            var collection = connection.collection(request.getLocation());
            var docId = getDocId(request.getLocation());

            collection.remove(docId);
        } else if(op.hasReplace()){
            var request = op.getReplace();
            var collection = connection.collection(request.getLocation());
            var docId = getDocId(request.getLocation());

            collection.replace(docId, request.getContentJson());
        } else {
            throw new InternalPerformerFailure(new IllegalArgumentException("Unknown operation"));
        }
    }
}
