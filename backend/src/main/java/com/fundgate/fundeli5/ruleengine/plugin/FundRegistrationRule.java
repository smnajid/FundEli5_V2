package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.bankdata.InvestorType;
import com.fundgate.fundeli5.eligibility.RuleOutcome;
import com.fundgate.fundeli5.fundregistrations.Registration;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.util.List;

/**
 * The bank's Fund Registration decides where a class may be offered (fund-registrations contract).
 * Not a rule on the card, so not a plug-in: the engine's frame asks it for one outcome per check.
 * NotRegistered gives Fail, or Review when the client initiated the order; Unknown gives Review.
 */
public final class FundRegistrationRule {

    private final InvestorCategoryPlugin investors = new InvestorCategoryPlugin();

    public RuleOutcome evaluate(Facts facts, Lookups lookups) {
        var client = facts.profile().client();
        if (client == null || client.residenceCountry() == null) {
            return outcome(RuleOutcome.Result.REVIEW, "The client's country of residence is missing, so the "
                    + "Fund Registration cannot be read.");
        }
        var country = client.residenceCountry();
        Registration registration = lookups.registrations().registration(facts.request().shareClass(), country);
        boolean initiated = facts.request().clientInitiated();
        return switch (registration) {
            case Registration.Unknown u -> outcome(RuleOutcome.Result.REVIEW,
                    "The bank holds no Fund Registration list for this class: the dealer decides.");
            case Registration.NotRegistered n -> initiated
                    ? outcome(RuleOutcome.Result.REVIEW, "The class is not registered in " + country.code()
                            + ", but the client initiated the order: reverse solicitation, the dealer records the evidence.")
                    : outcome(RuleOutcome.Result.FAIL, "The class is not registered in " + country.code()
                            + ", according to the bank's Fund Registration.");
            case Registration.Registered r -> registered(r, country.code(), facts, lookups, initiated);
        };
    }

    private RuleOutcome registered(Registration.Registered r, String country, Facts facts, Lookups lookups,
            boolean initiated) {
        if (r.investorTypes().contains(InvestorType.ALL)) {
            return outcome(RuleOutcome.Result.PASS, "The class is registered in " + country + " for all investors.");
        }
        boolean[] unknown = {false};
        for (InvestorType type : r.investorTypes().stream().sorted().toList()) {
            if (investors.clientIs(type, facts, lookups, unknown)) {
                return outcome(RuleOutcome.Result.PASS, "The class is registered in " + country
                        + " for the client's investor type.");
            }
        }
        if (unknown[0]) {
            return outcome(RuleOutcome.Result.REVIEW, "A fact needed to tell whether the client is covered by the "
                    + "registration in " + country + " is missing.");
        }
        return initiated
                ? outcome(RuleOutcome.Result.REVIEW, "The registration in " + country + " does not cover the client's "
                        + "investor type, but the client initiated the order: reverse solicitation, the dealer records the evidence.")
                : outcome(RuleOutcome.Result.FAIL, "The registration in " + country
                        + " does not cover the client's investor type.");
    }

    private static RuleOutcome outcome(RuleOutcome.Result result, String explanation) {
        return new RuleOutcome(RuleKind.FUND_REGISTRATION, result, explanation, List.of());
    }
}
