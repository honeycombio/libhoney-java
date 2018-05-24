package io.honeycomb.libhoney.transport;

import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.responses.ResponseObservable;

/**
 * Transport for sending events to HoneyComb. Used by the {@link io.honeycomb.libhoney.HoneyClient} internals.
 */
public interface Transport extends AutoCloseable {
    /**
     * Submit an event to HoneyComb via the Transport. The Transport may reject the event if its internal bounded buffer
     * has overflowed.
     *
     * @param event the resolved event containing its final data
     * @return whether the event was accepted by the transport
     * @see io.honeycomb.libhoney.responses.ClientRejected.RejectionReason#QUEUE_OVERFLOW
     */
    boolean submit(ResolvedEvent event);

    /**
     * Get the {@link ResponseObservable} that is linked to this Transport. Can be used to register an
     * {@link io.honeycomb.libhoney.ResponseObserver} for inspection of the event responses.
     *
     * @return the response observable
     */
    ResponseObservable getResponseObservable();
}
