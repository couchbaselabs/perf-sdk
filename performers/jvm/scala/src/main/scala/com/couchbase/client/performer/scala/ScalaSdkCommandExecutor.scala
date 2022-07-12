package com.couchbase.client.performer.scala

import com.couchbase.client.performer.core.commands.SdkCommandExecutor
import com.couchbase.client.performer.core.util.TimeUtil.getTimeNow
import com.couchbase.client.performer.grpc.{PerfSdkCommandResult, SdkCommand}
import com.couchbase.client.performer.scala.util.ClusterConnection
import com.couchbase.client.scala.json.JsonObject

class ScalaSdkCommandExecutor(val connection: ClusterConnection) extends SdkCommandExecutor {
  override protected def performOperation(result: PerfSdkCommandResult.Builder, op: SdkCommand): Unit = {
    if (op.hasInsert) {
      val request = op.getInsert
      val collection = connection.collection(request.getLocation)
      val content = JsonObject.fromJson(request.getContentJson)
      val docId = getDocId(request.getLocation)
      result.setInitiated(getTimeNow)
      val start = System.nanoTime
      collection.insert(docId, content).get
      result.setElapsedNanos(System.nanoTime - start)
    }
    else if (op.hasGet) {
      val request = op.getGet
      val collection = connection.collection(request.getLocation)
      val docId = getDocId(request.getLocation)
      result.setInitiated(getTimeNow)
      val start = System.nanoTime
      collection.get(docId).get
      result.setElapsedNanos(System.nanoTime - start)
    }
    else if (op.hasRemove) {
      val request = op.getRemove
      val collection = connection.collection(request.getLocation)
      val docId = getDocId(request.getLocation)
      result.setInitiated(getTimeNow)
      val start = System.nanoTime
      collection.remove(docId).get
      result.setElapsedNanos(System.nanoTime - start)
    }
    else if (op.hasReplace) {
      val request = op.getReplace
      val collection = connection.collection(request.getLocation)
      val docId = getDocId(request.getLocation)
      result.setInitiated(getTimeNow)
      val start = System.nanoTime
      collection.replace(docId, request.getContentJson).get
      result.setElapsedNanos(System.nanoTime - start)
    }
    else throw new UnsupportedOperationException(new IllegalArgumentException("Unknown operation"))
  }
}

