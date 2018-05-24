package io.honeycomb.libhoney.responses.impl;

import io.honeycomb.libhoney.Metrics;
import io.honeycomb.libhoney.responses.ServerAccepted;
import io.honeycomb.libhoney.utils.ObjectUtils;

import java.util.Arrays;
import java.util.Map;

class SimpleServerAccepted implements ServerAccepted {

    private final byte[] rawHttpResponseBody;
    private final Map<String, Object> eventMetadata;
    private final Metrics metrics;
    private final BatchData batchData;
    private final int eventStatusCode;
    private final String message;

    SimpleServerAccepted(final byte[] rawHttpResponseBody,
                         final Map<String, Object> eventMetadata,
                         final Metrics metrics,
                         final BatchData batchData,
                         final int eventStatusCode,
                         final String message) {
        this.rawHttpResponseBody = rawHttpResponseBody;
        this.eventMetadata = eventMetadata;
        this.metrics = metrics;
        this.batchData = batchData;
        this.eventStatusCode = eventStatusCode;
        this.message = message;
    }

    @Override
    public int getEventStatusCode() {
        return eventStatusCode;
    }

    @Override
    public byte[] getRawHttpResponseBody() {
        return rawHttpResponseBody;
    }

    @Override
    public BatchData getBatchData() {
        return batchData;
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
        return "SimpleServerAccepted{" +
            "rawHttpResponseBody=" + Arrays.toString(rawHttpResponseBody) +
            ", eventMetadata=" + eventMetadata +
            ", metrics=" + metrics +
            ", batchData=" + batchData +
            ", eventStatusCode=" + eventStatusCode +
            ", message='" + message + '\'' +
            '}';
    }
}
