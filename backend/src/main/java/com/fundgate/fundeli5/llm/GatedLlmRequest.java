package com.fundgate.fundeli5.llm;

import com.fundgate.fundeli5.privacygate.GatedEmail;
import java.util.Objects;

/** One online call to a model. Its prompt is Privacy Gate output: a raw String cannot be put here (ADR 0004). */
public record GatedLlmRequest(String model, String system, GatedEmail email) {

    public GatedLlmRequest {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(system, "system");
        Objects.requireNonNull(email, "email");
    }
}
