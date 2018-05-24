package io.honeycomb.libhoney.transport.batch;

/**
 * Simple interface to encapsulate the provision of timestamp.
 * Useful to switch out the real clock with a mock clock in testing.
 */
public interface ClockProvider {
    /**
     * @return the current time in millis (since the epoch).
     */
    long getWallTime();

    /**
     * Used for measuring elapsed time, not system or wall-clock time.
     * @return time in nanoseconds
     */
    long getMonotonicTime();
}
