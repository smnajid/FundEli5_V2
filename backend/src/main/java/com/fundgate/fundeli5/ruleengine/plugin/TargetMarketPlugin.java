package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.bankdata.BankCode;
import com.fundgate.fundeli5.clientprofiles.ClientProfile;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.util.Optional;

/**
 * One Target Market dimension of the fund against the client, on the bank's scales translated by the
 * {@link ScaleMapping} (ADR 0002). Outside the positive Target Market gives Review; a client below the
 * required knowledge and experience is in the negative Target Market and gives Fail (assumed, Samuel
 * confirms). Risk, ability to bear losses and horizon are read from the portfolio, which carries the
 * mandate's profile under a discretionary mandate; knowledge and experience from the client.
 * The Verdict binds in Luxembourg; with no source on the card Switzerland only warns.
 */
public final class TargetMarketPlugin implements RuleKindPlugin<Condition.TargetMarket> {

    @Override
    public RuleKind kind() {
        return RuleKind.TARGET_MARKET;
    }

    @Override
    public Class<Condition.TargetMarket> conditionType() {
        return Condition.TargetMarket.class;
    }

    @Override
    public String draftingLine() {
        return "target_market: one rule per dimension, from the KID: client_type (all, retail, professional), "
                + "knowledge_experience (basic, informed, advanced), loss_bearing (no_loss, limited_loss, full_loss), "
                + "risk_sri (1-7, the summary risk indicator), horizon_years (the recommended holding period).";
    }

    @Override
    public Optional<Finding> evaluate(Condition.TargetMarket condition, Facts facts, Lookups lookups) {
        ClientProfile profile = facts.profile();
        var investment = profile.portfolio() == null ? null : profile.portfolio().investmentProfile();
        return Optional.of(switch (condition.dimension()) {
            case CLIENT_TYPE -> clientType(condition.value());
            case RISK_SRI -> risk(condition.value(), investment == null ? null : investment.riskProfile(), lookups);
            case LOSS_BEARING -> loss(condition.value(),
                    investment == null ? null : investment.abilityToBearLosses(), lookups);
            case HORIZON_YEARS -> horizon(condition.value(), investment == null ? null : investment.horizonYears());
            case KNOWLEDGE_EXPERIENCE -> knowledge(condition.value(), facts, lookups);
        });
    }

    /** No rule of the kind on the card: the Target Market has no source. */
    @Override
    public Optional<Finding> whenNoRule(Facts facts, Lookups lookups) {
        return Optional.of("LOC".equals(facts.profile().legalEntity())
                ? Finding.pass("There is no Target Market source for this class: a warning only in Switzerland.")
                : Finding.review("There is no Target Market source for this class, and the Target Market binds "
                        + "outside Switzerland."));
    }

    private static Finding clientType(String value) {
        return "all".equalsIgnoreCase(value.trim())
                ? Finding.pass("The fund is suitable for all types of investors.")
                : Finding.review("The fund's intended client type (" + value + ") is not matched by the engine.");
    }

    private static Finding risk(String value, BankCode riskProfile, Lookups lookups) {
        Integer sri = integer(value);
        if (sri == null) {
            return Finding.review("The fund's risk indicator (" + value + ") is not a number from 1 to 7.");
        }
        if (riskProfile == null) {
            return Finding.review("The portfolio has no risk profile, so the fund's risk indicator " + sri
                    + " cannot be matched.");
        }
        Optional<Integer> max = lookups.scaleMapping().maxSri(riskProfile);
        if (max.isEmpty()) {
            return Finding.review("The risk profile " + riskProfile.code() + " is not on the Scale Mapping.");
        }
        String profile = "risk profile " + label(riskProfile) + " (maximum SRI " + max.get() + ")";
        return sri <= max.get()
                ? Finding.pass("The fund's risk indicator " + sri + " is within the portfolio's " + profile + ".")
                : Finding.review("The fund's risk indicator " + sri + " is above the portfolio's " + profile + ".");
    }

    private static Finding loss(String value, BankCode ability, Lookups lookups) {
        var needed = ScaleMapping.LossBearing.fromCard(value);
        if (needed.isEmpty()) {
            return Finding.review("The fund's loss profile (" + value + ") is not on the Scale Mapping.");
        }
        if (ability == null) {
            return Finding.review("The portfolio has no ability to bear losses on file.");
        }
        var has = lookups.scaleMapping().lossBearing(ability);
        if (has.isEmpty()) {
            return Finding.review("The ability to bear losses " + ability.code() + " is not on the Scale Mapping.");
        }
        String text = "the portfolio's ability to bear losses (" + label(ability) + ")";
        return has.get().compareTo(needed.get()) >= 0
                ? Finding.pass("The fund's possible loss is within " + text + ".")
                : Finding.review("The fund's possible loss (" + value.replace('_', ' ') + ") is beyond " + text + ".");
    }

    private static Finding horizon(String value, Integer horizonYears) {
        Integer holding = integer(value);
        if (holding == null) {
            return Finding.review("The fund's recommended holding period (" + value + ") is not a number of years.");
        }
        if (horizonYears == null) {
            return Finding.review("The portfolio has no investment horizon on file.");
        }
        return horizonYears >= holding
                ? Finding.pass("The portfolio's horizon of " + horizonYears + " years reaches the recommended "
                        + "holding period of " + holding + " years.")
                : Finding.review("The portfolio's horizon of " + horizonYears + " years is shorter than the "
                        + "recommended holding period of " + holding + " years.");
    }

    private static Finding knowledge(String value, Facts facts, Lookups lookups) {
        var required = ScaleMapping.Knowledge.fromCard(value);
        if (required.isEmpty()) {
            return Finding.review("The fund's required knowledge and experience (" + value
                    + ") is not on the Scale Mapping.");
        }
        BankCode productType = facts.shareClass() == null ? null : facts.shareClass().productType();
        var client = facts.profile().client();
        var entries = client == null ? null : client.knowledgeAndExperience();
        if (productType == null || entries == null) {
            return Finding.review("The client's knowledge and experience for this class's product type is unknown.");
        }
        Optional<BankCode> level = entries.stream()
                .filter(e -> e.productType() != null && e.productType().code().equals(productType.code()))
                .map(ClientProfile.KnowledgeAndExperience::level).filter(java.util.Objects::nonNull).findFirst();
        if (level.isEmpty()) {
            return Finding.review("The client has no knowledge and experience on file for " + label(productType) + ".");
        }
        var has = lookups.scaleMapping().knowledge(level.get());
        if (has.isEmpty()) {
            return Finding.review("The knowledge level " + level.get().code() + " is not on the Scale Mapping.");
        }
        String text = "the client's knowledge and experience in " + label(productType) + " (" + label(level.get()) + ")";
        return has.get().compareTo(required.get()) >= 0
                ? Finding.pass("The fund requires " + value + " knowledge and experience, and " + text + " is enough.")
                : Finding.fail("The fund requires " + value + " knowledge and experience, and " + text
                        + " is below it: the client is in the negative Target Market.");
    }

    private static Integer integer(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String label(BankCode code) {
        return code.label() == null || code.label().isBlank() ? code.code() : code.label();
    }

    @Override
    public boolean accumulates() {
        return true;
    }
}
