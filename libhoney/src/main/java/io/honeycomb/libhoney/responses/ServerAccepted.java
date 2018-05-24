package io.honeycomb.libhoney.responses;

/**
 * Marker for event responses that were accepted by the honeycomb server.
 *
 * In this case, the event's batch and the event itself were accepted by the server.
 *
 * See https://honeycomb.io/docs/reference/api/ for a detailed view of the batching API responses.
 */
public interface ServerAccepted extends ServerResponse {

}
