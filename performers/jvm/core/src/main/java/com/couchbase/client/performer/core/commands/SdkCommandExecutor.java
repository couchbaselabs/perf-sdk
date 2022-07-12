package com.couchbase.client.performer.core.commands;

import com.couchbase.client.performer.grpc.DocLocation;
import com.couchbase.client.performer.grpc.PerfSdkCommandResult;
import com.couchbase.client.performer.grpc.SdkCommand;
import com.couchbase.client.performer.grpc.SdkCommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.couchbase.client.performer.core.util.TimeUtil.getTimeNow;

public abstract class SdkCommandExecutor {
    protected final Logger logger = LoggerFactory.getLogger(SdkCommandExecutor.class);
    private final AtomicInteger counter = new AtomicInteger(0);
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    abstract protected void performOperation(PerfSdkCommandResult.Builder result, SdkCommand op);

    public PerfSdkCommandResult run(SdkCommand command) {
        var result = PerfSdkCommandResult.newBuilder()
                .setInitiated(getTimeNow());

        try {
            performOperation(result, command);

            result.setSdkResult(SdkCommandResult.newBuilder()
                    .setSuccess(true));
            return result.build();
        }
        catch (RuntimeException err) {
            result.setSdkResult(SdkCommandResult.newBuilder()
                    .setUnknownException(err.getClass().getSimpleName()));
            logger.warn("Operation failed with {}", err.toString());
        }

        return result.build();
    }

    protected String getDocId(DocLocation location) {
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
                throw new UnsupportedOperationException("Unrecognised pool selection strategy");
            }

            return pool.getIdPreface() + next;
        }
        else {
            throw new UnsupportedOperationException("Unknown doc location type");
        }
    }
}
