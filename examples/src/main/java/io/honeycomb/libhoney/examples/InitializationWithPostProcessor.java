package io.honeycomb.libhoney.examples;

import io.honeycomb.libhoney.EventPostProcessor;
import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.eventdata.EventData;

import java.util.HashMap;
import java.util.Map;

import static io.honeycomb.libhoney.LibHoney.create;
import static io.honeycomb.libhoney.LibHoney.options;

public class InitializationWithPostProcessor {
    private HoneyClient honeyClient;
    private Map<String, User> userData;

    public InitializationWithPostProcessor() {
        userData = new HashMap<>();
        userData.put("user-1", new User("Kevin", 12));
        userData.put("user-2", new User("Alice", 22));

        honeyClient = create(
            options()
                .setWriteKey("myTeamWriteKey")
                .setDataset("Cluster Dataset")
                .setEventPostProcessor(new EventPostProcessor() {
                    @Override
                    public void process(final EventData<?> eventData) {
                        enrichUserData(eventData);
                    }
                })
                .build()
        );
    }

    // to avoid blocking the thread, this operation should be quick (e.g. use a cache rather than making DB call)!
    public void enrichUserData(EventData<?> eventData) {
        Object userId = eventData.getFields().get("userId");
        if (userId instanceof String) {
            String id = (String) userId;
            User user = lookupUser(id);
            if (user != null) {
                eventData.addField("userName", user.getName());
                eventData.addField("userAge", user.getAge());
            }
        }
    }

    public User lookupUser(String userId) {
        return userData.get(userId);
    }

    static class User {
        private String name;
        private int age;

        User(String name, int age) {

        }

        String getName() {
            return name;
        }

        int getAge() {
            return age;
        }
    }
}
