package com.fundgate.fundeli5.llm;

/**
 * The online entry to a model (ADR 0004): accepts a {@link GatedLlmRequest} only, so nothing but
 * Privacy Gate output can reach a model from the dealer app. The offline pipeline, which reads
 * public documents, keeps its own entry, {@link LlmEgress}.
 */
public interface GatedLlmEgress {

    LlmResponse complete(GatedLlmRequest request);

    /** The online entry on top of an egress adapter (replay, Claude plan, ..). */
    static GatedLlmEgress over(LlmEgress adapter) {
        return new GatedEgress(adapter);
    }
}
