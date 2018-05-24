package io.honeycomb.libhoney.responses;

import io.honeycomb.libhoney.Metrics;

import java.util.Map;

/**
 * Common type for all responses from the HoneyClient, which can be consumed via the
 * {@link io.honeycomb.libhoney.ResponseObserver}.
 */
public interface Response {
    /**
     * The event metadata that was originally passed in with the event, which can be used to track and link back to the
     * original send. This is not modified by the client and not sent to the HoneyComb server.
     *
     * @return the metadata map - may be empty.
     */
    Map<String, Object> getEventMetadata();

    /**
     * Metrics that may have been collected during the lifetime of the event.
     * These metrics will only be complete if a response from the honeycomb server was received.
     *
     * @return a metrics holder.
     */
    Metrics getMetrics();

    /**
     * Human readable message explaining the response.
     *
     * @return response message.
     */
    String getMessage();
}
