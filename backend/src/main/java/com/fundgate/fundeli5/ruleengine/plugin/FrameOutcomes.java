package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.eligibility.RuleOutcome;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.util.List;

/**
 * The outcomes the engine's frame gives when no rule can be read at all. They sit with the plug-ins
 * because naming a {@link RuleKind} is the plug-ins' business alone (ArchUnit 6b).
 */
public final class FrameOutcomes {

    private FrameOutcomes() {}

    public static RuleOutcome fundNotCovered() {
        return review("Fund not covered: no Rule Card exists for this Share Class.");
    }

    public static RuleOutcome regimeNotCovered(String legalEntity) {
        return review("Legal Entity " + legalEntity + ": regime not covered. Only Switzerland and Luxembourg are.");
    }

    public static RuleOutcome cardNotValidated() {
        return review("Rule Card not validated: only a Validated Rule Card decides.");
    }

    public static RuleOutcome noPlugIn(Condition condition) {
        return review("No plug-in reads a " + condition.kind().name() + " rule yet, so it was not applied.");
    }

    public static RuleOutcome noRuleApplies() {
        return review("No rule of the Rule Card applies to this Share Class.");
    }

    private static RuleOutcome review(String explanation) {
        return new RuleOutcome(RuleKind.UNMODELLED_RESTRICTION, RuleOutcome.Result.REVIEW, explanation, List.of());
    }
}
