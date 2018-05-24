package io.honeycomb.libhoney;

import io.honeycomb.libhoney.eventdata.EventData;
import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.responses.ClientRejected;
import io.honeycomb.libhoney.responses.ServerAccepted;
import io.honeycomb.libhoney.responses.ServerRejected;
import io.honeycomb.libhoney.responses.Unknown;
import io.honeycomb.libhoney.responses.ResponseObservable;
import io.honeycomb.libhoney.transport.Transport;
import io.honeycomb.libhoney.transport.batch.ClockProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static io.honeycomb.libhoney.LibHoneyTest.supplierOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class HoneyClientTest {

    private HoneyClient honeyClient;

    private Transport mockTransport;
    private ResponseObservable mockObservable;

    private ArgumentCaptor<ResolvedEvent> eventCaptor = ArgumentCaptor.forClass(ResolvedEvent.class);
    private ArgumentCaptor<ClientRejected> responseCaptor = ArgumentCaptor.forClass(ClientRejected.class);

    @Before
    public void setUp() throws Exception {
        mockObservable = spy(new ResponseObservable());
        mockTransport = mock(Transport.class);
        when(mockTransport.getResponseObservable()).thenReturn(mockObservable);
        allowSubmissionToTransport();
    }

    @After
    public void tearDown() throws Exception {
    }

    private void allowSubmissionToTransport() {
        when(mockTransport.submit(any(ResolvedEvent.class))).thenReturn(true);
    }


    private void disallowSubmissionToTransport() {
        when(mockTransport.submit(any(ResolvedEvent.class))).thenReturn(false);
    }

    private void createHoneyClient() {
        honeyClient = new HoneyClient(LibHoney.options()
            .setDataset("testDataset")
            .setWriteKey("testWriteKey")
            .build(), mockTransport);
    }

    @Test
    public void GIVEN_aNormalHoneyClient_WHEN_callingClose_EXPECT_mockTransportToAlsoBeClosed() throws Exception {
        createHoneyClient();

        honeyClient.close();

        verify(mockTransport).close();
    }

    @Test
    public void GIVEN_aNormalHoneyClient_WHEN_addingObserver_EXPECT_addToBeCalledOnTheObservableInstance() {
        createHoneyClient();
        final AnyOldObserver observer = new AnyOldObserver();

        honeyClient.addResponseObserver(observer);

        verify(mockTransport).getResponseObservable();
        verify(mockObservable).add(observer);
    }

    @Test
    public void GIVEN_aNormalHoneyClient_WHEN_removingObserver_EXPECT_removeToBeCalledOnTheObservableInstance() {
        createHoneyClient();

        final AnyOldObserver observer = new AnyOldObserver();
        honeyClient.removeResponseObserver(observer);

        verify(mockTransport).getResponseObservable();
        verify(mockObservable).remove(observer);
    }

    @Test
    public void GIVEN_anEventThatIsNotSampled_WHEN_sendingEvent_EXPECT_NoSubmission_AND_ResponseToBePublished() {
        createHoneyClient();
        // this has an extremely low chance of being sampled
        final Event event = honeyClient.createEvent().setSampleRate(Integer.MAX_VALUE)
            .addField("test", 123);

        honeyClient.sendEvent(event);

        verify(mockTransport).getResponseObservable();
        verify(mockTransport, never()).submit(any(ResolvedEvent.class));
        verify(mockObservable).publish(any(ClientRejected.class));
    }

    @Test
    public void GIVEN_anEventThatIsSampled_WHEN_sendingEvent_EXPECT_Submission_AND_NoResponseToBePublished() {
        createHoneyClient();
        allowSubmissionToTransport();
        final Event event = honeyClient.createEvent().setSampleRate(1).addField("test", 123);

        honeyClient.sendEvent(event);

        verify(mockTransport, never()).getResponseObservable();
        verify(mockTransport).submit(any(ResolvedEvent.class));
    }

    @Test
    public void GIVEN_anEventThatIsRejectedByTransport_WHEN_sendingEvent_EXPECT_ResponseToBePublished() {
        createHoneyClient();
        disallowSubmissionToTransport();
        final Event event = honeyClient.createEvent().addField("test", 123);

        honeyClient.sendEventPresampled(event);

        verify(mockTransport).submit(any(ResolvedEvent.class));
        verify(mockObservable).publish(any(ClientRejected.class));
    }

    @Test
    public void GIVEN_variousConfiguredAndSuppliedFields_WHEN_usingShortcutSend_EXPECT_submittedEventToContainCorrectData() {
        final Map<String, Object> globalFields = new HashMap<>();
        globalFields.put("key1", "data1");
        globalFields.put("key2", "data2");
        final Map<String, ValueSupplier<Object>> globalDynamicFields = new HashMap<>();
        globalDynamicFields.put("key3", supplierOf("dynData1"));
        globalDynamicFields.put("key4", supplierOf("dynData2"));
        honeyClient = new HoneyClient(LibHoney.options()
            .setGlobalFields(globalFields)
            .setGlobalDynamicFields(globalDynamicFields)
            .setDataset("testDataset")
            .setWriteKey("testWriteKey")
            .build(),
            mockTransport);
        allowSubmissionToTransport();
        final Map<String, Object> fields = new HashMap<>();
        fields.put("key2", "info2");
        fields.put("key3", "info3");

        honeyClient.send(fields);

        verify(mockTransport).submit(eventCaptor.capture());
        final ResolvedEvent value = eventCaptor.getValue();
        assertThat(value.getFields())
            .containsEntry("key1", "data1")
            .containsEntry("key2", "info2")
            .containsEntry("key3", "info3")
            .containsEntry("key4", "dynData2");
    }

    @Test
    public void GIVEN_aGlobalConfig_WHEN_sendingAnEvent_EXPECT_ConfigurationToBeSetOnSubmittedEvent() {
        honeyClient = new HoneyClient(LibHoney.options()
            .setApiHost(URI.create("http://example.com"))
            .setWriteKey("globalWriteKey")
            .setDataset("globalDataset")
            .setSampleRate(4321)
            .build(),
            mockTransport);
        allowSubmissionToTransport();
        final Event event = honeyClient.createEvent().addField("test", 456);

        honeyClient.sendEventPresampled(event);

        verify(mockTransport).submit(eventCaptor.capture());
        final ResolvedEvent value = eventCaptor.getValue();
        assertThat(value.getApiHost()).isEqualTo(URI.create("http://example.com"));
        assertThat(value.getSampleRate()).isEqualTo(4321);
        assertThat(value.getWriteKey()).isEqualTo("globalWriteKey");
        assertThat(value.getDataset()).isEqualTo("globalDataset");
        assertThat(value.getFields()).hasSize(1);
        assertThat(value.getFields()).containsEntry("test", 456);
    }

    @Test
    public void GIVEN_aGlobalAndAnEventLevelConfig_WHEN_sendingAnEvent_EXPECT_eventLevelConfigToTakePrecedence() {
        final Map<String, Object> globalFields = new HashMap<>();
        globalFields.put("test", 123);
        honeyClient = new HoneyClient(LibHoney.options()
            .setApiHost(URI.create("http://example.com"))
            .setWriteKey("globalWriteKey")
            .setDataset("globalDataset")
            .setSampleRate(4321)
            .setGlobalFields(globalFields)
            .build(),
            mockTransport);
        allowSubmissionToTransport();
        final Event event = honeyClient
            .createEvent()
            .setSampleRate(1234)
            .setWriteKey("eventWriteKey")
            .setDataset("eventDataset")
            .setApiHost(URI.create("http://google.com"))
            .addField("test", 456);

        honeyClient.sendEventPresampled(event);

        verify(mockTransport).submit(eventCaptor.capture());
        final ResolvedEvent value = eventCaptor.getValue();
        assertThat(value.getApiHost()).isEqualTo(URI.create("http://google.com"));
        assertThat(value.getSampleRate()).isEqualTo(1234);
        assertThat(value.getWriteKey()).isEqualTo("eventWriteKey");
        assertThat(value.getDataset()).isEqualTo("eventDataset");
        assertThat(value.getFields()).hasSize(1);
        assertThat(value.getFields()).containsEntry("test", 456);
    }

    @Test
    public void GIVEN_anEventWithMetadata_WHEN_sendingEvent_EXPECT_metadataToBePresentOnSubmission() {
        honeyClient = new HoneyClient(LibHoney.options().build(), mockTransport);
        allowSubmissionToTransport();
        final Event event = honeyClient
            .createEvent()
            .setDataset("testDataset")
            .setWriteKey("testWriteKey")
            .addMetadata("metakey1", "metadata1")
            .addField("test", 456);

        honeyClient.sendEventPresampled(event);

        verify(mockTransport).submit(eventCaptor.capture());
        final ResolvedEvent value = eventCaptor.getValue();
        assertThat(value.getMetadata()).containsEntry("metakey1", "metadata1");
    }

    @Test
    public void GIVEN_anEventWithAnEmptyDataset_WHEN_sendingEvent_EXPECT_validationToFailAndNoSubmission() {
        honeyClient = new HoneyClient(LibHoney.options().build(), mockTransport);
        allowSubmissionToTransport();
        final Event event = honeyClient
            .createEvent()
            .setDataset("")
            .setWriteKey("testWriteKey")
            .addMetadata("metakey1", "metadata1")
            .addField("test", 456);

        try {
            honeyClient.sendEventPresampled(event);
        } catch (final IllegalArgumentException e) {
            verifyZeroInteractions(mockTransport, mockObservable);
            return;
        }
        fail("Expected exception to be thrown");
    }

    @Test
    public void GIVEN_anEventWithEmptyData_WHEN_sendingEvent_EXPECT_validationToFailAndNoSubmission() {
        honeyClient = new HoneyClient(LibHoney.options().build(), mockTransport);
        allowSubmissionToTransport();
        final Event event = honeyClient
            .createEvent()
            .setDataset("testDataset")
            .setWriteKey("testWriteKey")
            .addMetadata("metakey1", "metadata1");

        try {
            honeyClient.sendEventPresampled(event);
        } catch (final IllegalArgumentException e) {
            verifyZeroInteractions(mockTransport, mockObservable);
            return;
        }
        fail("Expected exception to be thrown");
    }

    @Test
    public void GIVEN_anEventWithAnEmptyWriteKey_WHEN_sendingEvent_EXPECT_validationToFailAndNoSubmission() {
        honeyClient = new HoneyClient(LibHoney.options().build(), mockTransport);
        allowSubmissionToTransport();
        final Event event = honeyClient
            .createEvent()
            .setDataset("testDataset")
            .setWriteKey("")
            .addMetadata("metakey1", "metadata1")
            .addField("test", 456);

        try {
            honeyClient.sendEventPresampled(event);
        } catch (final IllegalArgumentException e) {
            verifyZeroInteractions(mockTransport, mockObservable);
            return;
        }
        fail("Expected exception to be thrown");
    }

    @Test
    public void GIVEN_anEventWithAnInvalidSampleRate_WHEN_sendingEvent_EXPECT_validationToFailAndNoSubmission() {
        honeyClient = new HoneyClient(LibHoney.options().build(), mockTransport);
        allowSubmissionToTransport();
        final Event event = honeyClient
            .createEvent()
            .setDataset("testWriteKey")
            .setWriteKey("testWriteKey")
            .setSampleRate(0)
            .addMetadata("metakey1", "metadata1")
            .addField("test", 456);

        try {
            honeyClient.sendEventPresampled(event);
        } catch (final IllegalArgumentException e) {
            verifyZeroInteractions(mockTransport, mockObservable);
            return;
        }
        fail("Expected exception to be thrown");
    }

    @Test
    public void GIVEN_anEventWithANullApiHost_WHEN_sendingEvent_EXPECT_validationToFailAndNoSubmission() {
        honeyClient = new HoneyClient(LibHoney.options().build(), mockTransport);
        allowSubmissionToTransport();
        final Event event = honeyClient
            .createEvent()
            .setDataset("testWriteKey")
            .setWriteKey("testWriteKey")
            .setApiHost(null)
            .addMetadata("metakey1", "metadata1")
            .addField("test", 456);

        try {
            honeyClient.sendEventPresampled(event);
        } catch (final IllegalArgumentException e) {
            verifyZeroInteractions(mockTransport, mockObservable);
            return;
        }
        fail("Expected exception to be thrown");
    }

    @Test
    public void GIVEN_anEventWithTimestampSet_WHEN_sendingEvent_EXPECT_TimestampToBePresentOnSubmissions() {
        createHoneyClient();
        final long timestamp = System.currentTimeMillis() - 1000;
        final Event event = honeyClient.createEvent().setTimestamp(timestamp).addField("test", 456);

        honeyClient.sendEventPresampled(event);

        verify(mockTransport).submit(eventCaptor.capture());
        final ResolvedEvent value = eventCaptor.getValue();
        assertThat(value.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    public void GIVEN_anEventWithoutAnExplicitTimestampSet_WHEN_sendingEvent_EXPECT_TimestampToBeSetToNowOnSubmission() {
        final ClockProvider clockProvider = mock(ClockProvider.class);
        final long now = System.currentTimeMillis();
        when(clockProvider.getWallTime()).thenReturn(now);
        honeyClient = new HoneyClient(LibHoney.options()
            .setDataset("testDataset")
            .setWriteKey("testWriteKey")
            .build(), mockTransport, clockProvider);

        final Event event = honeyClient.createEvent().addField("test", 123);

        honeyClient.sendEventPresampled(event);

        verify(mockTransport).submit(eventCaptor.capture());
        final ResolvedEvent value = eventCaptor.getValue();
        assertThat(value.getTimestamp()).isEqualTo(now);
    }

    @Test
    public void GIVEN_anEventThatsUnlikelyToGetSampled_WHEN_sendingEvent_EXPECT_eventToBeRejectedDueToNOT_SAMPLED() {
        createHoneyClient();
        final ResponseObserver mockObserver = mock(ResponseObserver.class);
        honeyClient.addResponseObserver(mockObserver);
        honeyClient.sendEvent(
            honeyClient.createEvent().addField("test", 123).setSampleRate(Integer.MAX_VALUE)
        );

        verify(mockObserver).onClientRejected(responseCaptor.capture());
        assertThat(responseCaptor.getValue().getReason()).isEqualTo(ClientRejected.RejectionReason.NOT_SAMPLED);
        assertThat(responseCaptor.getValue().getException()).isNull();
    }

    @Test
    public void GIVEN_transportDoesNotAcceptAnymoreEvents_WHEN_sendingEvent_EXPECT_eventToBeRejectedDueToQUEUE_OVERFLOW() {
        createHoneyClient();
        when(mockTransport.submit(any(ResolvedEvent.class))).thenReturn(false);
        final ResponseObserver observer = mock(ResponseObserver.class);
        honeyClient.addResponseObserver(observer);

        honeyClient.sendEventPresampled(
            honeyClient.createEvent().addField("test", 123)
        );

        verify(observer).onClientRejected(responseCaptor.capture());
        assertThat(responseCaptor.getValue().getReason()).isEqualTo(ClientRejected.RejectionReason.QUEUE_OVERFLOW);
        assertThat(responseCaptor.getValue().getException()).isNull();
    }

    @Test
    public void GIVEN_transportDoesNotAcceptAnymoreEvents_WHEN_sendingEvent_EXPECT_eventToBeRejectedDueToDynamicFieldError() {
        createHoneyClient();
        final ResponseObserver mockObserver = mock(ResponseObserver.class);
        honeyClient.addResponseObserver(mockObserver);
        final Event badEvent = honeyClient
            .buildEventFactory()
            .addDynamicField("baddynamicField", new ValueSupplier<String>() {
                @Override
                public String supply() {
                    throw new SomeException("BOOOO!");
                }
            })
            .build()
            .createEvent();

        honeyClient.sendEventPresampled(badEvent);

        verify(mockObserver).onClientRejected(responseCaptor.capture());
        assertThat(responseCaptor.getValue().getReason()).isEqualTo(ClientRejected.RejectionReason.DYNAMIC_FIELD_RESOLUTION_ERROR);
        assertThat(responseCaptor.getValue().getException()).isInstanceOf(SomeException.class);
    }

    @Test
    public void GIVEN_transportDoesNotAcceptAnymoreEvents_WHEN_sendingEvent_EXPECT_eventToBeRejectedDueToPOSTPROCESSINGERROR() {
        honeyClient = new HoneyClient(LibHoney.options()
            .setDataset("testDataset")
            .setWriteKey("testWriteKey")
            .setEventPostProcessor(new EventPostProcessor() {
                @Override
                public void process(final EventData<?> eventData) {
                    throw new SomeException("BOOOO!");
                }
            })
            .build(), mockTransport);
        final ResponseObserver mockObserver = mock(ResponseObserver.class);
        honeyClient.addResponseObserver(mockObserver);

        honeyClient.sendEventPresampled(
            honeyClient.createEvent().addField("test", 123)
        );

        verify(mockObserver).onClientRejected(responseCaptor.capture());
        assertThat(responseCaptor.getValue().getReason()).isEqualTo(ClientRejected.RejectionReason.POST_PROCESSING_ERROR);
        assertThat(responseCaptor.getValue().getException()).isInstanceOf(SomeException.class);
    }

    private static class AnyOldObserver implements ResponseObserver {
        @Override
        public void onServerAccepted(final ServerAccepted serverAccepted) {
            //empty
        }

        @Override
        public void onServerRejected(final ServerRejected serverRejected) {
            //empty
        }

        @Override
        public void onClientRejected(final ClientRejected clientRejected) {
            //empty
        }

        @Override
        public void onUnknown(final Unknown unknown) {
            //empty
        }
    }

    private class SomeException extends RuntimeException {
        public SomeException(final String s) {
        }
    }
}
