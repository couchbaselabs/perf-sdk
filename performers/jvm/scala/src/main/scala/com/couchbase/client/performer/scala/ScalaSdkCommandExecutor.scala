package com.couchbase.client.performer.scala

import com.couchbase.client.performer.core.commands.SdkCommandExecutor
import com.couchbase.client.performer.core.util.TimeUtil.getTimeNow
import com.couchbase.client.performer.grpc
import com.couchbase.client.performer.grpc.{PerfSdkCommandResult, SdkCommand, SdkCommandGet, SdkCommandInsert, SdkCommandRemove, SdkCommandReplace, SdkCommandUpsert}
import com.couchbase.client.performer.scala.util.ClusterConnection
import com.couchbase.client.scala.durability.{Durability, PersistTo, ReplicateTo}
import com.couchbase.client.scala.durability.Durability.ClientVerified
import com.couchbase.client.scala.json.JsonObject
import com.couchbase.client.scala.kv.{GetOptions, InsertOptions, RemoveOptions, ReplaceOptions, UpsertOptions}

import java.lang
import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.concurrent.duration.{Duration, durationToPair}

class ScalaSdkCommandExecutor(val connection: ClusterConnection) extends SdkCommandExecutor {
  def convertDurability(durability: grpc.Durability): Durability = {
    if (durability.hasDurabilityLevel()) {
      durability.getDurabilityLevel() match {
        case grpc.DurabilityLevel.DURABILITY_NONE => Durability.Disabled
        case grpc.DurabilityLevel.DURABILITY_MAJORITY => Durability.Majority
        case grpc.DurabilityLevel.DURABILITY_MAJORITY_AND_PERSIST_TO_ACTIVE => Durability.MajorityAndPersistToActive
        case grpc.DurabilityLevel.DURABILITY_PERSIST_TO_MAJORITY => Durability.PersistToMajority
        case _ => throw new UnsupportedOperationException("Unknown durability level")
      }
    }
    else if (durability.hasObserve) {
      Durability.ClientVerified(durability.getObserve.getReplicateTo match {
        case grpc.ReplicateTo.REPLICATE_TO_NONE => ReplicateTo.None
        case grpc.ReplicateTo.REPLICATE_TO_ONE => ReplicateTo.One
        case grpc.ReplicateTo.REPLICATE_TO_TWO => ReplicateTo.Two
        case grpc.ReplicateTo.REPLICATE_TO_THREE => ReplicateTo.Three
        case _ => throw new UnsupportedOperationException("Unknown replicateTo level")
      }, durability.getObserve.getPersistTo match {
        case grpc.PersistTo.PERSIST_TO_NONE => PersistTo.None
        case grpc.PersistTo.PERSIST_TO_ACTIVE => PersistTo.Active
        case grpc.PersistTo.PERSIST_TO_ONE => PersistTo.One
        case grpc.PersistTo.PERSIST_TO_TWO => PersistTo.Two
        case grpc.PersistTo.PERSIST_TO_THREE => PersistTo.Three
        case grpc.PersistTo.PERSIST_TO_FOUR => PersistTo.Four
        case _ => throw new UnsupportedOperationException("Unknown persistTo level")
      })
    }
    else {
      throw new UnsupportedOperationException("Unknown durability")
    }
  }

  def convertExpiry(expiry: grpc.Expiry): Either[Instant, Duration] = {
    if (expiry.hasAbsoluteEpochSecs) {
      Left(Instant.ofEpochSecond(expiry.getAbsoluteEpochSecs))
    }
    else if (expiry.hasRelativeSecs) {
      Right(Duration.create(expiry.getRelativeSecs, TimeUnit.SECONDS))
    }
    else {
      throw new UnsupportedOperationException("Unknown expiry")
    }
  }

  override protected def performOperation(result: PerfSdkCommandResult.Builder, op: SdkCommand): Unit = {
    if (op.hasInsert) {
      val request = op.getInsert
      val collection = connection.collection(request.getLocation)
      val content = JsonObject.fromJson(request.getContentJson)
      val docId = getDocId(request.getLocation)
      val options = createOptions(request)
      result.setInitiated(getTimeNow)
      val start = System.nanoTime
      if (options == null) collection.insert(docId, content).get
      else collection.insert(docId, content, options).get
      result.setElapsedNanos(System.nanoTime - start)
    }
    else if (op.hasGet) {
      val request = op.getGet
      val collection = connection.collection(request.getLocation)
      val docId = getDocId(request.getLocation)
      val options = createOptions(request)
      result.setInitiated(getTimeNow)
      val start = System.nanoTime
      if (options == null) collection.get(docId).get
      else collection.get(docId, options).get
      result.setElapsedNanos(System.nanoTime - start)
    }
    else if (op.hasRemove) {
      val request = op.getRemove
      val collection = connection.collection(request.getLocation)
      val docId = getDocId(request.getLocation)
      val options = createOptions(request)
      result.setInitiated(getTimeNow)
      val start = System.nanoTime
      if (options == null) collection.remove(docId).get
      else collection.remove(docId, options).get
      result.setElapsedNanos(System.nanoTime - start)
    }
    else if (op.hasReplace) {
      val request = op.getReplace
      val collection = connection.collection(request.getLocation)
      val docId = getDocId(request.getLocation)
      val options = createOptions(request)
      result.setInitiated(getTimeNow)
      val start = System.nanoTime
      if (options == null) collection.replace(docId, request.getContentJson).get
      else collection.replace(docId, request.getContentJson, options).get
      result.setElapsedNanos(System.nanoTime - start)
    }
    else if (op.hasUpsert) {
      val request = op.getUpsert
      val collection = connection.collection(request.getLocation)
      val docId = getDocId(request.getLocation)
      val options = createOptions(request)
      result.setInitiated(getTimeNow)
      val start = System.nanoTime
      if (options == null) collection.upsert(docId, request.getContentJson).get
      else collection.upsert(docId, request.getContentJson, options).get
      result.setElapsedNanos(System.nanoTime - start)
    }
    else throw new UnsupportedOperationException(new IllegalArgumentException("Unknown operation"))
  }

  private def createOptions(request: SdkCommandInsert) = {
    if (request.hasOptions) {
      val opts = request.getOptions
      var out = InsertOptions()
      if (opts.hasTimeoutMsecs) out = out.timeout(Duration.create(opts.getTimeoutMsecs, TimeUnit.MILLISECONDS))
      if (opts.hasDurability) out = out.durability(convertDurability(opts.getDurability))
      if (opts.hasExpiry) out = convertExpiry(opts.getExpiry) match {
        case Left(expiry) => out.expiry(expiry)
        case Right(expiry) => out.expiry(expiry)
      }
      out
    }
    else null
  }

  private def createOptions(request: SdkCommandRemove) = {
    if (request.hasOptions) {
      val opts = request.getOptions
      var out = RemoveOptions()
      if (opts.hasTimeoutMsecs) out = out.timeout(Duration.create(opts.getTimeoutMsecs, TimeUnit.MILLISECONDS))
      if (opts.hasDurability) out = out.durability(convertDurability(opts.getDurability))
      if (opts.hasCas) out = out.cas(opts.getCas)
      out
    }
    else null
  }

  private def createOptions(request: SdkCommandGet) = {
    if (request.hasOptions) {
      val opts = request.getOptions
      var out = GetOptions()
      if (opts.hasTimeoutMsecs) out = out.timeout(Duration.create(opts.getTimeoutMsecs, TimeUnit.MILLISECONDS))
      if (opts.hasWithExpiry) out = out.withExpiry(opts.getWithExpiry)
      if (opts.getProjectionCount > 0) out = out.project(opts.getProjectionList.asByteStringList().toSeq.map(v => v.toString))
      out
    }
    else null
  }

  private def createOptions(request: SdkCommandReplace) = {
    if (request.hasOptions) {
      val opts = request.getOptions
      var out = ReplaceOptions()
      if (opts.hasTimeoutMsecs) out = out.timeout(Duration.create(opts.getTimeoutMsecs, TimeUnit.MILLISECONDS))
      if (opts.hasDurability) out = out.durability(convertDurability(opts.getDurability))
      if (opts.hasExpiry) out = convertExpiry(opts.getExpiry) match {
        case Left(expiry) => out.expiry(expiry)
        case Right(expiry) => out.expiry(expiry)
      }
      if (opts.hasPreserveExpiry) out = out.preserveExpiry(opts.getPreserveExpiry)
      if (opts.hasCas) out = out.cas(opts.getCas)
      out
    }
    else null
  }

  private def createOptions(request: SdkCommandUpsert) = {
    if (request.hasOptions) {
      val opts = request.getOptions
      var out = UpsertOptions()
      if (opts.hasTimeoutMsecs) out = out.timeout(Duration.create(opts.getTimeoutMsecs, TimeUnit.MILLISECONDS))
      if (opts.hasDurability) out = out.durability(convertDurability(opts.getDurability))
      if (opts.hasExpiry) out = convertExpiry(opts.getExpiry) match {
        case Left(expiry) => out.expiry(expiry)
        case Right(expiry) => out.expiry(expiry)
      }
      if (opts.hasPreserveExpiry) out = out.preserveExpiry(opts.getPreserveExpiry)
      out
    }
    else null
  }
}

