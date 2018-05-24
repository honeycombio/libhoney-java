package io.honeycomb.libhoney.transport.json;

import java.io.IOException;


public interface JsonSerializer<T> {
    /**
     * Serialize a 'T' into a {@code byte} array that encodes JSON in UTF-8.
     *
     * @param data to serialise to json - must not be null.
     * @return bytes of the UTF-8 JSON string.
     * @throws IOException if any error occurs during serialization
     */
    byte[] serialize(final T data) throws IOException;
}
