package io.honeycomb.libhoney.responses.impl;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchResponseBodyTest {

    public void assertIsBatchAccepted(final BatchResponseBody batchResponseBody) {
        assertThat(batchResponseBody.getCategory()).isEqualTo(BatchResponseBody.ServerResponseCategory.BATCH_ACCEPTED);
        assertThat(batchResponseBody.getBatchResponseElements()).isNotNull();
        assertThat(batchResponseBody.getServerApiError()).isNull();
        assertThat(batchResponseBody.getBatchError()).isNull();
    }

    public void assertIsBatchRejected(final BatchResponseBody batchResponseBody) {
        assertThat(batchResponseBody.getCategory()).isEqualTo(BatchResponseBody.ServerResponseCategory.BATCH_REJECTED);
        assertThat(batchResponseBody.getBatchError()).isNotNull();
        assertThat(batchResponseBody.getBatchResponseElements()).isNull();
        assertThat(batchResponseBody.getServerApiError()).isNull();
    }

    private void assertIsCannotInferState(final BatchResponseBody batchResponseBody) {
        assertThat(batchResponseBody.getCategory()).isEqualTo(BatchResponseBody.ServerResponseCategory.CANNOT_INFER_STATE);
        assertThat(batchResponseBody.getBatchError()).isNull();
        assertThat(batchResponseBody.getBatchResponseElements()).isNull();
        assertThat(batchResponseBody.getServerApiError()).isNotNull();
    }

    @Test
    public void GIVEN_OkStatusCode_AND_aValidBatchResponse_EXPECT_batchResponseElementsToBeInitialised() {
        final String batchBody = "[" +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"error\": \"Oops!\"," +
            "    \"status\": 400" +
            "  }" +
            "]";

        final int okCode = 200;
        final BatchResponseBody batchResponseBody = new BatchResponseBody(batchBody.getBytes(StandardCharsets.UTF_8), okCode);

        assertIsBatchAccepted(batchResponseBody);
        assertThat(batchResponseBody.getBatchResponseElements()).hasSize(2);

        assertThat(batchResponseBody.getBatchResponseElements().get(0).isAccepted()).isTrue();
        assertThat(batchResponseBody.getBatchResponseElements().get(0).getError()).isNull();
        assertThat(batchResponseBody.getBatchResponseElements().get(0).getStatus()).isEqualTo(202);

        assertThat(batchResponseBody.getBatchResponseElements().get(1).isAccepted()).isFalse();
        assertThat(batchResponseBody.getBatchResponseElements().get(1).getError()).isEqualTo("Oops!");
        assertThat(batchResponseBody.getBatchResponseElements().get(1).getStatus()).isEqualTo(400);
    }

    @Test
    public void GIVEN_OkStatusCode_BUT_anInvalidBatchResponse_EXPECT_batchErrorToBeInitialised() {
        final String batchBody = "[" +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"error\": \"Oops!\"," +
            "    \"status\": 400" +
            "  }"; // brackets not closed

        final int okCode = 200;
        final BatchResponseBody batchResponseBody = new BatchResponseBody(batchBody.getBytes(StandardCharsets.UTF_8), okCode);

        assertIsCannotInferState(batchResponseBody);
        assertThat(batchResponseBody.getServerApiError().getCause()).isInstanceOf(IOException.class);
        assertThat(batchResponseBody.getServerApiError().getMessage()).contains("Failed to parse batch response elements");
    }

    @Test
    public void GIVEN_NonOkStatusCode_EXPECT_batchErrorToBeInitialised() {
        final String errorBody = "{\"error\": \"Error!\"}";

        final int notOkCode = 400;
        final BatchResponseBody batchResponseBody = new BatchResponseBody(errorBody.getBytes(StandardCharsets.UTF_8), notOkCode);

        assertIsBatchRejected(batchResponseBody);
        assertThat(batchResponseBody.getBatchError().getError()).isEqualTo("Error!");
    }

    @Test
    public void GIVEN_NonOkStatusCode_AND_InvalidResponseBody_EXPECT_batchErrorToBeInitialised() {
        final String errorBody = "{\"message\": \"Error!\"}"; // wrong key

        final int okCode = 401;
        final BatchResponseBody batchResponseBody = new BatchResponseBody(errorBody.getBytes(StandardCharsets.UTF_8), okCode);

        assertIsCannotInferState(batchResponseBody);
        assertThat(batchResponseBody.getServerApiError().getCause()).isInstanceOf(IOException.class);
        assertThat(batchResponseBody.getServerApiError().getMessage()).contains("Failed to parse batch error response");
    }

    @Test
    public void GIVEN_503_AND_InvalidResponseBody_EXPECT_RawResponse_in_Message() {
        final String errorBody = "<html><head>test</head><body>This is probably coming from proxy server rather than honeycomb</body></html>";

        final int okCode = 503;
        final BatchResponseBody batchResponseBody = new BatchResponseBody(errorBody.getBytes(StandardCharsets.UTF_8), okCode);

        assertIsCannotInferState(batchResponseBody);
        assertThat(batchResponseBody.getServerApiError().getCause()).isInstanceOf(IOException.class);
        final String expectedResponse = "Failed to parse batch error response from response. Raw response: " +
            "```<html><head>test</head><body>This is probably coming from proxy server rather than honeycomb</body></html>```";
        assertThat(batchResponseBody.getServerApiError().getMessage()).isEqualTo(expectedResponse);
    }

    @Test
    public void GIVEN_ALazyBody_AND_OkStatusCode_AND_aValidBatchResponse_EXPECT_batchResponseElementsToBeInitialised() {
        final String batchBody = "[" +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"error\": \"Oops!\"," +
            "    \"status\": 400" +
            "  }" +
            "]";

        final int okCode = 200;
        final LazyServerResponse.LazyResponseBody lazy = new LazyServerResponse.LazyResponseBody(batchBody.getBytes(StandardCharsets.UTF_8), okCode);
        final BatchResponseBody batchResponseBody = lazy.get();

        assertIsBatchAccepted(batchResponseBody);
        assertThat(batchResponseBody.getBatchResponseElements()).hasSize(2);

        assertThat(batchResponseBody.getBatchResponseElements().get(0).isAccepted()).isTrue();
        assertThat(batchResponseBody.getBatchResponseElements().get(0).getError()).isNull();
        assertThat(batchResponseBody.getBatchResponseElements().get(0).getStatus()).isEqualTo(202);

        assertThat(batchResponseBody.getBatchResponseElements().get(1).isAccepted()).isFalse();
        assertThat(batchResponseBody.getBatchResponseElements().get(1).getError()).isEqualTo("Oops!");
        assertThat(batchResponseBody.getBatchResponseElements().get(1).getStatus()).isEqualTo(400);
    }


    @Test
    public void GIVEN_ALazyBody_AND_anInitialGet_EXPECT_SubsequentGetToReturnSameInstance() {
        final String batchBody = "[" +
            "  {" +
            "    \"status\": 202" +
            "  }," +
            "  {" +
            "    \"error\": \"Oops!\"," +
            "    \"status\": 400" +
            "  }" +
            "]";

        final int okCode = 200;
        final LazyServerResponse.LazyResponseBody lazy = new LazyServerResponse.LazyResponseBody(batchBody.getBytes(StandardCharsets.UTF_8), okCode);
        final BatchResponseBody batchResponseBody = lazy.get();
        final BatchResponseBody batchResponseBody2 = lazy.get();

        assertThat(batchResponseBody).isSameAs(batchResponseBody2);
    }

}
