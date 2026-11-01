package com.fundgate.fundeli5.bankdata;

import java.util.regex.Pattern;

/** An ISIN: the key of a Share Class. */
public record Isin(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{2}[A-Z0-9]{9}\\d$");

    public Isin {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Not an ISIN: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
