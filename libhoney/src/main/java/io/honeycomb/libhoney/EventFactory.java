package io.honeycomb.libhoney;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * This is used to generate Events with a shared set of properties. An instance can be constructed using
 * {@link HoneyClient#buildEventFactory()}. Any properties declared on the {@link EventFactory.Builder} take precedence
 * over ones from the original {@link HoneyClient} instance.
 * <p>
 * An instance of EventFactory is immutable and threadsafe, but a modifiable copy can be obtained with {@link #copy()}.
 * <p>
 * <em>Note: If you develop with Honeycomb SDKs in other programming languages,
 * this Event Factory is usually simply called a "Builder".</em>
 */
public class EventFactory {
    private final HoneyClient client;
    private final URI apiHost;
    private final String writeKey;
    private final String dataset;
    private final int sampleRate;
    private final Map<String, Object> fields;
    private final Map<String, ValueSupplier<?>> dynamicFields;

    EventFactory(final HoneyClient client,
                 final URI apiHost,
                 final String writeKey,
                 final String dataset,
                 final int sampleRate,
                 final Map<String, Object> fields,
                 final Map<String, ValueSupplier<?>> dynamicFields) {
        this.client = client;
        this.apiHost = apiHost;
        this.writeKey = writeKey;
        this.dataset = dataset;
        this.sampleRate = sampleRate;
        this.fields = new HashMap<>(fields);
        this.dynamicFields = new HashMap<>(dynamicFields);
    }

    EventFactory(final HoneyClient client, final Options options) {
        this(client,
            options.getApiHost(),
            options.getWriteKey(),
            options.getDataset(),
            options.getSampleRate(),
            options.getGlobalFields(),
            options.getGlobalDynamicFields());
    }

    /**
     * Returns an {@link Event} populated with the settings and fields of this factory instance.
     *
     * @return an event.
     */
    public Event createEvent() {
        return new Event(client, apiHost, writeKey, dataset, sampleRate, fields, dynamicFields);
    }

    /**
     * Provides a shortcut to sending an event by populating it with the settings and fields of this factory instance
     * and adding the provided map of fields. It is also subject to the factory's sampling rate.
     *
     * @param fields to provide to the event.
     * @throws IllegalArgumentException if client-side validation fails, see {@link HoneyClient}'s class
     *                                  documentation for the "Event Validation" rules.
     */
    public void send(final Map<String, ?> fields) {
        createEvent()
            .addFields(fields)
            .send();
    }

    /**
     * Creates a builder for a new factory, pre-populated with the fields and settings of this factory instance,
     * that can be further customised.
     *
     * @return builder to create a new factory from.
     */
    public Builder copy() {
        return Builder.newBuilder(client)
            .setApiHost(apiHost)
            .setDataset(dataset)
            .setSampleRate(sampleRate)
            .setWriteKey(writeKey)
            .addDynamicFields(dynamicFields)
            .addFields(fields);
    }

    /**
     * Helper class to construct an {@link EventFactory}.
     * For an explanation of the properties see the javadoc of {@link Options} and {@link LibHoney}.
     */
    public static final class Builder {
        private final HoneyClient client;
        private final Map<String, Object> fields = new HashMap<>();
        private final Map<String, ValueSupplier<?>> dynamicFields = new HashMap<>();
        private URI apiHost;
        private String writeKey;
        private String dataset;
        private int sampleRate;

        private Builder(final HoneyClient client) {
            this.client = client;
        }

        /**
         * @return a new instance of EventFactory.
         */
        public EventFactory build() {
            return new EventFactory(client, apiHost, writeKey, dataset, sampleRate, fields, dynamicFields);
        }

        /**
         * @param apiHost to set.
         * @return this.
         * @see Options.Builder#setApiHost(URI)
         */
        public Builder setApiHost(final URI apiHost) {
            this.apiHost = apiHost;
            return this;
        }

        /**
         * @param writeKey to set.
         * @return this.
         * @see Options.Builder#setWriteKey(String)
         */
        public Builder setWriteKey(final String writeKey) {
            this.writeKey = writeKey;
            return this;
        }

        /**
         * @param dataset to set.
         * @return this.
         * @see Options.Builder#setDataset(String)
         */
        public Builder setDataset(final String dataset) {
            this.dataset = dataset;
            return this;
        }

        /**
         * @param sampleRate to set.
         * @return this.
         * @see Options.Builder#setSampleRate(int)
         */
        public Builder setSampleRate(final int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }


        /**
         * Adds all fields from the provided map.
         *
         * @param fields to add.
         * @return this.
         * @see Options.Builder#setGlobalFields(Map)
         */
        public Builder addFields(final Map<String, ?> fields) {
            this.fields.putAll(fields);
            return this;
        }

        /**
         * Adds the key-value as a field.
         *
         * @param fieldKey   to add.
         * @param fieldValue to add.
         * @return this.
         * @see Options.Builder#setGlobalFields(Map)
         */
        public Builder addField(final String fieldKey, final Object fieldValue) {
            fields.put(fieldKey, fieldValue);
            return this;
        }

        /**
         * Adds all dynamic fields from the provided map.
         *
         * @param dynamicFields to add.
         * @return this.
         * @see Options.Builder#setGlobalDynamicFields(Map)
         */
        public Builder addDynamicFields(final Map<String, ? extends ValueSupplier<?>> dynamicFields) {
            this.dynamicFields.putAll(dynamicFields);
            return this;
        }

        /**
         * Adds the key-value as a dynamic field.
         *
         * @param dynamicFieldKey      to add.
         * @param dynamicFieldSupplier to add.
         * @return this.
         * @see Options.Builder#setGlobalDynamicFields(Map)
         */
        public Builder addDynamicField(final String dynamicFieldKey,
                                       final ValueSupplier<?> dynamicFieldSupplier) {
            dynamicFields.put(dynamicFieldKey, dynamicFieldSupplier);
            return this;
        }

        public static Builder newBuilder(final HoneyClient client) {
            return new Builder(client);
        }
    }
}
