package io.honeycomb.libhoney;

/**
 * <h1>The entry point to the honeycomb client library, used to create a {@link HoneyClient}.</h1>
 * Use {@link #options()} to set configuration options, and pass them to {@link #create(Options)} to create a
 * {@link HoneyClient} instance. The configuration options are documented on {@link Options.Builder}'s setters.
 * All configuration options are optional, and {@link Options} holds the default values for any that do not get
 * explicitly set. <br>
 * Usage and other details are documented on {@link HoneyClient}.
 *
 * <h2>Default client</h2>
 * You can use {@link #setDefault(HoneyClient)} (or call {@link HoneyClient#setAsDefault()} directly on an instance)
 * to set a globally accessible client instance, then retrievable via {@link #getDefault()}.
 */
public final class LibHoney {
    private static volatile HoneyClient defaultClient;

    private LibHoney() {
        // static utils
    }

    /**
     * Static method to obtain an instance of {@link HoneyClient}.
     *
     * @param options to use to customise the instance.
     * @return a HoneyClient.
     */
    public static HoneyClient create(final Options options) {
        return new HoneyClient(options);
    }

    /**
     * Static method to obtain an instance of {@link HoneyClient} with more advanced configuration options.
     * To most applications the advanced configuration options are not relevant.
     *
     * @param options          to use to customise applications settings of the instance.
     * @param transportOptions to use to customise the transport layer of the instance.
     * @return a HoneyClient.
     */
    public static HoneyClient create(final Options options, final TransportOptions transportOptions) {
        return new HoneyClient(options, transportOptions);
    }

    /**
     * Static method to obtain an instance of the builder of {@link Options}.
     *
     * @return a builder for a HoneyClient {@link Options}.
     */
    public static Options.Builder options() {
        return Options.builder();
    }

    /**
     * Static method to obtain an instance of the builder of {@link TransportOptions}, which offers more advanced
     * configuration options for customising the {@link HoneyClient} transport.
     *
     * @return a builder for {@link HoneyClient} {@link TransportOptions}.
     */
    public static TransportOptions.Builder transportOptions() {
        return TransportOptions.builder();
    }

    /**
     * Can be used to set a {@link HoneyClient} instance as a global default, for easier static access.
     *
     * @param client to set as the default.
     */
    public static void setDefault(final HoneyClient client) {
        defaultClient = client;
    }

    /**
     * Gets the current default client. This method requires the default to have been set previously
     * with {@link #setDefault(HoneyClient)}, else an {@link IllegalStateException} will be thrown.
     *
     * @return the current default instance.
     * @throws IllegalStateException if no default has been set.
     */
    public static HoneyClient getDefault() {
        if (defaultClient == null) {
            throw new IllegalStateException("Default LibHoney has not been initialised");
        } else {
            return defaultClient;
        }
    }

    /**
     * Utility to add a JVM shutdown hook that invokes {@link HoneyClient#close()} on the provided client instance.
     * Note that this will keep a strong reference to the instance, so should only be used if it is expected to live
     * for the lifetime of the application.
     *
     * @param client to close when the JVM shuts down.
     */
    public static void closeOnShutdown(final HoneyClient client) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                client.close();
            }
        }));
    }

}
