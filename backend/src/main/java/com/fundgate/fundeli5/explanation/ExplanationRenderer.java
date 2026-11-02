package com.fundgate.fundeli5.explanation;

import com.fundgate.fundeli5.citation.Citation;
import com.fundgate.fundeli5.eligibility.Justification;
import com.fundgate.fundeli5.eligibility.RuleOutcome;
import com.fundgate.fundeli5.eligibility.Verdict;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Renders the Explanation of a Verdict from its Justification. Pure and deterministic; no model is involved. */
public final class ExplanationRenderer {

    private final ExplanationTemplates templates;

    public ExplanationRenderer(ExplanationTemplates templates) {
        this.templates = templates;
    }

    /**
     * @param alternatives names of Share Classes the portfolio is Eligible for instead (the caller checked them with the engine), in the order to suggest them
     */
    public Explanation render(Verdict verdict, Justification justification, Language language,
            List<String> alternatives) {
        String statement = templates.get("VERDICT." + verdict.name() + "." + language.code());
        List<String> sentences = new ArrayList<>();
        for (RuleOutcome outcome : justification.outcomes()) {
            templates.sentence(outcome.kind().name(), outcome.result(), language)
                    .map(sentence -> sentence + citationMarker(outcome.citations()))
                    .ifPresent(sentences::add);
        }
        String suggestion = verdict == Verdict.ELIGIBLE || alternatives.isEmpty() ? null
                : templates.get("ALTERNATIVES." + language.code()).replace("{classes}", String.join(", ", alternatives));
        return new Explanation(verdict, statement, sentences, suggestion);
    }

    /** {@code " [Prospectus, p. 12]"} for each Citation; nothing when the rule cites no document passage. */
    private static String citationMarker(List<Citation> citations) {
        if (citations.isEmpty()) {
            return "";
        }
        return citations.stream().distinct().map(c -> c.document() + ", p. " + c.page() + (c.quoteFoundOnPage() ? "" : ", quote not verified"))
                .collect(Collectors.joining("; ", " [", "]"));
    }
}
