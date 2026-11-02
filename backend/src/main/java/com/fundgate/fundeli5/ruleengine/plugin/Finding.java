package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.eligibility.RuleOutcome;

/** What a plug-in found for one rule: a result and the sentence that explains it. */
public record Finding(RuleOutcome.Result result, String explanation, boolean warning) {

    public Finding(RuleOutcome.Result result, String explanation) {
        this(result, explanation, false);
    }

    /** A finding that does not bind: it is shown as a Pass outcome and repeated as a warning. */
    public static Finding warning(String explanation) {
        return new Finding(RuleOutcome.Result.PASS, explanation, true);
    }


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
