package com.couchbase.client.performer.core.util;

import com.google.protobuf.Timestamp;

public class TimeUtil {
    public static Timestamp getTimeNow() {
        long millis = System.currentTimeMillis();

        return Timestamp.newBuilder().setSeconds(millis / 1000)
                .setNanos((int) ((millis % 1000) * 1000000)).build();
    }
}
