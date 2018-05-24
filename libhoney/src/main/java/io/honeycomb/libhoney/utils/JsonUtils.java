package io.honeycomb.libhoney.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtils {
    /**
     * Object mapper for JSON de/serialization used globally throughout the SDK.
     * This instance is time-consuming to construct, but thread-safe.
     *
     * Users are advised to construct an {@link com.fasterxml.jackson.databind.ObjectWriter} or
     * {@link com.fasterxml.jackson.databind.ObjectReader} for particular use-cases rather than using the ObjectMapper
     * directly.
     */
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private JsonUtils() {
        // utils
    }
}
