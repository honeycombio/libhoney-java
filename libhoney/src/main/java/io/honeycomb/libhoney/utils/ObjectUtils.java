package io.honeycomb.libhoney.utils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public final class ObjectUtils {

    private ObjectUtils() {
        // utils class
    }

    public static <T> T getOrDefault(final T object, final T def) {
        return (object == null) ? def : object;
    }

    public static long getOrDefault(final Long number, final long def) {
        return (number == null) ? def : number;
    }

    public static int getOrDefault(final Integer number, final int def) {
        return (number == null) ? def : number;
    }

    public static boolean isNullOrEmpty(final String string) {
        return (string == null) || string.isEmpty();
    }

    /**
     * This returns a new SimpleDateFormat that we can use to format timestamps into an RFC3339 datetime string,
     * compatible with the honeycomb API. Beware that SimpleDateFormat is not thread safe.
     *
     * @return a formatter for a, RFC3339 date, compatible with the honeycomb API.
     */
    public static SimpleDateFormat getRFC3339DateTimeFormatter() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH);
    }

    public static Map<String, Object> nullsafe(final Map<String, Object> eventMetadata) {
        return (eventMetadata == null) ? Collections.<String, Object>emptyMap() : eventMetadata;
    }
}
