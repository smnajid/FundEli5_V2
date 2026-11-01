package com.fundgate.fundeli5.llm;

import java.util.Objects;

/** A model's answer, as text. */
public record LlmResponse(String text) {

    public LlmResponse {
        Objects.requireNonNull(text, "text");
    }
}
