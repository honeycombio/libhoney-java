package io.honeycomb.libhoney;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EventFactoryTest {

    private HoneyClient mock;

    @Before
    public void setUp() {
        mock = mock(HoneyClient.class);
    }

    @Test
    public void GIVEN_aNormalEventFactory_WHEN_copying_EXPECT_copyToEqualToOriginal() {
        final EventFactory eventFactory = new EventFactory(mock, LibHoney.options().build());

        assertThat(eventFactory.copy()).isEqualToComparingFieldByField(eventFactory);
        assertThat(eventFactory.copy().build()).isEqualToComparingFieldByField(eventFactory);
    }

    @Test
    public void GIVEN_aNormalEventFactory_WHEN_creatingEvent_EXPECT_eventToInheritProperties() {
        final EventFactory eventFactory = new EventFactory(mock, LibHoney.options().build());

        final Event event = eventFactory.createEvent();

        assertThat(event).isEqualToComparingOnlyGivenFields(eventFactory,
            "client", "apiHost", "writeKey", "dataset", "sampleRate", "fields", "dynamicFields");
    }

    @Test
    public void GIVEN_aNormalEventFactory_WHEN_sendingAnEventDirectly_EXPECT_eventToInheritPropertiesAndMapValuesCorrectly() {
        final EventFactory eventFactory = new EventFactory(mock, LibHoney.options().build());
        final Map<String, Object> fields = new HashMap<>();
        fields.put("key1", "data2");
        final Event expectedEvent = eventFactory.createEvent();
        expectedEvent.addFields(fields);

        eventFactory.send(fields);

        final ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(mock).sendEvent(captor.capture());
        assertThat(captor.getValue()).isEqualToComparingFieldByField(expectedEvent);
    }
}
