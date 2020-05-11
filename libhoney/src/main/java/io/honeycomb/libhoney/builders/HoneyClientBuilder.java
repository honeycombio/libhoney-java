package io.honeycomb.libhoney.builders;

import io.honeycomb.libhoney.DefaultDebugResponseObserver;
import io.honeycomb.libhoney.EventPostProcessor;
import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.Options;
import io.honeycomb.libhoney.ResponseObserver;
import io.honeycomb.libhoney.TransportOptions;
import io.honeycomb.libhoney.ValueSupplier;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HoneyClientBuilder {
    private List<ResponseObserver> responseObservers = new ArrayList<>();
    private boolean debugEnabled = false;

    // HoneyClient Options START
    private final Map<String, Object> globalFields = new HashMap<>();
    private final Map<String, ValueSupplier<?>> globalDynamicFields = new HashMap<>();
    // HoneyClient Options END

    private final Map<String, Credentials> credentialMap = new HashMap<>();
    // HoneyClient TransportOptions END

    TransportOptions.Builder transportOptionsBuilder = new TransportOptions.Builder();
    Options.Builder optionsBuilder = new Options.Builder();

    /**
     * Build new HoneyClient instance
     * @return the new HoneyClient instance
     */
    public HoneyClient build() {
        configureOptionBuilder();
        configureTransportOptionBuilder();
        final HoneyClient client = new HoneyClient(optionsBuilder.build(), transportOptionsBuilder.build());
        configureClient(client);
        return client;

    }

    private void configureClient(HoneyClient client) {
        if(!responseObservers.isEmpty()){
            for (ResponseObserver responseObserver : responseObservers) {
                client.addResponseObserver(responseObserver);
            }
        }
        if(debugEnabled){
            client.addResponseObserver(new DefaultDebugResponseObserver());
        }
    }

    private void configureTransportOptionBuilder() {
        if(!credentialMap.isEmpty()){
            transportOptionsBuilder.setCredentialsProvider(createCredentialsProvider());
        }
    }

    private CredentialsProvider createCredentialsProvider() {
        final BasicCredentialsProvider provider = new BasicCredentialsProvider();
        for (Map.Entry<String, Credentials> entry : credentialMap.entrySet()) {
            String proxy = entry.getKey();
            Credentials credential = entry.getValue();
            final HttpHost proxyHost = HttpHost.create(proxy);
            final AuthScope authScope = new AuthScope(proxyHost);
            provider.setCredentials(authScope, credential);
        }
        return provider;
    }

    private void configureOptionBuilder() {
        if(!globalFields.isEmpty()){
            optionsBuilder.setGlobalFields(globalFields);
        }
        if(!globalDynamicFields.isEmpty()){
            optionsBuilder.setGlobalDynamicFields(globalDynamicFields);
        }
    }

    public HoneyClientBuilder addGlobalField(String name, Object field) {
        this.globalFields.put(name, field);
        return this;
    }

    public HoneyClientBuilder addGlobalDynamicFields(String name, ValueSupplier<?> valueSupplier) {
        this.globalDynamicFields.put(name, valueSupplier);
        return this;
    }

    public HoneyClientBuilder addProxyCredential(String proxyHost, String username, String password) {
        final UsernamePasswordCredentials credential = new UsernamePasswordCredentials(username, password);
        credentialMap.put(proxyHost, credential);
        return this;
    }

    public HoneyClientBuilder sampleRate(int sampleRate) {
        optionsBuilder.setSampleRate(sampleRate);
        return this;
    }

    public HoneyClientBuilder eventPostProcessor(EventPostProcessor eventPostProcessor) {
        optionsBuilder.setEventPostProcessor(eventPostProcessor);
        return this;
    }

    public HoneyClientBuilder batchSize(int batchSize) {
        transportOptionsBuilder.setBatchSize(batchSize);
        return this;
    }

    public HoneyClientBuilder batchTimeoutMillis(long batchTimeoutMillis) {
        transportOptionsBuilder.setBatchTimeoutMillis(batchTimeoutMillis);
        return this;
    }

    public HoneyClientBuilder queueCapacity(int queueCapacity) {
        transportOptionsBuilder.setQueueCapacity(queueCapacity);
        return this;
    }

    public HoneyClientBuilder maximumPendingBatchRequests(int maximumPendingBatchRequests) {
        transportOptionsBuilder.setMaximumPendingBatchRequests(maximumPendingBatchRequests);
        return this;
    }

    public HoneyClientBuilder maxPendingBatchRequests(int maxPendingBatchRequests) {
        transportOptionsBuilder.setMaximumPendingBatchRequests(maxPendingBatchRequests);
        return this;
    }

    public HoneyClientBuilder maxConnections(int maxConnections) {
        transportOptionsBuilder.setMaxConnections(maxConnections);
        return this;
    }

    public HoneyClientBuilder maxConnectionsPerApiHost(int maxConnectionsPerApiHost) {
        transportOptionsBuilder.setMaxConnectionsPerApiHost(maxConnectionsPerApiHost);
        return this;
    }

    public HoneyClientBuilder connectTimeout(int connectTimeout) {
        transportOptionsBuilder.setConnectTimeout(connectTimeout);
        return this;
    }

    public HoneyClientBuilder connectionRequestTimeout(int connectionRequestTimeout) {
        transportOptionsBuilder.setConnectionRequestTimeout(connectionRequestTimeout);
        return this;
    }

    public HoneyClientBuilder socketTimeout(int socketTimeout) {
        transportOptionsBuilder.setSocketTimeout(socketTimeout);
        return this;
    }

    public HoneyClientBuilder bufferSize(int bufferSize) {
        transportOptionsBuilder.setBufferSize(bufferSize);
        return this;
    }

    public HoneyClientBuilder ioThreadCount(int ioThreadCount) {
        transportOptionsBuilder.setIoThreadCount(ioThreadCount);
        return this;
    }

    public HoneyClientBuilder maximumHttpRequestShutdownWait(long maximumHttpRequestShutdownWait) {
        transportOptionsBuilder.setMaximumHttpRequestShutdownWait(maximumHttpRequestShutdownWait);
        return this;
    }

    public HoneyClientBuilder additionalUserAgent(String additionalUserAgent) {
        transportOptionsBuilder.setAdditionalUserAgent(additionalUserAgent);
        return this;
    }

    public HoneyClientBuilder proxyNoCredentials(String proxyHost) {
        transportOptionsBuilder.setProxy(new HttpHost(proxyHost));
        return this;
    }

    public HoneyClientBuilder sslContext(SSLContext sslContext) {
        transportOptionsBuilder.setSSLContext(sslContext);
        return this;
    }

    public HoneyClientBuilder dataSet(String dataSet) {
        optionsBuilder.setDataset(dataSet);
        return this;
    }

    public HoneyClientBuilder apiHost(String apiHost) throws URISyntaxException {
        optionsBuilder.setApiHost(new URI(apiHost));
        return this;
    }

    public HoneyClientBuilder writeKey(String writeKey) {
        optionsBuilder.setWriteKey(writeKey);
        return this;
    }

    public HoneyClientBuilder debug(boolean flag){
        this.debugEnabled = flag;
        return this;
    }

    public HoneyClientBuilder addResponseObserver(ResponseObserver responseObserver) {
        responseObservers.add(responseObserver);
        return this;
    }


}
