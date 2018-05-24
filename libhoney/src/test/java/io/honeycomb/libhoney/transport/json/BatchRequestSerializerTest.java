package io.honeycomb.libhoney.transport.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.honeycomb.libhoney.transport.batch.impl.HoneycombBatchConsumer;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class BatchRequestSerializerTest {
    private static final ObjectReader OBJECT_READER = new ObjectMapper().reader();

    private final JsonSerializer jsonSerializer = new BatchRequestSerializer();

    @Test
    public void GIVEN_nonNestedDataInMap_WHEN_jsonSerializerIsCalled_THEN_dataMapIsSerializedCorrectly() throws Exception {
        final Map<String, Object> toSerialize = new HashMap<>();
        toSerialize.put("num", 123);
        toSerialize.put("str", "a str here");
        toSerialize.put("bool", true);

        final ArrayList<HoneycombBatchConsumer.BatchRequestElement> data = new ArrayList<>();
        data.add(new HoneycombBatchConsumer.BatchRequestElement(null, 0, toSerialize));
        final byte[] serialized = jsonSerializer.serialize(data);

        final String expected =  "[{\"samplerate\":0,\"data\":{\"bool\":true, " +
            "\"num\":123, " +
            "\"str\":\"a str here\"}}]";

        assertEquals(expected, serialized);
    }

    @Test
    public void GIVEN_nestedDataInMap_WHEN_jsonSerializerIsCalled_THEN_dataMapIsSerializedCorrectly() throws Exception {
        final Map<String, Object> toSerialize = new HashMap<>();
        toSerialize.put("obj", new InnerData("str", 999, true, new InnerData("strAgain", 55, false, null)));
        final List<Object> jsonArray = new ArrayList<>();
        jsonArray.add("element1");
        jsonArray.add(2);
        toSerialize.put("arr", jsonArray);

        final ArrayList<HoneycombBatchConsumer.BatchRequestElement> data = new ArrayList<>();
        data.add(new HoneycombBatchConsumer.BatchRequestElement(null, 0, toSerialize));
        final byte[] serialized = jsonSerializer.serialize(data);

        final String expected = "[{\"samplerate\":0,\"data\":{" +
            "\"arr\": [ \"element1\", 2 ], " +
            "\"obj\": {\"innerStr\": \"str\", " +
            "\"innerNum\": 999, " +
            "\"innerBool\": true, " +
            "\"innerInnerData\": {\"innerStr\":\"strAgain\"," +
            "\"innerNum\":55," +
            "\"innerBool\":false}}}}]";
        assertEquals(expected, serialized);
    }

    @Test
    public void GIVEN_nullValuesInDataMap_WHEN_jsonSerializerIsCalled_THEN_nullValuesAreNotIncludedInSerializedOutput() throws Exception {
        final Map<String, Object> toSerialize = new HashMap<>();
        toSerialize.put("key1", null);
        toSerialize.put("key2", null);
        toSerialize.put("key3", "non-null");

        final ArrayList<HoneycombBatchConsumer.BatchRequestElement> data = new ArrayList<>();
        data.add(new HoneycombBatchConsumer.BatchRequestElement(null, 0, toSerialize));
        final byte[] serialized = jsonSerializer.serialize(data);

        final String expected = "[{\"samplerate\":0,\"data\":{\"key3\":\"non-null\"}}]";
        assertEquals(expected, serialized);
    }

    @Test
    public void GIVEN_emptyDataMap_WHEN_jsonSerializerIsCalled_THEN_emptyByteArrayIsReturned() throws Exception {
        final byte[] serialized = jsonSerializer.serialize(
            Collections.<HoneycombBatchConsumer.BatchRequestElement>emptyList()
        );
        Assert.assertEquals(2, serialized.length);
    }

    private void assertEquals(final String expected, final byte[] actual) throws IOException {
        final JsonNode expectedTree = OBJECT_READER.readTree(expected);
        final JsonNode actualTree = OBJECT_READER.readTree(new ByteArrayInputStream(actual));
        Assert.assertEquals(expectedTree, actualTree);
    }

    private static class InnerData {
        private final String innerStr;
        private final int innerNum;
        private final boolean innerBool;
        private final InnerData innerInnerData;

        public InnerData(String innerStr, int innerNum, boolean innerBool, InnerData innerInnerData) {
            this.innerStr = innerStr;
            this.innerNum = innerNum;
            this.innerBool = innerBool;
            this.innerInnerData = innerInnerData;
        }

        public String getInnerStr() {
            return innerStr;
        }

        public int getInnerNum() {
            return innerNum;
        }

        public boolean getInnerBool() {
            return innerBool;
        }

        public InnerData getInnerInnerData() {
            return innerInnerData;
        }
    }
}
