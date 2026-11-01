package com.fundgate.fundeli5.funddocuments;

/** The id of a Fund Document. */
public record DocumentId(String value) {

    public DocumentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A document id cannot be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
