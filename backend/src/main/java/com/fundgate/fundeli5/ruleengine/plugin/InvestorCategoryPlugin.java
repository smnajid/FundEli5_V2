package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.InvestorCategory;
import com.fundgate.fundeli5.bankdata.InvestorType;
import com.fundgate.fundeli5.clientprofiles.ClientProfile;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Who may subscribe. The client's Investor Classification, mandate and declarations are read against each
 * category the card admits; qualified (CH) and well-informed (LU) are derived here, never stored.
 */
public final class InvestorCategoryPlugin implements RuleKindPlugin<Condition.InvestorCategory> {

    static final String HNWI_OPT_OUT = "hnwi_opt_out";
    static final String CISA_10_3TER_MANDATE_CLIENT = "cisa_art_10_3ter_mandate_client";
    /** A retail client with a written well-informed confirmation needs at least this much (assumed reading, row 13). */
    static final BigDecimal WELL_INFORMED_MINIMUM_EUR = new BigDecimal("100000");

    private enum Answer { YES, NO, UNKNOWN }

    @Override
    public RuleKind kind() {
        return RuleKind.INVESTOR_CATEGORY;
    }

    @Override
    public Class<Condition.InvestorCategory> conditionType() {
        return Condition.InvestorCategory.class;
    }

    @Override
    public String draftingLine() {
        return "investor_category: allowed lists who may subscribe (all, retail, professional, institutional, "
                + "qualified, well_informed); excluded names the categories the text carves out "
                + "(hnwi_opt_out, cisa_art_10_3ter_mandate_client).";
    }

    @Override
    public Optional<Finding> evaluate(Condition.InvestorCategory condition, Facts facts, Lookups lookups) {
        ClientProfile profile = facts.profile();
        InvestorCategory category = profile.classification() == null ? null : profile.classification().category();
        if (condition.allowed().contains(InvestorType.ALL) && condition.excluded().isEmpty()) {
            return Optional.of(Finding.pass("The class is open to all investors."));
        }
        if (category == null) {
            return Optional.of(Finding.review("The client's investor classification is unknown."));
        }
        Optional<Finding> excluded = excluded(condition.excluded(), profile, category);
        if (excluded.isPresent()) {
            return excluded;
        }
        if (condition.allowed().isEmpty()) {
            return Optional.of(Finding.review("The rule names no investor category the class admits."));
        }
        List<InvestorType> unknown = new ArrayList<>();
        // In enum order, so the same inputs always give the same sentence.
        for (InvestorType type : condition.allowed().stream().sorted().toList()) {
            switch (admits(type, profile, category, facts, lookups)) {
                case YES -> {
                    return Optional.of(Finding.pass("The client counts as " + describe(type) + ", which the class admits."));
                }
                case UNKNOWN -> unknown.add(type);
                case NO -> { }
            }
        }
        if (!unknown.isEmpty()) {
            return Optional.of(Finding.review("A fact needed to tell whether the client is " + describe(unknown.get(0))
                    + " is missing."));
        }
        return Optional.of(Finding.fail("The class admits " + names(condition.allowed()) + " only, and the client is "
                + category.name().toLowerCase() + "."));
    }

    private Optional<Finding> excluded(List<String> exclusions, ClientProfile profile, InvestorCategory category) {
        for (String exclusion : exclusions) {
            Answer answer = switch (exclusion) {
                case HNWI_OPT_OUT -> hasDeclaration(profile, ClientProfile.DeclarationType.FINSA_HNWI_OPT_OUT);
                case CISA_10_3TER_MANDATE_CLIENT -> isTenThreeTerMandateClient(profile, category);
                default -> Answer.UNKNOWN;
            };
            switch (answer) {
                case YES -> {
                    return Optional.of(Finding.fail("The class excludes " + describeExclusion(exclusion)
                            + ", and the client is one."));
                }
                case UNKNOWN -> {
                    return Optional.of(Finding.review("Whether the client is excluded as " + describeExclusion(exclusion)
                            + " cannot be told from the facts on file."));
                }
                case NO -> { }
            }
        }
        return Optional.empty();
    }

    private Answer admits(InvestorType type, ClientProfile profile, InvestorCategory category, Facts facts,
            Lookups lookups) {
        boolean professional = category != InvestorCategory.RETAIL;
        return switch (type) {
            case ALL, RETAIL -> Answer.YES;
            case PROFESSIONAL -> yesIf(professional);
            case INSTITUTIONAL -> category == InvestorCategory.INSTITUTIONAL ? Answer.YES : discretionary(profile);
            case QUALIFIED -> professional ? Answer.YES : isTenThreeTerMandateClient(profile, category);
            case WELL_INFORMED -> professional ? Answer.YES : wellInformedByConfirmation(profile, facts, lookups);
        };
    }

    /** A discretionary mandate counts as institutional (2010 Law art. 174). */
    private static Answer discretionary(ClientProfile profile) {
        var mandate = profile.portfolio() == null ? null : profile.portfolio().mandate();
        if (mandate == null || mandate.type() == null) {
            return Answer.UNKNOWN;
        }
        return yesIf(mandate.type() == ClientProfile.MandateType.DISCRETIONARY);
    }

    /** Retail, qualified only through a discretionary mandate with a supervised manager (the bank or an external one). */
    private static Answer isTenThreeTerMandateClient(ClientProfile profile, InvestorCategory category) {
        if (category != InvestorCategory.RETAIL) {
            return Answer.NO;
        }
        var mandate = profile.portfolio() == null ? null : profile.portfolio().mandate();
        if (mandate == null || mandate.type() == null) {
            return Answer.UNKNOWN;
        }
        if (mandate.type() != ClientProfile.MandateType.DISCRETIONARY) {
            return Answer.NO;
        }
        if (mandate.manager() == null || mandate.manager().kind() == null) {
            return Answer.UNKNOWN;
        }
        return yesIf(mandate.manager().kind() != ClientProfile.ManagerKind.EXTERNAL_OTHER);
    }

    /** Whether the client counts as the given investor type; the Fund Registration rule asks the same question. */
    boolean clientIs(InvestorType type, Facts facts, Lookups lookups, boolean[] unknown) {
        ClientProfile profile = facts.profile();
        InvestorCategory category = profile.classification() == null ? null : profile.classification().category();
        if (category == null) {
            unknown[0] = true;
            return false;
        }
        Answer answer = admits(type, profile, category, facts, lookups);
        unknown[0] |= answer == Answer.UNKNOWN;
        return answer == Answer.YES;
    }

    private Answer wellInformedByConfirmation(ClientProfile profile, Facts facts, Lookups lookups) {
        Answer confirmed = hasDeclaration(profile, ClientProfile.DeclarationType.LU_WELL_INFORMED_CONFIRMATION);
        if (confirmed != Answer.YES) {
            return confirmed;
        }
        if (facts.request().amount() == null) {
            return Answer.UNKNOWN;
        }
        BigDecimal inEur = Money.convert(facts.request().amount(), facts.request().currency(), new Currency("EUR"),
                lookups.fxRates()).orElse(null);
        if (inEur == null) {
            return Answer.UNKNOWN;
        }
        return yesIf(inEur.compareTo(WELL_INFORMED_MINIMUM_EUR) >= 0);
    }

    private static Answer hasDeclaration(ClientProfile profile, ClientProfile.DeclarationType type) {
        var declarations = profile.classification().declarations();
        if (declarations == null) {
            return Answer.UNKNOWN;
        }
        return yesIf(declarations.stream().anyMatch(d -> d.type() == type));
    }

    private static Answer yesIf(boolean condition) {
        return condition ? Answer.YES : Answer.NO;
    }

    private static String describe(InvestorType type) {
        return switch (type) {
            case ALL -> "any investor";
            case RETAIL -> "a retail investor";
            case PROFESSIONAL -> "a professional investor";
            case INSTITUTIONAL -> "an institutional investor";
            case QUALIFIED -> "a qualified investor";
            case WELL_INFORMED -> "a well-informed investor";
        };
    }

    private static String describeExclusion(String exclusion) {
        return switch (exclusion) {
            case HNWI_OPT_OUT -> "high-net-worth opt-out clients";
            case CISA_10_3TER_MANDATE_CLIENT -> "CISA art. 10(3ter) mandate clients";
            default -> exclusion;
        };
    }

    private static String names(java.util.Set<InvestorType> types) {
        return types.stream().map(t -> t.name().toLowerCase().replace('_', '-')).sorted()
                .collect(java.util.stream.Collectors.joining(", "));
    }

    @Override
    public boolean accumulates() {
        return false;
    }
}
