package com.fundgate.fundeli5.ruleengine.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.InvestorType;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.clientprofiles.fixture.FixtureClientProfiles;
import com.fundgate.fundeli5.eligibility.RuleOutcome.Result;
import com.fundgate.fundeli5.eligibility.SubscriptionRequest;
import com.fundgate.fundeli5.fxrates.fixture.FixtureFxRates;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.Condition.UsPersonDefinition;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** One example per behaviour of the first four rule kinds, on the demo portfolios. */
class RuleKindPluginsTest {

    static final Path FIXTURES = Path.of("../docs/playbook/contracts/fixtures");
    static final FixtureClientProfiles PROFILES = new FixtureClientProfiles(FIXTURES);
    static final Lookups LOOKUPS = new Lookups(null, new FixtureFxRates(FIXTURES));

    static Facts facts(String portfolio, String amount, String currency) {
        var ref = new PortfolioReference(portfolio);
        var request = new SubscriptionRequest(ref, new Isin("LU0348926287"), new BigDecimal(amount),
                new Currency(currency), false);
        return new Facts(request, PROFILES.clientProfile(ref), null);
    }

    static Result investor(Condition.InvestorCategory c, String portfolio, String amount, String currency) {
        return new InvestorCategoryPlugin().evaluate(c, facts(portfolio, amount, currency), LOOKUPS).orElseThrow().result();
    }

    static Condition.InvestorCategory allowed(List<String> excluded, InvestorType... types) {
        return new Condition.InvestorCategory(Set.of(types), excluded);
    }

    // ---- Investor category

    @Test
    void aClassOpenToAllAdmitsAnyone() {
        assertThat(investor(allowed(List.of(), InvestorType.ALL), "LOC90000101", "80000", "EUR")).isEqualTo(Result.PASS);
    }

    @Test
    void anAdvisoryRetailPortfolioIsNotInstitutional() {
        assertThat(investor(allowed(List.of(), InvestorType.INSTITUTIONAL), "LOC90000101", "80000", "EUR"))
                .isEqualTo(Result.FAIL);
    }

    @Test
    void aDiscretionaryMandateCountsAsInstitutional() {
        assertThat(investor(allowed(List.of(), InvestorType.INSTITUTIONAL), "LOC90000102", "80000", "EUR"))
                .isEqualTo(Result.PASS);
    }

    @Test
    void anHnwiOptOutIsNotInstitutionalButIsQualified() {
        assertThat(investor(allowed(List.of(), InvestorType.INSTITUTIONAL), "LOC90000201", "2000000", "CHF"))
                .isEqualTo(Result.FAIL);
        assertThat(investor(allowed(List.of(), InvestorType.QUALIFIED), "LOC90000201", "2000000", "CHF"))
                .isEqualTo(Result.PASS);
    }

    @Test
    void anExcludedHnwiOptOutFailsAQualifiedClass() {
        assertThat(investor(allowed(List.of("hnwi_opt_out", "cisa_art_10_3ter_mandate_client"), InvestorType.QUALIFIED),
                "LOC90000901", "500000", "CHF")).isEqualTo(Result.FAIL);
    }

    @Test
    void aTenThreeTerMandateClientIsQualifiedUnlessTheClassExcludesThem() {
        assertThat(investor(allowed(List.of(), InvestorType.QUALIFIED), "LOC90001001", "300000", "CHF"))
                .isEqualTo(Result.PASS);
        assertThat(investor(allowed(List.of("cisa_art_10_3ter_mandate_client"), InvestorType.QUALIFIED),
                "LOC90001001", "300000", "CHF")).isEqualTo(Result.FAIL);
    }

    @Test
    void aWrittenConfirmationMakesARetailClientWellInformedFromEur100000() {
        assertThat(investor(allowed(List.of(), InvestorType.WELL_INFORMED), "LUX90000501", "120000", "EUR"))
                .isEqualTo(Result.PASS);
        assertThat(investor(allowed(List.of(), InvestorType.WELL_INFORMED), "LUX90000501", "50000", "EUR"))
                .isEqualTo(Result.FAIL);
    }

    @Test
    void aProfessionalPerSeIsWellInformedAndARetailClientWithoutConfirmationIsNot() {
        assertThat(investor(allowed(List.of(), InvestorType.WELL_INFORMED), "LUX90000401", "50000", "EUR"))
                .isEqualTo(Result.PASS);
        assertThat(investor(allowed(List.of(), InvestorType.WELL_INFORMED), "LUX90000301", "500000", "EUR"))
                .isEqualTo(Result.FAIL);
    }

    @Test
    void anUnknownCategoryOrAnUnknownExclusionGivesReview() {
        var noClassification = new Facts(null, new com.fundgate.fundeli5.clientprofiles.ClientProfile(
                new PortfolioReference("LOC90000101"), PROFILES.clientProfile(new PortfolioReference("LOC90000101")).portfolioId(),
                "LOC", java.time.Instant.now(), null, null, null), null);
        assertThat(new InvestorCategoryPlugin().evaluate(allowed(List.of(), InvestorType.QUALIFIED), noClassification,
                LOOKUPS).orElseThrow().result()).isEqualTo(Result.REVIEW);
        assertThat(investor(allowed(List.of("something_new"), InvestorType.ALL), "LOC90000101", "1", "EUR"))
                .isEqualTo(Result.REVIEW);
    }

    // ---- US Person

    static Result usPerson(UsPersonDefinition definition, boolean citizens, String portfolio) {
        return new UsPersonExclusionPlugin().evaluate(new Condition.UsPersonExclusion(definition, citizens),
                facts(portfolio, "1000", "CHF"), LOOKUPS).orElseThrow().result();
    }

    @Test
    void regSDoesNotCountCitizenshipAlone() {
        assertThat(usPerson(UsPersonDefinition.REG_S, false, "LOC90000201")).isEqualTo(Result.PASS);
    }

    @Test
    void regSWithCitizensExcludedFailsAUsCitizen() {
        assertThat(usPerson(UsPersonDefinition.REG_S, true, "LOC90000201")).isEqualTo(Result.FAIL);
        assertThat(usPerson(UsPersonDefinition.REG_S, true, "LOC90000101")).isEqualTo(Result.PASS);
    }

    @Test
    void theIrcDefinitionFailsAnyUsCitizen() {
        assertThat(usPerson(UsPersonDefinition.IRC_CITIZENSHIP, true, "LOC90000201")).isEqualTo(Result.FAIL);
        assertThat(usPerson(UsPersonDefinition.IRC_CITIZENSHIP, true, "LOC90001001")).isEqualTo(Result.PASS);
    }

    @Test
    void theFatcaDefinitionReadsTheBanksFatcaStatus() {
        assertThat(usPerson(UsPersonDefinition.FATCA, false, "LOC90000201")).isEqualTo(Result.FAIL);
        assertThat(usPerson(UsPersonDefinition.FATCA, false, "LOC90000101")).isEqualTo(Result.PASS);
    }

    @Test
    void aMissingUsPersonStatusGivesReviewNeverFail() {
        assertThat(usPerson(UsPersonDefinition.REG_S, true, "LOC90001201")).isEqualTo(Result.REVIEW);
    }

    // ---- Minimum initial subscription

    static Result minimum(String amount, String currency, String min, String minCurrency) {
        return new MinimumInitialSubscriptionPlugin().evaluate(
                new Condition.MinimumInitialSubscription(new BigDecimal(min), new Currency(minCurrency)),
                facts("LOC90000101", amount, currency), LOOKUPS).orElseThrow().result();
    }

    @Test
    void anAmountAtOrAboveTheMinimumPasses() {
        assertThat(minimum("80000", "EUR", "75000", "EUR")).isEqualTo(Result.PASS);
        assertThat(minimum("75000", "EUR", "75000", "EUR")).isEqualTo(Result.PASS);
        assertThat(minimum("1", "EUR", "0", "EUR")).isEqualTo(Result.PASS);
    }

    @Test
    void anAmountFarBelowTheMinimumFails() {
        assertThat(minimum("50000", "EUR", "100000", "EUR")).isEqualTo(Result.FAIL);
    }

    @Test
    void anAmountBelowByAtMostFivePercentGivesReview() {
        assertThat(minimum("71250", "EUR", "75000", "EUR")).isEqualTo(Result.REVIEW);
        assertThat(minimum("71249", "EUR", "75000", "EUR")).isEqualTo(Result.FAIL);
    }

    @Test
    void theAmountIsConvertedAtTheBanksFxTable() {
        // CHF 70,000 at 0.9350 CHF per EUR is EUR 74,866.84: below EUR 75,000 by 0.18%.
        assertThat(minimum("70000", "CHF", "75000", "EUR")).isEqualTo(Result.REVIEW);
        assertThat(minimum("80000", "CHF", "75000", "EUR")).isEqualTo(Result.PASS);
        assertThat(minimum("100000", "EUR", "93500", "CHF")).isEqualTo(Result.PASS);
    }

    @Test
    void aMissingRateGivesReview() {
        assertThat(minimum("500000", "USD", "75000", "EUR")).isEqualTo(Result.REVIEW);
    }

    // ---- Mandate-only class

    static Result mandateOnly(String manager, String portfolio) {
        return new MandateOnlyClassPlugin().evaluate(new Condition.MandateOnlyClass(manager),
                facts(portfolio, "1000", "CHF"), LOOKUPS).orElseThrow().result();
    }

    @Test
    void anAdvisoryPortfolioFailsAMandateOnlyClass() {
        assertThat(mandateOnly("Swiss Life", "LOC90000901")).isEqualTo(Result.FAIL);
    }

    @Test
    void aMandateWithTheBankIsNotAMandateWithTheNamedManager() {
        assertThat(mandateOnly("Swiss Life", "LOC90001001")).isEqualTo(Result.FAIL);
    }

    @Test
    void anyDiscretionaryMandatePassesWhenTheCardNamesNoManager() {
        assertThat(mandateOnly(null, "LOC90001001")).isEqualTo(Result.PASS);
    }

    @Test
    void aBlankManagerOnTheCardMeansAnyManager() {
        assertThat(mandateOnly(" ", "LOC90001001")).isEqualTo(Result.PASS);
    }

    @Test
    void anUnknownGreenCardFlagGivesReviewNeverPass() {
        var base = facts("LOC90000101", "1000", "CHF");
        var client = base.profile().client();
        var unknownFlag = new com.fundgate.fundeli5.clientprofiles.ClientProfile(base.profile().portfolioRef(),
                base.profile().portfolioId(), "LOC", base.profile().retrievedAt(),
                new com.fundgate.fundeli5.clientprofiles.ClientProfile.Client(client.residenceCountry(),
                        client.nationalities(), client.taxResidences(), client.birthCountry(),
                        new com.fundgate.fundeli5.clientprofiles.ClientProfile.UsPerson(
                                com.fundgate.fundeli5.clientprofiles.ClientProfile.FatcaStatus.NON_US_PERSON, null),
                        client.knowledgeAndExperience()),
                base.profile().classification(), base.profile().portfolio());
        var result = new UsPersonExclusionPlugin().evaluate(
                new Condition.UsPersonExclusion(UsPersonDefinition.REG_S, false),
                new Facts(base.request(), unknownFlag, null), LOOKUPS).orElseThrow();
        assertThat(result.result()).isEqualTo(Result.REVIEW);
    }

    @Test
    void aRuleThatAdmitsNoCategoryGivesReview() {
        assertThat(investor(new Condition.InvestorCategory(Set.of(), List.of()), "LOC90000101", "1", "EUR"))
                .isEqualTo(Result.REVIEW);
    }

    // ---- Each plug-in's contract with the catalog

    @Test
    void eachPlugInHasADraftingLineAndTheWorstFindingDecides() {
        for (RuleKindPlugin<?> plugin : List.of(new InvestorCategoryPlugin(), new UsPersonExclusionPlugin(),
                new MinimumInitialSubscriptionPlugin(), new MandateOnlyClassPlugin())) {
            assertThat(plugin.draftingLine()).startsWith(plugin.kind().name().toLowerCase() + ":");
            assertThat(plugin.accumulates()).isFalse();
        }
    }
}
