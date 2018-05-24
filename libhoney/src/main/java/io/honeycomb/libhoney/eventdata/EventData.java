package io.honeycomb.libhoney.eventdata;

import io.honeycomb.libhoney.Event;
import io.honeycomb.libhoney.Options.Builder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class to hold properties common to both {@link Event} and {@link ResolvedEvent}.
 *
 * @param <T> That allows subclasses to fix the type parameter, so that builder methods in this class return the
 *            subclass type T.
 */
public abstract class EventData<T extends EventData<T>> {
    private final Map<String, Object> fields;
    private URI apiHost;
    private String writeKey;
    private String dataset;
    private int sampleRate;
    private Long timestamp;
    private Map<String, Object> metadata;

    protected EventData(final URI apiHost,
                        final String writeKey,
                        final String dataset,
                        final int sampleRate,
                        final Long timestamp,
                        final Map<String, Object> fields,
                        final Map<String, Object> metadata) {
        this.apiHost = apiHost;
        this.writeKey = writeKey;
        this.dataset = dataset;
        this.sampleRate = sampleRate;
        this.timestamp = timestamp;
        this.metadata = (metadata == null) ? new HashMap<String, Object>(): metadata;
        this.fields = new HashMap<>(fields);
    }

    protected abstract T getSelf();

    protected T setSampleRate(final int sampleRate) {
        this.sampleRate = sampleRate;
        return getSelf();
    }

    /**
     * @param apiHost to set.
     * @return this.
     * @see Builder#setApiHost(URI)
     */
    public T setApiHost(final URI apiHost) {
        this.apiHost = apiHost;
        return getSelf();
    }

    /**
     * @param writeKey to set.
     * @return this.
     * @see Builder#setWriteKey(String)
     */
    public T setWriteKey(final String writeKey) {
        this.writeKey = writeKey;
        return getSelf();
    }

    /**
     * @param dataset to set.
     * @return this.
     * @see Builder#setDataset(String)
     */
    public T setDataset(final String dataset) {
        this.dataset = dataset;
        return getSelf();
    }

    /**
     * Sets an explicit timestamp. The instant the timestamp describes must be measured in milliseconds since the epoch.
     * Not setting this means a timestamp set to "now" will be generated on send().
     *
     * @param timestamp to set (millis since the epoch).
     * @return this.
     * @see System#currentTimeMillis()
     */
    public T setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
        return getSelf();
    }

    /**
     * @param fieldKey   to add.
     * @param fieldValue to add.
     * @return this.
     * @see Builder#setGlobalFields(Map)
     */
    public T addField(final String fieldKey, final Object fieldValue) {
        fields.put(fieldKey, fieldValue);
        return getSelf();
    }

    /**
     * @param fields to add.
     * @return this.
     * @see Builder#setGlobalFields(Map)
     */
    public T addFields(final Map<String, ?> fields) {
        this.fields.putAll(fields);
        return getSelf();
    }

    /**
     * @param metadataKey   to add.
     * @param metadataValue to add.
     * @return this.
     * Add the provided key-value the event metadata.
     */
    public T addMetadata(final String metadataKey, final Object metadataValue) {
        this.metadata.put(metadataKey, metadataValue);
        return getSelf();
    }

    /**
     * @param metadata to add.
     * @return this.
     * Add the provided map of metadata to this event.
     */
    public T addMetadata(final Map<String, ?> metadata) {
        this.metadata.putAll(metadata);
        return getSelf();
    }

    public URI getApiHost() {
        return apiHost;
    }

    public String getWriteKey() {
        return writeKey;
    }

    public String getDataset() {
        return dataset;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        return metadata;
    }

    @Override
    public String toString() {
        return "EventData{" +
            "fields=" + fields +
            ", apiHost=" + apiHost +
            ", writeKey=**********" +
            ", dataset='" + dataset + '\'' +
            ", sampleRate=" + sampleRate +
            ", timestamp=" + timestamp +
            ", metadata=" + metadata +
            '}';
    }
}
