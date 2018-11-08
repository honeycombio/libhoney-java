package io.honeycomb.libhoney.examples;

import io.honeycomb.libhoney.HoneyClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.honeycomb.libhoney.LibHoney.create;
import static io.honeycomb.libhoney.LibHoney.options;

public class OverridingFields {
    public static HoneyClient initializeClient() {
        int globalSampleRate = 2;
        Map<String, Object> globalDataMap = new HashMap<>();
        globalDataMap.put("cpuCores", Runtime.getRuntime().availableProcessors());
        globalDataMap.put("appProperty", System.getProperty("app.property"));
        globalDataMap.put("userId", -1);

        return create(options()
            .setWriteKey("myTeamWriteKey")
            .setDataset("Cluster Dataset")
            .setGlobalFields(globalDataMap)
            .setSampleRate(globalSampleRate)
            .build()
        );
    }

    public static void main(String... args) {
        try (HoneyClient honeyClient = initializeClient()) {
            honeyClient
                .createEvent()
                .addField("userName", "Bob")
                .addField("userId", UUID.randomUUID().toString())
                .setTimestamp(System.currentTimeMillis())
                .send();
        }
    }
}
