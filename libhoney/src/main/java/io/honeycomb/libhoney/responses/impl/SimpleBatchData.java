package io.honeycomb.libhoney.responses.impl;

import io.honeycomb.libhoney.responses.ServerResponse;

class SimpleBatchData implements ServerResponse.BatchData {
    private final int positionInBatch;
    private final int batchStatusCode;

    SimpleBatchData(final int positionInBatch, final int batchStatusCode) {
        this.positionInBatch = positionInBatch;
        this.batchStatusCode = batchStatusCode;
    }

    @Override
    public int getPositionInBatch() {
        return positionInBatch;
    }

    @Override
    public int getBatchStatusCode() {
        return batchStatusCode;
    }

    @Override
    public String toString() {
        return "SimpleBatchData{" +
            "positionInBatch=" + positionInBatch +
            ", batchStatusCode=" + batchStatusCode +
            '}';
    }
}
