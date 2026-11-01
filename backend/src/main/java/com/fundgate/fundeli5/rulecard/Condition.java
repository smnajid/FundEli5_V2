package com.fundgate.fundeli5.rulecard;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.InvestorType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** What a rule demands. One record per rule kind; the engine's plug-in for the kind reads it. */
public sealed interface Condition {

    RuleKind kind();

    /** Who may subscribe; {@code excluded} names the exclusions the card cites (e.g. an opt-out). */
    record InvestorCategory(Set<InvestorType> allowed, List<String> excluded) implements Condition {
        public InvestorCategory {
            allowed = Set.copyOf(allowed);
            excluded = List.copyOf(excluded);
        }

        @Override
        public RuleKind kind() {
            return RuleKind.INVESTOR_CATEGORY;
        }
    }

    enum UsPersonDefinition { REG_S, FATCA, IRC_CITIZENSHIP, MIXED }

    record UsPersonExclusion(UsPersonDefinition definition, boolean excludesUsCitizens) implements Condition {
        @Override
        public RuleKind kind() {
            return RuleKind.US_PERSON_EXCLUSION;
        }
    }

    record MinimumInitialSubscription(BigDecimal amount, Currency currency) implements Condition {
        @Override
        public RuleKind kind() {
            return RuleKind.MINIMUM_INITIAL_SUBSCRIPTION;
        }
    }

    /** The class is reserved to mandates; {@code manager} names the manager when the card does. */
    record MandateOnlyClass(String manager) implements Condition {
        @Override
        public RuleKind kind() {
            return RuleKind.MANDATE_ONLY_CLASS;
        }
    }

    record KidRequiredForRetail(boolean kidAvailable) implements Condition {
        @Override
        public RuleKind kind() {
            return RuleKind.KID_REQUIRED_FOR_RETAIL;
        }
    }

    enum Dimension { CLIENT_TYPE, KNOWLEDGE_EXPERIENCE, LOSS_BEARING, RISK_SRI, HORIZON_YEARS }

    record TargetMarket(Dimension dimension, String value) implements Condition {
        @Override
        public RuleKind kind() {
            return RuleKind.TARGET_MARKET;
        }
    }

    /** Blocking until the Rule Card Validator marks it Noted (ADR 0003). */
    enum RestrictionStatus { BLOCKING, NOTED }

    record UnmodelledRestriction(RestrictionStatus status, String notedReason) implements Condition {
        @Override
        public RuleKind kind() {
            return RuleKind.UNMODELLED_RESTRICTION;
        }
    }
}
