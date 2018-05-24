package io.honeycomb.libhoney.transport.batch.impl;

import io.honeycomb.libhoney.transport.batch.ClockProvider;

/**
 * Returns the current time as reported by the system clock.
 */
public class SystemClockProvider implements ClockProvider {
    private static final SystemClockProvider INSTANCE = new SystemClockProvider();

    public static ClockProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public long getWallTime() {
        return System.currentTimeMillis();
    }

    @Override
    public long getMonotonicTime() {
        return System.nanoTime();
    }
}
