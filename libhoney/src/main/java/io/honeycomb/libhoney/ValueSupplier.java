package io.honeycomb.libhoney;

/**
 * Interface to supply field values dynamically.
 * <p>
 * Resolution of a supplier occurs on the thread that invokes any of the send*() methods.
 * <p>
 * Any exceptions occurring will stop the event from being sent and reported back as a
 * {@link io.honeycomb.libhoney.responses.ClientRejected} response.
 *
 * @param <V> The type of the field value.
 */
public interface ValueSupplier<V> {
    /**
     * Supply a value
     * @return the supplied value
     */
    V supply();
}
