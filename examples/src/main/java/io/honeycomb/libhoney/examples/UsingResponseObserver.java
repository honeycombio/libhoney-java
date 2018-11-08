package io.honeycomb.libhoney.examples;

import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.ResponseObserver;
import io.honeycomb.libhoney.responses.ClientRejected;
import io.honeycomb.libhoney.responses.ServerAccepted;
import io.honeycomb.libhoney.responses.ServerRejected;
import io.honeycomb.libhoney.responses.Unknown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.honeycomb.libhoney.LibHoney.create;
import static io.honeycomb.libhoney.LibHoney.options;

public class UsingResponseObserver {

    public static HoneyClient initializeClient() {
        return create(
            options()
                .setWriteKey("myTeamWriteKey")
                .setDataset("Cluster Dataset")
                .build()
        );
    }

    public static void main(String... args) throws UnknownHostException {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("randomString", UUID.randomUUID().toString());
        dataMap.put("cpuCores", Runtime.getRuntime().availableProcessors());
        dataMap.put("hostname", InetAddress.getLocalHost().getHostName());

        try (HoneyClient honeyClient = initializeClient()) {
            honeyClient.addResponseObserver(new LoggingObserver());
            honeyClient.send(dataMap);
        }
    }

    private static class LoggingObserver implements ResponseObserver {
        private static final Logger LOG = LoggerFactory.getLogger(LoggingObserver.class);

        @Override
        public void onServerAccepted(ServerAccepted acceptedEvent) {
            LOG.debug("Server accepted the event: {}", acceptedEvent);
        }

        @Override
        public void onServerRejected(ServerRejected rejectedEvent) {
            LOG.error("Server rejected the event with batch level status code {} and event level status code {}",
                rejectedEvent.getBatchData().getBatchStatusCode(),
                rejectedEvent.getEventStatusCode());
        }

        @Override
        public void onClientRejected(ClientRejected rejectedEvent) {
            if (rejectedEvent.getReason() == ClientRejected.RejectionReason.NOT_SAMPLED) {
                LOG.trace("Event was not sampled {}", rejectedEvent);
            }
            LOG.info("Event rejected on the client side due to {}",
                rejectedEvent.getReason(),
                rejectedEvent.getException());
        }

        @Override
        public void onUnknown(Unknown unknown) {
            LOG.error("Unknown state due to unexpected error: {}", unknown);
        }
    }
}
