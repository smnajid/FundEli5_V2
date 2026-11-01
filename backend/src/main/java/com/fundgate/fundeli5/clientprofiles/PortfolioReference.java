package com.fundgate.fundeli5.clientprofiles;

import java.util.regex.Pattern;

/** The bank's identifier of a portfolio. Client-identifying: never sent to an LLM (ADR 0004). */
public record PortfolioReference(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{3}\\d{8}$");

    public PortfolioReference {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Not a Portfolio Reference");
        }
    }

    /** The Legal Entity: the first three letters. */
    public String legalEntity() {
        return value.substring(0, 3);
    }

    @Override
    public String toString() {
        return value;
    }
}
