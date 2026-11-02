package com.fundgate.fundeli5.rulecardpipeline;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.citation.Citation;
import com.fundgate.fundeli5.llm.LlmEgress;
import com.fundgate.fundeli5.llm.LlmRequest;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.Rule;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.ruleengine.RuleCatalog;
import com.fundgate.fundeli5.ruleengine.plugin.RuleKindPlugin;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Drafts a Draft Rule Card from the documents the {@link SourceReader} admitted: selects pages, asks the
 * model through the egress, runs the quote check on every citation, and proposes every Unmodelled
 * Restriction as Blocking (ADR 0003). It reaches a model only through {@link LlmEgress} (ADR 0004) and
 * reads public documents only.
 */
public class CardDrafter {

    private final PageSelector selector;
    private final LlmEgress egress;
    private final RuleCatalog catalog;
    private final String model;
    private final String endpoint;
    private final Clock clock;

    public CardDrafter(PageSelector selector, LlmEgress egress, RuleCatalog catalog, String model, String endpoint,
            Clock clock) {
        this.selector = selector;
        this.egress = egress;
        this.catalog = catalog;
        this.model = model;
        this.endpoint = endpoint;
        this.clock = clock;
    }

    /** What drafting made: the card and the topics whose pages may have been missed. */
    public record Drafted(RuleCard card, Set<PageTopic> possiblyMissed) {
    }

    public Drafted draft(SourceReading reading) {
        if (!reading.canDraft()) {
            throw new DraftingFailed("No admitted document to draft from");
        }
        Map<SourceDocument, List<Integer>> chosen = new LinkedHashMap<>();
        Set<PageTopic> missed = new LinkedHashSet<>();
        Set<Isin> shareClasses = new LinkedHashSet<>();
        for (SourceDocument source : reading.sources()) {
            PageSelection selection = selector.select(source.pages());
            chosen.put(source, selection.pages());
            missed.addAll(selection.possiblyMissed());
            shareClasses.addAll(source.document().isins());
        }
        String prompt = DraftingPrompt.documents(List.copyOf(shareClasses), reading.sources(), chosen);
        DraftAnswer answer = DraftAnswer.parse(
                egress.complete(new LlmRequest(model, DraftingPrompt.system(catalog), prompt)).text());
        if (answer.fundName() == null || answer.fundName().isBlank()) {
            throw new DraftingFailed("The answer names no fund");
        }
        List<Rule> rules = answer.rules().stream().map(r -> checked(r, reading, shareClasses)).toList();
        List<RuleCard.Source> sources = reading.sources().stream()
                .map(s -> new RuleCard.Source(s.document().documentId().value(), s.document().contentSha256(),
                        s.document().fmdEditionDate(), s.document().pdfEditionDate(), s.document().confidentiality()))
                .toList();
        RuleCard card = new RuleCard(null, answer.fundName(), List.copyOf(shareClasses), 1, RuleCard.Status.DRAFT,
                rules, sources, new RuleCard.Drafting(model, endpoint, Instant.now(clock)), null);
        return new Drafted(card, Set.copyOf(missed));
    }

    private Rule checked(Rule rule, SourceReading reading, Set<Isin> shareClasses) {
        if (rule.shareClass() != null && !shareClasses.contains(rule.shareClass())) {
            throw new DraftingFailed("A rule names " + rule.shareClass() + ", which is no Share Class of these documents");
        }
        List<Citation> citations = new ArrayList<>();
        for (Citation c : rule.citations()) {
            SourceDocument source = reading.sources().stream()
                    .filter(s -> s.document().documentId().value().equals(c.document())).findFirst()
                    .orElseThrow(() -> new DraftingFailed("A citation names " + c.document() + ", which was not read"));
            citations.add(QuoteCheck.check(c, source.pages()));
        }
        Condition condition = correctedByItsKind(rule.condition());
        return new Rule(rule.scope(), rule.shareClass(), condition, citations, rule.summary(), rule.confidence(),
                rule.contradiction(), List.of(new Rule.HistoryEntry(Rule.DRAFTED, "pipeline", Instant.now(clock), condition)));
    }

    /** Applies the kind plug-in's correction of the condition as drafted, if the catalog knows the kind. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Condition correctedByItsKind(Condition condition) {
        return catalog.pluginFor(condition).map(p -> (Condition) ((RuleKindPlugin) p).asDrafted(condition))
                .orElse(condition);
    }
}
