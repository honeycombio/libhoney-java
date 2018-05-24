package io.honeycomb.libhoney;

import io.honeycomb.libhoney.responses.ClientRejected;
import io.honeycomb.libhoney.responses.ServerAccepted;
import io.honeycomb.libhoney.responses.ServerRejected;
import io.honeycomb.libhoney.responses.ServerResponse;
import io.honeycomb.libhoney.responses.Unknown;

/**
 * If registered with {@link HoneyClient#addResponseObserver(ResponseObserver)}, this is the interface that will be
 * notified about the outcome of events being sent through the client.
 * <p>
 * This can be useful to track in case there is any issues with events being sent, and can be captured by, for instance,
 * simple logging or metrics.
 * <p>
 * <b>Note that these might be called called either by an internal thread or the thread that originally enqueued the
 * event, and so any heavy processing should be done asynchronously to avoid
 * starvation (e.g. by handing off to a thread/threadpool).</b>
 */
public interface ResponseObserver {
    /**
     * This method will be notified for any event that was accepted on the server side.
     * Under ideal circumstances this would be invoked for every event being sent, so take note that this method might
     * be invoked frequently.
     *
     * See {@link ServerAccepted} and {@link ServerResponse} for details.
     *
     * @param serverAccepted response.
     */
    void onServerAccepted(ServerAccepted serverAccepted);

    /**
     * This method will be notified for any event that was rejected on the server side.
     * See {@link ServerRejected} and {@link ServerResponse} for details.
     *
     * @param serverRejected response.
     */
    void onServerRejected(ServerRejected serverRejected);

    /**
     * This method will be notified for any event that has been rejected on the client side.
     * See {@link ClientRejected} for details.
     *
     * @param clientRejected response.
     */
    void onClientRejected(ClientRejected clientRejected);

    /**
     * This method will be notified for any event where the outcome is not clear.
     *
     * @param unknown response.
     */
    void onUnknown(Unknown unknown);
}
