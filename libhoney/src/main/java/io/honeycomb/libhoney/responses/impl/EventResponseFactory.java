package io.honeycomb.libhoney.responses.impl;

import io.honeycomb.libhoney.Event;
import io.honeycomb.libhoney.Metrics;
import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.responses.ClientRejected;
import io.honeycomb.libhoney.responses.Unknown;

import java.util.Map;

public final class EventResponseFactory {
    private EventResponseFactory() {
        // utils
    }

    public static Unknown httpClientError(final ResolvedEvent event, final String message, final Exception exception) {
        return new SimpleUnknown(
            Unknown.ReasonType.HTTP_CLIENT_ERROR, event.getMetadata(), event.getMetrics(), message, exception
        );
    }

    public static ClientRejected queueOverflow(final ResolvedEvent event) {
        return new ClientRejectedBuilder(
            ClientRejected.RejectionReason.QUEUE_OVERFLOW, "Queue capacity reached")
            .setEventMetadata(event.getMetadata())
            .setMetrics(event.getMetrics())
            .build();
    }

    public static ClientRejected notSampled(final Event event) {
        return new ClientRejectedBuilder(
            ClientRejected.RejectionReason.NOT_SAMPLED, "Event sample rate was: " + event.getSampleRate())
            .setEventMetadata(event.getMetadata())
            .setMetrics(Metrics.create())
            .build();
    }

    public static ClientRejected requestBuildFailure(final ResolvedEvent event, final Exception exception) {
        return new ClientRejectedBuilder(
            ClientRejected.RejectionReason.REQUEST_BUILD_FAILURE,
            "Http client failure while assembling HTTP request due to exception: " + exception.getMessage())
            .setException(exception)
            .setEventMetadata(event.getMetadata())
            .setMetrics(event.getMetrics())
            .build();
    }

    public static ClientRejected dynamicFieldResolutionError(final Event event, final Exception exception) {
        return new ClientRejectedBuilder(
            ClientRejected.RejectionReason.DYNAMIC_FIELD_RESOLUTION_ERROR,
            "Dynamic field resolution failed due to unexpected exception: " + exception.getMessage())
            .setException(exception)
            .setEventMetadata(event.getMetadata())
            .setMetrics(Metrics.create())
            .build();
    }

    public static ClientRejected postProcessorError(final ResolvedEvent event, final Exception exception) {
        return new ClientRejectedBuilder(
            ClientRejected.RejectionReason.POST_PROCESSING_ERROR,
            "Post processing failed due to unexpected exception: " + exception.getMessage())
            .setException(exception)
            .setMetrics(event.getMetrics())
            .setEventMetadata(event.getMetadata())
            .build();
    }

    public static class ClientRejectedBuilder {
        private final ClientRejected.RejectionReason reason;
        private final String message;
        private Map<String, Object> eventMetadata;
        private Metrics metrics;
        private Exception exception;

        public ClientRejectedBuilder(final ClientRejected.RejectionReason reason, final String message) {
            this.reason = reason;
            this.message = message;
        }

        public SimpleClientRejected build() {
            this.metrics = (metrics == null) ? Metrics.create() : metrics;
            return new SimpleClientRejected(reason, exception, message, eventMetadata, metrics);
        }

        public ClientRejectedBuilder setEventMetadata(final Map<String, Object> eventMetadata) {
            this.eventMetadata = eventMetadata;
            return this;
        }

        public ClientRejectedBuilder setMetrics(final Metrics metrics) {
            this.metrics = metrics;
            return this;
        }

        public ClientRejectedBuilder setException(final Exception exception) {
            this.exception = exception;
            return this;
        }
    }
}
