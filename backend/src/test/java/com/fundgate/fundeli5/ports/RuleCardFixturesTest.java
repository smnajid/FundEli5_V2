package com.fundgate.fundeli5.ports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fundgate.fundeli5.bankdata.InvestorType;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.Rule;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.json.RuleCardJson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class RuleCardFixturesTest {

    static final Path CARDS = FixturePortsTest.FIXTURES.resolve("rule-cards");

    @Test
    void theFourDemoFundsGoldCardsParse() throws Exception {
        List<Path> files;
        try (Stream<Path> stream = Files.list(CARDS)) {
            files = stream.filter(f -> f.toString().endsWith(".json")).sorted().toList();
        }
        assertThat(files).hasSize(5);

        List<RuleCard> cards = files.stream().map(RuleCardJson::read).toList();

        assertThat(cards).extracting(RuleCard::status).containsOnly(RuleCard.Status.VALIDATED, RuleCard.Status.DRAFT);
        assertThat(cards.stream().filter(c -> c.status() == RuleCard.Status.VALIDATED)).hasSize(4)
                .allSatisfy(c -> {
                    assertThat(c.validation()).isNotNull();
                    assertThat(c.rules()).isNotEmpty();
                    assertThat(c.rules()).allSatisfy(r -> assertThat(r.citations()).isNotEmpty());
                });
    }

    @Test
    void aRuleKeepsItsScopeConditionCitationAndHistory() {
        RuleCard card = RuleCardJson.read(CARDS.resolve("nordea-sif-global-private-equity-v1.json"));

        assertThat(card.shareClasses()).extracting(Object::toString).containsExactly("LU2607415275");
        Rule category = card.rules().stream()
                .filter(r -> r.condition() instanceof Condition.InvestorCategory).findFirst().orElseThrow();
        assertThat(category.scope()).isEqualTo(Rule.Scope.SHARE_CLASS);
        assertThat(category.shareClass().value()).isEqualTo("LU2607415275");
        assertThat(category.condition()).isEqualTo(new Condition.InvestorCategory(Set.of(InvestorType.WELL_INFORMED), List.of()));
        assertThat(category.citations()).hasSize(2);
        assertThat(category.citations().getFirst().page()).isEqualTo(22);
        assertThat(category.history().getFirst().action()).isEqualTo("added_by_validator");
        assertThat(card.sources()).hasSize(2);
        assertThat(card.drafting().model()).startsWith("none");
    }

    @Test
    void aDraftCardHasNoValidation() {
        RuleCard card = RuleCardJson.read(CARDS.resolve("bnp-ch-bonds-chf-a-ac-v1-draft.json"));

        assertThat(card.status()).isEqualTo(RuleCard.Status.DRAFT);
        assertThat(card.validation()).isNull();
        assertThat(card.rules()).isEmpty();
    }

    @Test
    void everyKindOfConditionIsCoveredByTheFixtures() {
        Set<Class<?>> seen = new java.util.HashSet<>();
        try (Stream<Path> stream = Files.list(CARDS)) {
            stream.filter(f -> f.toString().endsWith(".json")).map(RuleCardJson::read).flatMap(c -> c.rules().stream()).forEach(r -> seen.add(r.condition().getClass()));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        assertThat(seen).contains(Condition.InvestorCategory.class, Condition.UsPersonExclusion.class,
                Condition.MinimumInitialSubscription.class, Condition.MandateOnlyClass.class,
                Condition.KidRequiredForRetail.class, Condition.UnmodelledRestriction.class);
    }

    @Test
    void anUnknownRuleKindFailsLoudlyInsteadOfDroppingTheRule() {
        var card = JsonMapper.builder().build().readTree("""
                {"fundName":"F","shareClasses":[],"version":1,"status":"validated","rules":[
                  {"scope":"fund","shareClass":null,"condition":{"kind":"lockup"},"citations":[],
                   "summary":"s","confidence":1.0,"contradiction":false,"history":[]}],
                 "sources":[],"drafting":null,"validation":null}""");

        assertThatThrownBy(() -> RuleCardJson.parse(card))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("lockup");
    }
}
