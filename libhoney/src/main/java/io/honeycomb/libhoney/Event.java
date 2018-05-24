package io.honeycomb.libhoney;

import io.honeycomb.libhoney.eventdata.EventData;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * An Event holds data to be sent to Honeycomb.
 * It also specifies configuration settings (e.g. sampleRate) relevant to the send operation.
 * Properties defined on the Event override the same ones it received from either {@link EventFactory} or
 * {@link LibHoney}. See {@link HoneyClient}'s class documentation for the "Event Validation" rules, and further
 * descriptions exist on the {@link Options} and {@link LibHoney} classes.
 * <p>
 * The metadata that can be set on Event is not sent to Honeycomb. It will be passed back to any registered
 * {@link ResponseObserver}s and may be useful for clients that implement response handling and want to, for instance,
 * match up the Response with the original source of an Event.
 * <p>
 * This class is mutable and not threadsafe.
 */
public class Event extends EventData<Event> {
    private final HoneyClient client;
    private final Map<String, ValueSupplier<?>> dynamicFields;

    Event(final HoneyClient client,
          final URI apiHost,
          final String writeKey,
          final String dataset,
          final int sampleRate,
          final Map<String, Object> fields,
          final Map<String, ValueSupplier<?>> dynamicFields) {
        super(apiHost, writeKey, dataset, sampleRate, null, fields, new HashMap<String, Object>());
        this.client = client;
        this.dynamicFields = new HashMap<>(dynamicFields);
    }

    @Override
    protected Event getSelf() {
        return this;
    }

    /**
     * Send this event - subject to sampling.
     * Note that it is possible to call this multiple times on the same instance, so be aware of how this might be
     * interpreted by honeycomb.
     *
     * @throws IllegalArgumentException if client-side validation fails, see {@link HoneyClient}'s class
     *                                  documentation for the "Event Validation" rules.
     */
    public void send() {
        this.client.sendEvent(this);
    }

    /**
     * Send this event - bypassing sampling.
     * Note that it is possible to call this multiple times on the same instance, so be aware of how this might be
     * interpreted by honeycomb.
     *
     * @throws IllegalArgumentException if client-side validation fails, see {@link HoneyClient}'s class
     *                                  documentation for the "Event Validation" rules.
     */
    public void sendPresampled() {
        this.client.sendEventPresampled(this);
    }

    /**
     * @param sampleRate to set.
     * @return this.
     * @see Options.Builder#setSampleRate(int)
     */
    @Override
    public Event setSampleRate(final int sampleRate) {
        super.setSampleRate(sampleRate);
        return getSelf();
    }

    Map<String, ValueSupplier<?>> getDynamicFields() {
        return dynamicFields;
    }

    @Override
    public String toString() {
        return "Event{" +
            "client=" + client +
            ", dynamicFields=" + dynamicFields +
            "} " + super.toString();
    }
}
