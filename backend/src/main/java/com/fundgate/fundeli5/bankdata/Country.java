package com.fundgate.fundeli5.bankdata;

import java.util.regex.Pattern;

/** An ISO 3166-1 alpha-2 country code. */
public record Country(String code) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{2}$");

    public Country {
        if (code == null || !FORMAT.matcher(code).matches()) {
            throw new IllegalArgumentException("Not a country code: " + code);
        }
    }

    @Override
    public String toString() {
        return code;
    }
}
