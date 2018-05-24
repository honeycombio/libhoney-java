package io.honeycomb.libhoney;

import io.honeycomb.libhoney.transport.batch.ClockProvider;
import io.honeycomb.libhoney.transport.batch.impl.SystemClockProvider;

import static io.honeycomb.libhoney.TransportOptions.Builder;

/**
 * Records measurements of elapsed time (in nanoseconds) for an event that has been submitted to the
 * {@link HoneyClient}.
 * These metrics are accessible by registering a {@link ResponseObserver} and consuming a response for an event.
 * The measurements may be incomplete, in which case a value of -1 will be returned.
 * For example, an event that was not sampled will not contain any measurements.
 */
public final class Metrics {
    /**
     * Indicates that the timing is not available.
     */
    public static final long ABSENT_TIME = -1L;

    private final ClockProvider clock;

    private volatile Long enqueueTime;
    private volatile Long startOfHttpRequestTime;
    private volatile Long endOfHttpRequestTime;

    // explicit init to null for clarity.
    @SuppressWarnings("PMD.NullAssignment")
    private Metrics(final ClockProvider clock) {
        this.clock = clock;
        this.enqueueTime = null;
        this.startOfHttpRequestTime = null;
        this.endOfHttpRequestTime = null;
    }

    /**
     * Create an empty Metrics instance using a particular {@link ClockProvider} implementation.
     * Overriding the clock may be useful for testing.
     *
     * @param clock to use as a timesource.
     * @return the empty metrics instance.
     */
    public static Metrics create(final ClockProvider clock) {
        return new Metrics(clock);
    }

    /**
     * Create an empty Metrics instance using the default system clock implementation.
     *
     * @return the empty metrics instance.
     */
    public static Metrics create() {
        return new Metrics(SystemClockProvider.getInstance());
    }

    /**
     * Used by the internals to record the time at which the {@link HoneyClient} accepted the event.
     */
    public void markEnqueueTime() {
        this.enqueueTime = clock.getMonotonicTime();
    }

    /**
     * Used by the {@link HoneyClient} internals to record the time at which the HTTP POST request was submitted to
     * the HTTP client.
     */
    public void markStartOfHttpRequest() {
        this.startOfHttpRequestTime = clock.getMonotonicTime();
    }

    /**
     * Used by the {@link HoneyClient} internals to record the time at which the HTTP POST response was received.
     */
    public void markEndOfHttpRequest() {
        this.endOfHttpRequestTime = clock.getMonotonicTime();
    }

    /**
     * Get the elapsed time from the event being accepted by the {@link HoneyClient} until the event was submitted to
     * the HTTP client as part of a batch request.
     * Under normal circumstances this should be close or under the configured
     * {@link Builder#setBatchTimeoutMillis(long)}, but if the HTTP client is overloaded it's backpressure might lead
     * to an increase in this measurement.
     *
     * @return the total elapsed time in nanoseconds, or -1 if the metric is not available.
     * @see Builder#setBatchSize(int)
     * @see Builder#setBatchTimeoutMillis(long)
     * @see Builder#setQueueCapacity(int)
     */
    public long getQueueDuration() {
        if ((enqueueTime == null) || (startOfHttpRequestTime == null)) {
            return ABSENT_TIME;
        }
        return startOfHttpRequestTime - enqueueTime;
    }

    /**
     * Get the total elapsed time from the event being accepted by the {@link HoneyClient} until the server responded
     * to the HTTP request containing the event.
     * This is equal to {@link #getQueueDuration()} plus {@link #getHttpRequestDuration()} (on the assumption that both
     * metrics are available).
     *
     * @return the total elapsed time in nanoseconds, or -1 if the metric is not available.
     */
    public long getTotalDuration() {
        if ((enqueueTime == null) || (endOfHttpRequestTime == null)) {
            return ABSENT_TIME;
        }
        return endOfHttpRequestTime - enqueueTime;
    }

    /**
     * Get the time it took for the server to respond to the batch request.
     * The timer starts from when the event's batch is handed to the HTTP client up to when it responds with a status
     * code. This means that exceptional completions of the request (e.g. due to networking issues) are not measured.
     * <p>
     * A high duration could be an indication of network latency, but also of the HTTP client being overloaded
     * (i.e. if it takes a long time for the event's batch to lease an HTTP connection).
     *
     * @return the total elapsed time in nanoseconds that the HTTP request to the server took to process, or -1 if the
     * metric is not available.
     * @see Builder#setMaxConnectionsPerApiHost(int)
     * @see Builder#setMaxConnectionsPerApiHost(int)
     * @see Builder#setMaximumPendingBatchRequests(int)
     */
    public long getHttpRequestDuration() {
        if ((startOfHttpRequestTime == null) || (endOfHttpRequestTime == null)) {
            return ABSENT_TIME;
        }
        return endOfHttpRequestTime - startOfHttpRequestTime;
    }

    @Override
    public String toString() {
        return "Metrics{" +
            "clock=" + clock +
            ", enqueueTime=" + enqueueTime +
            ", startOfHttpRequestTime=" + startOfHttpRequestTime +
            ", endOfHttpRequestTime=" + endOfHttpRequestTime +
            '}';
    }
}
