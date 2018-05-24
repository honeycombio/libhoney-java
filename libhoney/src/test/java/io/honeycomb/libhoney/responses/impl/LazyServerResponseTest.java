package io.honeycomb.libhoney.responses.impl;

import io.honeycomb.libhoney.Event;
import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.LibHoney;
import io.honeycomb.libhoney.Metrics;
import io.honeycomb.libhoney.TestUtils;
import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.responses.ServerAccepted;
import io.honeycomb.libhoney.responses.ServerRejected;
import io.honeycomb.libhoney.responses.ResponseObservable;
import io.honeycomb.libhoney.transport.batch.impl.SystemClockProvider;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class LazyServerResponseTest {

    private ArgumentCaptor<ServerAccepted> acceptedCaptor = ArgumentCaptor.forClass(ServerAccepted.class);
    private ArgumentCaptor<ServerRejected> serverRejectedCaptor = ArgumentCaptor.forClass(ServerRejected.class);

    @Test
    public void GIVEN_AnOkBatchResponse_AND_anAcceptedEvent_EXPECT_responseToResolveCorrectly() {
        final String batchBody = "[" +
            "  {" +
            "    \"status\": 202" +
            "  }" +
            "]";
        final byte[] batchBodyAsBytes = batchBody.getBytes(StandardCharsets.UTF_8);
        final Metrics metrics = Metrics.create();
        final int batchStatus = 200;
        final Map<String, Object> eventMetadata = Collections.<String, Object>singletonMap("metakey", "metadata");
        final LazyServerResponse.LazyResponseBody lazyBody = new LazyServerResponse.LazyResponseBody(batchBodyAsBytes, batchStatus);

        final LazyServerResponse responseElement = new LazyServerResponse(
            batchStatus,
            batchBodyAsBytes,
            eventMetadata,
            lazyBody,
            0,
            metrics
        );
        final ResponseObservable observable = spy(new ResponseObservable());
        responseElement.publishTo(observable);
        verify(observable).publish(acceptedCaptor.capture());
        final ServerAccepted value = acceptedCaptor.getValue();

        assertThat(value.getEventStatusCode()).isEqualTo(202);
        assertThat(value.getBatchData().getPositionInBatch()).isEqualTo(0);
        assertThat(value.getBatchData().getBatchStatusCode()).isEqualTo(200);
        assertThat(value.getEventMetadata()).isEqualTo(Collections.<String, Object>singletonMap("metakey", "metadata"));
        assertThat(value.getMessage()).isEqualTo("ACCEPTED");
        assertThat(value.getMetrics()).isSameAs(metrics);
        assertThat(value.getRawHttpResponseBody()).isEqualTo(batchBodyAsBytes);
        verifyNoMoreInteractions(observable);
    }

    @Test
    public void GIVEN_AnOkBatchResponse_BUT_anRejectedEvent_EXPECT_responseToResolveCorrectly() {
        final String batchBody = "[" +
            "  {" +
            "    \"error\": \"Oops!\"," +
            "    \"status\": 400" +
            "  }" +
            "]";
        final byte[] batchBodyAsBytes = batchBody.getBytes(StandardCharsets.UTF_8);
        final Metrics metrics = Metrics.create();
        final int batchStatus = 200;
        final Map<String, Object> eventMetadata = Collections.<String, Object>singletonMap("metakey", "metadata");
        final LazyServerResponse.LazyResponseBody lazyBody = new LazyServerResponse.LazyResponseBody(batchBodyAsBytes, batchStatus);

        final LazyServerResponse responseElement = new LazyServerResponse(
            batchStatus,
            batchBodyAsBytes,
            eventMetadata,
            lazyBody,
            0,
            metrics
        );
        final ResponseObservable observable = spy(new ResponseObservable());
        responseElement.publishTo(observable);

        verify(observable).publish(serverRejectedCaptor.capture());
        final ServerRejected value = serverRejectedCaptor.getValue();

        assertThat(value.getEventStatusCode()).isEqualTo(400);
        assertThat(value.getBatchData().getPositionInBatch()).isEqualTo(0);
        assertThat(value.getBatchData().getBatchStatusCode()).isEqualTo(batchStatus);
        assertThat(value.getEventMetadata()).isEqualTo(Collections.<String, Object>singletonMap("metakey", "metadata"));
        assertThat(value.getMessage()).isEqualTo("Oops!");
        assertThat(value.getMetrics()).isSameAs(metrics);
        assertThat(value.getRawHttpResponseBody()).isEqualTo(batchBodyAsBytes);
        verifyNoMoreInteractions(observable);
    }

    @Test
    public void GIVEN_AnOkBatchResponse_AND_TwoEvents_EXPECT_BatchPositionToRepresentBatchElements() {
        final String batchBody = "[" +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"error\": \"Oops!\"," +
            "    \"status\": 400" +
            "  }" +
            "]";
        final byte[] batchBodyAsBytes = batchBody.getBytes(StandardCharsets.UTF_8);
        final Metrics metrics = Metrics.create();
        final int batchStatus = 200;
        final Map<String, Object> eventMetadata = Collections.<String, Object>singletonMap("metakey", "metadata");
        final LazyServerResponse.LazyResponseBody lazyBody = new LazyServerResponse.LazyResponseBody(batchBodyAsBytes, batchStatus);
        final ResponseObservable observable = spy(new ResponseObservable());

        final LazyServerResponse firstElement = new LazyServerResponse(batchStatus, batchBodyAsBytes, eventMetadata, lazyBody, 0, metrics);
        firstElement.publishTo(observable);
        verify(observable).publish(acceptedCaptor.capture());
        final ServerAccepted value1 = acceptedCaptor.getValue();

        final LazyServerResponse secondElement = new LazyServerResponse(batchStatus, batchBodyAsBytes, eventMetadata, lazyBody, 1, metrics);
        secondElement.publishTo(observable);
        verify(observable).publish(serverRejectedCaptor.capture());
        final ServerRejected value2 = serverRejectedCaptor.getValue();

        assertThat(value1.getEventStatusCode()).isEqualTo(202);
        assertThat(value1.getBatchData().getPositionInBatch()).isEqualTo(0);
        assertThat(value2.getEventStatusCode()).isEqualTo(400);
        assertThat(value2.getBatchData().getPositionInBatch()).isEqualTo(1);
        verifyNoMoreInteractions(observable);
    }


    @Test
    public void GIVEN_AnErrorBatchResponse_AND_TwoExpectedEvents_EXPECT_BothEventsToReturnBatchLevelError() {
        final String errorBody = "{\"error\": \"Error!\"}";
        final byte[] batchBodyAsBytes = errorBody.getBytes(StandardCharsets.UTF_8);
        final Metrics metrics = Metrics.create();
        final int batchStatus = 400;
        final Map<String, Object> eventMetadata = Collections.<String, Object>singletonMap("metakey", "metadata");
        final LazyServerResponse.LazyResponseBody lazyBody = new LazyServerResponse.LazyResponseBody(batchBodyAsBytes, batchStatus);
        final ResponseObservable observable = spy(new ResponseObservable());

        final LazyServerResponse firstElement = new LazyServerResponse(batchStatus, batchBodyAsBytes, eventMetadata, lazyBody, 0, metrics);
        firstElement.publishTo(observable);
        verify(observable).publish(serverRejectedCaptor.capture());
        final ServerRejected value1 = serverRejectedCaptor.getValue();

        final LazyServerResponse secondElement = new LazyServerResponse(batchStatus, batchBodyAsBytes, eventMetadata, lazyBody, 1, metrics);
        secondElement.publishTo(observable);
        verify(observable, times(2)).publish(serverRejectedCaptor.capture());
        final ServerRejected value2 = serverRejectedCaptor.getValue();

        assertThat(value1.getBatchData().getBatchStatusCode()).isEqualTo(400);
        assertThat(value1.getEventStatusCode()).isEqualTo(-1);
        assertThat(value1.getMessage()).isEqualTo("Error!");
        assertThat(value2.getBatchData().getBatchStatusCode()).isEqualTo(400);
        assertThat(value2.getEventStatusCode()).isEqualTo(-1);
        assertThat(value2.getMessage()).isEqualTo("Error!");
        verifyNoMoreInteractions(observable);
    }

    @Test
    public void GIVEN_InitialisationWithOutOfBoundsBatchPosition__WHEN_AccessingBatchElementData_EXPECT_IllegalStateExceptionToBeThrown() {
        final String batchBody = "[" +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"error\": \"Oops!\"," +
            "    \"status\": 400" +
            "  }" +
            "]";
        final byte[] batchBodyAsBytes = batchBody.getBytes(StandardCharsets.UTF_8);
        final Metrics metrics = Metrics.create();
        final int batchStatus = 200;
        final Map<String, Object> eventMetadata = Collections.<String, Object>singletonMap("metakey", "metadata");
        final LazyServerResponse.LazyResponseBody lazyBody = new LazyServerResponse.LazyResponseBody(batchBodyAsBytes, batchStatus);
        final ResponseObservable observable = spy(new ResponseObservable());

        final LazyServerResponse outOfBoundsElement = new LazyServerResponse(
            batchStatus,
            batchBodyAsBytes,
            eventMetadata,
            lazyBody,
            2,
            metrics
        );

        try {
            outOfBoundsElement.publishTo(observable);
            fail("Expected ISE to be thrown!");
        } catch (final IllegalStateException s) {
        }
    }

    @Test
    public void GIVEN_threeEvents_WHEN_creatingServerResponsesViaFactoryMethod_EXPECT_responsesToMatchBodyContent() throws Exception {
        final String batchBody = "[" +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"error\": \"Oops!\"," +
            "    \"status\": 400" +
            "  }" +
            "]";
        final ResponseObservable observable = spy(new ResponseObservable());
        final List<ResolvedEvent> events = new ArrayList<>();
        events.add(TestUtils.createTestEvent());
        events.add(TestUtils.createTestEvent());
        events.add(TestUtils.createTestEvent());

        final List<LazyServerResponse> responses = LazyServerResponse.createEventsWithServerResponse(events, batchBody.getBytes(StandardCharsets.UTF_8), 200);
        assertThat(responses).hasSize(3);

        final LazyServerResponse firstElement = responses.get(0);
        firstElement.publishTo(observable);
        verify(observable).publish(acceptedCaptor.capture());
        final ServerAccepted value1 = acceptedCaptor.getValue();
        assertThat(value1.getBatchData().getBatchStatusCode()).isEqualTo(200);
        assertThat(value1.getEventStatusCode()).isEqualTo(202);
        assertThat(value1.getBatchData().getPositionInBatch()).isEqualTo(0);

        final LazyServerResponse secondElement = responses.get(1);
        secondElement.publishTo(observable);
        verify(observable, times(2)).publish(acceptedCaptor.capture());
        final ServerAccepted value2 = acceptedCaptor.getValue();
        assertThat(value2.getBatchData().getBatchStatusCode()).isEqualTo(200);
        assertThat(value2.getEventStatusCode()).isEqualTo(202);
        assertThat(value2.getBatchData().getPositionInBatch()).isEqualTo(1);

        final LazyServerResponse thirdElement = responses.get(2);
        thirdElement.publishTo(observable);
        verify(observable).publish(serverRejectedCaptor.capture());
        final ServerRejected value3 = serverRejectedCaptor.getValue();
        assertThat(value3.getBatchData().getBatchStatusCode()).isEqualTo(200);
        assertThat(value3.getEventStatusCode()).isEqualTo(400);
        assertThat(value3.getBatchData().getPositionInBatch()).isEqualTo(2);
    }

    @Test
    public void GIVEN_anEvent_WHEN_creatingServerResponsesViaFactoryMethod_EXPECT_responseToContainExpectedMetadataAndMetrics() throws Exception {
        final String batchBody = "[" +
            "  {" +
            "    \"status\": 202" +
            "  }" +
            "]";
        final ResponseObservable observable = spy(new ResponseObservable());
        final ResolvedEvent resolvedEvent;
        try (final HoneyClient honeyClient = LibHoney.create(LibHoney.options().build())) {
            final Event event1 = honeyClient.createEvent();
            event1.addMetadata("metakey", "metavalue");
            resolvedEvent = ResolvedEvent.of(
                Collections.<String, Object>emptyMap(),
                event1,
                SystemClockProvider.getInstance());
        }

        final List<ResolvedEvent> events = new ArrayList<>();
        events.add(resolvedEvent);

        final List<LazyServerResponse> responses = LazyServerResponse.createEventsWithServerResponse(events, batchBody.getBytes(StandardCharsets.UTF_8), 200);
        assertThat(responses).hasSize(1);

        final LazyServerResponse element = responses.get(0);
        element.publishTo(observable);
        verify(observable).publish(acceptedCaptor.capture());
        final ServerAccepted value = acceptedCaptor.getValue();
        assertThat(value.getMetrics()).isSameAs(resolvedEvent.getMetrics());
        assertThat(value.getEventMetadata()).isEqualTo(Collections.singletonMap("metakey", "metavalue"));
    }
}
