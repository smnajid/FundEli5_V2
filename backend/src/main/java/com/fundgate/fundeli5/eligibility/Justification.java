package com.fundgate.fundeli5.eligibility;

import java.util.List;

/** The rule-by-rule explanation behind a Verdict. Warnings never change the Verdict. */
public record Justification(List<RuleOutcome> outcomes, List<String> warnings) {

    public Justification {
        outcomes = List.copyOf(outcomes);
        warnings = List.copyOf(warnings);
    }

    public Justification(List<RuleOutcome> outcomes) {
        this(outcomes, List.of());
    }
}
