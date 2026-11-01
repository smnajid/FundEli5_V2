package com.fundgate.fundeli5.llm;

import java.util.Objects;

/** One call to a model. Replay matches on all three parts, the model included. */
public record LlmRequest(String model, String system, String prompt) {

    public LlmRequest {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(system, "system");
        Objects.requireNonNull(prompt, "prompt");
    }
}
