package com.couchbase.client.performer.java;

import com.couchbase.client.core.msg.kv.DurabilityLevel;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.CommonDurabilityOptions;
import com.couchbase.client.java.kv.GetOptions;
import com.couchbase.client.java.kv.InsertOptions;
import com.couchbase.client.java.kv.PersistTo;
import com.couchbase.client.java.kv.RemoveOptions;
import com.couchbase.client.java.kv.ReplaceOptions;
import com.couchbase.client.java.kv.ReplicateTo;
import com.couchbase.client.java.kv.UpsertOptions;
import com.couchbase.client.performer.core.commands.SdkCommandExecutor;
import com.couchbase.client.performer.grpc.Durability;
import com.couchbase.client.performer.grpc.PerfSdkCommandResult;
import com.couchbase.client.performer.grpc.SdkCommand;
import com.couchbase.client.performer.grpc.SdkCommandGet;
import com.couchbase.client.performer.grpc.SdkCommandInsert;
import com.couchbase.client.performer.grpc.SdkCommandRemove;
import com.couchbase.client.performer.grpc.SdkCommandReplace;
import com.couchbase.client.performer.grpc.SdkCommandUpsert;
import com.couchbase.client.performer.java.util.ClusterConnection;

import javax.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

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

    private static @Nullable InsertOptions createOptions(SdkCommandInsert request) {
        if (request.hasOptions()) {
            var opts = request.getOptions();
            var out = InsertOptions.insertOptions();   
            if (opts.hasTimeoutMsecs()) out.timeout(Duration.ofMillis(opts.getTimeoutMsecs()));
            if (opts.hasDurability()) convertDurability(opts.getDurability(), out);
            if (opts.hasExpiry()) {
                if (opts.getExpiry().hasAbsoluteEpochSecs()) out.expiry(Instant.ofEpochSecond(opts.getExpiry().getAbsoluteEpochSecs()));
                else if (opts.getExpiry().hasRelativeSecs()) out.expiry(Duration.ofSeconds(opts.getExpiry().getRelativeSecs()));
                else throw new UnsupportedOperationException("Unknown expiry");
            }
            return out;
        }
        else return null;
    }

    private static @Nullable RemoveOptions createOptions(SdkCommandRemove request) {
        if (request.hasOptions()) {
            var opts = request.getOptions();
            var out = RemoveOptions.removeOptions();
            if (opts.hasTimeoutMsecs()) out.timeout(Duration.ofMillis(opts.getTimeoutMsecs()));
            if (opts.hasDurability()) convertDurability(opts.getDurability(), out);
            if (opts.hasCas()) out.cas(opts.getCas());
            return out;
        }
        else return null;
    }

    private static @Nullable GetOptions createOptions(SdkCommandGet request) {
        if (request.hasOptions()) {
            var opts = request.getOptions();
            var out = GetOptions.getOptions();
            if (opts.hasTimeoutMsecs()) out.timeout(Duration.ofMillis(opts.getTimeoutMsecs()));
            if (opts.hasWithExpiry()) out.withExpiry(opts.getWithExpiry());
            if (opts.getProjectionCount() > 0) out.project(opts.getProjectionList().stream().toList());
            return out;
        }
        else return null;
    }

    private static @Nullable ReplaceOptions createOptions(SdkCommandReplace request) {
        if (request.hasOptions()) {
            var opts = request.getOptions();
            var out = ReplaceOptions.replaceOptions();
            if (opts.hasTimeoutMsecs()) out.timeout(Duration.ofMillis(opts.getTimeoutMsecs()));
            if (opts.hasDurability()) convertDurability(opts.getDurability(), out);
            if (opts.hasExpiry()) {
                if (opts.getExpiry().hasAbsoluteEpochSecs()) out.expiry(Instant.ofEpochSecond(opts.getExpiry().getAbsoluteEpochSecs()));
                else if (opts.getExpiry().hasRelativeSecs()) out.expiry(Duration.ofSeconds(opts.getExpiry().getRelativeSecs()));
                else throw new UnsupportedOperationException("Unknown expiry");
            }
            if (opts.hasPreserveExpiry()) out.preserveExpiry(opts.getPreserveExpiry());
            if (opts.hasCas()) out.cas(opts.getCas());
            return out;
        }
        else return null;
    }

    private static @Nullable UpsertOptions createOptions(SdkCommandUpsert request) {
        if (request.hasOptions()) {
            var opts = request.getOptions();
            var out = UpsertOptions.upsertOptions();
            if (opts.hasTimeoutMsecs()) out.timeout(Duration.ofMillis(opts.getTimeoutMsecs()));
            if (opts.hasDurability()) convertDurability(opts.getDurability(), out);
            if (opts.hasExpiry()) {
                if (opts.getExpiry().hasAbsoluteEpochSecs()) out.expiry(Instant.ofEpochSecond(opts.getExpiry().getAbsoluteEpochSecs()));
                else if (opts.getExpiry().hasRelativeSecs()) out.expiry(Duration.ofSeconds(opts.getExpiry().getRelativeSecs()));
                else throw new UnsupportedOperationException("Unknown expiry");
            }
            if (opts.hasPreserveExpiry()) out.preserveExpiry(opts.getPreserveExpiry());
            return out;
        }
        else return null;
    }
    
    private static void convertDurability(com.couchbase.client.performer.grpc.Durability durability, CommonDurabilityOptions options) {
        if (durability.hasDurabilityLevel()) {
            options.durability(switch (durability.getDurabilityLevel()) {
                case DURABILITY_NONE -> DurabilityLevel.NONE;
                case DURABILITY_MAJORITY -> DurabilityLevel.MAJORITY;
                case DURABILITY_MAJORITY_AND_PERSIST_TO_ACTIVE -> DurabilityLevel.MAJORITY_AND_PERSIST_TO_ACTIVE;
                case DURABILITY_PERSIST_TO_MAJORITY -> DurabilityLevel.PERSIST_TO_MAJORITY;
                default -> throw new UnsupportedOperationException("Unexpected value: " + durability.getDurabilityLevel());
            });
        }
        else if (durability.hasObserve()) {
            options.durability(switch (durability.getObserve().getPersistTo()) {
                        case PERSIST_TO_NONE -> PersistTo.NONE;
                        case PERSIST_TO_ACTIVE -> PersistTo.ACTIVE;
                        case PERSIST_TO_ONE -> PersistTo.ONE;
                        case PERSIST_TO_TWO -> PersistTo.TWO;
                        case PERSIST_TO_THREE -> PersistTo.THREE;
                        case PERSIST_TO_FOUR -> PersistTo.FOUR;
                        default -> throw new UnsupportedOperationException("Unexpected value: " + durability.getDurabilityLevel());
                    }, switch (durability.getObserve().getReplicateTo()) {
                        case REPLICATE_TO_NONE -> ReplicateTo.NONE;
                        case REPLICATE_TO_ONE -> ReplicateTo.ONE;
                        case REPLICATE_TO_TWO -> ReplicateTo.TWO;
                        case REPLICATE_TO_THREE -> ReplicateTo.THREE;
                        default -> throw new UnsupportedOperationException("Unexpected value: " + durability.getDurabilityLevel());
                    });
        }
        else {
            throw new UnsupportedOperationException("Unknown durability");
        }
    }
}
