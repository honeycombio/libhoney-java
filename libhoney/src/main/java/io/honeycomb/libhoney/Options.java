package io.honeycomb.libhoney;

import io.honeycomb.libhoney.utils.Assert;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.honeycomb.libhoney.utils.ObjectUtils.getOrDefault;

/**
 * Holds configuration options for {@link HoneyClient}, as well as their default values.
 * <p>
 * All configuration options are optional, and unless explicitly set, default values will be used.
 * For details on each option see the setter methods of the {@link Options.Builder} class,
 * as well as the {@link HoneyClient} class documentation.
 */
public class Options {
    /// honeyclient defaults
    public static final URI DEFAULT_API_HOST = URI.create("https://api.honeycomb.io/");
    public static final String DEFAULT_WRITE_KEY = null;
    public static final String DEFAULT_DATASET = null;
    public static final String DEFAULT_NON_CLASSIC_DATASET = "unknown_dataset";
    public static final int DEFAULT_SAMPLE_RATE = 1;
    public static final Map<String, Object> DEFAULT_FIELDS = Collections.emptyMap();
    public static final Map<String, ValueSupplier<?>> DEFAULT_DYNAMIC_FIELDS = Collections.emptyMap();
    public static final EventPostProcessor DEFAULT_EVENT_POST_PROCESSOR = null;

    /// honeyclient properties
    private final URI apiHost;
    private final String writeKey;
    private final String dataset;
    private final int sampleRate;
    private final Map<String, Object> globalFields;
    private final Map<String, ValueSupplier<?>> globalDynamicFields;
    private final EventPostProcessor eventPostProcessor;

    Options(final URI apiHost,
            final String writeKey,
            final String dataset,
            final Integer sampleRate,
            final Map<String, Object> globalFields,
            final Map<String, ValueSupplier<?>> globalDynamicFields,
            final EventPostProcessor eventPostProcessor) {
        this.apiHost = getOrDefault(apiHost, DEFAULT_API_HOST);
        this.writeKey = getOrDefault(writeKey, DEFAULT_WRITE_KEY);
        this.dataset = getOrDefault(dataset, DEFAULT_DATASET);
        this.globalFields = new HashMap<>(getOrDefault(globalFields, DEFAULT_FIELDS));
        this.globalDynamicFields = new HashMap<>(getOrDefault(globalDynamicFields, DEFAULT_DYNAMIC_FIELDS));
        this.sampleRate = getOrDefault(sampleRate, DEFAULT_SAMPLE_RATE);
        this.eventPostProcessor = getOrDefault(eventPostProcessor, DEFAULT_EVENT_POST_PROCESSOR);

        Assert.isTrue(this.sampleRate >= 1, "sampleRate must be 1 or greater");
    }

    private boolean isClassic() {
        return writeKey == null || writeKey.length() == 0 || writeKey.length() == 32;
    }

    /**
     * @return api host.
     * @see Builder#setApiHost(URI)
     */
    public URI getApiHost() {
        return apiHost;
    }

    /**
     * @return write key.
     * @see Builder#setWriteKey(String)
     */
    public String getWriteKey() {
        return writeKey;
    }

    /**
     * @return dataset string.
     * @see Builder#setDataset(String)
     */
    public String getDataset() {
        if (isClassic()) {
            return dataset;
        }
        if (dataset == null || dataset.trim().length() == 0) {
            System.err.println("WARN: Dataset is empty or whitespace, using default: " + DEFAULT_NON_CLASSIC_DATASET);
            return DEFAULT_NON_CLASSIC_DATASET;
        }
        String trimmed = dataset.trim();
        if (dataset != trimmed) {
            System.err.println("WARN: Dataset has unexpected whitespace, using trimmed version: " + trimmed);
        }
        return trimmed;
    }

    /**
     * @return sample rate.
     * @see Builder#setSampleRate(int)
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * @return global fields map.
     * @see Builder#setGlobalFields(Map)
     */
    public Map<String, Object> getGlobalFields() {
        return globalFields;
    }

    /**
     * @return global dynamic fields map.
     * @see Builder#setGlobalDynamicFields(Map)
     */
    public Map<String, ValueSupplier<?>> getGlobalDynamicFields() {
        return globalDynamicFields;
    }

    /**
     * @return post processor.
     * @see Builder#setEventPostProcessor(EventPostProcessor)
     */
    public EventPostProcessor getEventPostProcessor() {
        return eventPostProcessor;
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "Options{" +
            "apiHost=" + apiHost +
            ", writeKey=**********" +
            ", dataset='" + getDataset() + '\'' +
            ", sampleRate=" + sampleRate +
            ", globalFields=" + globalFields +
            ", globalDynamicFields=" + globalDynamicFields +
            ", eventPostProcessor=" + eventPostProcessor +
            '}';
    }

    /**
     * Helper class to construct {@link Options}.
     */
    public static class Builder {
        private URI apiHost;
        private String writeKey;
        private String dataset;
        private Integer sampleRate;
        private Map<String, Object> globalFields;
        private Map<String, ValueSupplier<?>> globalDynamicFields;
        private EventPostProcessor eventPostProcessor;

        /**
         * This creates a {@link Options} instance.
         *
         * @return A finished {@link Options}.
         * @throws IllegalArgumentException if the configuration fails validation.
         */
        public Options build() {
            return new Options(
                apiHost,
                writeKey,
                dataset,
                sampleRate,
                globalFields,
                globalDynamicFields,
                eventPostProcessor);
        }

        /**
         * @return the currently set apiHost.
         * @see Builder#setApiHost(URI)
         */
        public URI getApiHost() {
            return apiHost;
        }

        /**
         * APIHost is the hostname for the Honeycomb API server to which to send this event.
         * <p>
         * Default: {@code https://api.honeycomb.io/}
         *
         * @param apiHost to set.
         * @return this.
         */
        public Builder setApiHost(final URI apiHost) {
            this.apiHost = apiHost;
            return this;
        }

        /**
         * @return the currently set writeKey.
         * @see Builder#setWriteKey(String)
         */
        public String getWriteKey() {
            return writeKey;
        }

        /**
         * WriteKey is the Honeycomb authentication token.
         * If it is specified during {@link LibHoney} initialization, it will be used as the default write key for all
         * events.
         * If absent, write key must be explicitly set on a builder or event.
         * Find your team write key at https://ui.honeycomb.io/account
         * <p>
         * Default: None
         *
         * @param writeKey to set.
         * @return this.
         */
        public Builder setWriteKey(final String writeKey) {
            this.writeKey = writeKey;
            return this;
        }

        /**
         * @return the currently set dataset.
         * @see Builder#setDataset(String)
         */
        public String getDataset() {
            return dataset;
        }

        /**
         * Dataset is the name of the Honeycomb dataset to which to send these events.
         * If it is specified during {@link LibHoney} initialization, it will be used as the default dataset for all
         * events. If absent, dataset must be explicitly set on an {@link EventFactory} or {@link Event}.
         * <p>
         * Default: None
         *
         * @param dataset to set.
         * @return this.
         */
        public Builder setDataset(final String dataset) {
            this.dataset = dataset;
            return this;
        }

        /**
         * @return the currently set sampleRate.
         * @see Builder#setSampleRate(int)
         */
        public Integer getSampleRate() {
            return sampleRate;
        }

        /**
         * SampleRate is the rate at which to sample this event. Default is 1, meaning no sampling.
         * The probability of sending is {@code 1/sampleRate}. In other words, if one out of every 250 events is to be
         * sent when Send() is called, you would specify set sample rate to 250.
         * <p>
         * Must be greater than 1<br>
         * Default: 1
         *
         * @param sampleRate to set.
         * @return this.
         */
        public Builder setSampleRate(final int sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        /**
         * @return the currently set global fields.
         * @see Builder#setGlobalFields
         */
        public Map<String, ?> getGlobalFields() {
            return globalFields;
        }

        /**
         * Set this to supply fields to all events, where both keys and values are fixed.
         * Entries may be overridden before the event is sent to the server. See "Usage" on {@link HoneyClient}'s
         * class documentation.
         * <p>
         * Default: None
         *
         * @param globalFields to set.
         * @return this.
         */
        public Builder setGlobalFields(final Map<String, ?> globalFields) {
            this.globalFields = new HashMap<>(globalFields);
            return this;
        }

        /**
         * @return the currently set globalDynamicFields.
         * @see Builder#setGlobalDynamicFields
         */
        public Map<String, ValueSupplier<?>> getGlobalDynamicFields() {
            return globalDynamicFields;
        }

        /**
         * Set this to supply fields to events, where keys are fixed but values are dynamically created at runtime.
         * Entries may be overridden before the event is sent to the server. See "Usage" on {@link HoneyClient}'s
         * class documentation.
         * <p>
         * Default: None
         *
         * @param globalDynamicFields to set.
         * @return this.
         */
        public Builder setGlobalDynamicFields(final Map<String, ? extends ValueSupplier<?>> globalDynamicFields) {
            this.globalDynamicFields = new HashMap<>(globalDynamicFields);
            return this;
        }

        /**
         * @return the currently set eventPostProcessor.
         * @see Builder#setEventPostProcessor
         */
        public EventPostProcessor getEventPostProcessor() {
            return eventPostProcessor;
        }

        /**
         * Set this to apply post processing to any event about to be submitted to Honeycomb.
         * See {@link EventPostProcessor} for details.
         * <p>
         * Default: None
         *
         * @param eventPostProcessor to set.
         * @return this.
         */
        public Builder setEventPostProcessor(final EventPostProcessor eventPostProcessor) {
            this.eventPostProcessor = eventPostProcessor;
            return this;
        }
    }

}
