package io.honeycomb.libhoney.examples;

import io.honeycomb.libhoney.HoneyClient;
import io.honeycomb.libhoney.ValueSupplier;

import java.util.HashMap;
import java.util.Map;

import static io.honeycomb.libhoney.LibHoney.create;
import static io.honeycomb.libhoney.LibHoney.options;

public class InitializationWithDynamicFields {
    private HoneyClient honeyClient;

    public InitializationWithDynamicFields() {
        Map<String, ValueSupplier<Object>> dynamicFields = new HashMap<>();
        dynamicFields.put("total_memory", new ValueSupplier<Object>() {
            @Override
            public Object supply() {
                return Runtime.getRuntime().totalMemory();
            }
        });
        dynamicFields.put("free_memory", new ValueSupplier<Object>() {
            @Override
            public Object supply() {
                return Runtime.getRuntime().freeMemory();
            }
        });
        honeyClient = create(
            options()
                .setWriteKey("myTeamWriteKey")
                .setDataset("Cluster Dataset")
                .setGlobalDynamicFields(dynamicFields)
                .build()
        );
    }
}
