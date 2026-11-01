package com.fundgate.fundeli5.ruleengine;

import com.fundgate.fundeli5.bankdata.EditionDate;
import com.fundgate.fundeli5.clientprofiles.ClientProfile;
import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.eligibility.Justification;
import com.fundgate.fundeli5.eligibility.RuleOutcome;
import com.fundgate.fundeli5.eligibility.SubscriptionRequest;
import com.fundgate.fundeli5.eligibility.Verdict;
import com.fundgate.fundeli5.rulecard.Rule;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.RuleCards;
import com.fundgate.fundeli5.ruleengine.plugin.Facts;
import com.fundgate.fundeli5.ruleengine.plugin.Finding;
import com.fundgate.fundeli5.ruleengine.plugin.FrameOutcomes;
import com.fundgate.fundeli5.ruleengine.plugin.Lookups;
import com.fundgate.fundeli5.ruleengine.plugin.RuleKindPlugin;
import com.fundgate.fundeli5.shareclasses.ShareClass;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The engine's frame (ADR 0001, 0002): finds the card, refuses to decide without a Validated one,
 * hands each rule to its plug-in, and combines the outcomes. Deterministic: no model, clock or randomness.
 */
public class RuleCatalogEngine implements RuleEngine {

    private static final List<String> COVERED_LEGAL_ENTITIES = List.of("LOC", "LUX");

    private final RuleCards ruleCards;
    private final ClientProfiles clientProfiles;
    private final ShareClasses shareClasses;
    private final Lookups lookups;
    private final RuleCatalog catalog;
    private final EditionLookup editions;

    public RuleCatalogEngine(RuleCards ruleCards, ClientProfiles clientProfiles, ShareClasses shareClasses,
            Lookups lookups, RuleCatalog catalog, EditionLookup editions) {
        this.ruleCards = ruleCards;
        this.clientProfiles = clientProfiles;
        this.shareClasses = shareClasses;
        this.lookups = lookups;
        this.catalog = catalog;
        this.editions = editions;
    }

    @Override
    public EngineResult check(SubscriptionRequest request) {
        Optional<RuleCard> found = ruleCards.forShareClass(request.shareClass());
        if (found.isEmpty()) {
            return review(FrameOutcomes.fundNotCovered());
        }
        RuleCard card = found.get();
        ClientProfile profile = clientProfiles.clientProfile(request.portfolio());
        if (!COVERED_LEGAL_ENTITIES.contains(profile.legalEntity())) {
            return review(FrameOutcomes.regimeNotCovered(profile.legalEntity()));
        }
        if (card.status() != RuleCard.Status.VALIDATED) {
            return review(FrameOutcomes.cardNotValidated());
        }
        ShareClass shareClass = shareClasses.all().stream()
                .filter(s -> s.isin().equals(request.shareClass())).findFirst().orElse(null);
        Facts facts = new Facts(request, profile, shareClass);

        List<RuleOutcome> outcomes = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (List<Rule> sameKind : applicableRulesByKind(card, request).values()) {
            evaluateKind(sameKind, facts, outcomes, warnings);
        }
        if (outcomes.isEmpty()) {
            outcomes.add(FrameOutcomes.noRuleApplies());
        }
        staleWarning(card).ifPresent(warnings::add);
        return new EngineResult(combine(outcomes), new Justification(outcomes, warnings));
    }

    /** Any Fail gives Not Eligible, else any Review gives Needs Review, else Eligible. */
    static Verdict combine(List<RuleOutcome> outcomes) {
        if (outcomes.stream().anyMatch(o -> o.result() == RuleOutcome.Result.FAIL)) {
            return Verdict.NOT_ELIGIBLE;
        }
        if (outcomes.stream().anyMatch(o -> o.result() == RuleOutcome.Result.REVIEW)) {
            return Verdict.NEEDS_REVIEW;
        }
        return Verdict.ELIGIBLE;
    }

    private static EngineResult review(RuleOutcome outcome) {
        return new EngineResult(Verdict.NEEDS_REVIEW, new Justification(List.of(outcome)));
    }

    /** Fund-level rules and the rules of the requested class, grouped by kind in card order. */
    private static Map<Class<?>, List<Rule>> applicableRulesByKind(RuleCard card, SubscriptionRequest request) {
        Map<Class<?>, List<Rule>> byKind = new LinkedHashMap<>();
        for (Rule rule : card.rules()) {
            boolean applies = rule.scope() == Rule.Scope.FUND || request.shareClass().equals(rule.shareClass());
            if (applies) {
                byKind.computeIfAbsent(rule.condition().getClass(), k -> new ArrayList<>()).add(rule);
            }
        }
        return byKind;
    }

    private void evaluateKind(List<Rule> rules, Facts facts, List<RuleOutcome> outcomes, List<String> warnings) {
        Optional<RuleKindPlugin<?>> plugin = catalog.pluginFor(rules.get(0).condition());
        if (plugin.isEmpty()) {
            outcomes.add(FrameOutcomes.noPlugIn(rules.get(0).condition()));
            return;
        }
        List<RuleOutcome> ofKind = new ArrayList<>();
        boolean accumulates = plugin.get().accumulates();
        for (Rule rule : accumulates ? rules : classLevelOverridesFundLevel(rules)) {
            if (rule.contradiction()) {
                warnings.add("The documents contradict each other on: " + rule.summary());
            }
            evaluate(plugin.get(), rule, facts).ifPresent(ofKind::add);
        }
        if (accumulates) {
            outcomes.addAll(ofKind);
        } else {
            ofKind.stream().max(Comparator.comparingInt(RuleCatalogEngine::severity))
                    .ifPresent(outcomes::add);
        }
    }

    private static int severity(RuleOutcome outcome) {
        return switch (outcome.result()) {
            case PASS -> 0;
            case REVIEW -> 1;
            case FAIL -> 2;
        };
    }

    private static List<Rule> classLevelOverridesFundLevel(List<Rule> rules) {
        List<Rule> classLevel = rules.stream().filter(r -> r.scope() == Rule.Scope.SHARE_CLASS).toList();
        return classLevel.isEmpty() ? rules : classLevel;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Optional<RuleOutcome> evaluate(RuleKindPlugin plugin, Rule rule, Facts facts) {
        Optional<Finding> finding = plugin.evaluate(plugin.asDrafted(rule.condition()), facts, lookups);
        return finding.map(f -> new RuleOutcome(plugin.kind(), f.result(), f.explanation(), rule.citations()));
    }

    /** A Stale card still decides; it only warns that a newer edition of a source is listed. */
    private Optional<String> staleWarning(RuleCard card) {
        for (RuleCard.Source source : card.sources()) {
            EditionDate used = source.fmdEditionDate() != null ? source.fmdEditionDate() : source.pdfEditionDate();
            Optional<EditionDate> listed = editions.listedEdition(source.documentId());
            if (used != null && listed.isPresent() && isNewer(listed.get(), used)) {
                return Optional.of("Stale Rule Card: " + source.documentId() + " is listed in edition "
                        + listed.get() + ", the card was drafted from " + used + ".");
            }
        }
        return Optional.empty();
    }

    /** Compares at the precision both editions share (a month, or a day). */
    private static boolean isNewer(EditionDate listed, EditionDate used) {
        int length = Math.min(listed.value().length(), used.value().length());
        return listed.value().substring(0, length).compareTo(used.value().substring(0, length)) > 0;
    }
}
