package io.honeycomb.libhoney.transport.batch;

/**
 * A strategy interface to determine the key to an event's batch. In other words, it is a key for a group-by operation.
 *
 * @param <T> the type of event create a key for.
 * @param <K> the type of the key.
 */
public interface BatchKeyStrategy<T, K> {
    /**
     * @param event to to derive a batch key for.
     * @return An object that represents a key to the event's batch.
     */
    K getKey(final T event);
}
