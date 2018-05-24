package io.honeycomb.libhoney.eventdata;

import io.honeycomb.libhoney.Event;
import io.honeycomb.libhoney.Metrics;
import io.honeycomb.libhoney.transport.batch.ClockProvider;

import java.net.URI;
import java.util.Map;

/**
 * Concrete version of {@link EventData} to be used internally after post-processing and validation.
 */
public final class ResolvedEvent extends EventData<ResolvedEvent> {
    private final Metrics metrics;

    public ResolvedEvent(
        final URI apiHost,
        final String writeKey,
        final String dataset,
        final int sampleRate,
        final Long timestamp,
        final Map<String, Object> resolvedFields,
        final Map<String, Object> metadata,
        final ClockProvider clock) {
        super(apiHost, writeKey, dataset, sampleRate, timestamp, resolvedFields, metadata);
        metrics = Metrics.create(clock);
    }

    @Override
    protected ResolvedEvent getSelf() {
        return this;
    }

    public static ResolvedEvent of(final Map<String, Object> resolvedFields,
                                   final Event event,
                                   final ClockProvider clock) {
        return new ResolvedEvent(
            event.getApiHost(),
            event.getWriteKey(),
            event.getDataset(),
            event.getSampleRate(),
            event.getTimestamp(),
            resolvedFields,
            event.getMetadata(),
            clock
        );
    }

    public void markEnqueueTime() {
        metrics.markEnqueueTime();
    }

    public void markStartOfHttpRequest() {
        metrics.markStartOfHttpRequest();
    }

    public void markEndOfHttpRequest() {
        metrics.markEndOfHttpRequest();
    }

    public Metrics getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return "ResolvedEvent{" +
            "metrics=" + metrics +
            "} " + super.toString();
    }
}
