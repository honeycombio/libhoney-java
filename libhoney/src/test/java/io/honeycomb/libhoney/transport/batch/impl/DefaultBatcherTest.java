package io.honeycomb.libhoney.transport.batch.impl;

import io.honeycomb.libhoney.transport.batch.BatchConsumer;
import io.honeycomb.libhoney.transport.batch.BatchKeyStrategy;
import io.honeycomb.libhoney.transport.batch.ClockProvider;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultBatcherTest {
    private static final long DEFAULT_TIMEOUT = 10_000L;
    private static final int DEFAULT_QUEUE_CAPACITY = 10_000;
    private static final int DEFAULT_BATCH_SIZE = 10_000;

    private DefaultBatcher<TestEvent, String> batcher;

    private BatchConsumer<TestEvent> consumerMock;
    private BatchKeyStrategy<TestEvent, String> mockKeyGen;
    private TestClock mockClock;
    private TestBlockingQueue mockQueue;

    @Captor
    private ArgumentCaptor<List<TestEvent>> captor;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        consumerMock = mock(BatchConsumer.class);
        mockClock = new TestClock();
        mockKeyGen = mock(BatchKeyStrategy.class);
        mockQueue = new TestBlockingQueue(DEFAULT_QUEUE_CAPACITY, false);
        // Mock key generation strategy by passing the TestEvent's key back.
        when(mockKeyGen.getKey(any(TestEvent.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(final InvocationOnMock invocation) {
                return ((TestEvent) invocation.getArgument(0)).getKey();
            }
        });
    }

    @After
    public void tearDown() throws InterruptedException {
        if (batcher != null) {
            batcher.close();
        }
    }

    @Test
    public void GIVEN_aClosedBatcherInstance_EXPECT_submittedEventToBeRejected() throws InterruptedException {
        // GIVEN a closed batcher
        createDefaultBatcher();
        batcher.close();

        // WHEN sending event into closed batcher
        final boolean submitted = batcher.offerEvent(new TestEvent("key1", "data1"));

        // EXPECT no submission
        assertThat(submitted).isFalse();
    }

    @Test
    public void GIVEN_twoConsecutiveCallsToClose_EXPECT_noExceptions() throws InterruptedException {
        // GIVEN a closed batcher
        createDefaultBatcher();
        batcher.close();

        // EXPECT no exception WHEN calling close a second time
        try {
            batcher.close();
        } catch (final Exception e) {
            Assert.fail("Expect no exception");
        }
    }

    @Test
    public void GIVEN_anUnconsumedEvent_WHEN_closing_EXPECT_eventToBeFlushed() throws InterruptedException {
        // GIVEN an event in the batcher
        createDefaultBatcher();
        batcher.offerEvent(new TestEvent("key1", "data1"));

        // WHEN calling close
        batcher.close();

        // EXPECT 1 batch to be flushed
        verify(consumerMock).consume(ArgumentMatchers.<List<TestEvent>>any());
    }

    @Test
    public void GIVEN_someUnconsumedEvents_WHEN_closing_EXPECT_eventsToBeFlushed() throws InterruptedException {
        // GIVEN a batcher with 3 events, spread across 2 batches (key1, key2)
        createDefaultBatcher();
        batcher.offerEvent(new TestEvent("key1", "data1"));
        batcher.offerEvent(new TestEvent("key1", "data2"));
        batcher.offerEvent(new TestEvent("key2", "info1"));

        // WHEN closing
        batcher.close();

        // EXPECT 2 batches to be flushed
        verify(consumerMock, times(2)).consume(ArgumentMatchers.<List<TestEvent>>any());
    }

    @Test
    public void GIVEN_someUnconsumedEvents_WHEN_closing_EXPECT_theEventsBeingFlushedToEqualTheEventsThatWereOriginallySent() throws InterruptedException {
        // GIVEN a batcher with 3 events, spread across 2 batches (key1, key2)
        createDefaultBatcher();
        final TestEvent key1Element = new TestEvent("key1", "data1");
        final TestEvent key1Element2 = new TestEvent("key1", "data2");
        final TestEvent key2Element = new TestEvent("key2", "info1");
        batcher.offerEvent(key1Element);
        batcher.offerEvent(key1Element2);
        batcher.offerEvent(key2Element);

        // WHEN closing
        batcher.close();
        verify(consumerMock, times(2)).consume(captor.capture());

        // EXPECT consumed batches to match what was sent into the batcher
        final List<List<TestEvent>> allValues = captor.getAllValues();
        final List<TestEvent> expectedBatchWithKey1 = Arrays.asList(key1Element, key1Element2);
        final List<TestEvent> expectedBatchWithKey2 = Collections.singletonList(key2Element);
        assertThat(allValues).containsExactlyInAnyOrder(expectedBatchWithKey1, expectedBatchWithKey2);
    }

    @Test
    public void GIVEN_batchSizeLimitOf10_EXPECT_batchToBeConsumedAfter10Events() throws InterruptedException {
        // GIVEN a batcher with a configured batch size of 10
        batcherWithBatchSize10();
        // AND 9 events have so far been submitted to the batcher
        final List<TestEvent> batchWithKey1 = createEvents(9, "key1");
        for (final TestEvent testEvent : batchWithKey1) {
            batcher.offerEvent(testEvent);
        }
        verifyNoMoreInteractions(consumerMock); // make sure that so far nothing happened

        // WHEN submitting the 10th event
        final TestEvent tenthElement = new TestEvent("key1", "data1");
        batcher.offerEvent(tenthElement);

        // EXPECT batch of 10 to be consumed
        batchWithKey1.add(tenthElement); // add 10th element
        verify(consumerMock, timeout(1000)).consume(captor.capture());
        final List<List<TestEvent>> allValues = captor.getAllValues();
        assertThat(allValues).containsOnly(batchWithKey1);
    }

    @Test
    public void GIVEN_batchSizeLimitOf10_WHEN_submitting20Events_EXPECT_2batchesToBeConsumed() throws InterruptedException {
        // GIVEN a batcher with a configured batch size of 10
        batcherWithBatchSize10();
        // AND 20 events are submitted to the batcher
        final List<TestEvent> batchWithKey1 = createEvents(20, "key1");
        for (final TestEvent testEvent : batchWithKey1) {
            batcher.offerEvent(testEvent);
        }

        // EXPECT 2 batches of 10 to be consumed
        verify(consumerMock, timeout(1000).times(2)).consume(captor.capture());
        final List<List<TestEvent>> allValues2 = captor.getAllValues();
        assertThat(allValues2).containsOnly(batchWithKey1.subList(0, 10), batchWithKey1.subList(10, 20));
    }

    @Test
    public void GIVEN_batchSizeLimitOf10_WHEN_submittingEventsFor2BatchesInInterleavingManner_EXPECT_2batchesToBeCorrectlyConsumed() throws InterruptedException {
        // GIVEN a batcher with a configured batch size of 10
        batcherWithBatchSize10();
        // AND 9 events for batch 1 and 9 events for batch 2 have so far been submitted to the batcher
        final List<TestEvent> batchWithKey1 = createEvents(9, "key1");
        for (final TestEvent testEvent : batchWithKey1) {
            batcher.offerEvent(testEvent);
        }
        final List<TestEvent> batchWithKey2 = createEvents(9, "key2");
        for (final TestEvent testEvent : batchWithKey2) {
            batcher.offerEvent(testEvent);
        }
        verifyNoMoreInteractions(consumerMock); // double check nothing's happened so far

        // WHEN submitting the 10th event for batch 2
        final TestEvent tenthElementWithKey2 = new TestEvent("key2", "info1");
        batcher.offerEvent(tenthElementWithKey2);
        batchWithKey2.add(tenthElementWithKey2);

        // EXPECT only batch 2
        verify(consumerMock, timeout(1000)).consume(captor.capture());
        final List<TestEvent> consumeElements = captor.getValue();
        assertThat(consumeElements).containsOnlyElementsOf(batchWithKey2);

        // WHEN submitting the 10th event for batch 1
        final TestEvent tenthElementWithKey1 = new TestEvent("key1", "data1");
        batcher.offerEvent(tenthElementWithKey1);
        batchWithKey1.add(tenthElementWithKey1);

        // EXPECT only batch 1
        verify(consumerMock, timeout(1000).times(2)).consume(captor.capture());
        final List<TestEvent> consumeElements2 = captor.getValue();
        assertThat(consumeElements2).containsOnlyElementsOf(batchWithKey1);
    }

    @Test
    public void GIVEN_12EventsSubmittedInSequence_WHEN_callingClose_EXPECT2Batches() throws InterruptedException {
        // GIVEN a batcher with a configured batch size of 10
        batcherWithBatchSize10();
        // AND 12 events submitted
        final List<TestEvent> batchWithKey1 = createEvents(12, "key1");
        for (final TestEvent testEvent : batchWithKey1) {
            batcher.offerEvent(testEvent);
        }

        // WHEN closing the batcher
        batcher.close();

        // EXPECT 1 full batch and 1 flushed batch (of size 2)
        verify(consumerMock, timeout(100).times(2)).consume(captor.capture());
        final List<List<TestEvent>> consumeElements2 = captor.getAllValues();
        assertThat(consumeElements2).containsOnly(batchWithKey1.subList(0, 10), batchWithKey1.subList(10, 12));
    }

    @Test
    public void GIVEN_aBatcherWithTimeout_EXPECT_nonFullBatchesToBeEventuallyConsumed()
        throws InterruptedException, BrokenBarrierException {
        // GIVEN a batcher with the clock set to 0 and a timeout of 10
        batcherWithBlockingMockQueueAndMockClockAndTimeoutOf10();
        mockQueue.sync();
        // AND a submitted and processed event
        batcher.offerEvent(new TestEvent("key1", "data1"));
        mockQueue.cycleAndSync();
        expectConsumerInteractions(0);

        // WHEN moving the time just before batch timeout
        mockClock.setCurrentTime(9);
        mockQueue.cycleAndSync();

        // EXPECT no interaction
        expectConsumerInteractions(0);

        // BUT WHEN moving the time to the batch timeout
        mockClock.setCurrentTime(10);
        mockQueue.cycleAndSync();

        // EXPECT event to have been consumed
        expectConsumerInteractions(1);
    }

    @Test
    public void GIVEN_aBatcherWithTimeout_EXPECT_nonFullBatchesToBeEventuallyConsumedInSequence()
        throws InterruptedException, BrokenBarrierException {
        // GIVEN a batcher with the clock set to 0 and a timeout of 10
        batcherWithBlockingMockQueueAndMockClockAndTimeoutOf10();
        mockQueue.sync();
        // AND a submitted and processed event
        batcher.offerEvent(new TestEvent("key1", "data1"));
        mockQueue.cycleAndSync();

        // WHEN moving the time to batch timeout
        mockClock.setCurrentTime(10);
        mockQueue.cycleAndSync();

        // EXPECT first event to be consumed
        expectConsumerInteractions(1);

        // AND GIVEN a second event with the same key
        batcher.offerEvent(new TestEvent("key1", "data2"));
        mockQueue.cycleAndSync();

        // WHEN moving the time just before batch timeout
        mockClock.setCurrentTime(19);
        mockQueue.cycleAndSync();

        // EXPECT no further interaction
        expectConsumerInteractions(1);

        // BUT WHEN moving the time to the batch timeout
        mockClock.setCurrentTime(20);
        mockQueue.cycleAndSync();

        // EXPECT second event to be consumed
        expectConsumerInteractions(2);
    }

    @Test
    public void GIVEN_2EventsWithDifferentKeys_WHEN_nonFullAndHittingTheTimeout_EXPECT_2BatchesToBeConsumed()
        throws InterruptedException, BrokenBarrierException {
        // GIVEN a batcher with the clock set to 0 and a timeout of 10
        batcherWithBlockingMockQueueAndMockClockAndTimeoutOf10();
        // AND 2 events with different keys submitted and processed
        batcher.offerEvent(new TestEvent("key1", "data1"));
        batcher.offerEvent(new TestEvent("key2", "info1"));
        mockQueue.cycleAndSync();
        mockQueue.cycleAndSync();

        // WHEN moving the time just before batch timeout
        mockClock.setCurrentTime(9);
        mockQueue.cycleAndSync();
        // EXPECT no interaction
        expectConsumerInteractions(0);

        // BUT WHEN moving the time to the batch timeout
        mockClock.setCurrentTime(10);
        mockQueue.cycleAndSync();
        // EXPECT 2 batches to have been consumed
        expectConsumerInteractions(2);
    }

    @Test
    public void GIVEN_2EventsWithSameKeys_WHEN_nonFullAndHittingTheTimeout_EXPECT_Only1BatchToBeConsumed()
        throws InterruptedException, BrokenBarrierException {
        // GIVEN a batcher with the clock set to 0 and a timeout of 10
        batcherWithBlockingMockQueueAndMockClockAndTimeoutOf10();
        // AND 2 events with the same keys submitted and processed
        final TestEvent event1 = new TestEvent("key1", "data1");
        final TestEvent event2 = new TestEvent("key1", "data2");
        batcher.offerEvent(event1);
        batcher.offerEvent(event2);
        mockQueue.cycleAndSync();
        mockQueue.cycleAndSync();

        // WHEN moving the time just before batch timeout
        mockClock.setCurrentTime(9);
        mockQueue.cycleAndSync();

        // EXPECT no interaction
        expectConsumerInteractions(0);

        // BUT WHEN moving the time to the batch timeout
        mockClock.setCurrentTime(10);
        mockQueue.cycleAndSync();

        // EXPECT 1 batch to have been consumed
        verify(consumerMock, timeout(1000).times(1)).consume(captor.capture());
        final List<List<TestEvent>> capturedBatches = captor.getAllValues();
        assertThat(capturedBatches).containsOnly(Arrays.asList(event1, event2));
    }

    @Test
    public void GIVEN_2EventsWithDifferentKeys_WHEN_SubmittingWithSomeTimeApart_EXPECT_2BatchesToBeConsumedAtDifferentTimes()
        throws InterruptedException, BrokenBarrierException {
        // GIVEN a batcher with the clock set to 0 and a timeout of 10
        batcherWithBlockingMockQueueAndMockClockAndTimeoutOf10();
        // AND an event submitted at time 0
        final TestEvent event1 = new TestEvent("key1", "data1");
        batcher.offerEvent(event1);
        mockClock.setCurrentTime(0);
        mockQueue.cycleAndSync();
        expectConsumerInteractions(0);
        // AND another event with a different key submitted at time 2
        final TestEvent event2 = new TestEvent("key2", "info1");
        batcher.offerEvent(event2);
        mockClock.setCurrentTime(2);
        mockQueue.cycleAndSync();
        expectConsumerInteractions(0);

        // EXPECT event 1 to be consumed 10 later at 10
        mockClock.setCurrentTime(10);
        mockQueue.cycleAndSync();
        verify(consumerMock, timeout(1000).times(1)).consume(captor.capture());
        final List<List<TestEvent>> capturedBatches = captor.getAllValues();
        assertThat(capturedBatches).containsOnly(singletonList(event1));

        // AND EXPECT event 2 to be consumed 10 later at 12
        mockClock.setCurrentTime(12);
        mockQueue.cycleAndSync();
        verify(consumerMock, timeout(1000).times(2)).consume(captor.capture());
        final List<List<TestEvent>> laterCapturedBatches = captor.getAllValues();
        assertThat(laterCapturedBatches).containsOnly(singletonList(event1), singletonList(event2));
    }

    @Test
    public void GIVEN_2EventsWithSameKeys_WHEN_SubmittingWithSomeTimeApart_EXPECT_1BatchToBeConsumed()
        throws InterruptedException, BrokenBarrierException {
        // GIVEN a batcher with the clock set to 0 and a timeout of 10
        batcherWithBlockingMockQueueAndMockClockAndTimeoutOf10();
        // AND an event at time 0
        final TestEvent event1 = new TestEvent("key1", "data1");
        batcher.offerEvent(event1);
        mockClock.setCurrentTime(0);
        mockQueue.cycleAndSync();
        expectConsumerInteractions(0);
        // AND another event with the same key at time 2
        final TestEvent event2 = new TestEvent("key1", "data2");
        batcher.offerEvent(event2);
        mockClock.setCurrentTime(2);
        mockQueue.cycleAndSync();
        expectConsumerInteractions(0);

        // EXPECT both events to have been consumed at the same time (10 after the first event in the current batch)
        mockClock.setCurrentTime(10);
        mockQueue.cycleAndSync();
        verify(consumerMock, timeout(1000).times(1)).consume(captor.capture());
        final List<List<TestEvent>> capturedBatches = captor.getAllValues();
        assertThat(capturedBatches).containsOnly(Arrays.asList(event1, event2));
    }


    @Test
    public void GIVEN_noEventWithSpecifcKeyIsSubmittedForAWhile_EXPECT_InternalBatchDataStructureToBeCleanedUp()
        throws InterruptedException, BrokenBarrierException {
        batcherWithBlockingMockQueueAndMockClockAndTimeoutOf10();

        mockClock.setCurrentTime(0);
        batcher.offerEvent(new TestEvent("key1", "data1"));
        batcher.offerEvent(new TestEvent("key2", "data1"));
        mockQueue.cycleAndSync();
        mockQueue.cycleAndSync();

        for (int i = 1; i < 21; i++) {
            mockClock.setCurrentTime(i * 10);
            batcher.offerEvent(new TestEvent("key2", "data1"));
            mockQueue.cycleAndSync();
            assertThat(batcher.getCurrentlyActiveBatches()).isEqualTo(2);
        }
        mockClock.setCurrentTime(21 * 10);
        mockQueue.cycleAndSync();
        assertThat(batcher.getCurrentlyActiveBatches()).isEqualTo(1);
    }

    @Test
    public void GIVEN_BatchConsumerIsBlocking_WHEN_EventQueueIsFull_EXPECT_EventsAreDroppedDueToQueueOverflow() throws InterruptedException {
        //GIVEN BatchConsumer is Blocking
        // Mock the batch consumer such that it will block on the first call to consume
        // This simulates the behaviour when the batch consumer blocks and creates back pressure for the batcher, e.g. when the internal HTTP client is not fast enough
        final Lock batchConsumerLock = new ReentrantLock();
        batchConsumerLock.lock();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                batchConsumerLock.lock();
                batchConsumerLock.unlock();
                return null;
            }
        }).when(consumerMock).consume(ArgumentMatchers.<TestEvent>anyList());

        // Create a batcher containing the blocking batch consumer, a high timeout, a queue capacity of 10 events, and a batch size of 5 events
        batcher = new DefaultBatcher<>(
            mockKeyGen,
            consumerMock,
            SystemClockProvider.getInstance(),
            new ArrayBlockingQueue<TestEvent>(10),
            5,
            DEFAULT_TIMEOUT);

        // The first 10 events should be accepted on to the queue
        for (final TestEvent testEvent : createEvents(10, "key1")) {
            Assert.assertTrue(batcher.offerEvent(testEvent));
        }

        // We expect that the batch consumer will be called with the first 5 events in a batch
        // This call will block though on the lock
        expectConsumerInteractions(1);

        //WHEN Event Queue is full
        // We expect that the next 5 events will be accepted into the queue (thereby reaching the queue capacity of 10)
        for (final TestEvent testEvent : createEvents(5, "key1")) {
            Assert.assertTrue(batcher.offerEvent(testEvent));
        }

        // We expect that the batch consumer will not be called as it is still blocking the batcher thread
        expectConsumerInteractions(1);

        //EXPECT Events are dropped due to queue overflow
        // We expect that the next 5 events will be rejected as the queue will have overflowed (this is the backpressure)
        for (final TestEvent testEvent : createEvents(5, "key1")) {
            Assert.assertFalse(batcher.offerEvent(testEvent));
        }

        // We expect that the batch consumer will not be called as it is still blocking the batcher thread
        expectConsumerInteractions(1);

        // We stop the batch consumer from blocking the batcher thread
        batchConsumerLock.unlock();

        // We expect the remaining three batches consumed (i.e. we submitted 20 events, 5 were rejected and the batch size is 5)
        expectConsumerInteractions(3);
    }

    @Test
    public void GIVEN_BatchConsumerIsBlocking_WHEN_BatcherThreadIsInterrupted_EXPECT_AllAcceptedEventsAreFlushed() throws InterruptedException {
        //GIVEN BatchConsumer is Blocking
        // Mock the batch consumer such that it will block on the first call to consume (this simulates the behaviour
        // when the batch consumer blocks and creates back pressure for the batcher, e.g. when the internal HTTP client is not fast enough).
        // The first call will block and when unblocked (via the lock used in the test thread) will throw an InterruptedException, as though the Batcher thread has been shutdown.
        // The subsequent calls to consume will not block (as the lock will be locked and unlocked by the Batcher thread itself).
        final Lock batchConsumerLock = new ReentrantLock();
        batchConsumerLock.lock();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                batchConsumerLock.lock();
                batchConsumerLock.unlock();
                throw new InterruptedException();
            }
        }).doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                batchConsumerLock.lock();
                batchConsumerLock.unlock();
                return null;
            }
        }).when(consumerMock).consume(ArgumentMatchers.<TestEvent>anyList());

        // Create a batcher containing the blocking batch consumer, a high timeout, a queue capacity of 10 events, and a batch size of 5 events
        batcher = new DefaultBatcher<>(
            mockKeyGen,
            consumerMock,
            SystemClockProvider.getInstance(),
            new ArrayBlockingQueue<TestEvent>(10),
            5,
            DEFAULT_TIMEOUT);

        // The first 10 events should be accepted on to the queue
        for (final TestEvent testEvent : createEvents(10, "key1")) {
            Assert.assertTrue(batcher.offerEvent(testEvent));
        }

        expectConsumerInteractions(1);

        // We expect that the next 5 events will be accepted into the queue (thereby reaching the queue capacity of 10)
        for (final TestEvent testEvent : createEvents(5, "key1")) {
            Assert.assertTrue(batcher.offerEvent(testEvent));
        }

        expectConsumerInteractions(1);

        // We expect that the next 5 events will be rejected as the queue will have overflowed (this is the backpressure)
        for (final TestEvent testEvent : createEvents(5, "key1")) {
            Assert.assertFalse(batcher.offerEvent(testEvent));
        }

        expectConsumerInteractions(1);

        //WHEN Batcher thread is interrupted
        //This will cause the InterruptedException to be thrown
        batchConsumerLock.unlock();

        //EXPECT All accepted events are flushed
        //The first consumer call that blocked and threw an Interrupt is already counted. So we see 3 additional calls to the consumer for the 15 accepted events (in batches of 5).
        expectConsumerInteractions(4);
    }

    private void expectConsumerInteractions(final int times) throws InterruptedException {
        verify(consumerMock, timeout(1000).times(times)).consume(ArgumentMatchers.<List<TestEvent>>any());
    }

    private List<TestEvent> createEvents(final int limit, final String key) {
        final List<TestEvent> testEvents = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            testEvents.add(new TestEvent(key, "data" + i));
        }
        return testEvents;
    }

    private void createDefaultBatcher() {
        batcher = new DefaultBatcher<>(
            mockKeyGen,
            consumerMock,
            SystemClockProvider.getInstance(),
            new ArrayBlockingQueue<TestEvent>(DEFAULT_QUEUE_CAPACITY),
            DEFAULT_BATCH_SIZE,
            DEFAULT_TIMEOUT);
    }

    private void batcherWithBatchSize10() {
        batcher = new DefaultBatcher<>(
            mockKeyGen,
            consumerMock,
            SystemClockProvider.getInstance(),
            new ArrayBlockingQueue<TestEvent>(DEFAULT_QUEUE_CAPACITY),
            10,
            DEFAULT_TIMEOUT);
    }

    private void batcherWithBlockingMockQueueAndMockClockAndTimeoutOf10() {
        mockQueue = new TestBlockingQueue(DEFAULT_QUEUE_CAPACITY, true);
        batcher = new DefaultBatcher<>(
            mockKeyGen,
            consumerMock,
            mockClock,
            mockQueue,
            DEFAULT_BATCH_SIZE,
            10);
    }

    /**
     * This is a wrapper around a queue that gives us some control over its behaviour for testing purposes, as the
     * worker thread in the batcher otherwise runs ahead or behind the junit test thread.
     *
     * The sync method allows us to make sure the worker has been initialised.
     * cycleAndSync is to ensure that the worker does one complete iteration of its loop, which ensures a "unit of work"
     * is done.
     * Poll has been modified so we can still control it even if the timeout has gone to Long.MAX_VALUE
     * (when no batches currently exist).
     */
    private static class TestBlockingQueue extends ArrayBlockingQueue<TestEvent> {
        private final boolean blocking;
        private final CyclicBarrier barrier = new CyclicBarrier(2);

        TestBlockingQueue(final int capacity, final boolean blocking) {
            super(capacity);
            this.blocking = blocking;
        }

        void sync() throws InterruptedException, BrokenBarrierException {
            barrier.await();
        }

        void cycleAndSync() throws InterruptedException, BrokenBarrierException {
            barrier.await();
            barrier.await();
        }

        @Override
        public TestEvent poll(long timeout, TimeUnit unit) throws InterruptedException {
            if (timeout == Long.MAX_VALUE || blocking) {
                try {
                    barrier.await();
                } catch (final BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }
            return super.poll(timeout, unit);
        }
    }

    private static class TestClock implements ClockProvider {

        private volatile int currentTime = 0;

        void setCurrentTime(final int time) {
            currentTime = time;
        }

        @Override
        public long getWallTime() {
            return currentTime;
        }

        @Override
        public long getMonotonicTime() {
            return currentTime * 1000 * 1000;
        }

    }

    private static class TestEvent {
        private final String key;
        private final String data;

        private TestEvent(final String key, final String data) {
            this.key = key;
            this.data = data;
        }

        String getKey() {
            return key;
        }

        String getData() {
            return data;
        }
    }

    @Test
    public void GIVEN_variousIllegalParameters_EXPECT_IAEToBeThrown() {
        try (final AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            assertIAEThrown(softly, new CreateBatcherCallable(null, consumerMock, mockClock, mockQueue, 10, 10));
            assertIAEThrown(softly, new CreateBatcherCallable(mockKeyGen, null, mockClock, mockQueue, 10, 10));
            assertIAEThrown(softly, new CreateBatcherCallable(mockKeyGen, consumerMock, null, mockQueue, 10, 10));
            assertIAEThrown(softly, new CreateBatcherCallable(mockKeyGen, consumerMock, mockClock, null, 10, 10));
            assertIAEThrown(softly, new CreateBatcherCallable(mockKeyGen, consumerMock, mockClock, mockQueue, 0, 10));
            assertIAEThrown(softly, new CreateBatcherCallable(mockKeyGen, consumerMock, mockClock, mockQueue, 10, 0));
        }
    }

    private void assertIAEThrown(final AutoCloseableSoftAssertions softly,
                                 final CreateBatcherCallable createBatcherCallable) {
        softly.assertThatThrownBy(createBatcherCallable).isInstanceOf(IllegalArgumentException.class);
    }

    private static class CreateBatcherCallable implements ThrowableAssert.ThrowingCallable {
        private final BatchKeyStrategy<TestEvent, String> keys;
        private final BatchConsumer<TestEvent> consumer;
        private final int batchSize;
        private final int timeout;
        private final ClockProvider clock;
        private final BlockingQueue<TestEvent> queue;

        CreateBatcherCallable(final BatchKeyStrategy<TestEvent, String> keys,
                              final BatchConsumer<TestEvent> consumer,
                              final ClockProvider clock,
                              final BlockingQueue<TestEvent> queue,
                              final int batchSize,
                              final int timeout) {
            this.keys = keys;
            this.consumer = consumer;
            this.clock = clock;
            this.queue = queue;
            this.batchSize = batchSize;
            this.timeout = timeout;
        }

        @Override
        public void call() throws Throwable {
            try (final DefaultBatcher<TestEvent, String> closable = new DefaultBatcher<>(
                keys, consumer, clock, queue, batchSize, timeout)
            ) {
            }
        }
    }

    private static class BlockingBatchConsumer<T> implements BatchConsumer<T> {
        private final Lock lock;

        private BlockingBatchConsumer(final Lock lock) {
            this.lock = lock;
        }

        @Override
        public void consume(List<T> batch) throws InterruptedException {
            lock.lock();
            lock.unlock();
        }

        @Override
        public void close() throws Exception {
            //Do nothing
        }
    }
}
