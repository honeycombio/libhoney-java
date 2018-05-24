package io.honeycomb.libhoney.transport.batch.impl;

import io.honeycomb.libhoney.transport.batch.BatchConsumer;
import io.honeycomb.libhoney.transport.batch.BatchKeyStrategy;
import io.honeycomb.libhoney.transport.batch.Batcher;
import io.honeycomb.libhoney.transport.batch.ClockProvider;
import io.honeycomb.libhoney.utils.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A batcher that accepts events (asynchronously) and separates them into batches distinguished by the event's key -
 * as determined by the provided {@link BatchKeyStrategy}. Batches are ready to be sent on to the {@link BatchConsumer},
 * either when they are full (when they reach {@link #batchSize}) or when they have been around for enough time (when
 * {@link #batchTimeoutNanos} triggers).
 * <p>
 * Internally, this maintains a worker thread, so for cleanup you must call {@link #close()}.
 *
 * @param <T> The type of the events.
 * @param <K> The type of the key events of 'T' return (which keeps the keystrategy generic).
 */
// refactor to deal with this rule makes for a less clean design
@SuppressWarnings("PMD.AccessorMethodGeneration")
public class DefaultBatcher<T, K> implements Batcher<T> {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultBatcher.class);
    /*
     * Implementation Notes:
     * - We preserve interrupted states in this class because:
     * "Only code that implements a thread's interruption policy may swallow an interruption request.
     * General-purpose task and library code should never swallow interruption requests." - Java Concurrency in Practice
     * - The two nested classes are non-static, so they can access the configuration of the enclosing class. They are
     * safe since we never leak them to outside of DefaultBatcher.
     */
    private static final long CLEANUP_THRESHOLD = 20L;
    private static final long SHUTDOWN_TIMEOUT = 5_000L;

    private final int batchSize;
    private final long batchTimeoutNanos;

    private final BlockingQueue<T> pendingQueue;
    private final Map<K, Batch> batches;
    private final ExecutorService executor;
    private final BatchConsumer<T> batchConsumer;
    private final BatchKeyStrategy<T, K> batchKeyStrategy;
    private final ClockProvider clockProvider;

    private final CountDownLatch closingLatch;
    private volatile boolean running = true;

    public DefaultBatcher(final BatchKeyStrategy<T, K> batchKeyStrategy,
                          final BatchConsumer<T> batchConsumer,
                          final ClockProvider clockProvider,
                          final BlockingQueue<T> pendingQueue,
                          final int batchSize,
                          final long batchTimeoutMillis) {
        Assert.isTrue(batchSize > 0, "batchSize must be > 0");
        Assert.isTrue(batchTimeoutMillis > 0L, "batchTimeoutMillis must be > 0");
        Assert.notNull(batchKeyStrategy, "batchKeyStrategy must not be null");
        Assert.notNull(batchConsumer, "batchConsumer must not be null");
        Assert.notNull(clockProvider, "clockProvider must not be null");
        Assert.notNull(pendingQueue, "pendingQueue must not be null");

        this.batchTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(batchTimeoutMillis);
        this.pendingQueue = pendingQueue;
        this.batchConsumer = batchConsumer;
        this.batchSize = batchSize;
        this.batchKeyStrategy = batchKeyStrategy;
        this.clockProvider = clockProvider;

        this.batches = new HashMap<>();
        this.closingLatch = new CountDownLatch(1);
        this.executor = Executors.newSingleThreadExecutor();
        this.executor.submit(new BatchingWorker());
    }

    /**
     * Worker doing the batching work and to be passed to the {@link DefaultBatcher}'s executor.
     *
     * Implementation Notes:
     * - The poll on the queue can return in 3 cases:
     * -- It times out. In which case no element is returned and we have to deal with the batch timeout triggers.
     * -- The queue returns an element and so we have to add it to a batch, and if full, send it off.
     * -- The thread is interrupted because the Executor is being shut down, so we have to flush and cleanup.
     */
    private class BatchingWorker implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    final T event = pendingQueue.poll(getLowestTimeout(), TimeUnit.NANOSECONDS);
                    if (event != null) {
                        handleNewEvent(event);
                    }
                    handleTimeoutTriggers();
                } catch (final InterruptedException ignored) {
                    LOG.debug("Batcher thread interrupted. Initiating flush prior to shutdown.");
                    Thread.currentThread().interrupt(); // preserve interrupted state to break the loop condition
                }
            }
            flush();
        }
    }

    private void handleNewEvent(final T event) throws InterruptedException {
        final K key = batchKeyStrategy.getKey(event);
        Batch batch = batches.get(key);
        if (batch == null) { // no batch for the corresponding key yet, so create a new one
            batch = new Batch();
            batches.put(key, batch);
        }
        batch.add(event);
        if (batch.isFull()) { // reached the size limit, so submit to consumer
            submitBatch(batch);
        }
    }

    // visible for testing - so we can check that unused batch entries eventually get cleaned up
    int getCurrentlyActiveBatches() {
        return batches.size();
    }

    @Override
    public boolean offerEvent(final T event) {
        if (!running) { // doors are shut, reject event
            return false;
        }

        final boolean offer = pendingQueue.offer(event); // NOPMD false positive for PrematureDeclaration

        // slight chance that close and flush happened concurrently, after the initial check and before offer returned
        if (!running) { // doors have shut after the initial check
            try {
                closingLatch.await(); // wait for close to return
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt(); // just in case, preserve interrupted state
            }
            // if event is in queue, it didn't get flushed - so report back to the caller as "false -> rejected"
            return !pendingQueue.contains(event);
        }
        return offer;
    }

    /**
     * Close down this batcher, flushing any outstanding batches and then stopping the worker thread.
     * If the containing {@link BatchConsumer} also needs to be closed, then do so after this batcher has been closed,
     * to ensure the flush of batches works.
     * <p>
     * Subsequent calls to this are safe and have no effect.
     */
    @Override
    public void close() {
        try {
            LOG.debug("Shutting down Batcher thread");
            running = false;
            executor.shutdownNow();
            try {
                executor.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (final InterruptedException ex) {
                LOG.error("Interrupted during wait for batcher to terminate", ex);
                Thread.currentThread().interrupt();
                //Preserve interrupt state
            }
            LOG.debug("Batcher thread shutdown complete");
        } finally {
            closingLatch.countDown();
        }
    }

    private void flush() {
        try {
            final Collection<T> remainingEvents = new ArrayList<>();
            pendingQueue.drainTo(remainingEvents);
            for (final T t : remainingEvents) {
                handleNewEvent(t);
            }

            for (final Batch batch : batches.values()) {
                if (!batch.isEmpty()) {
                    submitBatch(batch);
                }
            }
            batches.clear();
        } catch (final InterruptedException ex) {
            // Interrupt called again during flush, exiting flush early
            LOG.error("Interrupt thrown during flush. Exiting flush early.", ex);
            // Preserve interrupt state for correctness
            Thread.currentThread().interrupt();
        }
    }

    private long getLowestTimeout() {
        // This return MAX_VALUE when no batches exist. Thus, the thread blocks until an event is in the queue.
        long min = Long.MAX_VALUE;
        for (final Batch batch : batches.values()) {
            if (batch.getTriggerInstant() < min) {
                min = batch.getTriggerInstant();
            }
        }
        return min - clockProvider.getMonotonicTime();
    }

    private void submitBatch(final Batch batch) throws InterruptedException {
        final List<T> batchContents = batch.drainBatch();
        try {
            batchConsumer.consume(batchContents);
        } catch (final InterruptedException ex) {
            batch.add(batchContents);
            throw ex;
        }
    }

    private void handleTimeoutTriggers() throws InterruptedException {
        final Iterator<Map.Entry<K, Batch>> iterator = batches.entrySet().iterator();

        while (iterator.hasNext()) {
            final Batch batch = iterator.next().getValue();
            if (batch.hasReachedTriggerInstant()) {
                if (batch.isEmpty()) { // empty implies it's not used, so check whether we want to clean it up
                    batch.markNotUsed();
                    if (batch.hasReachedCleanupThreshold()) {
                        iterator.remove();
                    }
                } else {
                    submitBatch(batch);
                }
            }
        }
    }

    /**
     * Class to manage the lifecycle of a "batch". This means it contains elements of {@link T}, and knows whether it's
     * ready to be consumed based on two conditions: either ({@link #isFull} or {@link #hasReachedTriggerInstant}).
     * It also knows when it's ready for cleanup: {@link #notUsedCounter} is incremented every time the
     * {@link #triggerInstant} is reached, but the batch is empty at that time.
     */
    private class Batch {
        private final List<T> elements = new ArrayList<>(batchSize);
        private long triggerInstant = calculateNextTriggerInstant();
        private long notUsedCounter;

        private long calculateNextTriggerInstant() {
            return clockProvider.getMonotonicTime() + batchTimeoutNanos;
        }

        /**
         * @param event to add as an element to this batch.
         */
        void add(final T event) {
            elements.add(event);
        }

        void add(final List<T> events) {
            elements.addAll(events);
        }

        /**
         * @return true if the batch has reached the batch limit, i.e. the configured "batchSize".
         */
        boolean isFull() {
            return elements.size() >= batchSize;
        }

        /**
         * Implementation Notes:
         * - Clearing arraylist maintains capacity.
         * - This also sets a new trigger instant, so in case this batch becomes unused we can eventually
         * mark it for cleanup.
         *
         * @return a new list with all elements of this batch and clear the batch's internal list.
         */
        List<T> drainBatch() {
            final List<T> newList = new ArrayList<>(elements);
            elements.clear();
            triggerInstant = calculateNextTriggerInstant();
            return newList;
        }


        long getTriggerInstant() {
            return triggerInstant;
        }

        boolean isEmpty() {
            return elements.isEmpty();
        }

        void markNotUsed() {
            notUsedCounter++;
            triggerInstant = calculateNextTriggerInstant();
        }

        /**
         * @return true if this batch has reached the {@link DefaultBatcher#CLEANUP_THRESHOLD}.
         */
        boolean hasReachedCleanupThreshold() {
            return notUsedCounter >= CLEANUP_THRESHOLD;
        }

        boolean hasReachedTriggerInstant() {
            return (triggerInstant - clockProvider.getMonotonicTime()) <= 0L;
        }
    }

}
