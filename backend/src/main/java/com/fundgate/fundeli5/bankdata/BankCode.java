package com.fundgate.fundeli5.bankdata;

/**
 * A value on one of the bank's own scales, passed through unchanged (ADR 0008). Only the validated
 * Scale Mapping translates it.
 */
public record BankCode(String code, String label) {

    public BankCode {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("A bank code needs a code");
        }
    }
}
