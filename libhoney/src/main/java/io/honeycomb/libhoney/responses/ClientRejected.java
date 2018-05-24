package io.honeycomb.libhoney.responses;

import io.honeycomb.libhoney.transport.impl.ConsoleTransport;

/**
 * A response indicating that the event was rejected on the client-side and was never sent to the server.
 * {@link RejectionReason} captures the various reasons why an event might be rejected.
 */
public interface ClientRejected extends Response {
    /**
     * @return the reason for the event having been rejected.
     */
    RejectionReason getReason();

    /**
     * @return exception that may have caused the rejections, null if no exception was the cause.
     */
    Exception getException();

    enum RejectionReason {
        /**
         * Event was not sampled. This is not an error.
         * See {@link io.honeycomb.libhoney.Options.Builder#setSampleRate(int)}.
         */
        NOT_SAMPLED,
        /**
         * The queue of pending events has reached capacity and therefore events are being rejected.
         * This is an indication that the transport layer cannot send events fast enough to process the queue.
         * See {@link io.honeycomb.libhoney.TransportOptions.Builder#setQueueCapacity(int)}.
         */
        QUEUE_OVERFLOW,
        /**
         * A dynamic field supplier ({@link io.honeycomb.libhoney.ValueSupplier}) threw an exception, which indicates a
         * programming error. Inspect the exception for details.
         */
        DYNAMIC_FIELD_RESOLUTION_ERROR,
        /**
         * The {@link io.honeycomb.libhoney.EventPostProcessor} threw an exception, which indicates a programming error.
         * Inspect the exception for details.
         */
        POST_PROCESSING_ERROR,
        /**
         * The http client failed while assembling the HTTP request.
         * Inspect the exception for details.
         */
        REQUEST_BUILD_FAILURE,
        /**
         * Event is not sent to a server as no remote link was set up. This should not happen during normal operation.
         * This is for simple (e.g. {@link ConsoleTransport}) or mocked transports that
         * do not make HTTP requests.
         */
        DEAD_END
    }
}
