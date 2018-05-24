package io.honeycomb.libhoney.responses.impl;

import io.honeycomb.libhoney.Metrics;
import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.responses.ServerAccepted;
import io.honeycomb.libhoney.responses.ServerRejected;
import io.honeycomb.libhoney.responses.ServerResponse;
import io.honeycomb.libhoney.responses.Unknown;
import io.honeycomb.libhoney.responses.ResponseObservable;
import io.honeycomb.libhoney.utils.Assert;
import io.honeycomb.libhoney.utils.Lazy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.honeycomb.libhoney.responses.ServerResponse.EVENT_STATUS_NOT_AVAILABLE;

/**
 * Wrapper class to represent a server response and is capable of generating an appropriate
 * {@link io.honeycomb.libhoney.responses.Response} to a {@link io.honeycomb.libhoney.ResponseObserver}.
 */
public class LazyServerResponse {
    public static final String EVENT_ACCEPTED_MESSAGE = "ACCEPTED";

    private final byte[] rawHttpResponseBody;
    private final Map<String, Object> eventMetadata;
    private final Lazy<BatchResponseBody> lazyBody;
    private final Metrics metrics;
    private final ServerResponse.BatchData batchData;

    LazyServerResponse(
        final int batchStatusCode,
        final byte[] rawHttpResponseBody,
        final Map<String, Object> eventMetadata,
        final LazyResponseBody lazyBody,
        final int batchPosition,
        final Metrics metrics
    ) {
        this.rawHttpResponseBody = rawHttpResponseBody;
        this.eventMetadata = eventMetadata;
        this.lazyBody = lazyBody;
        this.metrics = metrics;
        this.batchData = new SimpleBatchData(batchPosition, batchStatusCode);
    }

    public void publishTo(final ResponseObservable observable) {
        final BatchResponseBody batchResponseBody = lazyBody.get();

        switch (batchResponseBody.getCategory()) {
            case BATCH_ACCEPTED:
                // batch size guard
                final BatchResponseBody.BatchResponseElement element = assertBatchElements(batchResponseBody);
                if (element.isAccepted()) {
                    final ServerAccepted accepted = new SimpleServerAccepted(
                        rawHttpResponseBody, eventMetadata, metrics, batchData, element.getStatus(),
                        EVENT_ACCEPTED_MESSAGE
                    );
                    observable.publish(accepted);
                } else {
                    final String elementMessage = element.getError();
                    final ServerRejected rejected = new SimpleServerRejected(
                        rawHttpResponseBody, eventMetadata, metrics, batchData, element.getStatus(), elementMessage
                    );
                    observable.publish(rejected);
                }
                break;

            case BATCH_REJECTED:
                final String batchMessage = batchResponseBody.getBatchError().getError();
                final ServerRejected rejected = new SimpleServerRejected(
                    rawHttpResponseBody, eventMetadata, metrics, batchData, EVENT_STATUS_NOT_AVAILABLE, batchMessage
                );
                observable.publish(rejected);
                break;

            case CANNOT_INFER_STATE:
                final String message = batchResponseBody.getServerApiError().getMessage();
                final IOException cause = batchResponseBody.getServerApiError().getCause();
                final Unknown unknown = new SimpleUnknown(
                    Unknown.ReasonType.SERVER_API_ERROR, eventMetadata, metrics, message, cause
                );
                observable.publish(unknown);
                break;

            default:
                throw new IllegalStateException("Switch not exhaustive");
        }
    }

    private BatchResponseBody.BatchResponseElement assertBatchElements(final BatchResponseBody batchResponseBody) {
        final List<BatchResponseBody.BatchResponseElement> batchResponseElements =
            batchResponseBody.getBatchResponseElements();
        Assert.state(batchResponseElements != null,
            "Event data requested, but elements array is null");
        Assert.state(batchData.getPositionInBatch() < batchResponseElements.size(),
            "This event's index in the batch exceeds the batch response's size");

        return batchResponseElements.get(batchData.getPositionInBatch());
    }

    public static List<LazyServerResponse> createEventsWithServerResponse(
        final List<ResolvedEvent> events,
        final byte[] rawHttpResponseBody,
        final int httpCode
    ) {
        final List<LazyServerResponse> lazyResponses = new ArrayList<>(events.size());
        final LazyResponseBody lazyResponseBody = new LazyResponseBody(rawHttpResponseBody, httpCode);
        for (int i = 0; i < events.size(); i++) {
            final ResolvedEvent event = events.get(i);
            lazyResponses.add(new LazyServerResponse(
                httpCode, rawHttpResponseBody, event.getMetadata(), lazyResponseBody, i, event.getMetrics()
            ));
        }
        return lazyResponses;
    }

    static class LazyResponseBody extends Lazy<BatchResponseBody> {
        private final byte[] rawHttpResponseBody;
        private final int httpCode;

        @SuppressWarnings("PMD.ArrayIsStoredDirectly")
        LazyResponseBody(final byte[] rawHttpResponseBody, final int httpCode) {
            super();
            this.rawHttpResponseBody = rawHttpResponseBody;
            this.httpCode = httpCode;
        }

        @Override
        protected BatchResponseBody init() {
            return new BatchResponseBody(rawHttpResponseBody, httpCode);
        }
    }
}
