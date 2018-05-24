package io.honeycomb.libhoney;

import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.transport.batch.impl.SystemClockProvider;

import java.net.URI;
import java.util.Collections;
import java.util.Date;

public class TestUtils {

    public static ResolvedEvent createTestEvent() {
        return new ResolvedEvent(
            URI.create("http://example.com"),
            "testkey",
            "testset",
            1,
            5555L,
            Collections.<String, Object>emptyMap(),
            Collections.<String, Object>emptyMap(),
            SystemClockProvider.getInstance());
    }

    public static ResolvedEvent createTestEvent(final int sampleRate) {
        return new ResolvedEvent(
            URI.create("http://example.com"),
            "testkey",
            "testset",
            sampleRate,
            5555L,
            Collections.<String, Object>emptyMap(),
            Collections.<String, Object>emptyMap(),
            SystemClockProvider.getInstance());
    }

    public static ResolvedEvent createTestEvent(final Date timestamp) {
        return new ResolvedEvent(
            URI.create("http://example.com"),
            "testkey",
            "testset",
            1,
            timestamp.getTime(),
            Collections.<String, Object>emptyMap(),
            Collections.<String, Object>emptyMap(),
            SystemClockProvider.getInstance());
    }
}
