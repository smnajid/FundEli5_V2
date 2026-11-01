package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.eligibility.RuleOutcome;

/** What a plug-in found for one rule: a result and the sentence that explains it. */
public record Finding(RuleOutcome.Result result, String explanation) {

    public Finding {
        java.util.Objects.requireNonNull(result);
        java.util.Objects.requireNonNull(explanation);
    }

    public static Finding pass(String explanation) {
        return new Finding(RuleOutcome.Result.PASS, explanation);
    }

    public static Finding fail(String explanation) {
        return new Finding(RuleOutcome.Result.FAIL, explanation);
    }

    public static Finding review(String explanation) {
        return new Finding(RuleOutcome.Result.REVIEW, explanation);
    }
}
