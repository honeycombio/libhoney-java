package io.honeycomb.libhoney.examples;

import io.honeycomb.libhoney.EventFactory;
import io.honeycomb.libhoney.HoneyClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.honeycomb.libhoney.LibHoney.create;
import static io.honeycomb.libhoney.LibHoney.options;

public class UsingEventFactory {
    public static HoneyClient initializeGlobalClient() {
        int globalSampleRate = 10;
        Map<String, Object> globalDataMap = new HashMap<>();
        globalDataMap.put("cpuCores", Runtime.getRuntime().availableProcessors());
        globalDataMap.put("appProperty", System.getProperty("app.property"));

        return create(options()
            .setWriteKey("myTeamWriteKey")
            .setDataset("Cluster Dataset")
            .setGlobalFields(globalDataMap)
            .setSampleRate(globalSampleRate)
            .build()
        );
    }

    public static void main(String... args) {
        try (HoneyClient honeyClient = initializeGlobalClient()) {
            UserService service = new UserService(honeyClient);
            service.sendEvent("Bob");
            service.sendEvent("Alice");
            service.sendEvent("Kevin");
        }
    }

    static class UserService {
        private final EventFactory localBuilder;

        UserService(HoneyClient libHoney) {
            int serviceLevelSampleRate = 2;
            localBuilder = libHoney.buildEventFactory()
                .addField("serviceName", "userService")
                .setSampleRate(serviceLevelSampleRate)
                .build();
        }

        void sendEvent(String username) {
            localBuilder
                .createEvent()
                .addField("userName", username)
                .addField("userId", UUID.randomUUID().toString())
                .setTimestamp(System.currentTimeMillis())
                .send();
        }
    }
}
