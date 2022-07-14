package com.couchbase.client.performer.kotlin

import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.kotlin.CommonOptions
import com.couchbase.client.kotlin.kv.Durability
import com.couchbase.client.kotlin.kv.Expiry
import com.couchbase.client.kotlin.kv.PersistTo
import com.couchbase.client.kotlin.kv.ReplicateTo
import com.couchbase.client.performer.core.commands.SdkCommandExecutor
import com.couchbase.client.performer.core.util.TimeUtil
import com.couchbase.client.performer.grpc.PerfSdkCommandResult
import com.couchbase.client.performer.grpc.SdkCommand
import com.couchbase.client.performer.kotlin.util.ClusterConnection
import kotlinx.coroutines.runBlocking
import java.time.Instant
import kotlin.math.exp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * SdkOperation performs each requested SDK operation
 */
class KotlinSdkCommandExecutor(private val connection: ClusterConnection) : SdkCommandExecutor() {
    fun createCommon(hasTimeout: Boolean, timeout: Int): CommonOptions {
        if (hasTimeout) {
            return CommonOptions(timeout = timeout.milliseconds)
        }
        else {
            return CommonOptions.Default
        }
    }

    fun convertDurability(hasDurability: Boolean, durability: com.couchbase.client.performer.grpc.Durability): Durability {
        if (hasDurability) {
            if (durability.hasDurabilityLevel()) {
                return when (durability.getDurabilityLevel()) {
                    com.couchbase.client.performer.grpc.DurabilityLevel.DURABILITY_NONE -> Durability.none()
                    com.couchbase.client.performer.grpc.DurabilityLevel.DURABILITY_MAJORITY -> Durability.majority()
                    com.couchbase.client.performer.grpc.DurabilityLevel.DURABILITY_MAJORITY_AND_PERSIST_TO_ACTIVE -> Durability.majorityAndPersistToActive()
                    com.couchbase.client.performer.grpc.DurabilityLevel.DURABILITY_PERSIST_TO_MAJORITY -> Durability.persistToMajority()
                    else -> throw UnsupportedOperationException("Unknown durability")
                }
            }
            else if (durability.hasObserve()) {
                return Durability.clientVerified(when (durability.getObserve().getPersistTo()) {
                    com.couchbase.client.performer.grpc.PersistTo.PERSIST_TO_NONE -> PersistTo.NONE
                    com.couchbase.client.performer.grpc.PersistTo.PERSIST_TO_ACTIVE -> PersistTo.ACTIVE
                    com.couchbase.client.performer.grpc.PersistTo.PERSIST_TO_ONE -> PersistTo.ONE
                    com.couchbase.client.performer.grpc.PersistTo.PERSIST_TO_TWO -> PersistTo.TWO
                    com.couchbase.client.performer.grpc.PersistTo.PERSIST_TO_THREE -> PersistTo.THREE
                    com.couchbase.client.performer.grpc.PersistTo.PERSIST_TO_FOUR -> PersistTo.FOUR
                    else -> throw UnsupportedOperationException("Unknown durability")
                }, when (durability.getObserve().getReplicateTo()) {
                    com.couchbase.client.performer.grpc.ReplicateTo.REPLICATE_TO_NONE -> ReplicateTo.NONE
                    com.couchbase.client.performer.grpc.ReplicateTo.REPLICATE_TO_ONE -> ReplicateTo.ONE
                    com.couchbase.client.performer.grpc.ReplicateTo.REPLICATE_TO_TWO -> ReplicateTo.TWO
                    com.couchbase.client.performer.grpc.ReplicateTo.REPLICATE_TO_THREE -> ReplicateTo.THREE
                    else -> throw UnsupportedOperationException("Unknown durability")
                })
            }
            else {
                throw UnsupportedOperationException("Unknown durability")
            }
        }
        else {
            return Durability.None
        }
    }

    fun convertExpiry(hasExpiry: Boolean, expiry: com.couchbase.client.performer.grpc.Expiry): Expiry {
        if (hasExpiry) {
            if (expiry.hasAbsoluteEpochSecs()) {
                return Expiry.of(Instant.ofEpochSecond(expiry.absoluteEpochSecs))
            }
            else if (expiry.hasRelativeSecs()) {
                return Expiry.of(expiry.relativeSecs.seconds)
            }
            else {
                throw UnsupportedOperationException("Unknown expiry")
            }
        }
        else {
            return Expiry.None
        }
    }

    override fun performOperation(result: PerfSdkCommandResult.Builder, op: SdkCommand) {
        runBlocking {
            if (op.hasInsert()) {
                val request = op.insert
                val collection = connection.collection(request.location)
                val content = JsonObject.fromJson(request.contentJson)
                val docId = getDocId(request.location)
                result.initiated = TimeUtil.getTimeNow()
                val start = System.nanoTime()
                if (request.hasOptions()) {
                    val options = request.options
                    collection.insert(docId, content,
                        common = createCommon(options.hasTimeoutMsecs(), options.timeoutMsecs),
                        durability = convertDurability(options.hasDurability(), options.durability),
                        expiry = convertExpiry(options.hasExpiry(), options.expiry))
                }
                else collection.insert(docId, content)
                result.elapsedNanos = System.nanoTime() - start
            } else if (op.hasGet()) {
                val request = op.get
                val collection = connection.collection(request.location)
                val docId = getDocId(request.location)
                result.initiated = TimeUtil.getTimeNow()
                val start = System.nanoTime()
                if (request.hasOptions()) {
                    val options = request.options
                    collection.get(docId,
                        common = createCommon(options.hasTimeoutMsecs(), options.timeoutMsecs),
                        withExpiry = if (options.hasWithExpiry()) options.hasWithExpiry() else false,
                        project = options.projectionList.toList())
                }
                else collection.get(docId)
                result.elapsedNanos = System.nanoTime() - start
            } else if (op.hasRemove()) {
                val request = op.remove
                val collection = connection.collection(request.location)
                val docId = getDocId(request.location)
                result.initiated = TimeUtil.getTimeNow()
                val start = System.nanoTime()
                if (request.hasOptions()) {
                    val options = request.options
                    collection.remove(docId,
                        common = createCommon(options.hasTimeoutMsecs(), options.timeoutMsecs),
                        durability = convertDurability(options.hasDurability(), options.durability),
                        cas = if (options.hasCas()) options.cas else 0)
                }
                else collection.remove(docId)
                result.elapsedNanos = System.nanoTime() - start
            } else if (op.hasReplace()) {
                val request = op.replace
                val collection = connection.collection(request.location)
                val docId = getDocId(request.location)
                result.initiated = TimeUtil.getTimeNow()
                val start = System.nanoTime()
                if (request.hasOptions()) {
                    val options = request.options
                    collection.replace(docId, request.contentJson,
                        common = createCommon(options.hasTimeoutMsecs(), options.timeoutMsecs),
                        durability = convertDurability(options.hasDurability(), options.durability),
                        expiry = convertExpiry(options.hasExpiry(), options.expiry),
                        preserveExpiry = if (options.hasPreserveExpiry()) options.preserveExpiry else false,
                        cas = if (options.hasCas()) options.cas else 0)
                }
                else collection.replace(docId, request.contentJson)
                result.elapsedNanos = System.nanoTime() - start
            } else if (op.hasUpsert()) {
                val request = op.upsert
                val collection = connection.collection(request.location)
                val docId = getDocId(request.location)
                result.initiated = TimeUtil.getTimeNow()
                val start = System.nanoTime()
                if (request.hasOptions()) {
                    val options = request.options
                    collection.upsert(docId, request.contentJson,
                        common = createCommon(options.hasTimeoutMsecs(), options.timeoutMsecs),
                        durability = convertDurability(options.hasDurability(), options.durability),
                        expiry = convertExpiry(options.hasExpiry(), options.expiry),
                        preserveExpiry = if (options.hasPreserveExpiry()) options.preserveExpiry else false)
                }
                else collection.upsert(docId, request.contentJson)
                result.elapsedNanos = System.nanoTime() - start
            } else {
                throw UnsupportedOperationException(IllegalArgumentException("Unknown operation"))
            }
        }
    }
}