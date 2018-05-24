package io.honeycomb.libhoney.transport.impl;


import io.honeycomb.libhoney.Metrics;
import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.responses.ClientRejected;
import io.honeycomb.libhoney.responses.ResponseObservable;
import io.honeycomb.libhoney.transport.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ConsoleTransport implements Transport {
    private static final Logger LOG = LoggerFactory.getLogger(ConsoleTransport.class);

    private final ResponseObservable observable;

    public ConsoleTransport(final ResponseObservable observable) {
        this.observable = observable;
    }

    @Override
    public boolean submit(final ResolvedEvent event) {
        LOG.info("Submitting event to dead-end: {}", event);
        observable.publish(new ConsoleTransportDeadEnd(event));
        return true;
    }

    @Override
    public ResponseObservable getResponseObservable() {
        return observable;
    }

    @Override
    public void close() {
        LOG.info("ConsoleTransport: Closed!");
    }

    private static class ConsoleTransportDeadEnd implements ClientRejected {
        private final ResolvedEvent event;

        public ConsoleTransportDeadEnd(final ResolvedEvent event) {
            this.event = event;
        }

        @Override
        public RejectionReason getReason() {
            return RejectionReason.DEAD_END;
        }

        @Override
        public Exception getException() {
            return null;
        }

        @Override
        public Map<String, Object> getEventMetadata() {
            return event.getMetadata();
        }

        @Override
        public Metrics getMetrics() {
            return event.getMetrics();
        }

        @Override
        public String getMessage() {
            return "ConsoleTransport";
        }
    }
}
