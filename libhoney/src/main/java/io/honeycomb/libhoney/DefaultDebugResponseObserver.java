package io.honeycomb.libhoney;

import io.honeycomb.libhoney.responses.ClientRejected;
import io.honeycomb.libhoney.responses.ServerAccepted;
import io.honeycomb.libhoney.responses.ServerRejected;
import io.honeycomb.libhoney.responses.Unknown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

public class DefaultDebugResponseObserver implements ResponseObserver {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDebugResponseObserver.class);

    protected static final String ERROR_TEMPLATE_401 = "Server responded with a 401 HTTP error code to a batch request." +
        " This is likely caused by using an incorrect 'Team Write Key'. Check https://ui.honeycomb.io/account to verify your " +
        "team write key. Rejected event: {}";
    protected static final String ERROR_TEMPLATE_UNKNOWN_HOST_EXCEPTION = "Could not reach determine destination server. " +
        "This could be due to 1) invalid apiHost 2) Could not resolve apiHost (firewall? dns?) 3) problem with proxy configuration. " +
        "Unknown event: {}";
    private static final int STATUS_UNAUTHORIZED = 401;

    @Override
    public void onServerAccepted(final ServerAccepted serverAccepted) {
        LOG.trace("Event successfully sent to Honeycomb: {}", serverAccepted);
    }

    @Override
    public void onServerRejected(final ServerRejected serverRejected) {
        if (serverRejected.getBatchData().getBatchStatusCode() == STATUS_UNAUTHORIZED) {
            handle401(serverRejected);
        } else {
            LOG.debug("Event rejected by Honeycomb server: {}", serverRejected);
        }
    }

    @Override
    public void onClientRejected(final ClientRejected clientRejected) {
        LOG.debug("Event rejected on the client side: {}", clientRejected);
    }

    @Override
    public void onUnknown(final Unknown unknown) {
        if(unknown.getException()!=null && UnknownHostException.class.isAssignableFrom(unknown.getException().getClass())){
            LOG.debug(ERROR_TEMPLATE_UNKNOWN_HOST_EXCEPTION, unknown);
        } else {
            LOG.debug("Received an unknown error while trying to send Event to Honeycomb: {}", unknown);
        }
    }

    protected void handle401(final ServerRejected serverRejected) {
        LOG.debug(ERROR_TEMPLATE_401, serverRejected);
    }
}
