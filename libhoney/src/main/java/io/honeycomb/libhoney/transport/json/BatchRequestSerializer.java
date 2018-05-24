package io.honeycomb.libhoney.transport.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.honeycomb.libhoney.transport.batch.impl.HoneycombBatchConsumer.BatchRequestElement;
import io.honeycomb.libhoney.utils.JsonUtils;

import java.io.IOException;
import java.util.List;

/**
 * A serializer that uses Jackson to serializes a list of {@link BatchRequestElement} into
 * a valid json request body for the Honeycomb API.
 */
public class BatchRequestSerializer implements JsonSerializer<List<BatchRequestElement>> {
    private static final ObjectWriter OBJECT_WRITER;

    static {
        OBJECT_WRITER = JsonUtils.OBJECT_MAPPER.writerFor(new TypeReference<List<BatchRequestElement>>() {});
    }

    @Override
    public byte[] serialize(final List<BatchRequestElement> data) throws IOException {
        return OBJECT_WRITER.writeValueAsBytes(data);
    }
}
