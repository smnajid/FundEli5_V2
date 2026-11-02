package com.fundgate.fundeli5.privacygate;

import java.util.List;
import java.util.Objects;

/**
 * A prompt the Privacy Gate built from allow-listed facts (not from raw text): Portfolio Ids, Verdicts, rule
 * explanations and Citations. The constructor is package-private, so holding one proves the Gate built it.
 */
public final class GatedPrompt implements GatedText {

    private final String text;
    private final List<String> fieldsSent;

    GatedPrompt(String text, List<String> fieldsSent) {
        this.text = Objects.requireNonNull(text);
        this.fieldsSent = List.copyOf(fieldsSent);
    }

    @Override
    public String text() {
        return text;
    }

    @Override
    public List<String> fieldsSent() {
        return fieldsSent;
    }

    @Override
    public String toString() {
        return "GatedPrompt[" + text.length() + " chars]";
    }
}
