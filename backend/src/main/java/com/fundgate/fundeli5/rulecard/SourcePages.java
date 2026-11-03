package com.fundgate.fundeli5.rulecard;

import java.util.Optional;

/**
 * Port: the text of the pages a Rule Card cites, stored by the pipeline next to the Draft card (public Fund
 * Documentation only). It lets the validation screen show a page without the dealer runtime reading Fund
 * Documents (ADR 0004).
 */
public interface SourcePages {

    /** The text of {@code page} of {@code document} as read when the card was drafted, if it was kept. */
    Optional<String> text(String fundName, int version, String document, int page);
}
