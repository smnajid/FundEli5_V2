package com.fundgate.fundeli5.privacygate;

import com.fundgate.fundeli5.clientprofiles.ClientProfile;
import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The per-call allow-list (ADR 0004): of a portfolio's Client Profile, only the facts a call asks for
 * by {@link Fact}, rendered as short text, and none of them identifying. Minimum per call: the
 * caller names what it needs.
 */
public class FactAllowList {

    private final ClientProfiles clientProfiles;

    public FactAllowList(ClientProfiles clientProfiles) {
        this.clientProfiles = clientProfiles;
    }

    public AllowedFacts facts(PortfolioReference reference, Set<Fact> needed) {
        ClientProfile profile = clientProfiles.clientProfile(reference);
        Map<Fact, String> values = new EnumMap<>(Fact.class);
        for (Fact fact : needed) {
            String value = value(profile, fact);
            if (value != null) {
                values.put(fact, value);
            }
        }
        return new AllowedFacts(profile.portfolioId(), values);
    }

    private static String value(ClientProfile p, Fact fact) {
        ClientProfile.Client client = p.client();
        ClientProfile.PortfolioFacts portfolio = p.portfolio();
        ClientProfile.InvestmentProfile investment = portfolio == null ? null : portfolio.investmentProfile();
        return switch (fact) {
            case LEGAL_ENTITY -> p.legalEntity();
            case INVESTOR_CLASSIFICATION -> p.classification() == null || p.classification().category() == null
                    ? null : p.classification().category().toString();
            case MANDATE_TYPE -> portfolio == null || portfolio.mandate() == null || portfolio.mandate().type() == null
                    ? null : portfolio.mandate().type().toString();
            case RESIDENCE_COUNTRY -> client == null || client.residenceCountry() == null
                    ? null : client.residenceCountry().code();
            case NATIONALITIES -> client == null ? null : codes(client.nationalities());
            case TAX_RESIDENCES -> client == null ? null : codes(client.taxResidences());
            case US_BORN -> client == null || client.birthCountry() == null
                    ? null : client.birthCountry().code().equals("US") ? "yes" : "no";
            case FATCA_STATUS -> client == null || client.usPerson() == null || client.usPerson().fatcaStatus() == null
                    ? null : client.usPerson().fatcaStatus().toString();
            case US_PERMANENT_RESIDENT -> client == null || client.usPerson() == null
                    || client.usPerson().usPermanentResident() == null
                    ? null : client.usPerson().usPermanentResident().toString();
            case KNOWLEDGE_AND_EXPERIENCE -> client == null || client.knowledgeAndExperience() == null ? null
                    : client.knowledgeAndExperience().stream()
                            .map(k -> k.productType().code() + ":" + k.level().code())
                            .collect(Collectors.joining(","));
            case RISK_PROFILE -> investment == null || investment.riskProfile() == null
                    ? null : investment.riskProfile().code();
            case ABILITY_TO_BEAR_LOSSES -> investment == null || investment.abilityToBearLosses() == null
                    ? null : investment.abilityToBearLosses().code();
            case HORIZON_YEARS -> investment == null || investment.horizonYears() == null
                    ? null : investment.horizonYears().toString();
        };
    }

    private static String codes(List<com.fundgate.fundeli5.bankdata.Country> countries) {
        return countries == null ? null
                : countries.stream().map(com.fundgate.fundeli5.bankdata.Country::code).collect(Collectors.joining(","));
    }
}
