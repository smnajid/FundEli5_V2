package com.fundgate.fundeli5.eligibility;

import com.fundgate.fundeli5.citation.Citation;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.util.List;

/** What one rule gave for one check, with its Citations. */
public record RuleOutcome(RuleKind kind, Result result, String explanation, List<Citation> citations) {

    public enum Result { PASS, FAIL, REVIEW }

    public RuleOutcome {
        java.util.Objects.requireNonNull(kind);
        java.util.Objects.requireNonNull(result);
        citations = List.copyOf(citations);
    }
}
