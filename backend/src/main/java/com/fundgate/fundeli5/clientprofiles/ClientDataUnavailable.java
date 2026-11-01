package com.fundgate.fundeli5.clientprofiles;

/** The bank's client systems are down or timed out. No Verdict is given on partial data. */
public class ClientDataUnavailable extends RuntimeException {

    public ClientDataUnavailable(String message, Throwable cause) {
        super(message, cause);
    }
}
