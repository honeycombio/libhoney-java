package io.honeycomb.libhoney.responses.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectReader;
import io.honeycomb.libhoney.utils.JsonUtils;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * This class encapsulates 3 categories of responses that we might expect from the batch API.
 * <ol>
 * <li>{@link BatchResponseBody.ServerResponseCategory#BATCH_ACCEPTED}, where the response code was 200 and the
 * response body is a list of elements that provide information about success/failure of each individual event sent to
 * the server</li>
 * <li>{@link BatchResponseBody.ServerResponseCategory#BATCH_REJECTED}, where the response code was not 200 and the
 * body contains an error message about why the batch failed.</li>
 * <li>{@link BatchResponseBody.ServerResponseCategory#CANNOT_INFER_STATE}, where given the status codes conditions
 * of 1 and 2, there was a failure to deserialize the response body.</li>
 * </ol>
 * <p>
 * To be used together with {@link LazyServerResponse} to avoid the cost of repeatedly deserializing the response body.
 */
public class BatchResponseBody {
    /**
     * The HTTP response code indicating that the batch as a whole was accepted, so we can assume that the response body
     * is an array of response elements.
     */
    public static final int BATCH_ACCEPTED_STATUS_CODE = HttpStatus.SC_OK;
    /**
     * The status code, found within a response element in the body, which indicates success.
     */
    public static final int BATCH_ELEMENT_SUCCESS_CODE = HttpStatus.SC_ACCEPTED;

    private static final ObjectReader BATCH_RESPONSE_ELEMENTS_READER = JsonUtils.OBJECT_MAPPER.readerFor(
        // using array (rather than type descriptor or typefactory) as it appears to be slightly more performant
        BatchResponseElement[].class);
    private static final ObjectReader BATCH_ERROR_RESPONSE_READER = JsonUtils.OBJECT_MAPPER.readerFor(
        BatchErrorResponse.class);

    private final List<BatchResponseElement> batchResponseElements;
    private final BatchErrorResponse batchErrorResponse;
    private final ServerApiError serverApiError;
    private final ServerResponseCategory category;

    // null assignments done for clarity
    @SuppressWarnings("PMD.NullAssignment")
    BatchResponseBody(final byte[] rawHttpResponseBody, final int httpCode) {
        BatchErrorResponse tempError;
        List<BatchResponseElement> tempElements;
        ServerApiError tempServerApiError;
        ServerResponseCategory tempCategory;
        if (httpCode == BATCH_ACCEPTED_STATUS_CODE) {
            try {
                tempElements = Arrays.asList(BATCH_RESPONSE_ELEMENTS_READER.<BatchResponseElement[]>readValue(rawHttpResponseBody));
                tempError = null;
                tempServerApiError = null;
                tempCategory = ServerResponseCategory.BATCH_ACCEPTED;
            } catch (final IOException e) {
                tempElements = null;
                tempError = null;
                tempServerApiError = new ServerApiError(
                    "Failed to parse batch response elements from response", e);
                tempCategory = ServerResponseCategory.CANNOT_INFER_STATE;
            }
        } else {
            try {
                tempElements = null;
                tempError = BATCH_ERROR_RESPONSE_READER.readValue(rawHttpResponseBody);
                tempServerApiError = null;
                tempCategory = ServerResponseCategory.BATCH_REJECTED;
            } catch (final IOException e) {
                tempElements = null;
                tempError = null;
                tempServerApiError = new ServerApiError(
                    "Failed to parse batch error response from response", e);
                tempCategory = ServerResponseCategory.CANNOT_INFER_STATE;
            }
        }
        this.batchErrorResponse = tempError;
        this.batchResponseElements = tempElements;
        this.serverApiError = tempServerApiError;
        this.category = tempCategory;
    }

    ServerResponseCategory getCategory() {
        return category;
    }

    /**
     * @return batch error if {@link #getCategory()} returned {@link ServerResponseCategory#BATCH_REJECTED},
     * else null.
     */
    BatchErrorResponse getBatchError() {
        return batchErrorResponse;
    }

    /**
     * @return list of elements if {@link #getCategory()} returned {@link ServerResponseCategory#BATCH_ACCEPTED},
     * else null.
     */
    List<BatchResponseElement> getBatchResponseElements() {
        return batchResponseElements;
    }

    /**
     * @return api error if {@link #getCategory()} returned {@link ServerResponseCategory#CANNOT_INFER_STATE},
     * else null.
     */
    ServerApiError getServerApiError() {
        return serverApiError;
    }

    enum ServerResponseCategory {
        BATCH_ACCEPTED,
        BATCH_REJECTED,
        CANNOT_INFER_STATE
    }

    static class BatchResponseElement {
        private final int status;

        private final String error;

        BatchResponseElement(@JsonProperty("status") final int status, @JsonProperty("error") final String error) {
            this.status = status;
            this.error = error;
        }

        @JsonIgnore
        boolean isAccepted() {
            return status == BATCH_ELEMENT_SUCCESS_CODE;
        }

        int getStatus() {
            return status;
        }

        String getError() {
            return error;
        }

        @JsonIgnore
        @Override
        public String toString() {
            return "BatchResponseElement{" +
                "status=" + status +
                ", error='" + error + '\'' +
                '}';
        }
    }

    static class BatchErrorResponse {

        private final String error;

        BatchErrorResponse(@JsonProperty("error") final String error) {
            this.error = error;
        }

        String getError() {
            return error;
        }

        @JsonIgnore
        @Override
        public String toString() {
            return "BatchErrorResponse{" +
                "error='" + error + '\'' +
                '}';
        }
    }

    static class ServerApiError {
        private final String message;

        private final IOException cause;

        ServerApiError(final String message, final IOException cause) {

            this.message = message;
            this.cause = cause;
        }

        String getMessage() {
            return message;
        }

        IOException getCause() {
            return cause;
        }

        @Override
        public String toString() {
            return "ServerApiError{" +
                "message='" + message + '\'' +
                ", cause=" + cause +
                '}';
        }
    }
}
