package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * The amount is compared with the class's minimum in the minimum's currency, converted at the bank's FX table.
 * Below the minimum by at most 5% gives Review (assumed tolerance), further below gives Fail.
 */
public final class MinimumInitialSubscriptionPlugin implements RuleKindPlugin<Condition.MinimumInitialSubscription> {

    static final BigDecimal TOLERANCE = new BigDecimal("0.05");

    @Override
    public RuleKind kind() {
        return RuleKind.MINIMUM_INITIAL_SUBSCRIPTION;
    }

    @Override
    public Class<Condition.MinimumInitialSubscription> conditionType() {
        return Condition.MinimumInitialSubscription.class;
    }

    @Override
    public String draftingLine() {
        return "minimum_initial_subscription: amount and currency of the minimum initial investment; "
                + "amount 0 when the class has none.";
    }

    @Override
    public Optional<Finding> evaluate(Condition.MinimumInitialSubscription condition, Facts facts, Lookups lookups) {
        var minimum = condition.currency().code() + " " + condition.amount().toPlainString();
        var request = facts.request();
        if (condition.amount().signum() == 0) {
            return Optional.of(Finding.pass("The class has no minimum initial subscription."));
        }
        Optional<BigDecimal> converted = Money.convert(request.amount(), request.currency(), condition.currency(),
                lookups.fxRates());
        if (converted.isEmpty()) {
            return Optional.of(Finding.review("The bank's FX table has no rate between " + request.currency()
                    + " and " + condition.currency() + ", so the amount cannot be compared with the minimum of "
                    + minimum + "."));
        }
        BigDecimal amount = converted.get();
        String shown = condition.currency().code() + " " + amount.setScale(0, java.math.RoundingMode.HALF_EVEN).toPlainString();
        if (amount.compareTo(condition.amount()) >= 0) {
            return Optional.of(Finding.pass("The amount, " + shown + ", reaches the minimum of " + minimum + "."));
        }
        BigDecimal shortfall = condition.amount().subtract(amount).divide(condition.amount(), 10,
                java.math.RoundingMode.HALF_EVEN);
        return Optional.of(shortfall.compareTo(TOLERANCE) <= 0
                ? Finding.review("The amount, " + shown + ", is below the minimum of " + minimum
                        + " by at most 5%: the dealer decides.")
                : Finding.fail("The amount, " + shown + ", is below the minimum of " + minimum + "."));
    }

    @Override
    public boolean accumulates() {
        return false;
    }
}
