package com.couchbase.client.performer.java;

import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.performer.core.commands.SdkCommandExecutor;
import com.couchbase.client.performer.grpc.PerfSdkCommandResult;
import com.couchbase.client.performer.grpc.SdkCommand;
import com.couchbase.client.performer.java.util.ClusterConnection;

import static com.couchbase.client.performer.core.util.TimeUtil.getTimeNow;


/**
 * SdkOperation performs each requested SDK operation
 */
public class JavaSdkCommandExecutor extends SdkCommandExecutor {
    private final ClusterConnection connection;
    
    public JavaSdkCommandExecutor(ClusterConnection connection) {
        this.connection = connection;
    }
    
    @Override
    protected void performOperation(PerfSdkCommandResult.Builder result, SdkCommand op) {
        if (op.hasInsert()){
            var request = op.getInsert();
            var collection = connection.collection(request.getLocation());
            var content = JsonObject.fromJson(request.getContentJson());
            var docId = getDocId(request.getLocation());

            result.setInitiated(getTimeNow());
            long start = System.nanoTime();
            collection.insert(docId, content);
            result.setElapsedNanos(System.nanoTime() - start);
        } else if (op.hasGet()) {
            var request = op.getGet();
            var collection = connection.collection(request.getLocation());
            var docId = getDocId(request.getLocation());

            result.setInitiated(getTimeNow());
            long start = System.nanoTime();
            collection.get(docId);
            result.setElapsedNanos(System.nanoTime() - start);
        } else if (op.hasRemove()){
            var request = op.getRemove();
            var collection = connection.collection(request.getLocation());
            var docId = getDocId(request.getLocation());

            result.setInitiated(getTimeNow());
            long start = System.nanoTime();
            collection.remove(docId);
            result.setElapsedNanos(System.nanoTime() - start);
        } else if (op.hasReplace()){
            var request = op.getReplace();
            var collection = connection.collection(request.getLocation());
            var docId = getDocId(request.getLocation());

            result.setInitiated(getTimeNow());
            long start = System.nanoTime();
            collection.replace(docId, request.getContentJson());
            result.setElapsedNanos(System.nanoTime() - start);
        } else {
            throw new UnsupportedOperationException(new IllegalArgumentException("Unknown operation"));
        }
    }
}
