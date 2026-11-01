package com.fundgate.fundeli5.checkrecord;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The Fund Dealer's correction of one field the AI read, kept beside the Check Record it concerns.
 * The record itself is never changed; the correction is appended (screens.md, "Are edits recorded?").
 */
public record Correction(UUID recordId, String field, String parsedValue, String dealerValue, String by, Instant at) {

    public Correction {
        Objects.requireNonNull(recordId);
        Objects.requireNonNull(field);
        Objects.requireNonNull(parsedValue);
        Objects.requireNonNull(dealerValue);
        Objects.requireNonNull(by);
        Objects.requireNonNull(at);
    }
}
