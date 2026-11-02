package com.fundgate.fundeli5.explanation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.citation.Citation;
import com.fundgate.fundeli5.eligibility.Justification;
import com.fundgate.fundeli5.eligibility.RuleOutcome;
import com.fundgate.fundeli5.eligibility.Verdict;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExplanationRendererTest {

    private final ExplanationRenderer renderer = new ExplanationRenderer(ExplanationTemplates.load());
    private final Citation page12 = new Citation("Prospectus", 12, "quote", true);

    private Justification justification() {
        return new Justification(List.of(
                new RuleOutcome(RuleKind.INVESTOR_CATEGORY, RuleOutcome.Result.FAIL, "engine text", List.of(page12)),
                new RuleOutcome(RuleKind.MINIMUM_INITIAL_SUBSCRIPTION, RuleOutcome.Result.PASS, "ok", List.of(page12)),
                new RuleOutcome(RuleKind.TARGET_MARKET, RuleOutcome.Result.REVIEW, "engine text", List.of())));
    }

    @Test
    void onlyFailAndReviewRulesGetASentenceAndEachKeepsItsCitationMarker() {
        Explanation e = renderer.render(Verdict.NOT_ELIGIBLE, justification(), Language.EN, List.of("BP-EUR"));

        assertThat(e.sentences()).hasSize(2);
        assertThat(e.sentences().get(0)).contains("investor category").endsWith("[Prospectus, p. 12]");
        assertThat(e.sentences().get(1)).contains("target market").doesNotContain("[");
        assertThat(e.text()).contains("Not Eligible").contains("BP-EUR");
    }

    @Test
    void theSameInputGivesTheSameText() {
        assertThat(renderer.render(Verdict.NOT_ELIGIBLE, justification(), Language.EN, List.of()).text())
                .isEqualTo(renderer.render(Verdict.NOT_ELIGIBLE, justification(), Language.EN, List.of()).text());
    }

    @Test
    void anEligibleVerdictSuggestsNoAlternative() {
        Explanation e = renderer.render(Verdict.ELIGIBLE, new Justification(List.of()), Language.EN, List.of("BP-EUR"));
        assertThat(e.alternatives()).isNull();
    }

    @Test
    void frenchIsPickedByLanguage() {
        assertThat(renderer.render(Verdict.NOT_ELIGIBLE, justification(), Language.FR, List.of()).text())
                .contains("catégorie d'investisseur");
    }

    @Test
    void everyRuleKindHasAFailAndAReviewTemplateInEveryLanguage() {
        ExplanationTemplates templates = ExplanationTemplates.load();
        for (RuleKind kind : RuleKind.values()) {
            for (RuleOutcome.Result result : List.of(RuleOutcome.Result.FAIL, RuleOutcome.Result.REVIEW)) {
                for (Language language : Language.values()) {
                    assertThat(templates.sentence(kind.name(), result, language)).as("%s %s %s", kind, result, language)
                            .isPresent();
                }
            }
        }
    }
}
