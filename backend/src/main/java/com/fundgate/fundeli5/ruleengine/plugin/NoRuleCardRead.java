package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.eligibility.RuleOutcome;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.util.List;

/** The Review outcome of the walking skeleton's engine: nothing read a Validated Rule Card. */
public final class NoRuleCardRead {

    private NoRuleCardRead() {}

    public static RuleOutcome outcome() {
        return new RuleOutcome(RuleKind.UNMODELLED_RESTRICTION, RuleOutcome.Result.REVIEW,
                "The Rule Engine is not built yet: no Rule Card was read.", List.of());
    }
}
