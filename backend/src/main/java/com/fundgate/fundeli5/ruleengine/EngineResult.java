package com.fundgate.fundeli5.ruleengine;

import com.fundgate.fundeli5.eligibility.Justification;
import com.fundgate.fundeli5.eligibility.Verdict;

/** A Verdict and the rule-by-rule explanation behind it. */
public record EngineResult(Verdict verdict, Justification justification) {

    public EngineResult {
        java.util.Objects.requireNonNull(verdict);
        java.util.Objects.requireNonNull(justification);
    }
}
