package io.honeycomb.libhoney.responses.impl;

import io.honeycomb.libhoney.Metrics;
import io.honeycomb.libhoney.responses.Unknown;
import io.honeycomb.libhoney.utils.ObjectUtils;

import java.util.Map;

public class SimpleUnknown implements Unknown {
    private final ReasonType reasonType;
    private final Map<String, Object> eventMetadata;
    private final Metrics metrics;
    private final String message;
    private final Exception exception;

    SimpleUnknown(final ReasonType reasonType,
                  final Map<String, Object> eventMetadata,
                  final Metrics metrics,
                  final String message,
                  final Exception exception) {

        this.reasonType = reasonType;
        this.eventMetadata = eventMetadata;
        this.metrics = metrics;
        this.message = message;
        this.exception = exception;
    }

    @Override
    public ReasonType getReason() {
        return reasonType;
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
        return reasonType + ": " + message +
            (exception == null ? "" : " - With exception message: " + exception.getMessage());
    }

    @Override
    public String toString() {
        return "SimpleUnknown{" +
            "reasonType=" + reasonType +
            ", eventMetadata=" + eventMetadata +
            ", metrics=" + metrics +
            ", message='" + message + '\'' +
            ", exception=" + exception +
            '}';
    }
}
