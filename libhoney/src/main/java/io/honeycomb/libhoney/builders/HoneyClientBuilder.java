package io.honeycomb.libhoney.builders;

import io.honeycomb.libhoney.DefaultDebugResponseObserver;
import io.honeycomb.libhoney.Event;
import io.honeycomb.libhoney.EventFactory;
import io.honeycomb.libhoney.EventPostProcessor;
import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.LibHoney;
import io.honeycomb.libhoney.Options;
import io.honeycomb.libhoney.ResponseObserver;
import io.honeycomb.libhoney.TransportOptions;
import io.honeycomb.libhoney.ValueSupplier;
import io.honeycomb.libhoney.transport.Transport;
import io.honeycomb.libhoney.transport.batch.impl.HoneycombBatchConsumer;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HoneyClientBuilder {
    private final Map<String, Object> globalFields = new HashMap<>();
    private final Map<String, ValueSupplier<?>> globalDynamicFields = new HashMap<>();
    private final Map<String, Credentials> credentialMap = new HashMap<>();
    private final List<ResponseObserver> responseObservers = new ArrayList<>();
    protected TransportOptions.Builder transportOptionsBuilder = new TransportOptions.Builder();
    protected Options.Builder optionsBuilder = new Options.Builder();
    private Transport transport = null;
    private boolean debugEnabled = false;

    /**
     * Build new HoneyClient instance as configured by calling the various builder methods previous to this call.
     * <h3>Example</h3>
     * <pre>{@code
     * HoneyClient client = new HoneyClientBuilder()
     *                          .dataSet("dataset")
     *                          .writeKey("write key")
     *                          .addProxy("proxy.domain.com")
     *                          .build()}</pre>
     *
     * @return new HoneyClient instance
     */
    public HoneyClient build() {
        configureOptionBuilder();
        configureTransportOptionBuilder();
        final HoneyClient client = createClient();
        configureClient(client);
        return client;

    }

    private HoneyClient createClient() {
        if(transport==null){
            return new HoneyClient(optionsBuilder.build(), transportOptionsBuilder.build());
        }
        return new HoneyClient(optionsBuilder.build(), transport);
    }

    private void configureClient(final HoneyClient client) {
        if (!responseObservers.isEmpty()) {
            for (final ResponseObserver responseObserver : responseObservers) {
                client.addResponseObserver(responseObserver);
            }
        }
        if (debugEnabled) {
            client.addResponseObserver(new DefaultDebugResponseObserver());
        }
    }

    private void configureTransportOptionBuilder() {
        if (!credentialMap.isEmpty()) {
            transportOptionsBuilder.setCredentialsProvider(createCredentialsProvider());
        }
    }

    private CredentialsProvider createCredentialsProvider() {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        for (final Map.Entry<String, Credentials> entry : credentialMap.entrySet()) {
            final String proxy = entry.getKey();
            final Credentials credential = entry.getValue();
            final HttpHost proxyHost = HttpHost.create(proxy);
            final AuthScope authScope = new AuthScope(proxyHost);
            provider.setCredentials(authScope, credential);
        }
        return provider;
    }

    private void configureOptionBuilder() {
        if (!globalFields.isEmpty()) {
            optionsBuilder.setGlobalFields(globalFields);
        }
        if (!globalDynamicFields.isEmpty()) {
            optionsBuilder.setGlobalDynamicFields(globalDynamicFields);
        }
    }

    /**
     * Use this to add fields to all events, where both keys and values are fixed.
     * Entries may be overridden before the event is sent to the server. See "Usage" on {@link HoneyClient}'s
     * class documentation.
     * <p>
     * Default: None
     *
     * @param name  the "key".
     * @param field the "value"
     * @return HoneyClientBuilder instance
     * @see Options.Builder#setGlobalFields(java.util.Map)
     */
    public HoneyClientBuilder addGlobalField(final String name, final Object field) {
        this.globalFields.put(name, field);
        return this;
    }

    /**
     * Use this method to supply fields to events, where keys are fixed but values are dynamically created at runtime.
     * Entries may be overridden before the event is sent to the server. See "Usage" on {@link HoneyClient}'s
     * class documentation.
     * <p>
     * Default: None
     *
     * @param name          the "key"
     * @param valueSupplier calculates value
     * @return HoneyClientBuilder instance
     * @see Options.Builder#setGlobalDynamicFields(java.util.Map)
     */
    public HoneyClientBuilder addGlobalDynamicFields(final String name, final ValueSupplier<?> valueSupplier) {
        this.globalDynamicFields.put(name, valueSupplier);
        return this;
    }

    /**
     * Use this method to configure the HTTP client to use a proxy that needs authentication.
     * <p>
     * For configuring a proxy server without authentication see: {@link #addProxy(String)}
     *
     * @param proxyHost hostname of the proxy, frequently FQDN of the server
     * @param username  username for authentication with proxy server
     * @param password  password for authentication with proxy server
     * @return HoneyClientBuilder instance
     */
    public HoneyClientBuilder addProxy(final String proxyHost, final String username, final String password) {
        final UsernamePasswordCredentials credential = new UsernamePasswordCredentials(username, password);
        credentialMap.put(proxyHost, credential);
        return this;
    }

    /**
     * SampleRate is the rate at which to sample this event. Default is 1, meaning no sampling.
     * The probability of sending is {@code 1/sampleRate}. In other words, if one out of every 250 events is to be
     * sent when Send() is called, you would specify set sample rate to 250.
     * <p>
     * Must be greater than 1<br>
     * Default: 1
     *
     * @param sampleRate average number of events until sample is taken.
     * @return HoneyClientBuilder instance
     */
    public HoneyClientBuilder sampleRate(final int sampleRate) {
        optionsBuilder.setSampleRate(sampleRate);
        return this;
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
    public HoneyClientBuilder eventPostProcessor(final EventPostProcessor eventPostProcessor) {
        optionsBuilder.setEventPostProcessor(eventPostProcessor);
        return this;
    }

    /**
     * This determines that maximum number of events that get sent to the Honeycomb server (via a batch request).
     * In other words, this is a trigger that will cause a batch request to be created if a batch reaches this
     * maximum size.
     * <p>
     * Also see {@link io.honeycomb.libhoney.TransportOptions.Builder#setBatchTimeoutMillis}, as that might cause batch request to be created
     * earlier (triggering on time rather than space).
     * <p>
     * Note: Events are grouped into batches that have the same write key, dataset name and API host.
     * See {@link Event#setWriteKey(String)}, {@link Event#setDataset(String)}, and {@link Event#setApiHost(URI)}.
     *
     * @param batchSize max number of events to send in single data transmission
     * @return HoneyClientBuilder instance
     */
    public HoneyClientBuilder batchSize(final int batchSize) {
        transportOptionsBuilder.setBatchSize(batchSize);
        return this;
    }

    /**
     * If batches do no fill up to the batch size in time (as defined by {@link TransportOptions.Builder#setBatchSize(int)}), then
     * this timeout will trigger a batch request to the Honeycomb server. Essentially, for batches that fill
     * slowly, this ensures that there is a temporal upper bound to when events are sent via a batch request.
     * The time is measured in milliseconds.
     * <p>
     * Note: Events are grouped into batches that have the same write key, dataset name and API host.
     * See {@link Event#setWriteKey(String)}, {@link Event#setDataset(String)}, and {@link Event#setApiHost(URI)}.
     * <p>
     * Default: 100
     *
     * @param batchTimeoutMillis max milliseconds to send a non-empty but not-full batch.
     * @return HoneyClientBuilder instance
     */
    public HoneyClientBuilder batchTimeoutMillis(final long batchTimeoutMillis) {
        transportOptionsBuilder.setBatchTimeoutMillis(batchTimeoutMillis);
        return this;
    }

    /**
     * This sets the capacity of the queue that events are submitted to before they get processed for batching
     * and eventually sent to the honeycomb HTTP endpoint.
     * <p>
     * Under normal circumstances this queue should remain near empty, but in case of heavy load it acts as a
     * bounded buffer against a build up of backpressure from the batching and http client implementation.
     * <p>
     * Default: 10000
     *
     * @param queueCapacity queue size.
     * @return HoneyClientBuilder instance
     * @see io.honeycomb.libhoney.responses.ClientRejected.RejectionReason#QUEUE_OVERFLOW
     * @see io.honeycomb.libhoney.transport.batch.BatchConsumer#consume(java.util.List)
     */
    public HoneyClientBuilder queueCapacity(final int queueCapacity) {
        transportOptionsBuilder.setQueueCapacity(queueCapacity);
        return this;
    }


    /**
     * This determines the maximum number of batch requests that can be still pending completion at any one time.
     * Set to -1 if there is no maximum, i.e. the number of batch requests pending completion is allowed to grow
     * without bound.
     * <p>
     * Once a batch request has been triggered (see {@link TransportOptions.Builder#setBatchSize(int)} and
     * {@link TransportOptions.Builder#setBatchTimeoutMillis(long)}), then the batch request is submitted
     * to {@link io.honeycomb.libhoney.transport.batch.BatchConsumer#consume(java.util.List)}.
     * <p>
     * If the maximum pending requests is reached, then
     * {@link io.honeycomb.libhoney.transport.batch.BatchConsumer#consume(java.util.List)} may block until the number of
     * pending requests has dropped below the threshold.
     * <p>
     * This allows backpressure to be created if the {@link io.honeycomb.libhoney.transport.batch.BatchConsumer}
     * implementation cannot keep up with the number of batch requests being submitted. The intended consequence of
     * this is that the event queue may reach its capacity and overflow.
     * <p>
     * See {@link TransportOptions.Builder#setQueueCapacity(int)}.
     * <p>
     * This configuration differs from {@link TransportOptions.Builder#getMaxConnections()} in that a batch request may be pending
     * completion, but it may be still be waiting for an HTTP connection. This is the case in the default
     * {@link HoneycombBatchConsumer} where the
     * {@link org.apache.http.nio.client.HttpAsyncClient} maintains an internal unbounded pending queue for
     * requests that are waiting for a connection. This configuration effectively puts a bound on the total
     * number of batch requests being serviced by the HTTP client, regardless of whether they have
     * a connection or not.
     * <p>
     * Default: 250
     *
     * @param maxPendingBatchRequests max simultaneous requests.
     * @return HoneyClientBuilder instance
     * @see TransportOptions.Builder#setMaxConnections(int)
     */
    public HoneyClientBuilder maxPendingBatchRequests(final int maxPendingBatchRequests) {
        transportOptionsBuilder.setMaximumPendingBatchRequests(maxPendingBatchRequests);
        return this;
    }

    /**
     * Set this to define the maximum amount of connections the http client may hold in its connection pool.
     * In effect this is the maximum level of concurrent HTTP requests that may be in progress at any given time.
     * <p>
     * Default: 200
     *
     * @param maxConnections maximum number of connections.
     * @return HoneyClientBuilder instance
     * @see HttpAsyncClientBuilder#setMaxConnTotal(int)
     */
    public HoneyClientBuilder maxConnections(final int maxConnections) {
        transportOptionsBuilder.setMaxConnections(maxConnections);
        return this;
    }

    /**
     * Set this to define the maximum amount of connections the http client may hold in its connection pool for a
     * given hostname.
     * In effect this limits how many concurrent requests may be sent to a single host.
     * <p>
     * Default: 100
     *
     * @param maxConnectionsPerApiHost pool size for per hostname
     * @return HoneyClientBuilder instance
     * @see HttpAsyncClientBuilder#setMaxConnPerRoute(int)
     */
    public HoneyClientBuilder maxConnectionsPerApiHost(final int maxConnectionsPerApiHost) {
        transportOptionsBuilder.setMaxConnectionsPerApiHost(maxConnectionsPerApiHost);
        return this;
    }

    /**
     * Set this to define the http client's connect timeout in milliseconds. Set to 0 for no timeout.
     * <p>
     * Default: 0
     *
     * @param connectTimeout to set.
     * @return HoneyClientBuilder instance
     * @see RequestConfig#getConnectTimeout()
     */
    public HoneyClientBuilder connectionTimeout(final int connectTimeout) {
        transportOptionsBuilder.setConnectTimeout(connectTimeout);
        return this;
    }

    /**
     * Set this to define the http client's connection request timeout in milliseconds. This defines the maximum
     * time that a batch request can wait for a connection from the connection manager after
     * submission to the HTTP client. Set to 0 for no timeout.
     * <p>
     * Beware that setting this to a non-zero value might conflict with the backpressure effect of the
     * {@link io.honeycomb.libhoney.transport.batch.BatchConsumer} implementation, and so might see an increase
     * in failed batches. See {@link #maxPendingBatchRequests(int)} for more detail.
     * <p>
     * Default: 0
     *
     * @param connectionRequestTimeout to set.
     * @return HoneyClientBuilder instance
     * @see RequestConfig#getConnectionRequestTimeout()
     */
    public HoneyClientBuilder connectionRequestTimeout(final int connectionRequestTimeout) {
        transportOptionsBuilder.setConnectionRequestTimeout(connectionRequestTimeout);
        return this;
    }

    /**
     * Set this to define the http client's socket timeout in milliseconds. Set to 0 for no timeout.
     * <p>
     * Default: 3000
     *
     * @param socketTimeout to set.
     * @return HoneyClientBuilder instance
     * @see RequestConfig#getSocketTimeout()
     */
    public HoneyClientBuilder socketTimeout(final int socketTimeout) {
        transportOptionsBuilder.setSocketTimeout(socketTimeout);
        return this;
    }

    /**
     * Set this to define the http client's socket buffer size in bytes.
     * <p>
     * Default: 8192
     *
     * @param bufferSize to set.
     * @return HoneyClientBuilder instance
     * @see org.apache.http.config.ConnectionConfig.Builder#setBufferSize(int)
     */
    public HoneyClientBuilder bufferSize(final int bufferSize) {
        transportOptionsBuilder.setBufferSize(bufferSize);
        return this;
    }

    /**
     * Set this to define the http client's io thread count. This is usually set to the number of CPU cores.
     * <p>
     * Default: System CPU cores.
     *
     * @param ioThreadCount to set, must be between 1 and the system's number of CPU cores.
     * @return HoneyClientBuilder instance
     * @see <a href="https://hc.apache.org/httpcomponents-core-ga/tutorial/html/nio.html">Apache http client NIO</a>
     * @see IOReactorConfig#getIoThreadCount()
     */
    public HoneyClientBuilder ioThreadCount(final int ioThreadCount) {
        transportOptionsBuilder.setIoThreadCount(ioThreadCount);
        return this;
    }

    /**
     * Defines the maximum time (in milliseconds) that we should wait for any pending HTTP requests to complete
     * during the client shutdown process.
     * <p>
     * Any requests that are still pending at the end of this wait period will be terminated.
     * <p>
     * Default: 2000
     *
     * @param maximumHttpRequestShutdownWait milliseconds.
     * @return HoneyClientBuilder instance
     * @see TransportOptions.Builder#setMaximumHttpRequestShutdownWait(long)
     */
    public HoneyClientBuilder maximumHttpRequestShutdownWait(final long maximumHttpRequestShutdownWait) {
        transportOptionsBuilder.setMaximumHttpRequestShutdownWait(maximumHttpRequestShutdownWait);
        return this;
    }

    /**
     * Set this to add an additional component to the user agent header sent to Honeycomb when Events are submitted.
     * This is usually only of interest for instrumentation libraries that wrap LibHoney.
     * <p>
     * Default: None
     *
     * @param additionalUserAgent added to the user agent on http request header.
     * @return HoneyClientBuilder instance
     * @see TransportOptions.Builder#setAdditionalUserAgent(java.lang.String)
     */
    public HoneyClientBuilder additionalUserAgent(final String additionalUserAgent) {
        transportOptionsBuilder.setAdditionalUserAgent(additionalUserAgent);
        return this;
    }

    /**
     * Use this method to configure the HTTP client to use a proxy without authentication.
     * <p>
     * For configuring a proxy server with authentication see: {@link #addProxy(String, String, String)}
     *
     * @param host hostname of the proxy, frequently FQDN of the server
     * @return HoneyClientBuilder instance
     */
    public HoneyClientBuilder addProxy(final String host) {
        transportOptionsBuilder.setProxy(new HttpHost(host));
        return this;
    }

    public HoneyClientBuilder sslContext(final SSLContext sslContext) {
        transportOptionsBuilder.setSSLContext(sslContext);
        return this;
    }

    /**
     * Dataset is the name of the Honeycomb dataset to which to send these events.
     * If it is specified during {@link LibHoney} initialization, it will be used as the default dataset for all
     * events. If absent, dataset must be explicitly set on an {@link EventFactory} or {@link Event}.
     *
     * @param dataSet to set.
     * @return HoneyClientBuilder instance
     * @see Options.Builder#setDataset(java.lang.String)
     * <p>
     * Default: None
     */
    public HoneyClientBuilder dataSet(final String dataSet) {
        optionsBuilder.setDataset(dataSet);
        return this;
    }

    /**
     * APIHost is the hostname for the Honeycomb API server to which to send this event.
     * <p>
     * Default: {@code https://api.honeycomb.io/}
     *
     * @param apiHost to set.
     * @return HoneyClientBuilder instance
     * @throws URISyntaxException if host is not valid URI syntax
     * @see Options.Builder#setApiHost(java.net.URI)
     */
    public HoneyClientBuilder apiHost(final String apiHost) throws URISyntaxException {
        optionsBuilder.setApiHost(new URI(apiHost));
        return this;
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
     * @return HoneyClientBuilder instance
     * @see Options.Builder#setWriteKey(java.lang.String)
     */
    public HoneyClientBuilder writeKey(final String writeKey) {
        optionsBuilder.setWriteKey(writeKey);
        return this;
    }

    /**
     * Setting Debug to true will ensure the DefaultDebugResponseObserver to the HoneyClient's list of response observers.
     * Default: false
     *
     * @param enabled true to enable debug response observer
     * @return HoneyClientBuilder instance
     */
    public HoneyClientBuilder debug(final boolean enabled) {
        this.debugEnabled = enabled;
        return this;
    }

    public HoneyClientBuilder addResponseObserver(final ResponseObserver responseObserver) {
        responseObservers.add(responseObserver);
        return this;
    }

    /**
     * Transport for sending events to HoneyComb. Used by the {@link io.honeycomb.libhoney.HoneyClient} internals.
     * This can also be used to disable sending events to Honeycomb by passing in a mock Transport.
     */
    public HoneyClientBuilder transport(final Transport transport){
        this.transport = transport;
        return this;
    }


}
