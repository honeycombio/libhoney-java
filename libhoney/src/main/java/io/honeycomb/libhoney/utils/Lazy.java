package io.honeycomb.libhoney.utils;

/**
 * Lazily initialize an object in a thread-safe manner. Extend this class and implement the {@link #init()} method.
 * <p>
 * Based on Apache Commons Lang 3 code: {@code org.apache.commons.lang3.concurrent.LazyInitializer}.
 *
 * @param <T> the type of the object to be lazily initialized.
 */
public abstract class Lazy<T> {
    private volatile T object;

    /**
     * @return the lazily initialized object
     */
    public T get() {
        // use a temporary variable to reduce the number of reads of the
        // volatile field
        T result = object;

        if (result == null) {
            synchronized (this) {
                result = object;
                if (result == null) {
                    object = result = init();
                }
            }
        }

        return result;
    }

    /**
     * The initialization action.
     * @return the initialized object
     */
    protected abstract T init();
}
