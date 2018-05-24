package io.honeycomb.libhoney;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class LibHoneyTest {
    private static final Map<String, Object> NO_FIELDS = Collections.emptyMap();
    private static final Map<String, ValueSupplier<Object>> NO_DYN_FIELDS = Collections.emptyMap();

    private HoneyClient honeyClient;

    @Before
    public void setUp() throws Exception {
        LibHoney.setDefault(null); // clear static state
    }

    private void createDefaultClient() {
        this.honeyClient = LibHoney.create(LibHoney.options().build());
    }

    private void createHoneyClient() {
        this.honeyClient = LibHoney.create(
            LibHoney.options()
                .setDataset("testset")
                .setWriteKey("testkey")
                .setApiHost(URI.create("http://example.com"))
                .setSampleRate(12)
                .build()
        );
    }

    private void createHoneyClientWithGlobalFields(final Map<String, Object> fields) {
        this.honeyClient = LibHoney.create(
            LibHoney.options()
                .setGlobalFields(fields)
                .setDataset("testset")
                .setWriteKey("testkey")
                .setApiHost(URI.create("http://example.com"))
                .setSampleRate(12)
                .build()
        );
    }

    private void createHoneyClientWithGlobalDynamicFields(final Map<String, ValueSupplier<Object>> fields) {
        this.honeyClient = LibHoney.create(
            LibHoney.options()
                .setGlobalDynamicFields(fields)
                .setDataset("testset")
                .setWriteKey("testkey")
                .setApiHost(URI.create("http://example.com"))
                .setSampleRate(12)
                .build()
        );
    }

    @After
    public void tearDown() throws Exception {
        if (honeyClient != null) {
            honeyClient.close();
        }
    }

    @Test
    public void GIVEN_defaultConfiguration_EXPECT_noExceptionsToBeThrown() {
        try {
            createDefaultClient();
            honeyClient.createEvent();
            honeyClient.buildEventFactory().build().createEvent();
        } catch (final Exception e) {
            fail("No exceptions should be thrown when simply using defaults");
        }
    }

    @Test
    public void GIVEN_sampleRateSmallerThan1_EXPECT_IAEToBeThrown() {
        final Options.Builder builder = LibHoney.options().setSampleRate(0);
        try {
            builder.build();
        } catch (final IllegalArgumentException e) {
            return;
        }
        fail("Exception not thrown");
    }

    @Test
    public void GIVEN_batchSizeSmallerThan1_EXPECT_IAEToBeThrown() {
        final TransportOptions.Builder builder = LibHoney.transportOptions().setBatchSize(0);
        try {
            builder.build();
        } catch (final IllegalArgumentException e) {
            return;
        }
        fail("Exception not thrown");
    }

    @Test
    public void GIVEN_queueCapacitySmallerThan1_EXPECT_IAEToBeThrown() {
        final TransportOptions.Builder builder = LibHoney.transportOptions().setQueueCapacity(0);
        try {
            builder.build();
        } catch (final IllegalArgumentException e) {
            return;
        }
        fail("Exception not thrown");
    }

    @Test
    public void GIVEN_batchTimeoutSmallerThan1_EXPECT_IAEToBeThrown() {
        final TransportOptions.Builder builder = LibHoney.transportOptions().setBatchTimeoutMillis(0);
        try {
            builder.build();
        } catch (final IllegalArgumentException e) {
            return;
        }
        fail("Exception not thrown");
    }

    private void assertThatEventHasCorrectInitialState(final Event event,
                                                       final Map<String, Object> fields,
                                                       final Map<String, ValueSupplier<Object>> dynamicFields) {
        assertThat(event.getDataset()).isEqualTo("testset");
        assertThat(event.getWriteKey()).isEqualTo("testkey");
        assertThat(event.getApiHost()).isEqualTo(URI.create("http://example.com"));
        assertThat(event.getSampleRate()).isEqualTo(12);
        assertThat(event.getTimestamp()).isNull();
        assertThat(event.getMetadata()).isEmpty();
        assertThat(event.getFields()).isEqualTo(fields);
        assertThat(event.getDynamicFields()).isEqualTo(dynamicFields);
    }

    @Test
    public void GIVEN_aNormallyConfiguredClient_WHEN_creatingEvent_EXPECT_configurationToBeCorrectlyPropagated() {
        createHoneyClient();

        final Event event = honeyClient.createEvent();

        assertThatEventHasCorrectInitialState(event, NO_FIELDS, NO_DYN_FIELDS);
    }

    @Test
    public void GIVEN_aNormallyConfiguredClient_WHEN_creatingEventUsingTheFactory_EXPECT_configurationToBeCorrectlyPropagated() {
        createHoneyClient();

        final EventFactory eventFactory = honeyClient.buildEventFactory().build();
        final Event event = eventFactory.createEvent();

        assertThatEventHasCorrectInitialState(event, NO_FIELDS, NO_DYN_FIELDS);
    }

    @Test
    public void GIVEN_aClientWithSomeGlobalFields_WHEN_creatingEvent_EXPECT_fieldsToBePresent() {
        final HashMap<String, Object> fields = new HashMap<>();
        fields.put("testField", "testData");
        fields.put("complexTestField", Arrays.asList("One", "Two", "Three"));
        createHoneyClientWithGlobalFields(new HashMap<>(fields));

        final Event event = honeyClient.createEvent();

        assertThatEventHasCorrectInitialState(event, fields, NO_DYN_FIELDS);
    }

    @Test
    public void GIVEN_aClientWithSomeGlobalFields_WHEN_creatingEventWithEventFactory_EXPECT_fieldsToBePresent() {
        final HashMap<String, Object> fields = new HashMap<>();
        fields.put("testField", "testData");
        fields.put("complexTestField", Arrays.asList("One", "Two", "Three"));
        createHoneyClientWithGlobalFields(new HashMap<>(fields));

        final EventFactory eventFactory = honeyClient.buildEventFactory().build();
        final Event event = eventFactory.createEvent();

        assertThatEventHasCorrectInitialState(event, fields, NO_DYN_FIELDS);
    }

    @Test
    public void GIVEN_aClientWithSomeDynamicGlobalFields_WHEN_overwritingFieldOnTheFactoryAndOnEvent_EXPECT_toPropagateToTheEvent() {
        final Map<String, Object> fields = new HashMap<>();
        fields.put("globalField", "testData1");
        fields.put("toBeOverwrittenAtFactoryLevel", "testData2");
        fields.put("toBeOverwrittenAtEventLevel", "testData3");
        createHoneyClientWithGlobalFields(fields);
        final EventFactory eventFactory = honeyClient
            .buildEventFactory()
            .addField("toBeOverwrittenAtFactoryLevel", "testInfo2")
            .build();

        final Event event = eventFactory.createEvent().addField("toBeOverwrittenAtEventLevel", "testBlob3");

        assertThat(event.getFields())
            .containsEntry("globalField", "testData1")
            .containsEntry("toBeOverwrittenAtFactoryLevel", "testInfo2")
            .containsEntry("toBeOverwrittenAtEventLevel", "testBlob3");
    }

    @Test
    public void GIVEN_aClientWithSomeDynamicGlobalFields_WHEN_creatingEvent_EXPECT_fieldsToBePresent() {
        final HashMap<String, ValueSupplier<Object>> dynamicFields = new HashMap<>();
        dynamicFields.put("testField", supplierOf("testData"));
        dynamicFields.put("complexTestField", supplierOf(Arrays.asList("One", "Two", "Three")));
        createHoneyClientWithGlobalDynamicFields(new HashMap<>(dynamicFields));

        final Event event = honeyClient.createEvent();

        assertThatEventHasCorrectInitialState(event, NO_FIELDS, dynamicFields);
    }

    @Test
    public void GIVEN_aClientWithSomeDynamicGlobalFields_WHEN_creatingEventUsingEventFactory_EXPECT_fieldsToBePresent() {
        final HashMap<String, ValueSupplier<Object>> dynamicFields = new HashMap<>();
        dynamicFields.put("testField", supplierOf("testData"));
        dynamicFields.put("complexTestField", supplierOf(Arrays.asList("One", "Two", "Three")));
        createHoneyClientWithGlobalDynamicFields(new HashMap<>(dynamicFields));

        final EventFactory eventFactory = honeyClient.buildEventFactory().build();
        final Event event = eventFactory.createEvent();

        assertThatEventHasCorrectInitialState(event, NO_FIELDS, dynamicFields);
    }

    @Test
    public void GIVEN_aClientWithSomeDynamicGlobalFields_WHEN_overwritingaDynamicFieldOnTheFactory_EXPECT_toPropagateToTheEvent() {
        final HashMap<String, ValueSupplier<Object>> dynamicFields = new HashMap<>();
        final ValueSupplier<Object> firstCallable = supplierOf("testData");
        final ValueSupplier<Object> secondCallable = supplierOf("testInfo2");
        dynamicFields.put("globalField", firstCallable);
        dynamicFields.put("toBeOverwrittenAtFactoryLevel", supplierOf("testData2"));
        createHoneyClientWithGlobalDynamicFields(dynamicFields);
        final EventFactory eventFactory = honeyClient
            .buildEventFactory()
            .addDynamicField("toBeOverwrittenAtFactoryLevel", secondCallable)
            .build();

        final Event event = eventFactory.createEvent();

        assertThat(event.getDynamicFields())
            .containsEntry("globalField", firstCallable)
            .containsEntry("toBeOverwrittenAtFactoryLevel", secondCallable);
    }

    @Test
    public void GIVEN_anEventFactoryThatOverwritesConfiguration_WHEN_creatingEventUsingTheFactory_EXPECT_newConfigurationToBeCorrectlyPropagated() {
        createHoneyClient();
        final EventFactory eventFactory = honeyClient
            .buildEventFactory()
            .setApiHost(URI.create("http://google.com"))
            .setDataset("factoryDataset")
            .setSampleRate(1000)
            .setWriteKey("factoryWriteKey")
            .build();
        final Event event = eventFactory.createEvent();

        assertThat(event.getDataset()).isEqualTo("factoryDataset");
        assertThat(event.getWriteKey()).isEqualTo("factoryWriteKey");
        assertThat(event.getApiHost()).isEqualTo(URI.create("http://google.com"));
        assertThat(event.getSampleRate()).isEqualTo(1000);
        assertThat(event.getTimestamp()).isNull();
        assertThat(event.getMetadata()).isEmpty();
        assertThat(event.getFields()).isEqualTo(NO_FIELDS);
        assertThat(event.getDynamicFields()).isEqualTo(NO_DYN_FIELDS);
    }

    @Test
    public void GIVEN_anEventThatOverwritesConfiguration_EXPECT_newConfigurationToBeOverwriteClientAndFactoryConfiguration() {
        createHoneyClient();
        final EventFactory eventFactory = honeyClient
            .buildEventFactory()
            .setApiHost(URI.create("http://google.com"))
            .setDataset("factoryDataset")
            .setSampleRate(1000)
            .setWriteKey("factoryWriteKey")
            .build();
        final Event event = eventFactory.createEvent()
            .setApiHost(URI.create("http://google.co.uk"))
            .setDataset("eventDataset")
            .setSampleRate(100_000)
            .setWriteKey("eventWriteKey");

        assertThat(event.getDataset()).isEqualTo("eventDataset");
        assertThat(event.getWriteKey()).isEqualTo("eventWriteKey");
        assertThat(event.getApiHost()).isEqualTo(URI.create("http://google.co.uk"));
        assertThat(event.getSampleRate()).isEqualTo(100_000);
        assertThat(event.getTimestamp()).isNull();
        assertThat(event.getMetadata()).isEmpty();
        assertThat(event.getFields()).isEqualTo(NO_FIELDS);
        assertThat(event.getDynamicFields()).isEqualTo(NO_DYN_FIELDS);
    }

    @Test
    public void GIVEN_NoDefaultClientHasBeenSet_EXPECT_ISEToBeThrown() {
        createHoneyClient();

        try {
            LibHoney.getDefault();
        } catch (final IllegalStateException e) {
            return;
        }
        fail("Expected exception to be thrown");
    }

    @Test
    public void GIVEN_defaultClientIsSetOnClient_EXPECT_ClientToBeGloballyGettable() {
        createHoneyClient();

        honeyClient.setAsDefault();

        assertThat(LibHoney.getDefault()).isEqualTo(honeyClient);
    }

    @Test
    public void GIVEN_defaultClientIsSetOnLibHoney_EXPECT_ClientToBeGloballyGettable() {
        createHoneyClient();

        LibHoney.setDefault(honeyClient);

        assertThat(LibHoney.getDefault()).isEqualTo(honeyClient);
    }

    @Test
    public void GIVEN_twoClients_WHEN_OneIsSetAsDefaultAfterTheOther_EXPECT_LastSetToPersist() throws Exception {
        createHoneyClient();
        final HoneyClient currentClient = honeyClient;
        LibHoney.setDefault(currentClient);
        assertThat(LibHoney.getDefault()).isEqualTo(currentClient);
        currentClient.close();

        createHoneyClient();
        final HoneyClient newClient = honeyClient;
        assertThat(currentClient).isNotEqualTo(newClient);
        LibHoney.setDefault(newClient);
        assertThat(LibHoney.getDefault()).isEqualTo(newClient);
    }

    public static ValueSupplier<Object> supplierOf(final Object testData) {
        return new ValueSupplier<Object>() {
            @Override
            public Object supply() {
                return testData;
            }
        };
    }

}
