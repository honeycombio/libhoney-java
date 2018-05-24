package io.honeycomb.libhoney.utils;

import java.util.Map;

/**
 * Asserts class to avoid adding dependencies.
 */
public final class Assert {

    private Assert() {
        // utils class
    }

    public static void isTrue(final boolean test, final String msg) {
        if (!test) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void isFalse(final boolean test, final String msg) {
        if (test) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static <T> void notNull(final T object, final String msg) {
        if (object == null) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void state(final boolean test, final String msg) {
        if (!test) {
            throw new IllegalStateException(msg);
        }
    }

    public static void notEmpty(final String test, final String msg) {
        if (ObjectUtils.isNullOrEmpty(test)) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void notEmpty(final Map<?,?> test, final String msg) {
        if (test == null || test.isEmpty()) {
            throw new IllegalArgumentException(msg);
        }
    }
}
