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

    public HoneyClient build() throws IllegalArgumentException, URISyntaxException {
        configureOptionBuilder();
        configureTransportOptionBuilder();
        final HoneyClient client = new HoneyClient(optionsBuilder.build(), transportOptionsBuilder.build());
        customizeClient(client);
        return client;

    }

    private void customizeClient(HoneyClient client) {
        if(!responseObservers.isEmpty()){
            for (ResponseObserver responseObserver : responseObservers) {
                client.addResponseObserver(responseObserver);
            }
        }
        if(debugEnabled){
            client.addResponseObserver(new DefaultDebugResponseObserver());
        }
    }

    private TransportOptions.Builder configureTransportOptionBuilder() {
        final TransportOptions.Builder transportBuilder = new TransportOptions.Builder();

        if(!credentialMap.isEmpty()){

            transportBuilder.setCredentialsProvider(createCredentialsProvider());
        }
        return transportBuilder;
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

    private Options.Builder configureOptionBuilder() throws URISyntaxException {
        if(!globalFields.isEmpty()){
            optionsBuilder.setGlobalFields(globalFields);
        }
        if(!globalDynamicFields.isEmpty()){
            optionsBuilder.setGlobalDynamicFields(globalDynamicFields);
        }

        return optionsBuilder;
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

    public HoneyClientBuilder setSampleRate(int sampleRate) {
        optionsBuilder.setSampleRate(sampleRate);
        return this;
    }

    public HoneyClientBuilder setEventPostProcessor(EventPostProcessor eventPostProcessor) {
        optionsBuilder.setEventPostProcessor(eventPostProcessor);
        return this;
    }

    public HoneyClientBuilder setBatchSize(int batchSize) {
        transportOptionsBuilder.setBatchSize(batchSize);
        return this;
    }

    public HoneyClientBuilder setBatchTimeoutMillis(long batchTimeoutMillis) {
        transportOptionsBuilder.setBatchTimeoutMillis(batchTimeoutMillis);
        return this;
    }

    public HoneyClientBuilder setQueueCapacity(int queueCapacity) {
        transportOptionsBuilder.setQueueCapacity(queueCapacity);
        return this;
    }

    public HoneyClientBuilder setMaximumPendingBatchRequests(int maximumPendingBatchRequests) {
        transportOptionsBuilder.setMaximumPendingBatchRequests(maximumPendingBatchRequests);
        return this;
    }

    public HoneyClientBuilder setMaxPendingBatchRequests(int maxPendingBatchRequests) {
        transportOptionsBuilder.setMaximumPendingBatchRequests(maxPendingBatchRequests);
        return this;
    }

    public HoneyClientBuilder setMaxConnections(int maxConnections) {
        transportOptionsBuilder.setMaxConnections(maxConnections);
        return this;
    }

    public HoneyClientBuilder setMaxConnectionsPerApiHost(int maxConnectionsPerApiHost) {
        transportOptionsBuilder.setMaxConnectionsPerApiHost(maxConnectionsPerApiHost);
        return this;
    }

    public HoneyClientBuilder setConnectTimeout(int connectTimeout) {
        transportOptionsBuilder.setConnectTimeout(connectTimeout);
        return this;
    }

    public HoneyClientBuilder setConnectionRequestTimeout(int connectionRequestTimeout) {
        transportOptionsBuilder.setConnectionRequestTimeout(connectionRequestTimeout);
        return this;
    }

    public HoneyClientBuilder setSocketTimeout(int socketTimeout) {
        transportOptionsBuilder.setSocketTimeout(socketTimeout);
        return this;
    }

    public HoneyClientBuilder setBufferSize(int bufferSize) {
        transportOptionsBuilder.setBufferSize(bufferSize);
        return this;
    }

    public HoneyClientBuilder setIoThreadCount(int ioThreadCount) {
        transportOptionsBuilder.setIoThreadCount(ioThreadCount);
        return this;
    }

    public HoneyClientBuilder setMaximumHttpRequestShutdownWait(long maximumHttpRequestShutdownWait) {
        transportOptionsBuilder.setMaximumHttpRequestShutdownWait(maximumHttpRequestShutdownWait);
        return this;
    }

    public HoneyClientBuilder setAdditionalUserAgent(String additionalUserAgent) {
        transportOptionsBuilder.setAdditionalUserAgent(additionalUserAgent);
        return this;
    }

    public HoneyClientBuilder setProxyNoCredentials(String proxyHost) {
        transportOptionsBuilder.setProxy(new HttpHost(proxyHost));
        return this;
    }

    public HoneyClientBuilder setSSLContext(SSLContext sslContext) {
        transportOptionsBuilder.setSSLContext(sslContext);
        return this;
    }

    public HoneyClientBuilder setDataSet(String dataSet) {
        optionsBuilder.setDataset(dataSet);
        return this;
    }

    public HoneyClientBuilder setApiHost(String apiHost) throws URISyntaxException {
        optionsBuilder.setApiHost(new URI(apiHost));
        return this;
    }

    public HoneyClientBuilder setWriteKey(String writeKey) {
        optionsBuilder.setWriteKey(writeKey);
        return this;
    }

    public HoneyClientBuilder setDebugFlag(boolean flag){
        this.debugEnabled = flag;
        return this;
    }

    public HoneyClientBuilder addResponseObserver(ResponseObserver responseObserver) {
        responseObservers.add(responseObserver);
        return this;
    }


}
