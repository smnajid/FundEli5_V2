package com.fundgate.fundeli5.citation;

/** A pointer to the exact passage of a Fund Document that a rule or outcome relies on. */
public record Citation(String document, int page, String quote, boolean quoteFoundOnPage) {

    public Citation {
        java.util.Objects.requireNonNull(document);
        java.util.Objects.requireNonNull(quote);
    }
}
