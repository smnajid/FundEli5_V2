package com.fundgate.fundeli5;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.answering.Answer;
import com.fundgate.fundeli5.answering.AnswerService;
import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.checkrecord.memory.InMemoryCheckRecords;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.clientprofiles.fixture.FixtureClientProfiles;
import com.fundgate.fundeli5.eligibility.RuleOutcome;
import com.fundgate.fundeli5.eligibility.SubscriptionRequest;
import com.fundgate.fundeli5.eligibility.Verdict;
import com.fundgate.fundeli5.fundregistrations.fixture.FixtureFundRegistrations;
import com.fundgate.fundeli5.fxrates.fixture.FixtureFxRates;
import com.fundgate.fundeli5.llm.LlmRequest;
import com.fundgate.fundeli5.llm.LlmResponse;
import com.fundgate.fundeli5.privacygate.DefaultPrivacyGate;
import com.fundgate.fundeli5.privacygate.fixture.FixturePersonalDataDetector;
import com.fundgate.fundeli5.replydrafter.stub.TemplateReplyDrafter;
import com.fundgate.fundeli5.requestparser.replay.ReplayRequestParser;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.RuleCards;
import com.fundgate.fundeli5.rulecard.fixture.FixtureRuleCards;
import com.fundgate.fundeli5.ruleengine.EditionLookup;
import com.fundgate.fundeli5.ruleengine.RuleCatalog;
import com.fundgate.fundeli5.ruleengine.RuleCatalogEngine;
import com.fundgate.fundeli5.ruleengine.plugin.InvestorCategoryPlugin;
import com.fundgate.fundeli5.ruleengine.plugin.KidRequiredForRetailPlugin;
import com.fundgate.fundeli5.ruleengine.plugin.Lookups;
import com.fundgate.fundeli5.ruleengine.plugin.MandateOnlyClassPlugin;
import com.fundgate.fundeli5.ruleengine.plugin.MinimumInitialSubscriptionPlugin;
import com.fundgate.fundeli5.ruleengine.plugin.TargetMarketPlugin;
import com.fundgate.fundeli5.ruleengine.plugin.UnmodelledRestrictionPlugin;
import com.fundgate.fundeli5.ruleengine.plugin.UsPersonExclusionPlugin;
import com.fundgate.fundeli5.shareclasscatalog.MatchingShareClassCatalog;
import com.fundgate.fundeli5.shareclasses.fixture.FixtureShareClasses;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The four promises, guarded in JUnit on frozen test-only fixtures (src/test/resources/fixtures), never the
 * demo content, so the team can rewrite the content without breaking the guards.
 */
class FourPromisesTest {

    static final Path FIXTURES = Path.of("src/test/resources/fixtures");
    static final Isin GCE_BP = new Isin("LU0348926287");
    static final Isin GCE_BI = new Isin("LU0348927095");
    static final Isin BNP_AAC = new Isin("CH0114292532");
    static final Pattern UUID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    static final String ANNA_ID = "9ae403db-1754-4033-9f06-a99edf92a574";

    final List<LlmRequest> sentToModel = new ArrayList<>();
    final InMemoryCheckRecords records = new InMemoryCheckRecords();
    final FixtureClientProfiles profiles = new FixtureClientProfiles(FIXTURES);
    final FixtureShareClasses shareClasses = new FixtureShareClasses(FIXTURES);
    RuleCards cards = new FixtureRuleCards(FIXTURES);
    String modelAnswer = "[]";

    RuleCatalogEngine engine() {
        RuleCatalog catalog = new RuleCatalog(List.of(new InvestorCategoryPlugin(), new UsPersonExclusionPlugin(),
                new MinimumInitialSubscriptionPlugin(), new MandateOnlyClassPlugin(), new KidRequiredForRetailPlugin(),
                new UnmodelledRestrictionPlugin(), new TargetMarketPlugin()));
        return new RuleCatalogEngine(cards, profiles, shareClasses,
                new Lookups(new FixtureFundRegistrations(FIXTURES), new FixtureFxRates(FIXTURES)), catalog,
                EditionLookup.none());
    }

    AnswerService service() {
        var egress = com.fundgate.fundeli5.llm.GatedLlmEgress.over((com.fundgate.fundeli5.llm.LlmEgress) request -> {
            sentToModel.add(request);
            return new LlmResponse(modelAnswer);
        });
        var gate = new DefaultPrivacyGate(profiles, new FixturePersonalDataDetector());
        var renderer = new com.fundgate.fundeli5.explanation.ExplanationRenderer(
                com.fundgate.fundeli5.explanation.ExplanationTemplates.load());
        return new AnswerService(gate,
                new ReplayRequestParser(egress), new MatchingShareClassCatalog(shareClasses), engine(),
                new com.fundgate.fundeli5.replydrafter.gated.GatedReplyDrafter(gate, egress, renderer,
                        new TemplateReplyDrafter(renderer)),
                new TemplateReplyDrafter(renderer),
                records, profiles, Clock.systemUTC(), "test", cards, "test", shareClasses);
    }

    SubscriptionRequest request(String portfolio, Isin isin, String amount) {
        return new SubscriptionRequest(new PortfolioReference(portfolio), isin, new BigDecimal(amount),
                new Currency("EUR"), false);
    }

    // ---- 1. The Privacy Gate fails closed

    @Test
    void anUnknownPortfolioReachesNoModel() {
        Answer answer = service().answerEmail("Can LOC90009999 subscribe EUR 5,000 to Nordea Global Climate BP-EUR?");
        assertThat(answer.refusal()).isNotNull();
        assertThat(answer.aiUsed()).isFalse();
        assertThat(sentToModel).isEmpty();
        assertThat(records.all()).isEmpty();
    }

    @Test
    void aNameInTheEmailReachesNoModel() {
        Answer answer = service().answerEmail("Can Anna Steinmann (LOC90000101) subscribe EUR 5,000?");
        assertThat(answer.refusal()).isNotNull();
        assertThat(sentToModel).isEmpty();
    }

    // ---- 2. A model sees Portfolio Ids only

    @Test
    void whatReachesTheModelNamesPortfoliosByIdOnly() {
        modelAnswer = "[{\"portfolioId\":\"" + ANNA_ID + "\",\"shareClass\":\"Nordea Global Climate BP-EUR\","
                + "\"amount\":80000,\"currency\":\"EUR\",\"clientInitiated\":false}]";
        Answer answer = service().answerEmail("Please check LOC90000101 for Nordea Global Climate BP-EUR, EUR 80,000.");
        assertThat(answer.refusal()).isNull();
        assertThat(sentToModel).isNotEmpty().allSatisfy(r -> {
            String seen = r.system() + r.prompt();
            assertThat(seen).doesNotContain("LOC90000101");
            assertThat(UUID.matcher(r.prompt()).find()).as("a Portfolio Id").isTrue();
        });
        assertThat(answer.reply()).contains("LOC90000101").doesNotContain(ANNA_ID);
    }

    // ---- 3. Every check writes one Check Record, with the Verdict shown

    @Test
    void everyEligibilityCheckWritesOneCheckRecordWithTheVerdictShown() {
        modelAnswer = "[{\"portfolioId\":\"" + ANNA_ID + "\",\"shareClass\":\"Nordea Global Climate BP-EUR\","
                + "\"amount\":80000,\"currency\":\"EUR\",\"clientInitiated\":false}]";
        Answer answer = service().answerEmail("Please check LOC90000101 for Nordea Global Climate BP-EUR, EUR 80,000.");
        assertThat(answer.checks()).isNotEmpty();
        assertThat(records.all()).hasSameSizeAs(answer.checks());
        for (int i = 0; i < answer.checks().size(); i++) {
            assertThat(records.all().get(i).verdict()).isEqualTo(answer.checks().get(i).verdict());
            assertThat(records.all().get(i).justification()).isEqualTo(answer.checks().get(i).justification());
        }
        Answer byHand = service().answerByHand(request("LOC90000101", GCE_BP, "80000"), "BP-EUR");
        assertThat(records.all()).hasSize(answer.checks().size() + 1);
        assertThat(records.all().get(records.all().size() - 1).verdict()).isEqualTo(byHand.checks().get(0).verdict());
    }

    // ---- 4. Verdict combination: any Fail, else any Review, else Eligible

    @Test
    void anyFailGivesNotEligible() {
        // Anna is retail: GCE-BI is institutional (Fail), and the registration and the minimum pass or fail beside it.
        assertThat(engine().check(request("LOC90000101", GCE_BI, "80000")).verdict()).isEqualTo(Verdict.NOT_ELIGIBLE);
    }

    @Test
    void noFailButAReviewGivesNeedsReview() {
        // Claire in Luxembourg is outside GCE-BP's positive Target Market: Review, with no Fail.
        var result = engine().check(request("LUX90000301", GCE_BP, "20000"));
        assertThat(result.justification().outcomes()).extracting(RuleOutcome::result)
                .contains(RuleOutcome.Result.REVIEW).doesNotContain(RuleOutcome.Result.FAIL);
        assertThat(result.verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
    }

    @Test
    void onlyPassesGiveEligible() {
        var result = engine().check(request("LOC90000101", GCE_BP, "80000"));
        assertThat(result.justification().outcomes()).extracting(RuleOutcome::result)
                .containsOnly(RuleOutcome.Result.PASS);
        assertThat(result.verdict()).isEqualTo(Verdict.ELIGIBLE);
    }

    // ---- 5. Only a Validated Rule Card decides

    @Test
    void aDraftRuleCardGivesNeedsReview() {
        var result = engine().check(request("LOC90001401", BNP_AAC, "50000"));
        assertThat(result.verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
    }

    @Test
    void aValidatedCardTurnedDraftNoLongerDecides() {
        RuleCards validated = cards;
        cards = isin -> validated.forShareClass(isin).map(c -> new RuleCard(c.fundId(), c.fundName(), c.shareClasses(),
                c.version(), RuleCard.Status.DRAFT, c.rules(), c.sources(), c.drafting(), null));
        assertThat(engine().check(request("LOC90000101", GCE_BP, "80000")).verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
    }

    @Test
    void noRuleCardGivesNeedsReview() {
        cards = isin -> Optional.empty();
        assertThat(engine().check(request("LOC90000101", GCE_BP, "80000")).verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
    }
}
