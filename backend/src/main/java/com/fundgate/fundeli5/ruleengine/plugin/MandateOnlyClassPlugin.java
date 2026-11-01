package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.clientprofiles.ClientProfile;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.util.Optional;

/** A class reserved to mandates: the portfolio needs a discretionary mandate, with the named manager if the card names one. */
public final class MandateOnlyClassPlugin implements RuleKindPlugin<Condition.MandateOnlyClass> {

    @Override
    public RuleKind kind() {
        return RuleKind.MANDATE_ONLY_CLASS;
    }

    @Override
    public Class<Condition.MandateOnlyClass> conditionType() {
        return Condition.MandateOnlyClass.class;
    }

    @Override
    public String draftingLine() {
        return "mandate_only_class: the class is reserved to clients with a mandate; manager names the manager "
                + "the mandate must be with, or is null when the text names none.";
    }

    @Override
    public Optional<Finding> evaluate(Condition.MandateOnlyClass condition, Facts facts, Lookups lookups) {
        var portfolio = facts.profile().portfolio();
        var mandate = portfolio == null ? null : portfolio.mandate();
        if (mandate == null || mandate.type() == null) {
            return Optional.of(Finding.review("The portfolio's mandate type is unknown."));
        }
        if (mandate.type() != ClientProfile.MandateType.DISCRETIONARY) {
            return Optional.of(Finding.fail("The class is reserved to mandates, and the portfolio has an "
                    + mandate.type().name().toLowerCase().replace('_', '-') + " relationship."));
        }
        if (condition.manager() == null) {
            return Optional.of(Finding.pass("The portfolio has a discretionary mandate."));
        }
        var manager = mandate.manager();
        if (manager == null || manager.kind() == null) {
            return Optional.of(Finding.review("The manager of the portfolio's mandate is unknown."));
        }
        if (manager.kind() == ClientProfile.ManagerKind.BANK) {
            return Optional.of(Finding.fail("The class is reserved to mandates with " + condition.manager()
                    + ", and the portfolio's mandate is with the bank."));
        }
        if (manager.name() == null || manager.name().isBlank()) {
            return Optional.of(Finding.review("The name of the mandate's external manager is unknown."));
        }
        return Optional.of(manager.name().toLowerCase().contains(condition.manager().toLowerCase())
                ? Finding.pass("The portfolio has a discretionary mandate with " + condition.manager() + ".")
                : Finding.fail("The class is reserved to mandates with " + condition.manager()
                        + ", and the portfolio's mandate is with another manager."));
    }

    @Override
    public boolean accumulates() {
        return false;
    }
}
