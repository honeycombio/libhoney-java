package io.honeycomb.libhoney.transport.batch.impl;

import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.transport.batch.BatchKeyStrategy;

/**
 * Key strategy operating on {@link ResolvedEvent}, returning a key that allows batching as described in the
 * <a href="https://honeycomb.io/docs/reference/sdk-spec/#transmission-1">SDK Spec#transmission</a>:
 * "the library must separate events into batches that all have API Host, writekey, and dataset in common".
 */
public class HoneycombBatchKeyStrategy implements BatchKeyStrategy<ResolvedEvent, String> {
    /**
     * @param event to use for deducing the key.
     * @return A string key to determine what batch this event goes into.
     */
    @Override
    public String getKey(final ResolvedEvent event) {
        return event.getApiHost() + ";" + event.getWriteKey() + ";" + event.getDataset();
    }
}
