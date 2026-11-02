package com.fundgate.fundeli5.ruleengine.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.bankdata.BankCode;
import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.eligibility.RuleOutcome.Result;
import com.fundgate.fundeli5.eligibility.SubscriptionRequest;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.Condition.Dimension;
import com.fundgate.fundeli5.shareclasses.ShareClass;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** The demo portfolios against Target Market rules, on the bank scales translated by the Scale Mapping. */
class TargetMarketPluginTest {

    private static final Isin GCE_BP = new Isin("LU0348926287");
    private static final Isin SIF_W = new Isin("LU2607415275");
    private static final Lookups LOOKUPS = new Lookups(null, null, ScaleMapping.demo());

    private final TargetMarketPlugin plugin = new TargetMarketPlugin();

    private static Facts facts(String portfolio, Isin isin, String productType) {
        var ref = new PortfolioReference(portfolio);
        var request = new SubscriptionRequest(ref, isin, new BigDecimal("20000"), new Currency("EUR"), false);
        var shareClass = new ShareClass(isin, null, "Fund", null, "Class", new Currency("EUR"), null, null,
                productType == null ? null : new BankCode(productType, productType), null, null, Instant.EPOCH);
        return new Facts(request, RuleKindPluginsTest.PROFILES.clientProfile(ref), shareClass);
    }

    private Finding check(Dimension dimension, String value, String portfolio, String productType) {
        return plugin.evaluate(new Condition.TargetMarket(dimension, value), facts(portfolio, GCE_BP, productType),
                LOOKUPS).orElseThrow();
    }

    // ---- risk (SRI against the maximum SRI of the risk profile)

    @Test
    void aFundRiskAboveTheProfilesMaximumSriGivesReview() {
        assertThat(check(Dimension.RISK_SRI, "4", "LUX90000301", "FUNDS").result()).isEqualTo(Result.REVIEW);
    }

    @Test
    void aFundRiskWithinTheProfilesMaximumSriPasses() {
        assertThat(check(Dimension.RISK_SRI, "4", "LOC90000102", "FUNDS").result()).isEqualTo(Result.PASS);
        assertThat(check(Dimension.RISK_SRI, "5", "LOC90000102", "FUNDS").result()).isEqualTo(Result.PASS);
    }

    // ---- ability to bear losses

    @Test
    void fullLossOnTheFundNeedsAClientWhoCanBearIt() {
        assertThat(check(Dimension.LOSS_BEARING, "full_loss", "LUX90000301", "FUNDS").result()).isEqualTo(Result.REVIEW);
        assertThat(check(Dimension.LOSS_BEARING, "full_loss", "LOC90000102", "FUNDS").result()).isEqualTo(Result.PASS);
    }

    // ---- horizon

    @Test
    void aHorizonShorterThanTheRecommendedHoldingPeriodGivesReview() {
        assertThat(check(Dimension.HORIZON_YEARS, "5", "LUX90000301", "FUNDS").result()).isEqualTo(Result.REVIEW);
        assertThat(check(Dimension.HORIZON_YEARS, "5", "LOC90000102", "FUNDS").result()).isEqualTo(Result.PASS);
    }

    @Test
    void aHorizonEqualToTheHoldingPeriodPasses() {
        assertThat(check(Dimension.HORIZON_YEARS, "5", "LUX90000501", "FUNDS").result()).isEqualTo(Result.PASS);
    }

    // ---- knowledge and experience, for the Share Class's product type

    @Test
    void knowledgeAtTheRequiredLevelPasses() {
        assertThat(check(Dimension.KNOWLEDGE_EXPERIENCE, "basic", "LUX90000301", "FUNDS").result())
                .isEqualTo(Result.PASS);
    }

    @Test
    void knowledgeBelowTheRequiredLevelFailsBecauseTheClientIsInTheNegativeTargetMarket() {
        assertThat(check(Dimension.KNOWLEDGE_EXPERIENCE, "basic", "LUX90000601", "PRIVATE_EQUITY").result())
                .isEqualTo(Result.FAIL);
    }

    @Test
    void knowledgeIsReadForTheClassesProductTypeOnly() {
        // LUX90000601 is informed in FUNDS but has none in PRIVATE_EQUITY.
        assertThat(check(Dimension.KNOWLEDGE_EXPERIENCE, "basic", "LUX90000601", "FUNDS").result())
                .isEqualTo(Result.PASS);
    }

    @Test
    void noEntryForTheProductTypeGivesReview() {
        assertThat(check(Dimension.KNOWLEDGE_EXPERIENCE, "basic", "LUX90000301", "PRIVATE_EQUITY").result())
                .isEqualTo(Result.REVIEW);
        assertThat(check(Dimension.KNOWLEDGE_EXPERIENCE, "basic", "LUX90000301", null).result())
                .isEqualTo(Result.REVIEW);
    }

    // ---- client type, and values the card words badly

    @Test
    void aFundForAllClientTypesPasses() {
        assertThat(check(Dimension.CLIENT_TYPE, "all", "LUX90000301", "FUNDS").result()).isEqualTo(Result.PASS);
    }

    @Test
    void aValueTheMappingCannotReadGivesReviewNotAGuess() {
        assertThat(check(Dimension.RISK_SRI, "high", "LUX90000301", "FUNDS").result()).isEqualTo(Result.REVIEW);
        assertThat(check(Dimension.LOSS_BEARING, "lots", "LUX90000301", "FUNDS").result()).isEqualTo(Result.REVIEW);
        assertThat(check(Dimension.CLIENT_TYPE, "retail only", "LUX90000301", "FUNDS").result())
                .isEqualTo(Result.REVIEW);
    }

    // ---- no Target Market source on the card: warns in Switzerland, binds in Luxembourg

    @Test
    void withoutASourceSwitzerlandOnlyWarnsAndLuxembourgGivesReview() {
        var ch = plugin.whenNoRule(facts("LOC90000901", GCE_BP, "FUNDS"), LOOKUPS).orElseThrow();
        assertThat(ch.result()).isEqualTo(Result.PASS);
        assertThat(ch.explanation()).contains("no Target Market source");
        var lu = plugin.whenNoRule(facts("LUX90000301", GCE_BP, "FUNDS"), LOOKUPS).orElseThrow();
        assertThat(lu.result()).isEqualTo(Result.REVIEW);
        assertThat(lu.explanation()).contains("no Target Market source");
    }

    @Test
    void everyDimensionIsShown() {
        assertThat(plugin.accumulates()).isTrue();
    }
}
