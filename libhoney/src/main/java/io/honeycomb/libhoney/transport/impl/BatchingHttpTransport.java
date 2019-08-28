package io.honeycomb.libhoney.transport.impl;

import io.honeycomb.libhoney.TransportOptions;
import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.responses.ResponseObservable;
import io.honeycomb.libhoney.transport.Transport;
import io.honeycomb.libhoney.transport.batch.BatchConsumer;
import io.honeycomb.libhoney.transport.batch.Batcher;
import io.honeycomb.libhoney.transport.batch.ClockProvider;
import io.honeycomb.libhoney.transport.batch.impl.DefaultBatcher;
import io.honeycomb.libhoney.transport.batch.impl.HoneycombBatchConsumer;
import io.honeycomb.libhoney.transport.batch.impl.HoneycombBatchKeyStrategy;
import io.honeycomb.libhoney.transport.batch.impl.SystemClockProvider;
import io.honeycomb.libhoney.transport.json.BatchRequestSerializer;
import io.honeycomb.libhoney.transport.json.JsonSerializer;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * The default {@link Transport} used by the SDK.
 * Batches incoming events
 */
public class BatchingHttpTransport implements Transport {
    private static final Logger LOG = LoggerFactory.getLogger(BatchingHttpTransport.class);
    private final Batcher<ResolvedEvent> batcher;
    private final BatchConsumer<ResolvedEvent> consumer;
    private final ResponseObservable responseObservable;

    public BatchingHttpTransport(final Batcher<ResolvedEvent> batcher,
                                 final BatchConsumer<ResolvedEvent> consumer,
                                 final ResponseObservable responseObservable) {
        this.batcher = batcher;
        this.consumer = consumer;
        this.responseObservable = responseObservable;
    }

    @Override
    public boolean submit(final ResolvedEvent event) {
        event.markEnqueueTime();
        return batcher.offerEvent(event);
    }

    @Override
    public ResponseObservable getResponseObservable() {
        return responseObservable;
    }

    @Override
    public void close() throws Exception {
        LOG.debug("Close called on BatchingHTTPTransport. Closing batcher.");
        batcher.close();
        LOG.debug("Closing BatchConsumer.");
        consumer.close();
        LOG.debug("Closing ResponseObservers and ResponseObservable");
        responseObservable.close();
        LOG.debug("Finished close");
    }

    public static BatchingHttpTransport init(final TransportOptions options) {
        return init(options, new BatchRequestSerializer());
    }

    /**
     * Construct a {@link BatchingHttpTransport} while overriding the {@link JsonSerializer} used by the transport
     * to convert batches of events into HTTP requests. This is an option for advanced user who want more
     * fine-grained control over JSON serialization. For instance, the user may want to exploit Jackson POJO
     * annotations.
     *
     * @param options the transport options
     * @param batchRequestSerializer for serializing event batches to JSON
     * @return the transport
     */
    public static BatchingHttpTransport init(final TransportOptions options, final JsonSerializer<List<HoneycombBatchConsumer.BatchRequestElement>> batchRequestSerializer) {
        // create various components that comprise consumer and batcher
        final ResponseObservable responseObservable = new ResponseObservable();
        final CloseableHttpAsyncClient httpAsyncClient = buildClient(options);
        httpAsyncClient.start();
        final HoneycombBatchKeyStrategy batchKeyStrategy = new HoneycombBatchKeyStrategy();
        final ClockProvider systemClockProvider = SystemClockProvider.getInstance();

        final HoneycombBatchConsumer honeycombBatchConsumer = new HoneycombBatchConsumer(
            httpAsyncClient,
            responseObservable,
            batchRequestSerializer,
            options.getMaxPendingBatchRequests(),
            options.getMaximumHttpRequestShutdownWait(),
            options.getAdditionalUserAgent());

        final Batcher<ResolvedEvent> batcher = new DefaultBatcher<>(
            batchKeyStrategy,
            honeycombBatchConsumer,
            systemClockProvider,
            new ArrayBlockingQueue<ResolvedEvent>(options.getQueueCapacity()),
            options.getBatchSize(),
            options.getBatchTimeoutMillis());

        return new BatchingHttpTransport(batcher, honeycombBatchConsumer, responseObservable);
    }

    public static CloseableHttpAsyncClient buildClient(final TransportOptions options) {
        return HttpAsyncClients.custom()
            .setMaxConnTotal(options.getMaxConnections())
            .setMaxConnPerRoute(options.getMaxHttpConnectionsPerApiHost())
            .setConnectionManagerShared(false)
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setProxy(options.getProxy())
                    .setConnectTimeout(options.getConnectTimeout())
                    .setConnectionRequestTimeout(options.getConnectionRequestTimeout())
                    .setSocketTimeout(options.getSocketTimeout())
                    .build()
            )
            .setDefaultIOReactorConfig(IOReactorConfig.custom()
                .setIoThreadCount(options.getIoThreadCount())
                .build())
            .setDefaultConnectionConfig(
                ConnectionConfig.custom()
                    .setBufferSize(options.getBufferSize())
                    .build()
            )
            .setDefaultCredentialsProvider(options.getCredentialsProvider())
            .build();
    }

}
