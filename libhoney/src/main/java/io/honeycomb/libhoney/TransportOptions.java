package io.honeycomb.libhoney;

import io.honeycomb.libhoney.transport.batch.impl.DefaultBatcher;
import io.honeycomb.libhoney.transport.batch.impl.HoneycombBatchConsumer;
import io.honeycomb.libhoney.utils.Assert;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.client.CredentialsProvider;

import java.net.URI;
import javax.net.ssl.SSLContext;

import static io.honeycomb.libhoney.utils.ObjectUtils.getOrDefault;

/**
 * Holds configuration options for the transport layer {@link HoneyClient}, as well as their default values.
 * These are advanced options to configure the internals, namely the batching operation, as well as the http client.
 * For most applications the defaults will be good enough.
 *
 * @see DefaultBatcher
 * @see HoneycombBatchConsumer
 */
public class TransportOptions {
    /// batching defaults
    public static final int DEFAULT_BATCH_SIZE = 50;
    public static final long DEFAULT_BATCH_TIMEOUT = 100L;
    public static final int DEFAULT_QUEUE_CAPACITY = 10000;
    public static final int DEFAULT_MAX_PENDING_BATCH_REQUESTS = 250;

    /// HTTP client defaults
    public static final int DEFAULT_MAX_CONNECTIONS = 200;
    public static final int DEFAULT_MAX_CONNECTIONS_PER_API_HOST = 100;
    public static final int DEFAULT_CONNECT_TIMEOUT = 0;
    public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 0;
    public static final int DEFAULT_SOCKET_TIMEOUT = 3000;
    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    public static final int DEFAULT_IO_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    public static final long DEFAULT_MAX_HTTP_REQUEST_SHUTDOWN_WAIT = 2000L;
    public static final String DEFAULT_ADDITIONAL_USER_AGENT = "";

    /// batching properties
    private final int batchSize;
    private final long batchTimeoutMillis;
    private final int queueCapacity;
    private final int maxPendingBatchRequests;

    /// HTTP client properties
    private final int maxConnections;
    private final int maxConnectionsPerApiHost;
    private final int connectTimeout;
    private final int connectionRequestTimeout;
    private final int socketTimeout;
    private final int bufferSize;
    private final int ioThreadCount;
    private final long maximumHttpRequestShutdownWait;
    private final String additionalUserAgent;
    private final HttpHost proxy;
    private final SSLContext sslContext;
    private final CredentialsProvider credentialsProvider;

    // parameter list is fine, since it's only used by the builder
    @SuppressWarnings("PMD.ExcessiveParameterList")
    TransportOptions(final Integer batchSize,
                     final Long batchTimeoutMillis,
                     final Integer queueCapacity,
                     final Integer maxPendingBatchRequests,
                     final Integer maxConnections,
                     final Integer maxConnectionsPerApiHost,
                     final Integer connectTimeout,
                     final Integer connectionRequestTimeout,
                     final Integer socketTimeout,
                     final Integer bufferSize,
                     final Integer ioThreadCount,
                     final Long maximumHttpRequestShutdownWait,
                     final String additionalUserAgent,
                     final HttpHost proxy,
                     final SSLContext sslContext,
                     final CredentialsProvider credentialsProvider) {

        //Batching-specific
        this.batchSize = getOrDefault(batchSize, DEFAULT_BATCH_SIZE);
        this.batchTimeoutMillis = getOrDefault(batchTimeoutMillis, DEFAULT_BATCH_TIMEOUT);
        this.queueCapacity = getOrDefault(queueCapacity, DEFAULT_QUEUE_CAPACITY);
        this.maxPendingBatchRequests = getOrDefault(maxPendingBatchRequests, DEFAULT_MAX_PENDING_BATCH_REQUESTS);

        //HTTP client-specific
        this.maxConnections = getOrDefault(maxConnections, DEFAULT_MAX_CONNECTIONS);
        this.maxConnectionsPerApiHost = getOrDefault(maxConnectionsPerApiHost, DEFAULT_MAX_CONNECTIONS_PER_API_HOST);
        this.connectTimeout = getOrDefault(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
        this.connectionRequestTimeout = getOrDefault(connectionRequestTimeout, DEFAULT_CONNECTION_REQUEST_TIMEOUT);
        this.socketTimeout = getOrDefault(socketTimeout, DEFAULT_SOCKET_TIMEOUT);
        this.bufferSize = getOrDefault(bufferSize, DEFAULT_BUFFER_SIZE);
        this.ioThreadCount = getOrDefault(ioThreadCount, DEFAULT_IO_THREAD_COUNT);
        this.maximumHttpRequestShutdownWait = getOrDefault(maximumHttpRequestShutdownWait,
            DEFAULT_MAX_HTTP_REQUEST_SHUTDOWN_WAIT);
        this.additionalUserAgent = getOrDefault(additionalUserAgent, DEFAULT_ADDITIONAL_USER_AGENT);
        this.proxy = proxy;
        this.sslContext = sslContext;
        this.credentialsProvider = credentialsProvider;

        Assert.isTrue(this.batchSize >= 1, "batchSize must be 1 or greater");
        Assert.isTrue(this.batchTimeoutMillis >= 1, "batchTimeoutMillis must be 1 or greater");
        Assert.isTrue(this.queueCapacity >= 1, "queueCapacity must be 1 or greater");
        Assert.isFalse(this.maxPendingBatchRequests == 0, "maxPendingBatchRequests must not be 0");
        Assert.isFalse(this.maxPendingBatchRequests < -1, "maxPendingBatchRequests must not be less than -1");

        Assert.isTrue(this.maxConnections >= 1, "maxConnections must be 1 or greater");
        Assert.isTrue(this.maxConnectionsPerApiHost >= 1, "maxConnectionsPerApiHost must be 1 or greater");
        Assert.isTrue(this.bufferSize >= 1024, "bufferSize must be 1024 or greater");
        Assert.isTrue(
            (this.ioThreadCount >= 1) && (this.ioThreadCount <= Runtime.getRuntime().availableProcessors()),
            "ioThreadCount must be at least 1 and at most the number of available CPU cores");
        Assert.isTrue(this.maximumHttpRequestShutdownWait > 0,
            "maximumHttpRequestShutdownWait must be positive");
    }

    /**
     * @return batch size.
     * @see TransportOptions.Builder#setBatchSize(int)
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * @return batch timeout (ms).
     * @see TransportOptions.Builder#setBatchTimeoutMillis(long)
     */
    public long getBatchTimeoutMillis() {
        return batchTimeoutMillis;
    }

    /**
     * @return queue capacity.
     * @see TransportOptions.Builder#setQueueCapacity(int)
     */
    public int getQueueCapacity() {
        return queueCapacity;
    }

    /**
     * @return maximum pending connections.
     * @see TransportOptions.Builder#setMaximumPendingBatchRequests(int)
     */
    public int getMaxPendingBatchRequests() {
        return maxPendingBatchRequests;
    }

    /**
     * @return max connections.
     * @see TransportOptions.Builder#setMaxConnections(int)
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * @return max connections per api gost.
     * @see TransportOptions.Builder#setMaxConnectionsPerApiHost(int)
     */
    public int getMaxHttpConnectionsPerApiHost() {
        return maxConnectionsPerApiHost;
    }

    /**
     * @return connection timeout.
     * @see TransportOptions.Builder#setConnectTimeout(int)
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * @return connection request timeout.
     * @see TransportOptions.Builder#setConnectionRequestTimeout(int)
     */
    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    /**
     * @return socket timeout.
     * @see TransportOptions.Builder#setSocketTimeout(int)
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * @return buffer size.
     * @see TransportOptions.Builder#setBufferSize(int)
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * @return io thread count.
     * @see TransportOptions.Builder#setIoThreadCount(int)
     */
    public int getIoThreadCount() {
        return ioThreadCount;
    }

    /**
     * @return maximum http request shutdown wait
     * @see TransportOptions.Builder#setMaximumHttpRequestShutdownWait(long)
     */
    public long getMaximumHttpRequestShutdownWait() {
        return maximumHttpRequestShutdownWait;
    }

    public String getAdditionalUserAgent() {
        return additionalUserAgent;
    }

    public HttpHost getProxy() {
        return proxy;
    }

    public SSLContext getSSLContext() {
            return sslContext;
    }

    public CredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    static TransportOptions.Builder builder() {
        return new TransportOptions.Builder();
    }

    @Override
    public String toString() {
        return "TransportOptions{" +
            "batchSize=" + batchSize +
            ", batchTimeoutMillis=" + batchTimeoutMillis +
            ", queueCapacity=" + queueCapacity +
            ", maxPendingBatchRequests=" + maxPendingBatchRequests +
            ", maxConnections=" + maxConnections +
            ", maxConnectionsPerApiHost=" + maxConnectionsPerApiHost +
            ", connectTimeout=" + connectTimeout +
            ", connectionRequestTimeout=" + connectionRequestTimeout +
            ", socketTimeout=" + socketTimeout +
            ", bufferSize=" + bufferSize +
            ", ioThreadCount=" + ioThreadCount +
            ", maximumHttpRequestShutdownWait=" + maximumHttpRequestShutdownWait +
            ", additionalUserAgent=" + additionalUserAgent +
            '}';
    }

    /**
     * Helper class to construct {@link TransportOptions}.
     */
    public static class Builder {
        /// batching properties
        private Integer batchSize;
        private Long batchTimeoutMillis;
        private Integer queueCapacity;
        private Integer maximumPendingBatchRequests;

        /// HTTP client properties
        private Integer maxConnections;
        private Integer maxConnectionsPerApiHost;
        private Integer connectTimeout;
        private Integer connectionRequestTimeout;
        private Integer socketTimeout;
        private Integer bufferSize;
        private Integer ioThreadCount;
        private Long maximumHttpRequestShutdownWait;
        private String additionalUserAgent;
        private HttpHost proxy;
        private SSLContext sslContext;
        private CredentialsProvider credentialsProvider;

        /**
         * This creates a {@link TransportOptions} instance.
         *
         * @return A finished {@link TransportOptions}.
         * @throws IllegalArgumentException if the configuration fails validation.
         */
        public TransportOptions build() {
            return new TransportOptions(
                batchSize,
                batchTimeoutMillis,
                queueCapacity,
                maximumPendingBatchRequests,
                maxConnections,
                maxConnectionsPerApiHost,
                connectTimeout,
                connectionRequestTimeout,
                socketTimeout,
                bufferSize,
                ioThreadCount,
                maximumHttpRequestShutdownWait,
                additionalUserAgent,
                proxy,
                sslContext,
                credentialsProvider);
        }

        /**
         * @return the currently set batchSize.
         * @see TransportOptions.Builder#setBatchSize(int)
         */
        public Integer getBatchSize() {
            return batchSize;
        }

        /**
         * This determines that maximum number of events that get sent to the Honeycomb server (via a batch request).
         * In other words, this is a trigger that will cause a batch request to be created if a batch reaches this
         * maximum size.
         * <p>
         * Also see {@link #setBatchTimeoutMillis}, as that might cause batch request to be created
         * earlier (triggering on time rather than space).
         * <p>
         * Note: Events are grouped into batches that have the same write key, dataset name and API host.
         * See {@link Event#setWriteKey(String)}, {@link Event#setDataset(String)}, and {@link Event#setApiHost(URI)}.
         *
         * @param batchSize to set.
         * @return this.
         */
        public TransportOptions.Builder setBatchSize(final int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        /**
         * @return the currently set batchTimeoutMillis.
         * @see TransportOptions.Builder#setBatchTimeoutMillis(long)
         */
        public Long getBatchTimeoutMillis() {
            return batchTimeoutMillis;
        }

        /**
         * If batches do no fill up to the batch size in time (as defined by {@link Builder#setBatchSize(int)}), then
         * this timeout will trigger a batch request to the Honeycomb server. Essentially, for batches that fill
         * slowly, this ensures that there is a temporal upper bound to when events are sent via a batch request.
         * The time is measured in milliseconds.
         * <p>
         * Note: Events are grouped into batches that have the same write key, dataset name and API host.
         * See {@link Event#setWriteKey(String)}, {@link Event#setDataset(String)}, and {@link Event#setApiHost(URI)}.
         * <p>
         * Default: 100
         *
         * @param batchTimeoutMillis to set.
         * @return this.
         */
        public TransportOptions.Builder setBatchTimeoutMillis(final long batchTimeoutMillis) {
            this.batchTimeoutMillis = batchTimeoutMillis;
            return this;
        }

        /**
         * @return the currently set queueCapacity.
         * @see TransportOptions.Builder#setQueueCapacity(int)
         */
        public Integer getQueueCapacity() {
            return queueCapacity;
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
         * @param queueCapacity to set.
         * @return this.
         * @see io.honeycomb.libhoney.responses.ClientRejected.RejectionReason#QUEUE_OVERFLOW
         * @see io.honeycomb.libhoney.transport.batch.BatchConsumer#consume(java.util.List)
         */
        public TransportOptions.Builder setQueueCapacity(final int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        /**
         * @return the currently set maxPendingBatchRequests
         * @see io.honeycomb.libhoney.transport.batch.BatchConsumer#consume(java.util.List)
         */
        public Integer getMaximumPendingBatchRequests() {
            return maximumPendingBatchRequests;
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
         * This configuration differs from {@link Builder#getMaxConnections()} in that a batch request may be pending
         * completion, but it may be still be waiting for an HTTP connection. This is the case in the default
         * {@link HoneycombBatchConsumer} where the
         * {@link org.apache.http.nio.client.HttpAsyncClient} maintains an internal unbounded pending queue for
         * requests that are waiting for a connection. This configuration effectively puts a bound on the total
         * number of batch requests being serviced by the HTTP client, regardless of whether they have
         * a connection or not.
         * <p>
         * Default: 250
         *
         * @param maximumPendingBatchRequests to set.
         * @return this.
         * @see Builder#setMaxConnections(int)
         */
        public TransportOptions.Builder setMaximumPendingBatchRequests(final int maximumPendingBatchRequests) {
            this.maximumPendingBatchRequests = maximumPendingBatchRequests;
            return this;
        }


        /**
         * @return the currently set maxConnections.
         * @see TransportOptions.Builder#setMaxConnections(int)
         */
        public int getMaxConnections() {
            return maxConnections;
        }

        /**
         * Set this to define the maximum amount of connections the http client may hold in its connection pool.
         * In effect this is the maximum level of concurrent HTTP requests that may be in progress at any given time.
         * <p>
         * Default: 200
         *
         * @param maxConnections to set.
         * @return this.
         * @see HttpAsyncClientBuilder#setMaxConnTotal(int)
         */
        public TransportOptions.Builder setMaxConnections(final int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        /**
         * @return the currently set maxConnectionsPerApiHost.
         * @see TransportOptions.Builder#setMaxConnectionsPerApiHost(int)
         */
        public int getMaxConnectionsPerApiHost() {
            return maxConnectionsPerApiHost;
        }

        /**
         * Set this to define the maximum amount of connections the http client may hold in its connection pool for a
         * given hostname.
         * In effect this limits how many concurrent requests may be sent to a single host.
         * <p>
         * Default: 100
         *
         * @param maxConnectionsPerApiHost to set.
         * @return this.
         * @see HttpAsyncClientBuilder#setMaxConnPerRoute(int)
         */
        public TransportOptions.Builder setMaxConnectionsPerApiHost(final int maxConnectionsPerApiHost) {
            this.maxConnectionsPerApiHost = maxConnectionsPerApiHost;
            return this;
        }

        /**
         * @return the currently set connectTimeout.
         * @see TransportOptions.Builder#setConnectTimeout(int)
         */
        public int getConnectTimeout() {
            return connectTimeout;
        }

        /**
         * Set this to define the http client's connect timeout in milliseconds. Set to 0 for no timeout.
         * <p>
         * Default: 0
         *
         * @param connectTimeout to set.
         * @return this.
         * @see RequestConfig#getConnectTimeout()
         */
        public TransportOptions.Builder setConnectTimeout(final int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * @return the currently set connectionRequestTimeout.
         * @see TransportOptions.Builder#setConnectionRequestTimeout(int)
         */
        public int getConnectionRequestTimeout() {
            return connectionRequestTimeout;
        }

        /**
         * Set this to define the http client's connection request timeout in milliseconds. This defines the maximum
         * time that a batch request can wait for a connection from the connection manager after
         * submission to the HTTP client. Set to 0 for no timeout.
         * <p>
         * Beware that setting this to a non-zero value might conflict with the backpressure effect of the
         * {@link io.honeycomb.libhoney.transport.batch.BatchConsumer} implementation, and so might see an increase
         * in failed batches. See {@link #setMaximumPendingBatchRequests(int)} for more detail.
         * <p>
         * Default: 0
         *
         * @param connectionRequestTimeout to set.
         * @return this.
         * @see RequestConfig#getConnectionRequestTimeout()
         */
        public TransportOptions.Builder setConnectionRequestTimeout(final int connectionRequestTimeout) {
            this.connectionRequestTimeout = connectionRequestTimeout;
            return this;
        }

        /**
         * @return the currently set socketTimeout.
         * @see TransportOptions.Builder#setSocketTimeout(int)
         */
        public int getSocketTimeout() {
            return socketTimeout;
        }

        /**
         * Set this to define the http client's socket timeout in milliseconds. Set to 0 for no timeout.
         * <p>
         * Default: 3000
         *
         * @param socketTimeout to set.
         * @return this.
         * @see RequestConfig#getSocketTimeout()
         */
        public TransportOptions.Builder setSocketTimeout(final int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        /**
         * @return the currently set bufferSize.
         * @see TransportOptions.Builder#setBufferSize(int)
         */
        public int getBufferSize() {
            return bufferSize;
        }

        /**
         * Set this to define the http client's socket buffer size in bytes.
         * <p>
         * Default: 8192
         *
         * @param bufferSize to set.
         * @return this.
         * @see org.apache.http.config.ConnectionConfig.Builder#setBufferSize(int)
         */
        public TransportOptions.Builder setBufferSize(final int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        /**
         * @return the currently set ioThreadCount.
         * @see TransportOptions.Builder#setBatchTimeoutMillis(long)
         */
        public Integer getIoThreadCount() {
            return ioThreadCount;
        }

        /**
         * Set this to define the http client's io thread count. This is usually set to the number of CPU cores.
         * <p>
         * Default: System CPU cores.
         *
         * @param ioThreadCount to set, must be between 1 and the system's number of CPU cores.
         * @return this.
         * @see <a href="https://hc.apache.org/httpcomponents-core-ga/tutorial/html/nio.html">Apache http client NIO</a>
         * @see IOReactorConfig#getIoThreadCount()
         */
        public TransportOptions.Builder setIoThreadCount(final int ioThreadCount) {
            this.ioThreadCount = ioThreadCount;
            return this;
        }

        /**
         * @return the currently set maximumHttpRequestShutdownWait.
         * @see TransportOptions.Builder#setMaximumHttpRequestShutdownWait(long)
         */
        public Long getMaximumHttpRequestShutdownWait() {
            return maximumHttpRequestShutdownWait;
        }

        /**
         * Defines the maximum time (in milliseconds) that we should wait for any pending HTTP requests to complete
         * during the client shutdown process.
         * <p>
         * Any requests that are still pending at the end of this wait period will be terminated.
         * <p>
         * Default: 2000
         *
         * @param maximumHttpRequestShutdownWait to set.
         * @return this.
         */
        public TransportOptions.Builder setMaximumHttpRequestShutdownWait(final long maximumHttpRequestShutdownWait) {
            this.maximumHttpRequestShutdownWait = maximumHttpRequestShutdownWait;
            return this;
        }

        /**
         * @return the currently set additional user agent string.
         * @see TransportOptions.Builder#setAdditionalUserAgent
         */
        public String getAdditionalUserAgent() {
            return additionalUserAgent;
        }

        /**
         * Set this to add an additional component to the user agent header sent to Honeycomb when Events are submitted.
         * This is usually only of interest for instrumentation libraries that wrap LibHoney.
         * <p>
         * Default: None
         *
         * @param additionalUserAgent to set.
         * @return this.
         */
        public TransportOptions.Builder setAdditionalUserAgent(final String additionalUserAgent) {
            this.additionalUserAgent = additionalUserAgent;
            return this;
        }

        public HttpHost getProxy() {
            return proxy;
        }

        public TransportOptions.Builder setProxy(final HttpHost proxy) {
            this.proxy = proxy;
            return this;
        }

        public SSLContext getSSLContext() {
            return sslContext;
        }

        public TransportOptions.Builder setSSLContext(final SSLContext sslContext) {
            this.sslContext = sslContext;
        }

        public CredentialsProvider getCredentialsProvider() {
            return credentialsProvider;
        }

        public TransportOptions.Builder setCredentialsProvider(final CredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }
    }
}
