package com.fundgate.fundeli5.bankdata;

/** The bank's fund master data cannot be read now. Never answered with an empty result. */
public class FundDataUnavailable extends RuntimeException {

    public FundDataUnavailable(String message, Throwable cause) {
        super(message, cause);
    }

    public FundDataUnavailable(String message) {
        super(message);
    }
}
