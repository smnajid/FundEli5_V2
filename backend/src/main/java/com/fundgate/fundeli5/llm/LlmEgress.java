package com.fundgate.fundeli5.llm;

/**
 * The only code path to a model (ADR 0004). On the online side the prompt is Privacy Gate output;
 * nothing else may call a model.
 */
public interface LlmEgress {

    /** The model's answer to {@code request}. */
    LlmResponse complete(LlmRequest request);
}
