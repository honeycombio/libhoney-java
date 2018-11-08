package io.honeycomb.libhoney.examples;

import io.honeycomb.libhoney.HoneyClient;

import static io.honeycomb.libhoney.LibHoney.create;
import static io.honeycomb.libhoney.LibHoney.options;

public class Initialization {
    private HoneyClient honeyClient;

    public Initialization() {
        honeyClient = create(
            options()
                .setWriteKey("myTeamWriteKey")
                .setDataset("Cluster Dataset")
                .setSampleRate(2)
                .build()
        );
    }
}
