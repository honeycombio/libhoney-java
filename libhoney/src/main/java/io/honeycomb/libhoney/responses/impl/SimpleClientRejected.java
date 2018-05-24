package io.honeycomb.libhoney.responses.impl;

import io.honeycomb.libhoney.Metrics;
import io.honeycomb.libhoney.responses.ClientRejected;
import io.honeycomb.libhoney.utils.ObjectUtils;

import java.util.Map;

public class SimpleClientRejected implements ClientRejected {
    private final String message;
    private final Metrics metrics;
    private final Map<String, Object> eventMetadata;
    private final RejectionReason reason;
    private final Exception exception;

    SimpleClientRejected(final RejectionReason reason,
                         final Exception exception,
                         final String message,
                         final Map<String, Object> eventMetadata,
                         final Metrics metrics) {
        this.reason = reason;
        this.exception = exception;
        this.message = message;
        this.eventMetadata = eventMetadata;
        this.metrics = metrics;
    }

    @Override
    public RejectionReason getReason() {
        return reason;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    @Override
    public Map<String, Object> getEventMetadata() {
        return ObjectUtils.nullsafe(eventMetadata);
    }

    @Override
    public Metrics getMetrics() {
        return metrics;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "SimpleClientRejected{" +
            "message='" + message + '\'' +
            ", metrics=" + metrics +
            ", eventMetadata=" + eventMetadata +
            ", reason=" + reason +
            ", exception=" + exception +
            '}';
    }
}
