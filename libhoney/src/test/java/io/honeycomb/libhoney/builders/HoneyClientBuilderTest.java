package io.honeycomb.libhoney.builders;

import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.Options;
import io.honeycomb.libhoney.TransportOptions;
import io.honeycomb.libhoney.ValueSupplier;
import io.honeycomb.libhoney.responses.ResponseObservable;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HoneyClientBuilderTest {

    @Mock
    ResponseObservable mockObservable;
    Options.Builder optionBuilder;
    TransportOptions.Builder transportBuilder;

    private HoneyClientBuilder builder;

    @Before
    public void setUp() {
        builder = new HoneyClientBuilder();
        transportBuilder = builder.transportOptionsBuilder = spy(builder.transportOptionsBuilder);
        optionBuilder = builder.optionsBuilder = spy(builder.optionsBuilder);

        when(optionBuilder.build()).thenCallRealMethod();
        when(transportBuilder.build()).thenCallRealMethod();
    }

    @Test
    public void testAddProxyCredentials() {
        final HoneyClient client = builder.addProxyCredential("host:80", "user", "pass").build();
        verify(transportBuilder, times(1)).setCredentialsProvider(any(CredentialsProvider.class));
        final CredentialsProvider actualValue = transportBuilder.getCredentialsProvider();
        final Credentials credentials = actualValue.getCredentials(AuthScope.ANY);
        Assert.assertNotNull("Expected proxy credential to be found", credentials);
        Assert.assertEquals("Expected proxy user to match", "user", credentials.getUserPrincipal().getName());
        Assert.assertEquals("Expected password to match", "pass", credentials.getPassword());
        verify(transportBuilder, times(1)).getCredentialsProvider();
        completeNegativeVerification();
    }

    @Test
    public void testAddGlobalDynamicField() {
        final ValueSupplier<Object> supplier1 = new ValueSupplier<Object>() {
            @Override
            public Object supply() {
                return null;
            }
        };
        final HoneyClient client = builder.addGlobalDynamicFields("name", supplier1).build();
        verify(optionBuilder, times(1)).setGlobalDynamicFields(any(Map.class));
        final Map<String, ValueSupplier<?>> actualFields = optionBuilder.getGlobalDynamicFields();
        verify(optionBuilder, times(1)).getGlobalDynamicFields();
        Assert.assertEquals("Expected 1 global dynamic field", 1, actualFields.size());
        Assert.assertTrue("Expected value supplier key to exist", actualFields.containsKey("name"));
        Assert.assertSame("Expected value supplier to match", supplier1, actualFields.get("name"));
        completeNegativeVerification();
    }

    @Test
    public void testAddGlobalField() {
        final HoneyClient client = builder.addGlobalField("name", "value").build();
        final Map<String, Object> fields = client.createEvent().getFields();

        Assert.assertEquals("Expected one field", 1, fields.size());
        Assert.assertEquals("Expected map to match", Collections.singletonMap("name", "value"), fields);
        verify(optionBuilder, times(1)).setGlobalFields(any(Map.class));
        completeNegativeVerification();
    }

    @Test
    public void testMultipleGlobalFields() {
        final HoneyClient client = builder
            .addGlobalField("name", "value")
            .addGlobalField("name2", "value2")
            .build();
        final Map<String, Object> fields = client.createEvent().getFields();
        Assert.assertEquals("Expected two global fields", 2, fields.size());
        Assert.assertTrue("Expected name as key", fields.containsKey("name"));
        Assert.assertTrue("Expected name2 as key", fields.containsKey("name2"));
        Assert.assertEquals("Expected value to match", "value", fields.get("name"));
        Assert.assertEquals("Expected value to match", "value2", fields.get("name2"));
        verify(optionBuilder, times(1)).setGlobalFields(any(Map.class));
        completeNegativeVerification();
    }

    @Test
    public void testWriteKey() {
        final HoneyClient client = builder.writeKey("testWriteKey").build();
        Assert.assertEquals("Expected write key", "testWriteKey", client.createEvent().getWriteKey());
        verify(optionBuilder, times(1)).setWriteKey("testWriteKey");
        completeNegativeVerification();
    }

    @Test
    public void testApiHost() throws URISyntaxException {
        final HoneyClient client = builder.apiHost("HOST:80").build();
        Assert.assertEquals("Expected host to be present", "HOST:80", client.createEvent().getApiHost().toString());
        verify(optionBuilder, times(1)).setApiHost(any(URI.class));
        completeNegativeVerification();
    }

    @Test
    public void testdataSet() {
        final HoneyClient client = builder.dataSet("set").build();
        Assert.assertEquals("Expected dataset to be set", "set", client.createEvent().getDataset());
        verify(optionBuilder, times(1)).setDataset("set");
        completeNegativeVerification();
    }

    @Test
    public void testSSLContext() {
        final SSLContext mockContext = mock(SSLContext.class);
        final HoneyClient client = builder.sslContext(mockContext).build();
        verify(transportBuilder, times(1)).setSSLContext(any(SSLContext.class));
        completeNegativeVerification();
    }

    @Test
    public void testProxyNoCredential() {
        final HoneyClient client = builder.proxyNoCredentials("proxyHost").build();
        verify(transportBuilder, times(1)).setProxy(any(HttpHost.class));
        completeNegativeVerification();
    }

    @Test
    public void testAdditionalUserAgent() {
        final HoneyClient client = builder.additionalUserAgent("agent").build();
        verify(transportBuilder, times(1)).setAdditionalUserAgent("agent");
        completeNegativeVerification();
    }

    @Test
    public void testMaximumHttpRequestShutdownWait() {
        final HoneyClient client = builder.maximumHttpRequestShutdownWait(345L).build();
        verify(transportBuilder, times(1)).setMaximumHttpRequestShutdownWait(345L);
        completeNegativeVerification();
    }

    @Test
    public void testIoThreadCount() {
        final HoneyClient client = builder.ioThreadCount(1).build();
        verify(transportBuilder, times(1)).setIoThreadCount(1);
        completeNegativeVerification();
    }

    @Test
    public void testBufferSize() {
        final HoneyClient client = builder.bufferSize(5_000).build();
        verify(transportBuilder, times(1)).setBufferSize(5_000);
        completeNegativeVerification();
    }

    @Test
    public void testSocketTimeout() {
        final HoneyClient client = builder.socketTimeout(123).build();
        verify(transportBuilder, times(1)).setSocketTimeout(123);
        completeNegativeVerification();
    }

    @Test
    public void testConnectionRequestTimeout() {
        final HoneyClient client = builder.connectionRequestTimeout(123).build();
        verify(transportBuilder, times(1)).setConnectionRequestTimeout(123);
        completeNegativeVerification();
    }

    @Test
    public void testMaximumPendingBatchRequests() {
        final HoneyClient client = builder.maxPendingBatchRequests(123).build();
        verify(transportBuilder, times(1)).setMaximumPendingBatchRequests(123);
        completeNegativeVerification();
    }

    @Test
    public void testMaxConnections() {
        final HoneyClient client = builder.maxConnections(123).build();
        verify(transportBuilder, times(1)).setMaxConnections(123);
        completeNegativeVerification();
    }

    @Test
    public void testMaxConnectionsPerApiHost() {
        final HoneyClient client = builder.maxConnectionsPerApiHost(123).build();
        verify(transportBuilder, times(1)).setMaxConnectionsPerApiHost(123);
        completeNegativeVerification();
    }

    @Test
    public void testConnectTimeout() {
        final HoneyClient client = builder.connectTimeout(123).build();
        verify(transportBuilder, times(1)).setConnectTimeout(123);
        completeNegativeVerification();
    }

    @Test
    public void testQueueCapacity() {
        final HoneyClient client = builder.queueCapacity(123).build();
        verify(transportBuilder, times(1)).setQueueCapacity(123);
        completeNegativeVerification();
    }

    @Test
    public void testBatchTimeoutMillis() {
        final HoneyClient client = builder.batchTimeoutMillis(123).build();
        verify(transportBuilder, times(1)).setBatchTimeoutMillis(123);
        completeNegativeVerification();
    }

    @Test
    public void testBatchSize() {
        final HoneyClient client = builder.batchSize(123).build();
        verify(transportBuilder, times(1)).setBatchSize(123);
        completeNegativeVerification();
    }

    private void completeNegativeVerification() {
        verify(optionBuilder, times(1)).build();
        verify(transportBuilder, times(1)).build();
        verifyNoMoreInteractions(optionBuilder);
        verifyNoMoreInteractions(transportBuilder);
    }
}
