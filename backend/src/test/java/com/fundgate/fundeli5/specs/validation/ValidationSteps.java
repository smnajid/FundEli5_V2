package com.fundgate.fundeli5.specs.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.Rule;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.json.RuleCardJson;
import com.fundgate.fundeli5.rulecard.memory.InMemoryRuleCards;
import com.fundgate.fundeli5.validation.CannotValidate;
import com.fundgate.fundeli5.validation.Review;
import com.fundgate.fundeli5.validation.ValidationService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.opentest4j.TestAbortedException;

/** Steps of the Validation scenarios. They run the real service on an in-memory store seeded from the fixtures. */
public class ValidationSteps {

    private static final Path FIXTURES = Path.of("../docs/playbook/contracts/fixtures");
    private static final Isin GCE_BP = new Isin("LU0348926287");
    private static final Isin GCE_BI = new Isin("LU0348927095");
    private static final String PROSPECTUS = "nordea1-prospectus-2026-06";
    private static final String VALIDATOR = "Compliance Validator";
    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

    private InMemoryRuleCards store;
    private ValidationService service;
    private RuleCard validatedV1;
    private RuleCard before;

    @Before("@validation")
    public void freshStore() {
        store = new InMemoryRuleCards();
        service = new ValidationService(store, Clock.fixed(NOW, ZoneOffset.UTC));
        validatedV1 = null;
    }

    private RuleCard draftNordea() {
        return RuleCardJson.read(FIXTURES.resolve("draft-rule-cards/nordea1-global-climate-and-environment-v1-draft.json"));
    }

    private RuleCard nordea() {
        if (store.all().isEmpty()) {
            store.save(draftNordea());
        }
        return store.all().get(0);
    }

    @Given("the Draft Rule Card of Nordea 1 Global Climate and Environment")
    public void theDraftCard() {
        store.save(draftNordea());
    }

    @Then("the screen shows a contradiction on the investor category of GCE-BI")
    public void contradictionShown() {
        RuleCard card = nordea();
        Review review = service.review(card.fundName(), card.version());
        assertThat(review.contradictions()).hasSize(1).allSatisfy(c -> {
            assertThat(c.shareClass()).isEqualTo(GCE_BI);
            assertThat(c.kind()).isEqualTo("INVESTOR_CATEGORY");
            assertThat(c.options()).extracting(Review.Option::document)
                    .contains(PROSPECTUS, "nordea1-gce-kid-bi-eur");
        });
    }

    @Then("the card cannot be validated")
    public void cannotBeValidated() {
        RuleCard card = nordea();
        assertThat(service.review(card.fundName(), card.version()).canValidate()).isFalse();
        assertThatThrownBy(() -> service.validate(card.fundName(), card.version(), VALIDATOR))
                .isInstanceOf(CannotValidate.class);
        assertThat(store.all().get(0).status()).isEqualTo(RuleCard.Status.DRAFT);
    }

    @When("the validator chooses to follow the prospectus")
    public void followProspectus() {
        RuleCard card = nordea();
        service.followDocument(card.fundName(), card.version(), GCE_BI, "INVESTOR_CATEGORY", PROSPECTUS, VALIDATOR);
    }

    @When("accepts or corrects every other rule")
    public void acceptEveryOtherRule() {
        RuleCard card = nordea();
        for (int rule : service.review(card.fundName(), card.version()).unreviewedRules()) {
            service.accept(card.fundName(), card.version(), rule, VALIDATOR);
        }
    }

    @Then("the card can be validated")
    public void canBeValidated() {
        RuleCard card = nordea();
        assertThat(service.review(card.fundName(), card.version()).canValidate()).isTrue();
        RuleCard validated = service.validate(card.fundName(), card.version(), VALIDATOR);
        assertThat(validated.status()).isEqualTo(RuleCard.Status.VALIDATED);
        assertThat(validated.validation().validator()).isEqualTo(VALIDATOR);
        assertThat(validated.validation().validatedAt()).isEqualTo(NOW);
    }

    @Given("Validated version 1 of GCE-BP's Rule Card")
    public void validatedVersionOne() {
        validatedV1 = RuleCardJson.read(FIXTURES.resolve("rule-cards/nordea1-global-climate-and-environment-v1.json"));
        assertThat(validatedV1.status()).isEqualTo(RuleCard.Status.VALIDATED);
        store.save(validatedV1);
    }

    @When("the pipeline drafts version 2 from a new prospectus edition")
    public void pipelineDraftsVersionTwo() {
        before = store.all().get(0);
        List<Rule> rules = validatedV1.rules().stream()
                .map(r -> new Rule(r.scope(), r.shareClass(), r.condition(), r.citations(), r.summary(),
                        r.confidence(), r.contradiction(),
                        List.of(new Rule.HistoryEntry(Rule.DRAFTED, "Rule Card pipeline", NOW, r.condition()))))
                .toList();
        store.save(new RuleCard(validatedV1.fundId(), validatedV1.fundName(), validatedV1.shareClasses(), 2,
                RuleCard.Status.DRAFT, rules, validatedV1.sources(), validatedV1.drafting(), null));
    }

    @Then("version 1 still decides Verdicts")
    public void versionOneDecides() {
        assertThat(store.forShareClass(GCE_BP)).get().satisfies(c -> {
            assertThat(c.version()).isEqualTo(1);
            assertThat(c.status()).isEqualTo(RuleCard.Status.VALIDATED);
        });
    }

    @Then("version 2 decides Verdicts only once validated")
    public void versionTwoDecidesOnceValidated() {
        String fund = validatedV1.fundName();
        assertThat(store.forShareClass(GCE_BP).orElseThrow().version()).isEqualTo(1);
        Review review = service.review(fund, 2);
        assertThat(review.canValidate()).isFalse();
        review.unreviewedRules().forEach(i -> service.accept(fund, 2, i, VALIDATOR));
        service.validate(fund, 2, VALIDATOR);
        assertThat(store.forShareClass(GCE_BP).orElseThrow().version()).isEqualTo(2);
    }

    @Then("version 1 is unchanged")
    public void versionOneUnchanged() {
        RuleCard v1 = store.all().stream().filter(c -> c.version() == 1).findFirst().orElseThrow();
        assertThat(v1).isEqualTo(before).isEqualTo(validatedV1);
    }

    @When("the validator marks the {string} clause Noted")
    public void markNoted(String clause) {
        RuleCard card = nordea();
        int rule = indexOfUnmodelled(card);
        service.mark(card.fundName(), card.version(), rule, Condition.RestrictionStatus.NOTED,
                "The board's right to " + clause + " sets no condition on the client", VALIDATOR);
    }

    @Then("the card's history records the marking, who made it and when")
    public void historyRecordsMarking() {
        RuleCard card = store.all().get(0);
        Rule rule = card.rules().get(indexOfUnmodelled(card));
        assertThat(rule.condition()).isInstanceOfSatisfying(Condition.UnmodelledRestriction.class,
                u -> assertThat(u.status()).isEqualTo(Condition.RestrictionStatus.NOTED));
        assertThat(rule.history()).last().satisfies(h -> {
            assertThat(h.action()).isEqualTo(Rule.MARKED_NOTED);
            assertThat(h.by()).isEqualTo(VALIDATOR);
            assertThat(h.at()).isEqualTo(NOW);
        });
    }

    /*
     * The @should scenario "No minimum found for a class" is not in this ticket. Its steps are defined so the
     * tag runs without an undefined step, and the scenario is aborted (skipped), as EngineSteps does.
     */

    @Given("the Draft Rule Card has no minimum initial subscription rule for GCE-BI")
    public void shouldNoMinimum() {
        throw new TestAbortedException("@should: possibly-missed rule kinds are not built yet");
    }

    @Then("the screen warns {string} and lists the pages not read")
    public void shouldWarn(String warning) {
        throw new TestAbortedException("@should: possibly-missed rule kinds are not built yet");
    }

    @Then("until the validator adds the rule or confirms there is none, checks on GCE-BI give Needs Review")
    public void shouldNeedsReview() {
        throw new TestAbortedException("@should: possibly-missed rule kinds are not built yet");
    }

    private static int indexOfUnmodelled(RuleCard card) {
        for (int i = 0; i < card.rules().size(); i++) {
            if (card.rules().get(i).condition() instanceof Condition.UnmodelledRestriction) {
                return i;
            }
        }
        throw new AssertionError("No Unmodelled Restriction on " + card.fundName());
    }
}
