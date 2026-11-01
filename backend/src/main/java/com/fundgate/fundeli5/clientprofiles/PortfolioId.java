package com.fundgate.fundeli5.clientprofiles;

import java.util.UUID;

/** The opaque id of a portfolio; one to one with its reference, and safe to share with an LLM. */
public record PortfolioId(UUID value) {

    public PortfolioId {
        java.util.Objects.requireNonNull(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
