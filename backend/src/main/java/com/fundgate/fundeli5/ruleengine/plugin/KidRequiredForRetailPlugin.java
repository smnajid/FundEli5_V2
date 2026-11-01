package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.bankdata.InvestorCategory;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.util.Optional;

/** A retail client may only buy a class that has a PRIIPs KID. */
public final class KidRequiredForRetailPlugin implements RuleKindPlugin<Condition.KidRequiredForRetail> {

    @Override
    public RuleKind kind() {
        return RuleKind.KID_REQUIRED_FOR_RETAIL;
    }

    @Override
    public Class<Condition.KidRequiredForRetail> conditionType() {
        return Condition.KidRequiredForRetail.class;
    }

    @Override
    public String draftingLine() {
        return "kid_required_for_retail: kidAvailable is true when a PRIIPs KID is published for the class.";
    }

    @Override
    public Optional<Finding> evaluate(Condition.KidRequiredForRetail condition, Facts facts, Lookups lookups) {
        if (condition.kidAvailable()) {
            return Optional.of(Finding.pass("A KID is published for the class."));
        }
        var classification = facts.profile().classification();
        InvestorCategory category = classification == null ? null : classification.category();
        if (category == null) {
            return Optional.of(Finding.review("No KID is published and the client's category is unknown."));
        }
        return Optional.of(category == InvestorCategory.RETAIL
                ? Finding.fail("No KID is published for the class, and a retail client may not buy without one.")
                : Finding.pass("No KID is published, and the client is not retail."));
    }

    @Override
    public boolean accumulates() {
        return false;
    }
}
