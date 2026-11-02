package com.fundgate.fundeli5.llm;

/** The online entry on top of an egress adapter (replay, Claude plan, ..). */
class GatedEgress implements GatedLlmEgress {

    private final LlmEgress adapter;

    GatedEgress(LlmEgress adapter) {
        this.adapter = adapter;
    }

    @Override
    public LlmResponse complete(GatedLlmRequest request) {
        return adapter.complete(new LlmRequest(request.model(), request.system(), request.prompt().text()));
    }
}
