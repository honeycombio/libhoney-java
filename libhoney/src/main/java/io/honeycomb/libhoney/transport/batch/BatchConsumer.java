package io.honeycomb.libhoney.transport.batch;

import java.util.List;

/**
 * BatchConsumer consumes batches of 'T'.
 *
 * @param <T> The element type of the batches.
 */
public interface BatchConsumer<T> extends AutoCloseable {
    void consume(List<T> batch) throws InterruptedException;
}
