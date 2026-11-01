package com.fundgate.fundeli5.privacygate;

import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import java.util.Map;

/**
 * The Privacy Gate's output: pseudonymised text, and the local map to swap the ids back. Only the
 * Privacy Gate constructs one. The map never leaves the dealer app.
 */
public record GatedEmail(String text, Map<PortfolioId, PortfolioReference> references) {

    public GatedEmail {
        java.util.Objects.requireNonNull(text);
        references = Map.copyOf(references);
    }

    /** Masked, like a reference: the map is client-identifying. */
    @Override
    public String toString() {
        return "GatedEmail[" + references.size() + " portfolio(s)]";
    }
}
