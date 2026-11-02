package com.fundgate.fundeli5.rulecardpipeline.jev;

/** Jev could not answer: no or a bad key (it answers 403), another status, or an unreadable reply. */
public class JevUnavailable extends RuntimeException {

    public JevUnavailable(String message) {
        super(message);
    }
}
