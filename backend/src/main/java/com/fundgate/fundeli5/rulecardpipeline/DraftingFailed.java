package com.fundgate.fundeli5.rulecardpipeline;

/** The model's answer holds no usable Rule Card: the pipeline fails loudly rather than save an empty draft. */
public class DraftingFailed extends RuntimeException {

    public DraftingFailed(String message) {
        super(message);
    }

    public DraftingFailed(String message, Throwable cause) {
        super(message, cause);
    }
}
