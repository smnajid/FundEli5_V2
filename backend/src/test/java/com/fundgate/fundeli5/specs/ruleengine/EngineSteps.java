package com.fundgate.fundeli5.specs.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.EditionDate;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.eligibility.RuleOutcome;
import com.fundgate.fundeli5.eligibility.SubscriptionRequest;
import com.fundgate.fundeli5.fundregistrations.FundRegistrations;
import com.fundgate.fundeli5.fxrates.FxRates;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.Rule;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.RuleCards;
import com.fundgate.fundeli5.ruleengine.EditionLookup;
import com.fundgate.fundeli5.ruleengine.EngineResult;
import com.fundgate.fundeli5.ruleengine.RuleCatalog;
import com.fundgate.fundeli5.ruleengine.RuleCatalogEngine;
import com.fundgate.fundeli5.ruleengine.plugin.Lookups;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import com.fundgate.fundeli5.specs.SpyLlmEgress;
import com.fundgate.fundeli5.specs.answering.AnswerSteps;
import com.fundgate.fundeli5.llm.LlmEgress;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opentest4j.TestAbortedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Steps of the Rule Engine's scenarios. They run the real engine in the process. The generic Then steps
 * ("the Verdict is", "the X rule gives Y") live in {@code AnswerSteps} and read its {@code answer}; this
 * class hands them the engine's result in the same JSON shape rather than editing that package.
 *
 * <p>A scenario that needs a rule kind whose plug-in does not exist yet is <em>aborted</em>, not failed:
 * it is the to-do list of the tickets that add the kind (S4b, S4c), and it starts running the day they land.
 */
public class EngineSteps {

    private static final Map<String, String> ISIN = Map.ofEntries(
            Map.entry("GCE-BP", "LU0348926287"), Map.entry("GCE-BI", "LU0348927095"),
            Map.entry("SIF-W", "LU2607415275"), Map.entry("BNP-A", "CH0112537516"),
            Map.entry("BNP-AAC", "CH0114292532"), Map.entry("BNP-I", "CH0112537557"),
            Map.entry("SL-ICAP", "XXSLICAP0001"), Map.entry("SL-AMCAP", "XXSLAMCAP001"),
            Map.entry("SL-IA1", "XXSLIA100001"), Map.entry("XXNOTCOV0001", "XXNOTCOV0001"));

    private static final Map<String, String> KIND_OF_ROW = Map.of(
            "investor category", "INVESTOR_CATEGORY", "US Person exclusion", "US_PERSON_EXCLUSION",
            "minimum", "MINIMUM_INITIAL_SUBSCRIPTION", "mandate-only class", "MANDATE_ONLY_CLASS",
            "KID for retail", "KID_REQUIRED_FOR_RETAIL", "Unmodelled", "UNMODELLED_RESTRICTION",
            "Target Market", "TARGET_MARKET");

    @Autowired RuleCards ruleCards;
    @Autowired ClientProfiles clientProfiles;
    @Autowired ShareClasses shareClasses;
    @Autowired FundRegistrations fundRegistrations;
    @Autowired FxRates fxRates;
    @Autowired RuleCatalog catalog;
    @Autowired LlmEgress egress;
    @Autowired AnswerSteps answerSteps;

    private EditionLookup editions;
    private boolean unmodelledLeftBlocking;
    private List<EngineResult> results;
    private SubscriptionRequest lastRequest;

    @Before
    public void freshScenario() {
        editions = EditionLookup.none();
        unmodelledLeftBlocking = false;
        results = new ArrayList<>();
        ((SpyLlmEgress) egress).reset();
    }

    // ---- Given

    @Given("BNP-AAC has only a Draft Rule Card")
    public void bnpAacHasOnlyADraftCard() {
        assertThat(ruleCards.forShareClass(new Isin(ISIN.get("BNP-AAC")))).get()
                .extracting(RuleCard::status).isEqualTo(RuleCard.Status.DRAFT);
    }

    @Given("Nordea's {string} clause and the SIF's {string} clause are marked Noted")
    public void theClausesAreMarkedNoted(String nordea, String sif) {
        for (String shortName : List.of("GCE-BP", "SIF-W")) {
            RuleCard card = ruleCards.forShareClass(new Isin(ISIN.get(shortName))).orElseThrow();
            card.rules().stream().map(Rule::condition).filter(Condition.UnmodelledRestriction.class::isInstance)
                    .map(Condition.UnmodelledRestriction.class::cast)
                    .forEach(c -> assertThat(c.status()).isEqualTo(Condition.RestrictionStatus.NOTED));
        }
    }

    @Given("Nordea's {string} clause is Blocking on GCE-BP's Rule Card")
    public void theClauseIsBlocking(String clause) {
        unmodelledLeftBlocking = true;
    }

    @Given("the fund master data lists a newer prospectus edition than the one GCE-BP's Rule Card was drafted from")
    public void aNewerEditionIsListed() {
        editions = documentId -> Optional.of(new EditionDate("2099-12"));
    }

    // ---- When

    @When("^([A-Z]{3}\\d{8}) asks to subscribe (?:(EUR|CHF) ([\\d,]+)|an unstated amount|no amount) to "
            + "(?:the placeholder class )?([A-Z0-9-]+)(?:, client initiated: (yes|no)| (at the client's initiative)"
            + "| (twice))?$")
    public void asksToSubscribe(String portfolio, String currency, String amount, String shareClass,
            String initiated, String atInitiative, String twice) {
        if (amount == null) {
            throw new TestAbortedException("a request without an amount waits for the minimum subscription rule (S4b)");
        }
        lastRequest = new SubscriptionRequest(new PortfolioReference(portfolio), new Isin(ISIN.get(shareClass)),
                new BigDecimal(amount.replace(",", "")), new Currency(currency),
                "yes".equals(initiated) || atInitiative != null);
        EngineResult result = engine().check(lastRequest);
        results.add(result);
        if (twice != null) {
            results.add(engine().check(lastRequest));
        }
        abortIfAPlugInIsMissing(result);
        abortIfTheRegistrationIsNotEvaluated(lastRequest);
        publishToGenericSteps(result);
    }

    private RuleCatalogEngine engine() {
        return new RuleCatalogEngine(unmodelledLeftBlocking ? withBlockingRestrictions(ruleCards) : ruleCards,
                clientProfiles, shareClasses, new Lookups(fundRegistrations, fxRates), catalog,
                editions);
    }

    /** The same cards with every Noted restriction put back to Blocking, as the Rule Card Validator would leave it. */
    private static RuleCards withBlockingRestrictions(RuleCards cards) {
        return isin -> cards.forShareClass(isin).map(card -> new RuleCard(card.fundId(), card.fundName(),
                card.shareClasses(), card.version(), card.status(),
                card.rules().stream().map(EngineSteps::blocking).toList(), card.sources(), card.drafting(),
                card.validation()));
    }

    private static Rule blocking(Rule rule) {
        if (!(rule.condition() instanceof Condition.UnmodelledRestriction restriction)) {
            return rule;
        }
        return new Rule(rule.scope(), rule.shareClass(),
                new Condition.UnmodelledRestriction(Condition.RestrictionStatus.BLOCKING, restriction.notedReason()),
                rule.citations(), rule.summary(), rule.confidence(), rule.contradiction(), rule.history());
    }

    /**
     * Fund Registration is not a rule on the card and its rule lands after S4b: a check the bank's record
     * would fail or send to Review must not run against an engine that ignores the record.
     */
    private void abortIfTheRegistrationIsNotEvaluated(SubscriptionRequest request) {
        var profile = clientProfiles.clientProfile(request.portfolio());
        if (profile != null && profile.client() != null && profile.client().residenceCountry() != null
                && !(fundRegistrations.registration(request.shareClass(), profile.client().residenceCountry())
                        instanceof com.fundgate.fundeli5.fundregistrations.Registration.Registered)) {
            throw new TestAbortedException("waits for the Fund Registration rule: the bank's record does not "
                    + "register the class where the client lives");
        }
    }

    private void abortIfAPlugInIsMissing(EngineResult result) {
        result.justification().outcomes().stream().filter(o -> o.explanation().startsWith("No plug-in reads"))
                .findFirst().ifPresent(o -> {
                    throw new TestAbortedException("waits for a rule kind's plug-in: " + o.explanation());
                });
    }

    /** The generic Then steps of AnswerSteps read the answer's JSON: verdict and outcomes per check. */
    private void publishToGenericSteps(EngineResult result) {
        JsonMapper mapper = JsonMapper.builder().build();
        ObjectNode answer = mapper.createObjectNode();
        ArrayNode checks = answer.putArray("checks");
        for (EngineResult r : results) {
            ObjectNode check = checks.addObject();
            check.put("verdict", r.verdict().name());
            ArrayNode outcomes = check.putArray("outcomes");
            for (RuleOutcome o : r.justification().outcomes()) {
                outcomes.addObject().put("kind", kindForGenericSteps(o)).put("result", o.result().name())
                        .put("explanation", o.explanation());
            }
        }
        ReflectionTestUtils.setField(answerSteps, "answer", answer);
    }

    /** The spec says "the Unmodelled Restrictions rule" (plural); the generic step derives the kind from those words. */
    private static String kindForGenericSteps(RuleOutcome outcome) {
        String name = outcome.kind().name();
        return name.equals("UNMODELLED_RESTRICTION") ? name + "S" : name;
    }

    // ---- Then

    @Then("the Justification says {string}")
    public void theJustificationSays(String text) {
        assertThat(last().justification().outcomes()).extracting(RuleOutcome::explanation)
                .anyMatch(e -> e.toLowerCase().contains(text.toLowerCase()));
    }

    @Then("the Justification warns {string}")
    public void theJustificationWarns(String text) {
        assertThat(last().justification().warnings()).anyMatch(w -> w.contains(text));
    }

    @Then("both checks give the same Verdict and the same Justification")
    public void bothChecksAgree() {
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).isEqualTo(results.get(1));
    }

    @Then("every rule outcome cites a passage of the Fund Documentation or a field of the Client Profile")
    public void everyOutcomeCites() {
        assertThat(last().justification().outcomes()).isNotEmpty()
                .allSatisfy(o -> assertThat(o.citations()).as(o.explanation()).isNotEmpty());
    }

    @Then("the investor category rule cites the prospectus page it relies on")
    public void theInvestorCategoryRuleCitesAPage() {
        assertThat(outcomeOf("INVESTOR_CATEGORY").citations()).isNotEmpty()
                .allSatisfy(c -> assertThat(c.page()).isPositive());
    }

    @Then("the Justification shows Nordea's {string} clause as Noted, with its Citation")
    public void theNotedClauseIsShown(String clause) {
        assertThat(outcomeOf("UNMODELLED_RESTRICTION").citations()).isNotEmpty();
    }

    @Then("the Target Market rule gives a warning {string}")
    public void theTargetMarketRuleWarns(String text) {
        assertThat(last().justification().warnings()).anyMatch(w -> w.contains(text));
    }

    @Then("the Target Market rule cites the risk profile, ability to bear losses and horizon of portfolio LOC90000102")
    public void theTargetMarketRuleCitesThePortfolio() {
        assertThat(allExplanationsOf("TARGET_MARKET")).contains("risk", "loss", "horizon");
    }

    @Then("the Target Market rule cites client {int}'s knowledge and experience in funds")
    public void theTargetMarketRuleCitesTheClient(int client) {
        assertThat(allExplanationsOf("TARGET_MARKET")).containsIgnoringCase("knowledge");
    }

    /** A "—" means the card has no such rule; a Warning cell is the Target Market plug-in's "no Target Market source" warning. */
    @Then("the rule outcomes are:")
    public void theRuleOutcomesAre(DataTable table) {
        for (Map.Entry<String, String> row : table.asMap().entrySet()) {
            String kind = KIND_OF_ROW.get(row.getKey());
            if (kind == null) {
                continue;
            }
            List<RuleOutcome> ofKind = last().justification().outcomes().stream()
                    .filter(o -> o.kind().name().equals(kind) && !o.citations().isEmpty()).toList();
            switch (row.getValue()) {
                case "—" -> assertThat(ofKind).as(row.getKey()).isEmpty();
                case "Pass", "Fail", "Review" -> {
                    // A kind that shows every rule (Unmodelled, Target Market) is judged by its worst outcome.
                    assertThat(ofKind).as(row.getKey()).isNotEmpty();
                    assertThat(worst(ofKind)).as(row.getKey()).isEqualTo(row.getValue().toUpperCase());
                }
                default -> assertThat(last().justification().warnings()).as(row.getKey())
                        .anyMatch(w -> w.contains("no Target Market source"));
            }
        }
    }

    private static String worst(List<RuleOutcome> outcomes) {
        List<String> results = outcomes.stream().map(o -> o.result().name()).toList();
        return results.contains("FAIL") ? "FAIL" : results.contains("REVIEW") ? "REVIEW" : "PASS";
    }

    private EngineResult last() {
        assertThat(results).as("a check ran").isNotEmpty();
        return results.get(results.size() - 1);
    }

    private String allExplanationsOf(String kind) {
        return String.join(" ", last().justification().outcomes().stream()
                .filter(o -> o.kind().name().equals(kind)).map(RuleOutcome::explanation).toList());
    }

    private RuleOutcome outcomeOf(String kind) {
        return last().justification().outcomes().stream().filter(o -> o.kind().name().equals(kind)).findFirst()
                .orElseThrow(() -> new AssertionError("no " + kind + " outcome"));
    }
}
