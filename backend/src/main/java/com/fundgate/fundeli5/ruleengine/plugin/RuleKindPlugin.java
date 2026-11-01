package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.util.Optional;

/**
 * One rule kind: its condition type, its line of the drafting prompt, and how it reads the facts.
 * Adding a kind costs one class and one line in the {@code RuleCatalog}. No clock, no randomness, no model (ADR 0002).
 */
public interface RuleKindPlugin<C extends Condition> {

    RuleKind kind();

    Class<C> conditionType();

    /** The line this kind adds to the Rule Card pipeline's drafting prompt. */
    String draftingLine();

    /** A correction applied to a condition as the model drafted it; most kinds need none. */
    default C asDrafted(C condition) {
        return condition;
    }

    /** Empty when the rule does not apply to this check. */
    Optional<Finding> evaluate(C condition, Facts facts, Lookups lookups);

    /**
     * True: every rule of the kind is shown, and class-level rules add to fund-level ones.
     * False: the worst finding decides, and a class-level rule overrides the fund-level one.
     */
    boolean accumulates();
}
