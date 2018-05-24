package io.honeycomb.libhoney.responses;

/**
 * Marker for event responses that have were rejected by the honeycomb server.
 *
 * This is caused by either the event's entire batch being rejected (e.g. the write key is not accepted and so a 401
 * HTTP response is returned), or because the event's batch was accepted but the event itself was rejected.
 *
 * See https://honeycomb.io/docs/reference/api/ for a detailed view of the batching API responses.
 */
public interface ServerRejected extends ServerResponse {

}
