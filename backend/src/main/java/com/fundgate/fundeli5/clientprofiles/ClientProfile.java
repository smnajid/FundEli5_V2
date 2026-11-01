package com.fundgate.fundeli5.clientprofiles;

import com.fundgate.fundeli5.bankdata.BankCode;
import com.fundgate.fundeli5.bankdata.Country;
import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.InvestorCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything an Eligibility Check reads about one portfolio. Every field except the first four may
 * be {@code null}: absent means unknown, and the adapter never fills a default. For lists,
 * {@code null} is unknown and an empty list is "none on file".
 */
public record ClientProfile(
        PortfolioReference portfolioRef,
        PortfolioId portfolioId,
        String legalEntity,
        Instant retrievedAt,
        Client client,
        Classification classification,
        PortfolioFacts portfolio) {

    public ClientProfile {
        java.util.Objects.requireNonNull(portfolioRef);
        java.util.Objects.requireNonNull(portfolioId);
        java.util.Objects.requireNonNull(legalEntity);
        java.util.Objects.requireNonNull(retrievedAt);
    }

    /** Facts about the person. */
    public record Client(
            Country residenceCountry,
            List<Country> nationalities,
            List<Country> taxResidences,
            Country birthCountry,
            UsPerson usPerson,
            List<KnowledgeAndExperience> knowledgeAndExperience) {

        public Client {
            nationalities = nationalities == null ? null : List.copyOf(nationalities);
            taxResidences = taxResidences == null ? null : List.copyOf(taxResidences);
            knowledgeAndExperience = knowledgeAndExperience == null ? null : List.copyOf(knowledgeAndExperience);
        }
    }

    public enum FatcaStatus { US_PERSON, NON_US_PERSON }

    /** The bank's own FATCA facts; shown next to the card's definition, never replacing it. */
    public record UsPerson(FatcaStatus fatcaStatus, Boolean usPermanentResident) {
    }

    public record KnowledgeAndExperience(BankCode productType, BankCode level) {
    }

    public enum Regime { FINSA, MIFID }

    /** The Investor Classification given by the portfolio's own Legal Entity. */
    public record Classification(
            Regime regime, InvestorCategory category, LocalDate classifiedOn, List<Declaration> declarations) {

        public Classification {
            declarations = declarations == null ? null : List.copyOf(declarations);
        }
    }

    public enum DeclarationType {
        FINSA_HNWI_OPT_OUT, FINSA_OPT_IN_RETAIL, MIFID_PROFESSIONAL_ON_REQUEST, LU_WELL_INFORMED_CONFIRMATION, OTHER
    }

    public record Declaration(DeclarationType type, LocalDate date, String label) {
    }

    /** Facts about the dossier. */
    public record PortfolioFacts(Currency referenceCurrency, Mandate mandate, InvestmentProfile investmentProfile) {
    }

    public enum MandateType { ADVISORY, DISCRETIONARY, EXECUTION_ONLY }

    public record Mandate(MandateType type, LocalDate startDate, Manager manager) {
    }

    public enum ManagerKind { BANK, EXTERNAL_SUPERVISED, EXTERNAL_OTHER }

    public record Manager(ManagerKind kind, String name) {
    }

    public record InvestmentProfile(
            BankCode riskProfile, BankCode abilityToBearLosses, Integer horizonYears, LocalDate assessedOn) {
    }
}
