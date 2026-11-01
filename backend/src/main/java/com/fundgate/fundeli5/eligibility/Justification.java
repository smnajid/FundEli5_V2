package com.fundgate.fundeli5.eligibility;

import java.util.List;

/** The rule-by-rule explanation behind a Verdict. */
public record Justification(List<RuleOutcome> outcomes) {

    public Justification {
        outcomes = List.copyOf(outcomes);
    }
}
