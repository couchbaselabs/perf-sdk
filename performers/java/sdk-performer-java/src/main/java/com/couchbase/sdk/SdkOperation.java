package com.couchbase.sdk;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.grpc.sdk.protocol.*;
import com.couchbase.sdk.utils.ClusterConnection;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SdkOperation performs each requested SDK operation
 */
public class SdkOperation {
    private AtomicInteger removeCounter;
    private AtomicInteger replaceCounter;
    private AtomicInteger getCounter;

    public SdkOperation(AtomicInteger removeCounter, AtomicInteger replaceCounter, AtomicInteger getCounter){
        this.removeCounter = removeCounter;
        this.replaceCounter = replaceCounter;
        this.getCounter = getCounter;
    }

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
        }

        singleResult.setFinished(getTimeNow());
        return singleResult.build();
    }

    private static Timestamp getTimeNow() {
        long millis = System.currentTimeMillis();

        return Timestamp.newBuilder().setSeconds(millis / 1000)
                .setNanos((int) ((millis % 1000) * 1000000)).build();
    }

    private void performOperation(ClusterConnection connection,
                                  SdkCommand op) {
        if (op.hasInsert()){
            final CommandInsert request = op.getInsert();
            final Collection collection = connection.collection(request.getLocation());
            JsonObject content = JsonObject.fromJson(request.getContentJson());
            collection.insert(UUID.randomUUID().toString(), content);
        } else if(op.hasGet()) {
            final CommandGet request = op.getGet();
            final Collection collection = connection.collection(request.getLocation());
            collection.get(request.getKeyPreface() + getCounter.getAndIncrement());
        } else if(op.hasRemove()){
            final CommandRemove request = op.getRemove();
            final Collection collection = connection.collection(request.getLocation());
            collection.remove(request.getKeyPreface() + removeCounter.getAndIncrement());
        } else if(op.hasReplace()){
            final CommandReplace request = op.getReplace();
            final Collection collection = connection.collection(request.getLocation());
            collection.replace(request.getKeyPreface() + replaceCounter.getAndIncrement(), request.getContentJson());
        } else {
            throw new InternalPerformerFailure(new IllegalArgumentException("Unknown operation"));
        }
    }
}
