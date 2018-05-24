package io.honeycomb.libhoney.responses;

import io.honeycomb.libhoney.ResponseObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Maintains a collection of registered {@link ResponseObserver}s and publishes responses to them.
 */
public class ResponseObservable implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseObservable.class);

    private final CopyOnWriteArrayList<ResponseObserver> observers = new CopyOnWriteArrayList<>();

    public void publish(final ServerAccepted toPublish) {
        for (final ResponseObserver observer : observers) {
            observer.onServerAccepted(toPublish);
        }
    }

    public void publish(final ServerRejected toPublish) {
        for (final ResponseObserver observer : observers) {
            observer.onServerRejected(toPublish);
        }
    }

    public void publish(final ClientRejected toPublish) {
        for (final ResponseObserver observer : observers) {
            observer.onClientRejected(toPublish);
        }
    }

    public void publish(final Unknown toPublish) {
        for (final ResponseObserver listener : observers) {
            listener.onUnknown(toPublish);
        }
    }

    public void add(final ResponseObserver observer) {
        observers.add(observer);
        LOG.info("Added observer: {}", observer);
    }

    public void remove(final ResponseObserver observer) {
        if (observers.remove(observer)) {
            LOG.info("Removed observer: {}", observer);
        } else {
            LOG.info("Observer not present in observer list: {}", observer);
        }
    }

    public boolean hasObservers() {
        return !observers.isEmpty();
    }

    @Override
    public void close() {
        observers.clear();
    }
}
