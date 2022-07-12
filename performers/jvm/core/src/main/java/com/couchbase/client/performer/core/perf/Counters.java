package com.couchbase.client.performer.core.perf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Counters {
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public AtomicInteger getCounter(String counterId, int initialCount) {
        return counters.compute(counterId, (k, v) -> {
            if (v == null) return new AtomicInteger(initialCount);
            return v;
        });
    }
}
