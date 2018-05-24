package io.honeycomb.libhoney.transport.batch;

/**
 * Batcher accepts events (asynchronously) and collects them according to some batching strategy,
 * before sending batched events on for further processing.
 *
 * @param <T> The type of events to batch.
 */
public interface Batcher<T> extends AutoCloseable {
    /**
     * Offer an event to the batcher.
     * If this returns true, the event has been accepted for further processing (asynchronously).
     * If false the event has been rejected - either as a result of the internal queue being at capacity,
     * or because the Batcher has been closed.
     *
     * @param event to batch and process - must not be null.
     * @return true if event has been accepted for processing, or false if it's been rejected.
     */
    boolean offerEvent(T event);
}
