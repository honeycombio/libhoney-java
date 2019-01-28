package io.honeycomb.libhoney.transport.impl;

import io.honeycomb.libhoney.TestUtils;
import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.responses.ResponseObservable;
import io.honeycomb.libhoney.transport.batch.BatchConsumer;
import io.honeycomb.libhoney.transport.batch.Batcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class BatchingHttpTransportTest {

    private BatchingHttpTransport transport;
    private Batcher<ResolvedEvent> mockBatcher;
    private BatchConsumer<ResolvedEvent> mockConsumer;
    private ResponseObservable mockservable;

    @Before
    public void setUp() throws Exception {
        mockBatcher = mock(Batcher.class);
        mockConsumer = mock(BatchConsumer.class);
        mockservable = mock(ResponseObservable.class);
        transport = new BatchingHttpTransport(
            mockBatcher,
            mockConsumer,
            mockservable);
    }

    @Test
    public void WHEN_submittingEvent_EXPECT_batcherToBeOfferedEvent() throws Exception {
        final ResolvedEvent event = TestUtils.createTestEvent();
        when(mockBatcher.offerEvent(event)).thenReturn(true);

        final boolean submit = transport.submit(event);

        assertThat(submit).isTrue();
        verify(mockBatcher).offerEvent(event);
    }

    @Test
    public void GIVEN_submittedEventAndThusEnqueueTimeHavingBeenMarked_WHEN_callingMarkStartHttpPost_EXPECT_QueueDurationToReturnPositiveDuration() throws Exception {
        final ResolvedEvent event = TestUtils.createTestEvent();
        transport.submit(event);
        assertThat(event.getMetrics().getHttpRequestDuration()).isEqualTo(-1L);
        assertThat(event.getMetrics().getQueueDuration()).isEqualTo(-1L);
        assertThat(event.getMetrics().getTotalDuration()).isEqualTo(-1L);

        event.markStartOfHttpRequest();
        assertThat(event.getMetrics().getQueueDuration()).isPositive();
    }

    @Test
    public void WHEN_gettingObservable_EXPECT_toBeSameAsTheProvidedOne() {
        assertThat(transport.getResponseObservable()).isSameAs(mockservable);
    }

    @Test
    public void WHEN_callingClose_EXPECT_closeToBeCalledOnBatcherFirst_AND_thenOnConsumer_AND_thenTheObservable() throws Exception {
        transport.close();

        final InOrder inOrder = inOrder(mockBatcher, mockConsumer, mockservable);
        inOrder.verify(mockBatcher).close();
        inOrder.verify(mockConsumer).close();
        inOrder.verify(mockservable).close();
    }
}
