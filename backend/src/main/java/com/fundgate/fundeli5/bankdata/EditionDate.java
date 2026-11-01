package com.fundgate.fundeli5.bankdata;

import java.util.regex.Pattern;

/** The edition of a document: a full date or a month ({@code 2026-06-29} or {@code 2026-06}). */
public record EditionDate(String value) {

    private static final Pattern FORMAT = Pattern.compile("^\\d{4}-\\d{2}(-\\d{2})?$");

    public EditionDate {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Not an edition date: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
