package com.fundgate.fundeli5.specs.explanation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.eligibility.SubscriptionRequest;
import com.fundgate.fundeli5.eligibility.Verdict;
import com.fundgate.fundeli5.explanation.Explanation;
import com.fundgate.fundeli5.explanation.ExplanationRenderer;
import com.fundgate.fundeli5.explanation.ExplanationTemplates;
import com.fundgate.fundeli5.explanation.Language;
import com.fundgate.fundeli5.ruleengine.EngineResult;
import com.fundgate.fundeli5.ruleengine.RuleEngine;
import com.fundgate.fundeli5.shareclasses.ShareClass;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import com.fundgate.fundeli5.specs.ruleengine.EngineSteps;
import io.cucumber.java.en.Then;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Steps of 03-explain-a-verdict.feature. The checks run in {@code EngineSteps} ("asks to subscribe"); these steps
 * render the explanation of what it found. The alternative classes are the other classes the same request is
 * Eligible for, found by running the engine, so the explanation never decides anything itself.
 */
public class ExplanationSteps {

    private final ExplanationRenderer renderer = new ExplanationRenderer(ExplanationTemplates.load());

    @Autowired EngineSteps engineSteps;
    @Autowired RuleEngine engine;
    @Autowired ShareClasses shareClasses;

    @SuppressWarnings("unchecked")
    private List<EngineResult> results() {
        return (List<EngineResult>) ReflectionTestUtils.getField(engineSteps, "results");
    }

    private SubscriptionRequest request() {
        return (SubscriptionRequest) ReflectionTestUtils.getField(engineSteps, "lastRequest");
    }

    private List<Explanation> explanations() {
        List<EngineResult> results = results();
        assertThat(results).as("a check ran").isNotEmpty();
        SubscriptionRequest request = request();
        List<String> alternatives = shareClasses.all().stream()
                .filter(c -> !c.isin().equals(request.shareClass()))
                .filter(c -> engine.check(new SubscriptionRequest(request.portfolio(), c.isin(),
                        request.amount(), request.currency(), request.clientInitiated())).verdict() == Verdict.ELIGIBLE)
                .map(ShareClass::shareClassName).toList();
        return results.stream()
                .map(r -> renderer.render(r.verdict(), r.justification(), Language.EN, alternatives)).toList();
    }

    @Then("the explanation says in plain words that the deciding rule is the investor category")
    public void theExplanationNamesTheInvestorCategory() {
        assertThat(explanations().get(0).sentences())
                .anyMatch(s -> s.toLowerCase().contains("the deciding rule is the investor category"));
    }

    @Then("the explanation names {word} as a class she can buy")
    public void theExplanationNamesAClass(String shortName) {
        assertThat(explanations().get(0).text()).contains(shortName.replaceFirst("^GCE-", "") + "-EUR");
    }

    @Then("both explanations are identical")
    public void bothExplanationsAreIdentical() {
        List<Explanation> explanations = explanations();
        assertThat(explanations).hasSize(2);
        assertThat(explanations.get(0)).isEqualTo(explanations.get(1));
    }

    @Then("the explanation states the Verdict {}")
    public void theExplanationStatesTheVerdict(String verdict) {
        assertThat(explanations().get(0).verdictStatement()).contains(verdict);
    }
}
