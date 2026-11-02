package com.fundgate.fundeli5.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.EditionDate;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.citation.Citation;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.clientprofiles.fixture.FixtureClientProfiles;
import com.fundgate.fundeli5.eligibility.RuleOutcome;
import com.fundgate.fundeli5.eligibility.SubscriptionRequest;
import com.fundgate.fundeli5.eligibility.Verdict;
import com.fundgate.fundeli5.fundregistrations.fixture.FixtureFundRegistrations;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.Rule;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.RuleKind;
import com.fundgate.fundeli5.ruleengine.plugin.Facts;
import com.fundgate.fundeli5.ruleengine.plugin.Finding;
import com.fundgate.fundeli5.ruleengine.plugin.KidRequiredForRetailPlugin;
import com.fundgate.fundeli5.ruleengine.plugin.Lookups;
import com.fundgate.fundeli5.ruleengine.plugin.RuleKindPlugin;
import com.fundgate.fundeli5.shareclasses.fixture.FixtureShareClasses;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The engine's frame, on synthetic cards, with a test plug-in whose result a rule's manager name dictates. */
class RuleCatalogEngineTest {

    static final Path FIXTURES = Path.of("../docs/playbook/contracts/fixtures");
    static final Isin CLASS = new Isin("LU0348926287");
    static final Isin OTHER_CLASS = new Isin("LU0348927095");
    static final Citation PAGE_5 = new Citation("prospectus", 5, "Only qualified investors", true);
    static final Citation PAGE_9 = new Citation("prospectus", 9, "Class reserved to mandates", true);

    /** Pass, fail or review as the rule's {@code manager} says. */
    static class ScriptedPlugin implements RuleKindPlugin<Condition.MandateOnlyClass> {
        private final boolean accumulates;

        ScriptedPlugin(boolean accumulates) {
            this.accumulates = accumulates;
        }

        public RuleKind kind() {
            return RuleKind.MANDATE_ONLY_CLASS;
        }

        public Class<Condition.MandateOnlyClass> conditionType() {
            return Condition.MandateOnlyClass.class;
        }

        public String draftingLine() {
            return "mandate_only_class: scripted";
        }

        public Optional<Finding> evaluate(Condition.MandateOnlyClass condition, Facts facts, Lookups lookups) {
            return switch (condition.manager()) {
                case "pass" -> Optional.of(Finding.pass("scripted pass"));
                case "fail" -> Optional.of(Finding.fail("scripted fail"));
                case "review" -> Optional.of(Finding.review("scripted review"));
                default -> Optional.empty();
            };
        }

        public boolean accumulates() {
            return accumulates;
        }
    }

    static Rule fundRule(Condition condition, Citation... citations) {
        return new Rule(Rule.Scope.FUND, null, condition, List.of(citations), "summary", 1.0, false, List.of());
    }

    static Rule classRule(Condition condition, Citation... citations) {
        return new Rule(Rule.Scope.SHARE_CLASS, CLASS, condition, List.of(citations), "summary", 1.0, false, List.of());
    }

    static Rule scripted(String result) {
        return fundRule(new Condition.MandateOnlyClass(result), PAGE_9);
    }

    static RuleCard card(RuleCard.Status status, List<Rule> rules, List<RuleCard.Source> sources) {
        return new RuleCard("f", "Fund", List.of(CLASS), 1, status, rules, sources, null,
                status == RuleCard.Status.VALIDATED ? new RuleCard.Validation("v", java.time.Instant.EPOCH) : null);
    }

    static RuleCard validated(Rule... rules) {
        return card(RuleCard.Status.VALIDATED, List.of(rules), List.of());
    }

    static SubscriptionRequest request(String portfolio, Isin isin) {
        return new SubscriptionRequest(new PortfolioReference(portfolio), isin, new BigDecimal("80000"), new Currency("EUR"),
                false);
    }

    EditionLookup editions = EditionLookup.none();

    EngineResult check(RuleCard card, boolean accumulates, String portfolio) {
        return check(card == null ? Optional.empty() : Optional.of(card), accumulates, portfolio, CLASS);
    }

    EngineResult check(Optional<RuleCard> card, boolean accumulates, String portfolio, Isin isin) {
        RuleCatalog catalog = new RuleCatalog(List.of(new ScriptedPlugin(accumulates), new KidRequiredForRetailPlugin()));
        RuleCatalogEngine engine = new RuleCatalogEngine(i -> i.equals(CLASS) ? card : Optional.empty(),
                new FixtureClientProfiles(FIXTURES), new FixtureShareClasses(FIXTURES),
                new Lookups(new FixtureFundRegistrations(FIXTURES), null), catalog, editions);
        EngineResult result = engine.check(request(portfolio, isin));
        // The Fund Registration outcome is the frame's own; the tests here are about the plug-ins' outcomes.
        return new EngineResult(result.verdict(), new com.fundgate.fundeli5.eligibility.Justification(
                result.justification().outcomes().stream().filter(o -> o.kind() != RuleKind.FUND_REGISTRATION).toList(),
                result.justification().warnings()));
    }

    EngineResult check(RuleCard card, String portfolio) {
        return check(card, false, portfolio);
    }

    // ---- Verdict combination

    @Test
    void anyFailGivesNotEligibleEvenWithAReviewAndAPass() {
        EngineResult result = check(validated(scripted("pass"), scripted("review"), scripted("fail")), true,
                "LOC90000101");
        assertThat(result.verdict()).isEqualTo(Verdict.NOT_ELIGIBLE);
        assertThat(result.justification().outcomes()).extracting(RuleOutcome::result)
                .containsExactly(RuleOutcome.Result.PASS, RuleOutcome.Result.REVIEW, RuleOutcome.Result.FAIL);
    }

    @Test
    void aReviewWithoutAFailGivesNeedsReview() {
        assertThat(check(validated(scripted("pass"), scripted("review")), true, "LOC90000101").verdict())
                .isEqualTo(Verdict.NEEDS_REVIEW);
    }

    @Test
    void onlyPassesGiveEligible() {
        assertThat(check(validated(scripted("pass")), true, "LOC90000101").verdict()).isEqualTo(Verdict.ELIGIBLE);
    }

    // ---- Citations on every outcome

    @Test
    void everyOutcomeCarriesTheCitationsOfItsRule() {
        EngineResult result = check(validated(fundRule(new Condition.MandateOnlyClass("pass"), PAGE_5, PAGE_9)), true,
                "LOC90000101");
        assertThat(result.justification().outcomes()).singleElement()
                .satisfies(o -> assertThat(o.citations()).containsExactly(PAGE_5, PAGE_9));
    }

    // ---- Draft versus Validated

    @Test
    void aDraftCardNeverDecidesEvenWhenEveryRulePasses() {
        EngineResult result = check(card(RuleCard.Status.DRAFT, List.of(scripted("pass")), List.of()), "LOC90000101");
        assertThat(result.verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
        assertThat(result.justification().outcomes()).singleElement()
                .satisfies(o -> assertThat(o.explanation()).contains("Rule Card not validated"));
    }

    @Test
    void aShareClassWithNoCardIsNotCovered() {
        EngineResult result = check(Optional.empty(), false, "LOC90000101", CLASS);
        assertThat(result.verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
        assertThat(result.justification().outcomes()).singleElement()
                .satisfies(o -> assertThat(o.explanation()).contains("Fund not covered"));
    }

    @Test
    void aRegimeOtherThanSwissOrLuxembourgGivesReviewWhateverTheCard() {
        EngineResult result = check(validated(scripted("pass")), "SIN90001301");
        assertThat(result.verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
        assertThat(result.justification().outcomes()).singleElement()
                .satisfies(o -> assertThat(o.explanation()).contains("regime not covered"));
    }

    // ---- Class-level and fund-level

    @Test
    void whenTheKindDoesNotAccumulateAClassRuleOverridesTheFundRule() {
        EngineResult result = check(validated(scripted("fail"),
                classRule(new Condition.MandateOnlyClass("pass"), PAGE_9)), false, "LOC90000101");
        assertThat(result.verdict()).isEqualTo(Verdict.ELIGIBLE);
        assertThat(result.justification().outcomes()).hasSize(1);
    }

    @Test
    void whenTheKindDoesNotAccumulateTheWorstFindingDecides() {
        EngineResult result = check(validated(scripted("pass"), scripted("review"), scripted("fail")), false,
                "LOC90000101");
        assertThat(result.justification().outcomes()).singleElement()
                .satisfies(o -> assertThat(o.result()).isEqualTo(RuleOutcome.Result.FAIL));
    }

    @Test
    void whenTheKindAccumulatesClassRulesAddToFundRules() {
        EngineResult result = check(validated(scripted("pass"),
                classRule(new Condition.MandateOnlyClass("fail"), PAGE_9)), true, "LOC90000101");
        assertThat(result.justification().outcomes()).hasSize(2);
        assertThat(result.verdict()).isEqualTo(Verdict.NOT_ELIGIBLE);
    }

    @Test
    void aRuleOfAnotherShareClassOfTheCardIsIgnored() {
        Rule elsewhere = new Rule(Rule.Scope.SHARE_CLASS, OTHER_CLASS, new Condition.MandateOnlyClass("fail"),
                List.of(PAGE_9), "s", 1.0, false, List.of());
        assertThat(check(validated(scripted("pass"), elsewhere), true, "LOC90000101").verdict())
                .isEqualTo(Verdict.ELIGIBLE);
    }

    // ---- Contradictions, Stale cards, unknown kinds

    @Test
    void aContradictionBetweenDocumentsIsFlaggedAndGivesReview() {
        Rule contradicted = new Rule(Rule.Scope.FUND, null, new Condition.MandateOnlyClass("pass"), List.of(PAGE_9),
                "Minimum differs between prospectus and KID", 0.6, true, List.of());
        EngineResult result = check(validated(contradicted), "LOC90000101");
        assertThat(result.verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
        assertThat(result.justification().warnings()).singleElement()
                .satisfies(w -> assertThat(w).contains("contradict").contains("Minimum differs"));
    }

    @Test
    void aStaleCardStillDecidesAndWarns() {
        RuleCard drafted = card(RuleCard.Status.VALIDATED, List.of(scripted("pass")), List.of(new RuleCard.Source(
                "prospectus", "abc", null, new EditionDate("2026-06"), null)));
        editions = documentId -> Optional.of(new EditionDate("2026-09"));
        EngineResult result = check(drafted, "LOC90000101");
        assertThat(result.verdict()).isEqualTo(Verdict.ELIGIBLE);
        assertThat(result.justification().warnings()).singleElement()
                .satisfies(w -> assertThat(w).contains("Stale Rule Card"));
    }

    @Test
    void aCurrentCardDoesNotWarn() {
        RuleCard drafted = card(RuleCard.Status.VALIDATED, List.of(scripted("pass")), List.of(new RuleCard.Source(
                "prospectus", "abc", null, new EditionDate("2026-09"), null)));
        editions = documentId -> Optional.of(new EditionDate("2026-09-15"));
        assertThat(check(drafted, "LOC90000101").justification().warnings()).isEmpty();
    }

    @Test
    void aRuleWithNoPlugInIsNeverSkippedSilently() {
        EngineResult result = check(validated(fundRule(new Condition.UsPersonExclusion(
                Condition.UsPersonDefinition.REG_S, true), PAGE_5)), "LOC90000101");
        assertThat(result.verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
    }

    @Test
    void aValidatedCardWithNoRuleForTheClassNeverGivesEligible() {
        assertThat(check(validated(), "LOC90000101").verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
    }

    @Test
    void aRuleWithoutACitationCannotPass() {
        assertThat(check(validated(fundRule(new Condition.MandateOnlyClass("pass"))), true, "LOC90000101").verdict())
                .isEqualTo(Verdict.NEEDS_REVIEW);
    }

    @Test
    void aClassRuleThatDoesNotApplyLeavesTheFundRuleInForce() {
        EngineResult result = check(validated(scripted("fail"),
                classRule(new Condition.MandateOnlyClass("n/a"), PAGE_9)), false, "LOC90000101");
        assertThat(result.verdict()).isEqualTo(Verdict.NOT_ELIGIBLE);
    }

    @Test
    void aValidatedCardWithoutAValidationRecordDoesNotDecide() {
        RuleCard unsigned = new RuleCard("f", "Fund", List.of(CLASS), 1, RuleCard.Status.VALIDATED,
                List.of(scripted("pass")), List.of(), null, null);
        assertThat(check(unsigned, "LOC90000101").verdict()).isEqualTo(Verdict.NEEDS_REVIEW);
    }

    // ---- The same inputs give the same answer

    @Test
    void theSameCheckTwiceGivesTheSameResult() {
        RuleCard card = validated(scripted("pass"), scripted("review"));
        assertThat(check(card, true, "LOC90000101")).isEqualTo(check(card, true, "LOC90000101"));
    }

    // ---- The catalog

    @Test
    void theCatalogRefusesTwoPlugInsForOneKind() {
        List<RuleKindPlugin<?>> twice = new ArrayList<>(List.of(new ScriptedPlugin(true), new ScriptedPlugin(false)));
        assertThatThrownBy(() -> new RuleCatalog(twice)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theCatalogGivesOneDraftingLinePerPlugInInOrder() {
        RuleCatalog catalog = new RuleCatalog(List.of(new ScriptedPlugin(true), new KidRequiredForRetailPlugin()));
        assertThat(catalog.draftingLines()).hasSize(2).first().isEqualTo("mandate_only_class: scripted");
    }

    // ---- The first plug-in

    @Test
    void kidAvailableIsAPassAndAMissingKidFailsOnlyForRetail() {
        KidRequiredForRetailPlugin kid = new KidRequiredForRetailPlugin();
        var profiles = new FixtureClientProfiles(FIXTURES);
        Facts retail = new Facts(request("LOC90000101", CLASS), profiles.clientProfile(new PortfolioReference("LOC90000101")), null);
        Facts professional = new Facts(request("LOC90000201", CLASS), profiles.clientProfile(new PortfolioReference("LOC90000201")), null);
        Lookups lookups = new Lookups(null, null);

        assertThat(kid.evaluate(new Condition.KidRequiredForRetail(true), retail, lookups).orElseThrow().result())
                .isEqualTo(RuleOutcome.Result.PASS);
        assertThat(kid.evaluate(new Condition.KidRequiredForRetail(false), retail, lookups).orElseThrow().result())
                .isEqualTo(RuleOutcome.Result.FAIL);
        assertThat(kid.evaluate(new Condition.KidRequiredForRetail(false), professional, lookups).orElseThrow().result())
                .isEqualTo(RuleOutcome.Result.PASS);
    }
}
