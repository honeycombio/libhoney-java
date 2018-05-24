package io.honeycomb.libhoney.responses.impl;

import io.honeycomb.libhoney.ResponseObserver;
import io.honeycomb.libhoney.responses.ClientRejected;
import io.honeycomb.libhoney.responses.ResponseObservable;
import io.honeycomb.libhoney.responses.ServerAccepted;
import io.honeycomb.libhoney.responses.ServerRejected;
import io.honeycomb.libhoney.responses.Unknown;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;


public class ResponseObservableTest {

    private ResponseObservable observable;
    private ResponseObserver mockObserver;

    @Before
    public void setUp() throws Exception {
        observable = new ResponseObservable();
        mockObserver = mock(ResponseObserver.class);
    }

    @Test
    public void GIVEN_newObservable_EXPECT_notObservers() {
        assertThat(observable.hasObservers()).isFalse();
    }

    @Test
    public void GIVEN_anAddedObserver_EXPECT_hasObservers() {
        observable.add(new TestObserver());

        assertThat(observable.hasObservers()).isTrue();
    }


    @Test
    public void GIVEN_anAddedObserver_WHEN_removingWithReferenceToTheSameInstance_EXPECT_noObservers() {
        final TestObserver observerToRemoveAgain = new TestObserver();
        observable.add(observerToRemoveAgain);

        observable.remove(new TestObserver());
        assertThat(observable.hasObservers()).isTrue();
        observable.remove(observerToRemoveAgain);

        assertThat(observable.hasObservers()).isFalse();
    }

    @Test
    public void GIVEN_twoAddedObserver_WHEN_removingOneObserver_EXPECT_toStillHaveObservers() {
        final TestObserver observerToRemoveAgain = new TestObserver();
        observable.add(observerToRemoveAgain);
        observable.add(new TestObserver());

        observable.remove(observerToRemoveAgain);

        assertThat(observable.hasObservers()).isTrue();
    }

    @Test
    public void GIVEN_twoAddedObserver_WHEN_closingObserver_EXPECT_NoObservers() {
        observable.add(new TestObserver());
        observable.add(new TestObserver());

        observable.close();

        assertThat(observable.hasObservers()).isFalse();
    }

    @Test
    public void WHEN_publishingServerAccepted_EXPECT_observerToBeNotified() {
        observable.add(mockObserver);

        observable.publish(mock(ServerAccepted.class));

        verify(mockObserver).onServerAccepted(any(ServerAccepted.class));
        verifyNoMoreInteractions(mockObserver);
    }

    @Test
    public void WHEN_publishingServerRejected_EXPECT_observerToBeNotified() {
        observable.add(mockObserver);

        observable.publish(mock(ServerRejected.class));

        verify(mockObserver).onServerRejected(any(ServerRejected.class));
        verifyNoMoreInteractions(mockObserver);
    }

    @Test
    public void WHEN_publishingUnknown_EXPECT_observerToBeNotified() {
        observable.add(mockObserver);

        observable.publish(mock(Unknown.class));

        verify(mockObserver).onUnknown(any(Unknown.class));
        verifyNoMoreInteractions(mockObserver);
    }

    @Test
    public void WHEN_publishingClientRejected_EXPECT_observerToBeNotified() {
        observable.add(mockObserver);

        observable.publish(mock(ClientRejected.class));

        verify(mockObserver).onClientRejected(any(ClientRejected.class));
        verifyNoMoreInteractions(mockObserver);
    }

    @Test
    public void GIVEN_addedThenRemovedObserver_WHEN_publishingAnyResponses_EXPECT_ObserverNotToBeNotified() {
        observable.add(mockObserver);
        observable.remove(mockObserver);

        observable.publish(mock(ClientRejected.class));
        observable.publish(mock(Unknown.class));
        observable.publish(mock(ServerRejected.class));
        observable.publish(mock(ServerAccepted.class));

        verifyZeroInteractions(mockObserver);
    }

    @Test
    public void GIVEN_addedObserverThenClosedObservable_WHEN_publishingAnyResponses_EXPECT_ObserverNotToBeNotified() {
        observable.add(mockObserver);
        observable.close();

        observable.publish(mock(ClientRejected.class));
        observable.publish(mock(Unknown.class));
        observable.publish(mock(ServerRejected.class));
        observable.publish(mock(ServerAccepted.class));

        verifyZeroInteractions(mockObserver);
    }

    private static class TestObserver implements ResponseObserver {
        @Override
        public void onServerAccepted(final ServerAccepted serverAccepted) {

        }

        @Override
        public void onServerRejected(final ServerRejected serverRejected) {

        }

        @Override
        public void onClientRejected(final ClientRejected clientRejected) {

        }

        @Override
        public void onUnknown(final Unknown unknown) {

        }
    }
}
