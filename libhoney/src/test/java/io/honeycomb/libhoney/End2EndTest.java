package io.honeycomb.libhoney;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.honeycomb.libhoney.eventdata.EventData;
import io.honeycomb.libhoney.responses.ClientRejected;
import io.honeycomb.libhoney.responses.Response;
import io.honeycomb.libhoney.responses.ServerAccepted;
import io.honeycomb.libhoney.responses.ServerRejected;
import io.honeycomb.libhoney.responses.ServerResponse;
import io.honeycomb.libhoney.responses.Unknown;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.honeycomb.libhoney.TransportOptions.DEFAULT_BATCH_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

public class End2EndTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(8089);

    private HoneyClient honeyClient;
    private BlockingQueue<Response> notifyQueue;

    @Before
    public void setup() {
        stubServer(200, "[" +
            "  {\"status\": 202}" +
            "]");
    }

    // this client will not timeout batches
    private void createDefaultClient() {
        honeyClient = new HoneyClient(LibHoney.options()
            .setWriteKey("testWriteKey")
            .setDataset("testDataSet")
            .setApiHost(URI.create("http://localhost:8089"))
            .build());
        notifyQueue = createObserverQueue();
    }

    private void createClientWithoutTimeout() {
        honeyClient = new HoneyClient(LibHoney.options()
            .setWriteKey("testWriteKey")
            .setDataset("testDataSet")
            .setApiHost(URI.create("http://localhost:8089"))
            .build(),
            TransportOptions.builder().setBatchTimeoutMillis(Long.MAX_VALUE).build());
        notifyQueue = createObserverQueue();
    }

    @After
    public void tearDown() throws Exception {
        honeyClient.close();
    }

    @Test
    public void sendingOneSimpleEvent() throws InterruptedException {
        createDefaultClient();

        honeyClient.send(Collections.singletonMap("SimpleData", "SimpleValue"));
        final Response response = notifyQueue.poll(2000, TimeUnit.MILLISECONDS);

        assertThat(response).isInstanceOf(ServerAccepted.class);
        assertThat(((ServerResponse) response).getEventStatusCode()).isEqualTo(202);
        assertThat(((ServerResponse) response).getBatchData().getPositionInBatch()).isEqualTo(0);
        assertThat(((ServerResponse) response).getBatchData().getBatchStatusCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("ACCEPTED");
        verify(postRequestedFor(urlPathMatching("/1/batch/testDataSet"))
            .withHeader("Content-Type", containing("application/json"))
            .withHeader("X-Honeycomb-Team", equalTo("testWriteKey"))
            .withHeader("User-Agent", matching("libhoney-java/\\d+\\.\\d+\\.\\d+"))
            .withRequestBody(equalToJson("[" +
                "  {" +
                "    \"data\": {\"SimpleData\": \"SimpleValue\"}," +
                "    \"samplerate\": 1" +
                "  }" +
                "]", false, true))
            .withRequestBody(matchingJsonPath("$[0].time"))
        );
    }

    @Test
    public void sending2SimpleEvents() throws InterruptedException {
        stubServer(200, "[{\"status\": 202},{\"status\": 202}]");
        createDefaultClient();

        honeyClient.send(Collections.singletonMap("SimpleData", "SimpleValue"));
        honeyClient.send(Collections.singletonMap("SimpleData2", "SimpleValue2"));
        final Response response1 = notifyQueue.poll(2000, TimeUnit.MILLISECONDS);
        final Response response2 = notifyQueue.poll(2000, TimeUnit.MILLISECONDS);
        assertThat(response1.getMessage()).isEqualTo("ACCEPTED");
        assertThat(response2.getMessage()).isEqualTo("ACCEPTED");

        verify(postRequestedFor(urlPathMatching("/1/batch/testDataSet"))
            .withHeader("Content-Type", containing("application/json"))
            .withHeader("X-Honeycomb-Team", equalTo("testWriteKey"))
            .withHeader("User-Agent", matching("libhoney-java/\\d+\\.\\d+\\.\\d+"))
            .withRequestBody(equalToJson("[" +
                "  {" +
                "    \"data\": {\"SimpleData\": \"SimpleValue\"}," +
                "    \"samplerate\": 1" +
                "  }," +
                "  {" +
                "    \"data\": {\"SimpleData2\": \"SimpleValue2\"}," +
                "    \"samplerate\": 1" +
                "  }" +
                "]", false, true))
            .withRequestBody(matchingJsonPath("$[0].time"))
            .withRequestBody(matchingJsonPath("$[1].time"))
        );
    }

    @Test
    public void sendingAnEventWhereEveryFieldTypeIsConfiguredAndVariousDataTypesAreIncluded() throws InterruptedException {
        honeyClient = new HoneyClient(LibHoney.options()
            .setWriteKey("testWriteKey")
            .setDataset("testDataSet")
            .setGlobalFields(Collections.singletonMap("GlobalData", false))
            .setGlobalDynamicFields(Collections.singletonMap("GlobalDynamicData", new ValueSupplier<Object>() {
                @Override
                public Object supply() {
                    return new String[]{
                        "a", "b", "c"
                    };
                }
            }))
            .setEventPostProcessor(new EventPostProcessor() {
                @Override
                public void process(final EventData<?> eventData) {
                    eventData.addField("PostProcessData", new TestData().setInnerData("inner"));
                }
            })
            .setApiHost(URI.create("http://localhost:8089"))
            .build());
        final BlockingQueue<Response> notifyQueue = createObserverQueue();

        final Event event = honeyClient.buildEventFactory()
            .addDynamicField("DynamicData", new ValueSupplier<TestData>() {
                @Override
                public TestData supply() {
                    return new TestData().setInnerData(Boolean.TRUE);
                }
            })
            .addField("StaticData", 123)
            .build()
            .createEvent()
            .addField("SimpleData", "SimpleValue");

        event.send();
        final Response response = notifyQueue.poll(2000, TimeUnit.MILLISECONDS);

        assertThat(response).isNotNull();
        verify(postRequestedFor(urlPathMatching("/1/batch/testDataSet"))
            .withHeader("Content-Type", containing("application/json"))
            .withHeader("X-Honeycomb-Team", equalTo("testWriteKey"))
            .withHeader("User-Agent", matching("libhoney-java/\\d+\\.\\d+\\.\\d+"))
            .withRequestBody(equalToJson("[" +
                "  {" +
                "    \"data\": {" +
                "      \"SimpleData\": \"SimpleValue\"," +
                "      \"StaticData\": 123," +
                "      \"DynamicData\": {" +
                "        \"innerData\": true" +
                "      }," +
                "      \"PostProcessData\": {" +
                "        \"innerData\": \"inner\"" +
                "      }," +
                "      \"GlobalDynamicData\": [\"a\", \"b\", \"c\"]," +
                "      \"GlobalData\": false" +
                "    }" +
                "  }" +
                "]", false, true))
        );
    }

    @Test
    public void sendEnoughEventsToFill3Batches() throws InterruptedException {
        final String requestBody = joinElements(createResponseElements(DEFAULT_BATCH_SIZE, "{\"status\": 202}"));
        stubServer(200, requestBody);
        createClientWithoutTimeout();

        final int numberOfBatches = 3;
        sendTimes(DEFAULT_BATCH_SIZE * numberOfBatches);
        final List<Response> responses = collectResponses(DEFAULT_BATCH_SIZE * numberOfBatches);

        for (final Response response : responses) {
            assertThat(response).isNotNull();
        }
        simpleVerifyInteractionWithServer();
    }

    @Test
    public void sendEventsWhere60PercentOfEventsInTheBatchHaveFailed() throws InterruptedException {
        final List<String> badResponses = createResponseElements(30, "{\"status\": 400, \"error\": \"ERROR!\"}");
        final List<String> goodResponse = createResponseElements(20, "{\"status\": 202}");
        goodResponse.addAll(badResponses);
        Collections.shuffle(goodResponse);
        final String body = joinElements(goodResponse);
        stubServer(200, body);
        createClientWithoutTimeout();

        sendTimes(50);
        List<Response> responses = collectResponses(50);

        assertThat(responses).haveExactly(30, new Condition<Response>() {
            @Override
            public boolean matches(final Response value) {
                return (value instanceof ServerRejected) && (((ServerRejected) value).getEventStatusCode() == 400);
            }
        });
        assertThat(responses).haveExactly(20, new Condition<Response>() {
            @Override
            public boolean matches(final Response value) {
                return (value instanceof ServerAccepted) && (((ServerAccepted) value).getEventStatusCode() == 202);
            }
        });
        simpleVerifyInteractionWithServer();
    }

    @Test
    public void sendEventsWhereTheBatchRequestAsAWholeHasFailed() throws InterruptedException {
        stubServer(401, "{\"error\": \"ERROR!\"}");
        createClientWithoutTimeout();

        sendTimes(50);
        final List<Response> responses = collectResponses(50);

        assertThat(responses).haveExactly(50, new Condition<Response>() {
            @Override
            public boolean matches(final Response value) {
                return
                    (value instanceof ServerRejected) &&
                        (((ServerRejected) value).getBatchData().getBatchStatusCode() == 401) &&
                        (((ServerRejected) value).getEventStatusCode() == -1);
            }
        });
        simpleVerifyInteractionWithServer();
    }

    public void stubServer(final int status, final String responseBody) {
        stubFor(post(urlPathMatching("/1/batch/.*"))
            .willReturn(
                aResponse().withStatus(status)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody)));
    }

    public List<String> createResponseElements(final int numOfElements, final String elementString) {
        final List<String> elements = new ArrayList<>();
        for (int i = 0; i < numOfElements; i++) {
            elements.add(elementString);
        }
        return elements;
    }

    public String joinElements(final List<String> goodResponse) {
        return '[' + StringUtils.join(goodResponse, ",") + ']';
    }

    public List<Response> collectResponses(final int amount) throws InterruptedException {
        final List<Response> responses = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            final Response response = notifyQueue.poll(2000, TimeUnit.MILLISECONDS);
            assertThat(response).isNotNull();
            responses.add(response);
        }
        return responses;
    }

    public void sendTimes(final int iterations) {
        for (int i = 0; i < iterations; i++) {
            honeyClient.send(Collections.singletonMap("SimpleData", "SimpleValue"));
        }
    }

    private void simpleVerifyInteractionWithServer() {
        verify(postRequestedFor(urlPathMatching("/1/batch/testDataSet"))
            .withHeader("Content-Type", containing("application/json"))
            .withHeader("X-Honeycomb-Team", equalTo("testWriteKey"))
            .withHeader("User-Agent", matching("libhoney-java/\\d+\\.\\d+\\.\\d+"))
        );
    }

    private BlockingQueue<Response> createObserverQueue() {
        final BlockingResponseObserver observer = new BlockingResponseObserver();
        final BlockingQueue<Response> notifyQueue = observer.getNotifyQueue();
        honeyClient.addResponseObserver(observer);
        return notifyQueue;
    }

    private static class BlockingResponseObserver implements ResponseObserver {
        private final BlockingQueue<Response> notifyQueue = new ArrayBlockingQueue<>(1000);

        @Override
        public void onServerAccepted(final ServerAccepted serverAccepted) {
            notifyQueue.add(serverAccepted);
        }

        @Override
        public void onServerRejected(final ServerRejected serverRejected) {
            notifyQueue.add(serverRejected);
        }

        @Override
        public void onClientRejected(final ClientRejected clientRejected) {
            notifyQueue.add(clientRejected);
        }

        @Override
        public void onUnknown(final Unknown unknown) {
            notifyQueue.add(unknown);
        }

        public BlockingQueue<Response> getNotifyQueue() {
            return notifyQueue;
        }
    }

    private class TestData {
        private Object innerData;

        public Object getInnerData() {
            return innerData;
        }

        public TestData setInnerData(final Object innerData) {
            this.innerData = innerData;
            return this;
        }
    }
}
