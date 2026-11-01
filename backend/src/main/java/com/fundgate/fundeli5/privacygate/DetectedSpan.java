package com.fundgate.fundeli5.privacygate;

/** One piece of personal data a detector found: its entity type, where it sits in the text, and how sure the detector is. */
public record DetectedSpan(String entityType, int start, int end, double score) {

    public static final String PERSON = "PERSON";

    public DetectedSpan {
        java.util.Objects.requireNonNull(entityType);
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("Not a span: " + start + ".." + end);
        }
    }

    public boolean isPerson() {
        return PERSON.equals(entityType);
    }
}
