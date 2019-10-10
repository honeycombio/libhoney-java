package io.honeycomb.libhoney.transport.batch.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.honeycomb.libhoney.TestUtils;
import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.responses.ClientRejected;
import io.honeycomb.libhoney.responses.ResponseObservable;
import io.honeycomb.libhoney.responses.ServerAccepted;
import io.honeycomb.libhoney.responses.ServerRejected;
import io.honeycomb.libhoney.responses.Unknown;
import io.honeycomb.libhoney.transport.json.BatchRequestSerializer;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class HoneycombBatchConsumerTest {

    private HoneycombBatchConsumer consumer;

    private CloseableHttpAsyncClient clientMock;
    private ResponseObservable observableMock;
    private BatchRequestSerializer batchRequestSerializer = new BatchRequestSerializer();

    @Before
    public void setUp() throws Exception {
        clientMock = mock(CloseableHttpAsyncClient.class);
        observableMock = mock(ResponseObservable.class);
        consumer = new HoneycombBatchConsumer(clientMock, observableMock, batchRequestSerializer, 100, 200);
    }

    @Test
    public void WHEN_callingClose_EXPECT_clientToBeClosedAsWell() throws IOException {
        consumer.close();

        verify(clientMock).close();
    }

    @Test
    public void WHEN_consumingEvents_EXPECT_clientRequestToBeExecuted() throws InterruptedException {
        final List<ResolvedEvent> events = createTestEvents();

        consumer.consume(events);

        verify(clientMock).execute(any(HttpUriRequest.class), any(FutureCallback.class));
        verifyNoMoreInteractions(clientMock);
    }

    @Test
    public void GIVEN_normalTestEvents_EXPECT_finalUrlToContainApiHost_AND_batchPath_AND_dataset() throws InterruptedException {
        final List<ResolvedEvent> events = createTestEvents();

        consumer.consume(events);
        final HttpUriRequest value = captureRequest();

        assertThat(value.getURI()).isEqualTo(URI.create("http://example.com/1/batch/testset"));
    }

    @Test
    public void GIVEN_eventsWithUrlUnsafeCharactersInDataSet_EXPECT_clientToEncodeUnsafeCharacters() throws InterruptedException {
        final List<ResolvedEvent> events = createTestEvents();
        withDataSet(events, "Dataset with urlunsafe?");

        consumer.consume(events);
        final HttpUriRequest value = captureRequest();

        assertThat(value.getURI()).isEqualTo(URI.create("http://example.com/1/batch/Dataset%20with%20urlunsafe%3F"));
    }

    private void withDataSet(final List<ResolvedEvent> events, final String dataset) {
        for (final ResolvedEvent event : events) {
            event.setDataset(dataset);
        }
    }

    @Test
    public void GIVEN_normalTestEvents_EXPECT_requestToContainTeamHeaderWithWriteKey() throws InterruptedException {
        final List<ResolvedEvent> events = createTestEvents();

        consumer.consume(events);
        final HttpUriRequest value = captureRequest();

        assertThat(value.getFirstHeader("X-Honeycomb-Team").getValue()).isEqualTo("testkey");
    }

    @Test
    public void GIVEN_normalTestEvents_EXPECT_requestToContainUserAgentHeaderWithLibhoneyJavaAgent() throws InterruptedException {
        final List<ResolvedEvent> events = createTestEvents();

        consumer.consume(events);
        final HttpUriRequest value = captureRequest();

        assertThat(value.getFirstHeader("user-agent").getValue()).contains("libhoneycomb-java/");
    }

    @Test
    public void GIVEN_anEmptyUserAgentAddition_EXPECT_requestToContainUserAgentHeaderOnlyWithLibhoneyJavaAgent() throws InterruptedException {
        consumer = new HoneycombBatchConsumer(clientMock, observableMock, batchRequestSerializer, 100, 200, "");
        final List<ResolvedEvent> events = createTestEvents();

        consumer.consume(events);
        final HttpUriRequest value = captureRequest();

        assertThat(value.getFirstHeader("user-agent").getValue()).contains("libhoneycomb-java/");
    }

    @Test
    public void GIVEN_anAdditionalUserAgent_EXPECT_requestToContainUserAgentHeaderWithLibhoneyJavaAgentAndAdditionalUserAgen() throws InterruptedException {
        consumer = new HoneycombBatchConsumer(clientMock, observableMock, batchRequestSerializer, 100, 200, "beeline/1.0.0");
        final List<ResolvedEvent> events = createTestEvents();

        consumer.consume(events);
        final HttpUriRequest value = captureRequest();

        String userAgent = value.getFirstHeader("user-agent").getValue();
        assertThat(userAgent).matches(Pattern.compile("libhoneycomb-java/\\d+\\.\\d+\\.\\d+ beeline/\\d+\\.\\d+\\.\\d+"));
    }


    @Test
    public void GIVEN_normalTestEvents_EXPECT_requestToContainJsonEntity() throws InterruptedException {
        final List<ResolvedEvent> events = createTestEvents();

        consumer.consume(events);
        final HttpEntityEnclosingRequestBase value = (HttpEntityEnclosingRequestBase) captureRequest();

        assertThat(value.getEntity().getContentType().getValue()).contains("application/json");
    }

    @Test
    public void GIVEN_normalTestEvents_EXPECT_requestToContainBatchRequestJsonStructure() throws InterruptedException, IOException {
        final List<ResolvedEvent> events = createTestEvents();

        consumer.consume(events);
        final HttpEntityEnclosingRequestBase value = (HttpEntityEnclosingRequestBase) captureRequest();
        final String requestBody = EntityUtils.toString(value.getEntity());
        final ArrayNode batchArray = new ObjectMapper().readValue(requestBody, ArrayNode.class);

        assertThat(batchArray.size()).isEqualTo(2);
        final ObjectNode barleyEvent = (ObjectNode) batchArray.get(0);
        assertThat(barleyEvent.get("time").textValue()).isNotBlank();
        assertThat(barleyEvent.get("samplerate").numberValue()).isEqualTo(5);
        assertThat(barleyEvent.get("data").fieldNames()).containsExactlyInAnyOrder("field", "pasture", "paddock");
        final ObjectNode wheatEvent = (ObjectNode) batchArray.get(1);
        assertThat(wheatEvent.get("time").textValue()).isNotBlank();
        assertThat(wheatEvent.get("samplerate").numberValue()).isEqualTo(1);
        assertThat(wheatEvent.get("data").fieldNames()).containsExactlyInAnyOrder("field", "meadow");
    }

    @Test
    public void GIVEN_anEventWithItsTimestampSetToNow_EXPECT_requestToContainCorrectlyFormattedTimeField()
        throws InterruptedException, IOException, ParseException {
        final Date timestamp = new Date();
        final ResolvedEvent event = TestUtils.createTestEvent(timestamp);
        final List<ResolvedEvent> events = new ArrayList<>();
        events.add(event);

        consumer.consume(events);
        final String time = getTimeField();
        final Date parse = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSXXX").parse(time);

        assertThat(parse).isEqualTo(timestamp);
    }

    private String getTimeField() throws IOException {
        final HttpEntityEnclosingRequestBase value = (HttpEntityEnclosingRequestBase) captureRequest();
        final String requestBody = EntityUtils.toString(value.getEntity());
        final ArrayNode batchArray = new ObjectMapper().readValue(requestBody, ArrayNode.class);
        final ObjectNode barleyEvent = (ObjectNode) batchArray.get(0);
        return barleyEvent.get("time").textValue();
    }

    /*
     * With semaphore set to 2, we would expect 2 threads to be able to acquire, and the third get blocked - this
     * is on the basis that this runs against a mockClient, which does not release on completion.
     * So the test thread waits for 2 to countdown and checks whether the third one gets stuck.
     */
    @Test
    public void GIVEN_aSemaphoreHitsLimit_EXPECT_threadToBlock() throws InterruptedException {
        consumer = new HoneycombBatchConsumer(clientMock, new ResponseObservable(), batchRequestSerializer, 2, 200);
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.submit(new ConsumptionTask(countDownLatch));
        executorService.submit(new ConsumptionTask(countDownLatch));
        countDownLatch.await();

        final CountDownLatch newLatch = new CountDownLatch(1);
        final Future<?> taskResult = executorService.submit(new ConsumptionTask(newLatch));
        final boolean isLatchThreeRelease = newLatch.await(50, TimeUnit.MILLISECONDS);
        try {
            taskResult.get(100, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
        }

        assertThat(isLatchThreeRelease).isFalse();
        assertThat(taskResult).isNotDone();

        executorService.shutdownNow();
    }

    @Test
    public void GIVEN_aSemaphoreHitsLimit_WHEN_callingCallbackViaCancellation_EXPECT_semaphoreToBeReleaseAndThreadToBecomeUnblocked() throws Exception {
        // set maximumpendingrequests to 1 -> only one available permit on the semaphore
        consumer = new HoneycombBatchConsumer(clientMock, new ResponseObservable(), batchRequestSerializer, 1, 200);
        final ExecutorService executorService = Executors.newFixedThreadPool(5);

        // supply 1 task that does not release its permit
        final CountDownLatch firstTaskLatch = new CountDownLatch(1);
        executorService.submit(new ConsumptionTask(firstTaskLatch));
        firstTaskLatch.await();
        final FutureCallback<HttpResponse> callback = captureCallback();

        // supply another task that will have to wait for permit to be released
        final CountDownLatch secondTaskLatch = new CountDownLatch(1);
        final Future<?> secondTaskResult = executorService.submit(new ConsumptionTask(secondTaskLatch));
        boolean isLatchTwoRelease = secondTaskLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(isLatchTwoRelease).isFalse();
        assertThat(secondTaskResult).isNotDone();

        // release latch by cancelling the first task callback
        callback.cancelled();

        // 2nd task should finish
        isLatchTwoRelease = secondTaskLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(isLatchTwoRelease).isTrue();
        secondTaskResult.get(500, TimeUnit.MILLISECONDS);
        assertThat(secondTaskResult).isDone();

        executorService.shutdownNow();
    }

    @Test
    public void GIVEN_aSemaphoreHitsLimit_WHEN_callingCallbackViaException_EXPECT_semaphoreToBeReleaseAndThreadToBecomeUnblocked() throws Exception {
        // set maximumpendingrequests to 1 -> only one available permit on the semaphore
        consumer = new HoneycombBatchConsumer(clientMock, new ResponseObservable(), batchRequestSerializer, 1, 200);
        final ExecutorService executorService = Executors.newFixedThreadPool(5);

        // supply 1 task that does not release its permit
        final CountDownLatch firstTaskLatch = new CountDownLatch(1);
        executorService.submit(new ConsumptionTask(firstTaskLatch));
        firstTaskLatch.await();
        final FutureCallback<HttpResponse> callback = captureCallback();

        // supply another task that will have to wait for permit to be released
        final CountDownLatch secondTaskLatch = new CountDownLatch(1);
        final Future<?> secondTaskResult = executorService.submit(new ConsumptionTask(secondTaskLatch));
        boolean isLatchTwoRelease = secondTaskLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(isLatchTwoRelease).isFalse();
        assertThat(secondTaskResult).isNotDone();

        // release latch by failing the first task callback
        callback.failed(new SomeException());

        // 2nd task should finish
        isLatchTwoRelease = secondTaskLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(isLatchTwoRelease).isTrue();
        secondTaskResult.get(500, TimeUnit.MILLISECONDS);
        assertThat(secondTaskResult).isDone();

        executorService.shutdownNow();
    }

    @Test
    public void GIVEN_aSemaphoreHitsLimit_WHEN_callingCallbackViaNormalCompletion_EXPECT_semaphoreToBeReleaseAndThreadToBecomeUnblocked() throws Exception {
        // set maximumpendingrequests to 1 -> only one available permit on the semaphore
        consumer = new HoneycombBatchConsumer(clientMock, new ResponseObservable(), batchRequestSerializer, 1, 200);
        final ExecutorService executorService = Executors.newFixedThreadPool(5);

        // supply 1 task that does not release its permit
        final CountDownLatch firstTaskLatch = new CountDownLatch(1);
        executorService.submit(new ConsumptionTask(firstTaskLatch));
        firstTaskLatch.await();
        final FutureCallback<HttpResponse> callback = captureCallback();

        // supply another task that will have to wait for permit to be released
        final CountDownLatch secondTaskLatch = new CountDownLatch(1);
        final Future<?> secondTaskResult = executorService.submit(new ConsumptionTask(secondTaskLatch));
        boolean isLatchTwoRelease = secondTaskLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(isLatchTwoRelease).isFalse();
        assertThat(secondTaskResult).isNotDone();

        // release latch by completing the first task callback
        final HttpResponse result = new BasicHttpResponse(new HttpVersion(1, 1), 2000, "All groovy!");
        result.setEntity(new StringEntity("Body"));
        callback.completed(result);

        // 2nd task should finish
        isLatchTwoRelease = secondTaskLatch.await(500, TimeUnit.MILLISECONDS);
        assertThat(isLatchTwoRelease).isTrue();
        secondTaskResult.get(500, TimeUnit.MILLISECONDS);
        assertThat(secondTaskResult).isDone();

        executorService.shutdownNow();
    }

    @Test
    public void WHEN_cancellingARequest_EXPECT_observersToBeNotified() throws InterruptedException {
        final FutureCallback<HttpResponse> httpResponseFutureCallback = setupCallback();

        httpResponseFutureCallback.cancelled();

        final ArgumentCaptor<Unknown> captor = ArgumentCaptor.forClass(Unknown.class);
        verify(observableMock, times(2)).publish(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(Unknown.ReasonType.HTTP_CLIENT_ERROR);
        assertThat(captor.getValue().getException()).isNull();
    }

    @Test
    public void WHEN_failingARequest_EXPECT_observersToBeNotified() throws InterruptedException {
        final FutureCallback<HttpResponse> httpResponseFutureCallback = setupCallback();

        httpResponseFutureCallback.failed(new SomeException());

        final ArgumentCaptor<Unknown> captor = ArgumentCaptor.forClass(Unknown.class);
        verify(observableMock, times(2)).publish(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(Unknown.ReasonType.HTTP_CLIENT_ERROR);
        assertThat(captor.getValue().getException()).isInstanceOf(SomeException.class);
    }

    @Test
    public void WHEN_completingARequest_BUT_NoObserversRegistered_EXPECT_observersToNotBeNotified() throws InterruptedException, UnsupportedEncodingException {
        final FutureCallback<HttpResponse> httpResponseFutureCallback = setupCallback();
        final BasicHttpResponse result = new BasicHttpResponse(new HttpVersion(1, 1), 200, "All groovy!");
        result.setEntity(new StringEntity("[" +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"status\": 202" +
            "  }" +
            "]"));

        httpResponseFutureCallback.completed(result);

        verify(observableMock).hasObservers();
        verifyNoMoreInteractions(observableMock);
    }

    @Test
    public void WHEN_completingARequest_EXPECT_observersToBeNotified() throws InterruptedException, UnsupportedEncodingException {
        when(observableMock.hasObservers()).thenReturn(true);
        final FutureCallback<HttpResponse> httpResponseFutureCallback = setupCallback();
        final BasicHttpResponse result = new BasicHttpResponse(new HttpVersion(1, 1), 200, "All groovy!");
        result.setEntity(new StringEntity("[" +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"status\": 202" +
            "  }" +
            "]"));

        httpResponseFutureCallback.completed(result);

        verify(observableMock, times(2)).publish(any(ServerAccepted.class));
    }

    @Test
    public void GIVEN_responseWithOneSuccessAndOneFailure_WHEN_completingRequest_EXPECT_serverAcceptedAndServerRejectedObserversToBeNotified()
        throws InterruptedException, UnsupportedEncodingException {
        when(observableMock.hasObservers()).thenReturn(true);
        final FutureCallback<HttpResponse> httpResponseFutureCallback = setupCallback();
        final BasicHttpResponse result = new BasicHttpResponse(new HttpVersion(1, 1), 200, "All groovy!");
        result.setEntity(new StringEntity("[" +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"status\": 400," +
            "    \"error\": \"Something went wrong\"" +
            "  }" +
            "]"));

        httpResponseFutureCallback.completed(result);

        verify(observableMock, times(1)).publish(any(ServerAccepted.class));
        verify(observableMock, times(1)).publish(any(ServerRejected.class));
    }

    @Test
    public void GIVEN_anEventThatOnlyHasQueueTimeMarked_WHEN_consumingEvent_EXPECT_queueDurationToBeAvailable() throws InterruptedException {
        final ResolvedEvent testEvent = TestUtils.createTestEvent();
        testEvent.markEnqueueTime();
        assertThat(testEvent.getMetrics().getTotalDuration()).isEqualTo(-1L);
        assertThat(testEvent.getMetrics().getQueueDuration()).isEqualTo(-1L);
        assertThat(testEvent.getMetrics().getHttpRequestDuration()).isEqualTo(-1L);

        consumer.consume(Collections.singletonList(testEvent));

        assertThat(testEvent.getMetrics().getQueueDuration()).isGreaterThan(0L);
        assertThat(testEvent.getMetrics().getTotalDuration()).isEqualTo(-1L);
        assertThat(testEvent.getMetrics().getHttpRequestDuration()).isEqualTo(-1L);
    }

    @Test
    public void GIVEN_anEventThatHasQueueAndHttpPostStartTimeMarked_WHEN_completingTheCallback_EXPECT_allMetricsToBeAvailable()
        throws UnsupportedEncodingException, InterruptedException {
        final ResolvedEvent testEvent = TestUtils.createTestEvent();
        testEvent.markEnqueueTime();
        consumer.consume(Collections.singletonList(testEvent));
        final FutureCallback<HttpResponse> httpResponseFutureCallback = captureCallback();
        final BasicHttpResponse result = new BasicHttpResponse(new HttpVersion(1, 1), 200, "All groovy!");
        result.setEntity(new StringEntity("[" +
            "  {" +
            "    \"status\": 202" +
            "  }" +
            "]"));

        httpResponseFutureCallback.completed(result);

        assertThat(testEvent.getMetrics().getQueueDuration()).isGreaterThan(0L);
        assertThat(testEvent.getMetrics().getTotalDuration()).isGreaterThan(0L);
        assertThat(testEvent.getMetrics().getHttpRequestDuration()).isGreaterThan(0L);
    }

    @Test
    public void GIVEN_responseContentThatThrowsAnIOException_WHEN_completingRequest_EXPECT_UnknownObserversToBeNotified() throws InterruptedException, IOException {
        when(observableMock.hasObservers()).thenReturn(true);
        final FutureCallback<HttpResponse> httpResponseFutureCallback = setupCallback();
        final BasicHttpResponse result = new BasicHttpResponse(new HttpVersion(1, 1), 200, "All groovy!");
        final StringEntity entity = spy(new StringEntity("[" +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"status\": 400," +
            "    \"error\": \"Something went wrong\"" +
            "  }" +
            "]"));
        result.setEntity(entity);
        doThrow(IOException.class).when(entity).getContent();

        httpResponseFutureCallback.completed(result);

        verify(observableMock, times(2)).publish(any(Unknown.class));
    }

    @Test
    public void GIVEN_clientThrowsExceptions_EXPECT_errorHandlingToReportBackWithAWriteError() throws InterruptedException {
        doThrow(SomeException.class).when(clientMock).execute(any(HttpUriRequest.class), any(FutureCallback.class));
        final List<ResolvedEvent> events = createTestEvents();

        consumer.consume(events);
        final ArgumentCaptor<Unknown> captor = ArgumentCaptor.forClass(Unknown.class);
        verify(observableMock, times(2)).publish(captor.capture());

        assertThat(captor.getValue().getReason()).isEqualTo(Unknown.ReasonType.HTTP_CLIENT_ERROR);
        assertThat(captor.getValue().getException()).isNotNull();
    }

    @Test
    public void GIVEN_semaphoreIsSetTo2_AND_clientThrowsExceptions_EXPECT_errorHandlingToReleaseSemaphorePermits() throws InterruptedException {
        doThrow(SomeException.class).when(clientMock).execute(any(HttpUriRequest.class), any(FutureCallback.class));
        consumer = new HoneycombBatchConsumer(clientMock, observableMock, batchRequestSerializer, 2, 200);
        final List<ResolvedEvent> events = createTestEvents();

        consumer.consume(events);
        consumer.consume(events);
        consumer.consume(events); // if semaphores weren't released this would block

        verify(observableMock, times(6)).publish(any(Unknown.class));
    }

    @Test
    public void GIVEN_requestBuildingThrowsExceptions_EXPECT_errorHandlingToReportBackWithClientRejectedError() throws InterruptedException {
        final List<ResolvedEvent> events = createTestEvents();
        events.get(0).setApiHost(null);

        consumer.consume(events);
        final ArgumentCaptor<ClientRejected> captor = ArgumentCaptor.forClass(ClientRejected.class);
        verify(observableMock, times(2)).publish(captor.capture());

        assertThat(captor.getValue().getReason()).isEqualTo(ClientRejected.RejectionReason.REQUEST_BUILD_FAILURE);
        assertThat(captor.getValue().getException()).isNotNull();
    }


    @Test
    public void GIVEN_semaphoreIsSetTo2_AND_requestBuildingThrowsExceptions_EXPECT_errorHandlingToReleaseSemaphorePermits() throws InterruptedException {
        consumer = new HoneycombBatchConsumer(clientMock, observableMock, batchRequestSerializer, 2, 200);
        final List<ResolvedEvent> events = createTestEvents();
        events.get(0).setApiHost(null);

        consumer.consume(events);
        consumer.consume(events);
        consumer.consume(events); // if semaphores weren't released this would block

        verify(observableMock, times(6)).publish(any(ClientRejected.class));
    }

    @Test
    public void GIVEN_noBoundOnTheMaximumPendingRequests_WHEN_sendManyEvents_EXPECT_consumerDoesNotBlock() throws Exception {
        when(observableMock.hasObservers()).thenReturn(true);
        consumer = new HoneycombBatchConsumer(clientMock, observableMock, batchRequestSerializer, -1, 200);
        for (int i = 0; i < 500; i++) {
            consumer.consume(createTestEvents());
        }

        final ArgumentCaptor<FutureCallback> captor = ArgumentCaptor.forClass(FutureCallback.class);
        verify(clientMock, times(500)).execute(any(HttpUriRequest.class), captor.capture());

        final BasicHttpResponse result = new BasicHttpResponse(new HttpVersion(1, 1), 200, "All groovy!");
        result.setEntity(new StringEntity("[" +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"status\": 202" +
            "  }" +
            "]"));
        for (final FutureCallback callback : captor.getAllValues()) {
            callback.completed(result);
        }
        verify(observableMock, times(1000)).publish(any(ServerAccepted.class));
    }

    private FutureCallback<HttpResponse> setupCallback() throws InterruptedException {
        final List<ResolvedEvent> events = createTestEvents();
        consumer.consume(events);
        return captureCallback();
    }

    private FutureCallback<HttpResponse> captureCallback() {
        final ArgumentCaptor<FutureCallback> captor = ArgumentCaptor.forClass(FutureCallback.class);
        verify(clientMock).execute(any(HttpUriRequest.class), captor.capture());
        return captor.getValue();
    }

    private HttpUriRequest captureRequest() {
        final ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(clientMock).execute(captor.capture(), any(FutureCallback.class));
        return captor.getValue();
    }

    private List<ResolvedEvent> createTestEvents() {
        final ResolvedEvent barleyEvent = TestUtils.createTestEvent(5);
        barleyEvent.addField("field", "barley");
        barleyEvent.addField("pasture", "grass");
        barleyEvent.addField("paddock", "horses");
        final ResolvedEvent wheatEvent = TestUtils.createTestEvent();
        wheatEvent.addField("field", "wheat");
        wheatEvent.addField("meadow", "grass");
        final List<ResolvedEvent> events = new ArrayList<>();
        events.add(barleyEvent);
        events.add(wheatEvent);
        return events;
    }

    private class ConsumptionTask implements Runnable {
        private final CountDownLatch countDownLatch;

        public ConsumptionTask(final CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            final List<ResolvedEvent> events = createTestEvents();
            try {
                consumer.consume(events);
                countDownLatch.countDown();
                // add extra wait at the end to ensure tests wait for the task to actually finish
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class SomeException extends RuntimeException {
    }
}
