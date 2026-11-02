package com.fundgate.fundeli5.privacygate;

import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The Privacy Gate's output: pseudonymised text, and the local map to swap the ids back. The
 * constructor is package-private, so only the Privacy Gate can build one: holding a GatedEmail is
 * the proof that the Gate has passed the text. The map never leaves the dealer app.
 */
public final class GatedEmail {

    private final String text;
    private final Map<PortfolioId, PortfolioReference> references;

    GatedEmail(String text, Map<PortfolioId, PortfolioReference> references) {
        this.text = Objects.requireNonNull(text);
        this.references = Map.copyOf(references);
    }

    public String text() {
        return text;
    }

    public Map<PortfolioId, PortfolioReference> references() {
        return references;
    }

    /** The client fields this text carries to a model, for the Check Record: Portfolio Ids only. */
    public List<String> fieldsSent() {
        return List.of("portfolioId");
    }

    /** Masked, like a reference: the map is client-identifying. */
    @Override
    public String toString() {
        return "GatedEmail[" + references.size() + " portfolio(s)]";
    }
}
