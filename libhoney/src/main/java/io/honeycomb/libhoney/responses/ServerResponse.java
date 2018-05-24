package io.honeycomb.libhoney.responses;

/**
 * Supertype for event responses that have reached the honeycomb server and therefore have an associated HTTP response.
 *
 * See https://honeycomb.io/docs/reference/api/ for a detailed view of the batching API responses.
 */
public interface ServerResponse extends Response {
    int EVENT_STATUS_NOT_AVAILABLE = -1;

    /**
     * The event-specific status code, if it is available, otherwise -1.
     * For batched events this is the status code contained in the batch response body.
     *
     * @return status code for the event, -1 if not available.
     * @see #EVENT_STATUS_NOT_AVAILABLE
     */
    int getEventStatusCode();

    /**
     * @return a byte array of the raw response body.
     */
    byte[] getRawHttpResponseBody();

    /**
     * @return data about the batch response - null if the request was not batched.
     */
    BatchData getBatchData();

    /**
     * For batched events this will contain data about the the batch response as a whole.
     */
    interface BatchData {
        /**
         * @return the event's position within the batch.
         */
        int getPositionInBatch();

        /**
         * @return the http status code of the batch response, which may differ to an individual event's status code,
         * see {@link #getEventStatusCode()}.
         */
        int getBatchStatusCode();
    }
}
