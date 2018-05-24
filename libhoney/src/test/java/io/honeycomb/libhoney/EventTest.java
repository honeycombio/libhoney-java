package io.honeycomb.libhoney;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EventTest {

    private Event getEvent(final Options options, final HoneyClient mockClient) {
        return new Event(mockClient,
            options.getApiHost(),
            options.getWriteKey(),
            options.getDataset(),
            options.getSampleRate(),
            options.getGlobalFields(),
            options.getGlobalDynamicFields());
    }

    @Test
    public void GIVEN_defaultEvent_WHEN_invokingSend_EXPECT_clientToReceiveEvent() {
        final Options options = Options.builder().build();
        final HoneyClient mockClient = mock(HoneyClient.class);

        final Event event = getEvent(options, mockClient);
        event.send();

        verify(mockClient).sendEvent(event);
    }

    @Test
    public void GIVEN_defaultEvent_WHEN_invokingSendPresampled_EXPECT_clientToReceiveEventOnPresampledMethod() {
        final Options options = Options.builder().build();
        final HoneyClient mockClient = mock(HoneyClient.class);

        final Event event = getEvent(options, mockClient);
        event.sendPresampled();

        verify(mockClient).sendEventPresampled(event);
    }

    @Test
    public void GIVEN_someGlobalFields_WHEN_addingFields_EXPECT_putAllSemantics() {
        final Map<String, Object> globalFields = new HashMap<>();
        globalFields.put("key1", "data1");
        globalFields.put("key2", "data2");

        final Options build = Options.builder()
            .setGlobalFields(globalFields)
            .build();
        final HoneyClient mockClient = mock(HoneyClient.class);

        final Event event = getEvent(build, mockClient);
        final Map<String, Object> newFields = new HashMap<>();
        newFields.put("key2", "info1");
        newFields.put("key3", "data3");
        event.addFields(newFields);

        assertThat(event.getFields())
            .containsEntry("key1", "data1")
            .containsEntry("key2", "info1")
            .containsEntry("key3", "data3");
    }

    @Test
    public void GIVEN_defaultEvent_WHEN_addingMetadata_EXPECT_putSemantics() {
        final Options build = Options.builder().build();
        final HoneyClient mockClient = mock(HoneyClient.class);

        final Event event = getEvent(build, mockClient);
        final Map<String, Object> meta = new HashMap<>();
        meta.put("key1", "data1");
        meta.put("key2", "data2");
        event.addMetadata(meta);
        event.addMetadata("key2", "info1");
        final Map<String, Object> meta2 = new HashMap<>();
        meta2.put("key3", "data3");
        event.addMetadata(meta2);

        assertThat(event.getMetadata())
            .containsEntry("key1", "data1")
            .containsEntry("key2", "info1")
            .containsEntry("key3", "data3");    }

    @Test
    public void GIVEN_someGlobalFields_WHEN_addingFieldEntries_EXPECT_putSemantics() {
        final Map<String, Object> globalFields = new HashMap<>();
        globalFields.put("key1", "data1");
        globalFields.put("key2", "data2");

        final Options build = Options.builder()
            .setGlobalFields(globalFields)
            .build();
        final HoneyClient mockClient = mock(HoneyClient.class);

        final Event event = getEvent(build, mockClient);
        event.addField("key2", "info1");
        event.addField("key3", "data3");

        assertThat(event.getFields())
            .containsEntry("key1", "data1")
            .containsEntry("key2", "info1")
            .containsEntry("key3", "data3");
    }

}
