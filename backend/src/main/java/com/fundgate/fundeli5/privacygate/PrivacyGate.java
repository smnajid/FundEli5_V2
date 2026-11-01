package com.fundgate.fundeli5.privacygate;

/** The checkpoint every email passes before any model sees it (ADR 0004). Fails closed. */
public interface PrivacyGate {

    /** Swaps every Portfolio Reference for its Portfolio Id, or refuses the email. */
    GateOutcome gate(String rawEmail);
}
