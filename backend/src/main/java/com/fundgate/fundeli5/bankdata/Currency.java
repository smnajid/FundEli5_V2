package com.fundgate.fundeli5.bankdata;

import java.util.regex.Pattern;

/** An ISO 4217 currency code. */
public record Currency(String code) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{3}$");

    public Currency {
        if (code == null || !FORMAT.matcher(code).matches()) {
            throw new IllegalArgumentException("Not a currency code: " + code);
        }
    }

    @Override
    public String toString() {
        return code;
    }
}
