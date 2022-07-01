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
        var result = PerfSingleOperationResult.newBuilder()
                .setInitiated(getTimeNow());

        try {
            performOperation(result, connection, req.getCommand());
            result.setSdkResult(SdkOperationResult.newBuilder()
                    .setSuccess(true));
            return result.build();
        }
        catch (RuntimeException err) {
            result.setSdkResult(SdkOperationResult.newBuilder()
                    .setUnknownException(err.getClass().getSimpleName()));
            logger.warn("Operation failed with {}", err.toString());
        }

        return result.build();
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


    private void performOperation(PerfSingleOperationResult.Builder result,
                                  ClusterConnection connection,
                                  SdkCommand op) {
        if (op.hasInsert()){
            var request = op.getInsert();
            var collection = connection.collection(request.getLocation());
            var content = JsonObject.fromJson(request.getContentJson());
            var docId = getDocId(request.getLocation());

            result.setInitiated(getTimeNow());
            long start = System.nanoTime();
            collection.insert(docId, content);
            result.setElapsedNanos(System.nanoTime() - start);
        } else if(op.hasGet()) {
            var request = op.getGet();
            var collection = connection.collection(request.getLocation());
            var docId = getDocId(request.getLocation());

            result.setInitiated(getTimeNow());
            long start = System.nanoTime();
            collection.get(docId);
            result.setElapsedNanos(System.nanoTime() - start);
        } else if(op.hasRemove()){
            var request = op.getRemove();
            var collection = connection.collection(request.getLocation());
            var docId = getDocId(request.getLocation());

            result.setInitiated(getTimeNow());
            long start = System.nanoTime();
            collection.remove(docId);
            result.setElapsedNanos(System.nanoTime() - start);
        } else if(op.hasReplace()){
            var request = op.getReplace();
            var collection = connection.collection(request.getLocation());
            var docId = getDocId(request.getLocation());

            result.setInitiated(getTimeNow());
            long start = System.nanoTime();
            collection.replace(docId, request.getContentJson());
            result.setElapsedNanos(System.nanoTime() - start);
        } else {
            throw new InternalPerformerFailure(new IllegalArgumentException("Unknown operation"));
        }
    }
}
