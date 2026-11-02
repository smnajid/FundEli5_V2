package com.fundgate.fundeli5.llm;

import com.fundgate.fundeli5.privacygate.GatedText;
import java.util.Objects;

/** One online call to a model. Its prompt is Privacy Gate output: a raw String cannot be put here (ADR 0004). */
public record GatedLlmRequest(String model, String system, GatedText prompt) {

    public GatedLlmRequest {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(system, "system");
        Objects.requireNonNull(prompt, "prompt");
    }
}
