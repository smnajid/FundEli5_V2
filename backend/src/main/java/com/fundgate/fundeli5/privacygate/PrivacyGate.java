package com.fundgate.fundeli5.privacygate;

/** The checkpoint every email passes before any model sees it (ADR 0004). Fails closed. */
public interface PrivacyGate {

    /** Swaps every Portfolio Reference for its Portfolio Id, or refuses the email. */
    GateOutcome gate(String rawEmail);

    /**
     * Builds the prompt of a reply call from allow-listed facts, never from raw text. Fails closed
     * ({@link IllegalArgumentException}) when the text would carry a Portfolio Reference.
     */
    GatedPrompt gateReply(ReplyFacts facts);
}
