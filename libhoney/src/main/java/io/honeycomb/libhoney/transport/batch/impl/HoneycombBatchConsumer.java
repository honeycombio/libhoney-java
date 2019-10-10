package io.honeycomb.libhoney.transport.batch.impl;

import io.honeycomb.libhoney.LibHoney;
import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.responses.ResponseObservable;
import io.honeycomb.libhoney.responses.impl.EventResponseFactory;
import io.honeycomb.libhoney.responses.impl.LazyServerResponse;
import io.honeycomb.libhoney.transport.batch.BatchConsumer;
import io.honeycomb.libhoney.transport.json.BatchRequestSerializer;
import io.honeycomb.libhoney.transport.json.JsonSerializer;
import io.honeycomb.libhoney.utils.ObjectUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Consumer that transforms batches and sends them off to the Honeycomb Batch API.
 * The internal http client is asynchronous, so consume will not block to wait for responses.
 */
// AccessorMethodGeneration: refactor to deal with this rule makes for a less clean design.
// ExcessiveImports: cohesion seems good at the moment - any more functionality and we should decompose.
// AvoidCatchingGenericException: catch-all to make sure we correctly report back any failures. Part of the contract.
@SuppressWarnings({"PMD.AccessorMethodGeneration", "PMD.ExcessiveImports", "PMD.AvoidCatchingGenericException"})
public class HoneycombBatchConsumer implements BatchConsumer<ResolvedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(HoneycombBatchConsumer.class);

    private static final String BATCH_ENDPOINT_FORMAT = "/1/batch/%s";
    private static final String WRITE_KEY_HEADER = "X-Honeycomb-Team";
    /** The following variable defaults to "libhoneycomb-java/1.0.0 as the implementation version is injected by
     * the Maven build process and will not be available when running from IDE. This ensure unit tests run even without
     * creating an actual artifact.
     */
    private static final String USER_AGENT = "libhoneycomb-java/" +
        (LibHoney.class.getPackage().getImplementationVersion()==null ? "1.0.0" : LibHoney.class.getPackage().getImplementationVersion());

    private final CloseableHttpAsyncClient internalClient;
    private final ResponseObservable observable;
    private final JsonSerializer<List<BatchRequestElement>> batchSerializer;
    //Nullable
    private final Semaphore maximumPendingRequestSemaphore;
    private final int maximumPendingRequests;
    private final long maximumHttpRequestShutdownWait;

    private final String userAgentString;

    public HoneycombBatchConsumer(final CloseableHttpAsyncClient internalClient,
                                  final ResponseObservable observable,
                                  final BatchRequestSerializer batchRequestSerializer,
                                  final int maximumPendingRequests,
                                  final int maximumHTTPRequestShutdownWait) {
        this(internalClient,
            observable,
            batchRequestSerializer,
            maximumPendingRequests,
            maximumHTTPRequestShutdownWait,
            null);
    }

    @SuppressWarnings("PMD.NullAssignment") // the semaphore mechanism is optional via "null"
    public HoneycombBatchConsumer(final CloseableHttpAsyncClient internalClient,
                                  final ResponseObservable observable,
                                  final JsonSerializer<List<BatchRequestElement>> batchRequestSerializer,
                                  final int maximumPendingRequests,
                                  final long maximumHTTPRequestShutdownWait,
                                  final String additionalUserAgent) {
        this.internalClient = internalClient;
        this.observable = observable;
        this.batchSerializer = batchRequestSerializer;
        this.maximumPendingRequests = maximumPendingRequests;
        if (this.maximumPendingRequests == -1) {
            this.maximumPendingRequestSemaphore = null;
        } else {
            this.maximumPendingRequestSemaphore = new Semaphore(maximumPendingRequests);
        }
        this.maximumHttpRequestShutdownWait = maximumHTTPRequestShutdownWait;
        if (ObjectUtils.isNullOrEmpty(additionalUserAgent)) {
            this.userAgentString = USER_AGENT;
        } else {
            this.userAgentString = USER_AGENT + " " + additionalUserAgent;
        }
    }

    @Override
    public void consume(final List<ResolvedEvent> batch) throws InterruptedException {
        final HttpUriRequest httpPost; // NOPMD false positive
        try {
            final List<BatchRequestElement> toSerialize = transformToBatchRequestFormat(batch);
            final byte[] toSend = batchSerializer.serialize(toSerialize);
            httpPost = toPostRequest(toSend, batch.get(0));
            if (LOG.isDebugEnabled()) {
                // Avoids unnecessary conversions in non-DEBUG case
                LOG.debug("Sending HTTP request to HoneyComb. URI: {}. Body: {}. Headers: {}.",
                    httpPost.getURI(),
                    new String(toSend, StandardCharsets.UTF_8),
                    Arrays.asList(httpPost.getAllHeaders()));
            }
        } catch (final Exception ex) {
            requestBuildFailure(batch, ex);
            LOG.error(
                "Failed to construct HTTP request for submission to HTTP client. " +
                "Error has been reported to ResponseObservers.", ex);
            return;
        }

        if (maximumPendingRequestSemaphore != null) {
            maximumPendingRequestSemaphore.acquire();
        }

        try {
            internalClient.execute(httpPost, new ResponseHandlingFutureCallback(batch));
        } catch (final Exception ex) {
            releaseSemaphore();
            consumeFailed(batch, "Unexpected failure while submitting request to HTTP client", ex);
            LOG.error("HTTP client rejected batch request. Error has been reported to ResponseObservers.", ex);
        }
    }

    private HttpUriRequest toPostRequest(final byte[] toSend, final ResolvedEvent event) throws URISyntaxException {
        final String path = String.format(BATCH_ENDPOINT_FORMAT, event.getDataset());
        final URI finalUri = new URIBuilder(event.getApiHost()).setPath(path).build();

        return RequestBuilder
            .post(finalUri)
            .addHeader(WRITE_KEY_HEADER, event.getWriteKey())
            .addHeader(HttpHeaders.USER_AGENT, userAgentString)
            .setEntity(new ByteArrayEntity(toSend, ContentType.APPLICATION_JSON))
            .build();
    }

    /**
     * This converts the batch to structurally match what's required by the batch API call,
     * see <a href="https://honeycomb.io/docs/reference/api/#batched-events">Batch API docs</a>.
     * Whilst the "time" and "samplerate" fields are optional, we make sure to always set them anyway.
     *
     * @param batch to transform.
     * @return A list of batch elements.
     */
    private List<BatchRequestElement> transformToBatchRequestFormat(final List<ResolvedEvent> batch) {
        final List<BatchRequestElement> elements = new ArrayList<>(batch.size());
        final SimpleDateFormat localDateFormat = ObjectUtils.getRFC3339DateTimeFormatter();
        for (final ResolvedEvent event : batch) {
            final String dateTimeString = localDateFormat.format(new Date(event.getTimestamp()));
            elements.add(new BatchRequestElement(dateTimeString, event.getSampleRate(), event.getFields()));
        }
        return elements;
    }

    private void requestBuildFailure(final List<ResolvedEvent> batch, final Exception exception) {
        for (final ResolvedEvent resolvedEvent : batch) {
            observable.publish(EventResponseFactory.requestBuildFailure(resolvedEvent, exception));
        }
    }

    private void consumeFailed(final List<ResolvedEvent> batch, final String message, final Exception exception) {
        for (final ResolvedEvent resolvedEvent : batch) {
            observable.publish(EventResponseFactory.httpClientError(resolvedEvent, message, exception));
        }
    }

    private void releaseSemaphore() {
        if (maximumPendingRequestSemaphore != null) {
            maximumPendingRequestSemaphore.release();
        }
    }

    /**
     * Closes the internal client.
     *
     * @throws IOException in case there is a failure on closing the client.
     */
    @Override
    public void close() throws IOException {
        try {
            if (maximumPendingRequestSemaphore != null) { // NOPMD != null is fine!
                LOG.debug("Waiting for pending HTTP requests to complete.");
                maximumPendingRequestSemaphore.tryAcquire(maximumPendingRequests, maximumHttpRequestShutdownWait,
                    TimeUnit.MILLISECONDS);
            } else {
                LOG.debug("Waiting for pending HTTP requests to complete.");
                Thread.sleep(maximumHttpRequestShutdownWait);
            }
        } catch (final InterruptedException ex) {
            LOG.error("Interrupted during wait for HTTP requests to complete", ex);
            Thread.currentThread().interrupt();
            //Preserve interrupt state
        }
        LOG.debug("Closing HTTP client");
        internalClient.close();
        LOG.debug("Closed HTTP client");
    }

    /**
     * Class to match the Honeycomb Batch API's request schema structurally, so that we can serialise it into json.
     * including the 3 supported fields for "data", "time", and "samplerate".
     */
    public static class BatchRequestElement {
        private final String time;
        private final int samplerate;
        private final Map<String, Object> data;

        public BatchRequestElement(final String time, final int samplerate, final Map<String, Object> data) {
            this.time = time;
            this.samplerate = samplerate;
            this.data = data;
        }

        public String getTime() {
            return time;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public int getSamplerate() {
            return samplerate;
        }
    }

    private class ResponseHandlingFutureCallback implements FutureCallback<HttpResponse> {
        private final List<ResolvedEvent> batch;

        ResponseHandlingFutureCallback(final List<ResolvedEvent> batch) {
            this.batch = batch;
            markStartOfHttpRequest(batch);
        }

        private void markStartOfHttpRequest(final List<ResolvedEvent> batch) {
            for (final ResolvedEvent resolvedEvent : batch) {
                resolvedEvent.markStartOfHttpRequest();
            }
        }

        private void markEndOfHttpRequest() {
            for (final ResolvedEvent event : batch) {
                event.markEndOfHttpRequest();
            }
        }

        @Override
        public void completed(final HttpResponse httpResponse) {
            releaseSemaphore();
            consumeSuccessful(httpResponse);
        }

        @Override
        public void failed(final Exception exception) {
            releaseSemaphore();
            consumeFailed(batch, "HTTP client completed request with an exception", exception);
            LOG.error("Unexpected error. Batch request failed. An error has been published to the " +
                "ResponseObservers for each event in the errored batch.");
        }

        @Override
        public void cancelled() {
            releaseSemaphore();
            consumeFailed(batch, "HTTP client request was unexpectedly cancelled", null);
            LOG.error("Unexpected error. Batch request cancelled. An error has been published to the " +
                "ResponseObservers for each event in the errored batch.");
        }

        private void consumeSuccessful(final HttpResponse httpResponse) {
            markEndOfHttpRequest();
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED && !observable.hasObservers()) {
                // We log an error on any 401 because this is likely a critical configuration error and so should
                // not require ResponseObserver, but should be clear from the logs.
                // The alternative is to eagerly check the validity of the global write key on start-up (as in the
                // existing GO SDK), but that relies on undocumented API features.
                // Only log the error if there are no observers attached to handle it
                LOG.error("Server responded with a 401 HTTP error code to a batch request. This is likely caused by " +
                    "using an incorrect 'Team Write Key'. Check https://ui.honeycomb.io/account to verify your " +
                    "team write key. An error has been published to the ResponseObservers for each event " +
                    "in the errored batch.");
            }
            if (observable.hasObservers()) {
                try {
                    final List<LazyServerResponse> toPublish = LazyServerResponse.createEventsWithServerResponse(
                        batch,
                        EntityUtils.toByteArray(httpResponse.getEntity()),
                        httpResponse.getStatusLine().getStatusCode()
                    );
                    for (final LazyServerResponse response : toPublish) {
                        response.publishTo(observable);
                    }
                } catch (final IOException e) {
                    for (final ResolvedEvent resolvedEvent : batch) {
                        observable.publish(EventResponseFactory.httpClientError(
                            resolvedEvent, "Reading from HTTP response threw an exception", e)
                        );
                    }
                    LOG.error("Unable to read server HTTP response. " +
                        "An error has been published to the ResponseObservers.", e);
                }
            } else {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
                LOG.trace("No observers registered so not publishing to responses");
            }
        }
    }
}
