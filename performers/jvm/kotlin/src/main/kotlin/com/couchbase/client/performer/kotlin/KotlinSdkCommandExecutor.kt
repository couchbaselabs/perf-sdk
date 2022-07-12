package com.couchbase.client.performer.kotlin

import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.performer.core.commands.SdkCommandExecutor
import com.couchbase.client.performer.core.util.TimeUtil
import com.couchbase.client.performer.grpc.PerfSdkCommandResult
import com.couchbase.client.performer.grpc.SdkCommand
import com.couchbase.client.performer.kotlin.util.ClusterConnection
import kotlinx.coroutines.runBlocking

/**
 * SdkOperation performs each requested SDK operation
 */
class KotlinSdkCommandExecutor(private val connection: ClusterConnection) : SdkCommandExecutor() {
    override fun performOperation(result: PerfSdkCommandResult.Builder, op: SdkCommand) {
        runBlocking {
            if (op.hasInsert()) {
                val request = op.insert
                val collection = connection.collection(request.location)
                val content = JsonObject.fromJson(request.contentJson)
                val docId = getDocId(request.location)
                result.initiated = TimeUtil.getTimeNow()
                val start = System.nanoTime()
                collection.insert(docId, content)
                result.elapsedNanos = System.nanoTime() - start
            } else if (op.hasGet()) {
                val request = op.get
                val collection = connection.collection(request.location)
                val docId = getDocId(request.location)
                result.initiated = TimeUtil.getTimeNow()
                val start = System.nanoTime()
                collection.get(docId)
                result.elapsedNanos = System.nanoTime() - start
            } else if (op.hasRemove()) {
                val request = op.remove
                val collection = connection.collection(request.location)
                val docId = getDocId(request.location)
                result.initiated = TimeUtil.getTimeNow()
                val start = System.nanoTime()
                collection.remove(docId)
                result.elapsedNanos = System.nanoTime() - start
            } else if (op.hasReplace()) {
                val request = op.replace
                val collection = connection.collection(request.location)
                val docId = getDocId(request.location)
                result.initiated = TimeUtil.getTimeNow()
                val start = System.nanoTime()
                collection.replace(docId, request.contentJson)
                result.elapsedNanos = System.nanoTime() - start
            } else {
                throw UnsupportedOperationException(IllegalArgumentException("Unknown operation"))
            }
        }
    }
}