package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.util.Optional;

/**
 * A restriction no rule kind models (ADR 0003). While Blocking it gives Review; once a Rule Card Validator
 * marks it Noted it passes but stays in the Justification with its Citation.
 */
public final class UnmodelledRestrictionPlugin implements RuleKindPlugin<Condition.UnmodelledRestriction> {

    @Override
    public RuleKind kind() {
        return RuleKind.UNMODELLED_RESTRICTION;
    }

    @Override
    public Class<Condition.UnmodelledRestriction> conditionType() {
        return Condition.UnmodelledRestriction.class;
    }

    @Override
    public String draftingLine() {
        return "unmodelled_restriction: any restriction on who may subscribe that no other kind can express "
                + "(status blocking; the validator marks it noted).";
    }

    @Override
    public Optional<Finding> evaluate(Condition.UnmodelledRestriction condition, Facts facts, Lookups lookups) {
        if (condition.status() == Condition.RestrictionStatus.NOTED) {
            String reason = condition.notedReason() == null || condition.notedReason().isBlank()
                    ? "" : ": " + condition.notedReason();
            return Optional.of(Finding.pass("Noted restriction, no effect on the Verdict" + reason));
        }
        return Optional.of(Finding.review("A restriction the engine cannot read is Blocking until a Rule Card "
                + "Validator marks it Noted."));
    }

    @Override
    public boolean accumulates() {
        return true;
    }
}
