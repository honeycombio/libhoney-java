package io.honeycomb.libhoney.responses;

/**
 * An event response with unknown outcome. This means it cannot be established whether the event was ultimately
 * submitted (i.e. no clear indication of either success or failure), such as in the case of certain network problems.
 */
public interface Unknown extends Response {
    /**
     * @return reason for this response.
     */
    ReasonType getReason();

    /**
     * @return the exception cause - may be null if no exception was the cause.
     */
    Exception getException();

    enum ReasonType {
        HTTP_CLIENT_ERROR,
        SERVER_API_ERROR,
        OTHER;
    }
}
