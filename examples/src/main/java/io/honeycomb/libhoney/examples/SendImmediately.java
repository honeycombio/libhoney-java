package io.honeycomb.libhoney.examples;

import io.honeycomb.libhoney.HoneyClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.honeycomb.libhoney.LibHoney.create;
import static io.honeycomb.libhoney.LibHoney.options;

public class SendImmediately {
    public static HoneyClient initializeClient() {
        return create(options()
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
            honeyClient.send(dataMap);
        }
    }
}
