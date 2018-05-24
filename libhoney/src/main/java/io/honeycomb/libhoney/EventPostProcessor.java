package io.honeycomb.libhoney;

import io.honeycomb.libhoney.eventdata.EventData;

/**
 * Applies post-processing to events being submitted to Honeycomb. This occurs only for events that have been sampled,
 * and just after the event's dynamic fields have been resolved. The {@link EventData} being passed to the post
 * processor can be safely mutated, but must subsequently still pass validation. See {@link HoneyClient}'s class
 * documentation for the "Event Validation" rules.
 * <p>
 * Post-processing occurs on the thread that invokes any of the send*() methods.
 * <p>
 * Any exceptions occuring will stop the event from being sent and reported back as a
 * {@link io.honeycomb.libhoney.responses.ClientRejected} response.
 */
public interface EventPostProcessor {
    /**
     * Post-process an event just before it is submitted to Honeycomb.
     *
     * @param eventData the mutable contents of the event
     */
    void process(EventData<?> eventData);
}
