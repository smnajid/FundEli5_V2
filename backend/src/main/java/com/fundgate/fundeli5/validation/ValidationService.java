package com.fundgate.fundeli5.validation;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.Rule;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.RuleCardStore;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * The Rule Card Validator's work: review a Draft, settle contradictions, accept or correct each rule, mark
 * each Unmodelled Restriction, validate. Every act is kept in the rule's history with who and when. A
 * Validated card is never edited: a new edition is a new Draft version beside it.
 */
public class ValidationService {

    private final RuleCardStore store;
    private final Clock clock;

    public ValidationService(RuleCardStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    /** Every Draft card awaiting validation. */
    public List<RuleCard> drafts() {
        return store.all().stream().filter(c -> c.status() == RuleCard.Status.DRAFT).toList();
    }

    public Review review(String fundName, int version) {
        RuleCard card = find(fundName, version);
        List<Review.Contradiction> contradictions = contradictions(card);
        List<Integer> unreviewed = new ArrayList<>();
        List<Review.Signal> signals = new ArrayList<>();
        for (int i = 0; i < card.rules().size(); i++) {
            Rule rule = card.rules().get(i);
            if (!rule.reviewed()) {
                unreviewed.add(i);
            }
            if (rule.citations().isEmpty()) {
                signals.add(new Review.Signal(i, Review.SignalType.NO_SUPPORTING_QUOTE));
            }
            if (rule.citations().stream().anyMatch(c -> !c.quoteFoundOnPage())) {
                signals.add(new Review.Signal(i, Review.SignalType.QUOTE_NOT_FOUND_ON_PAGE));
            }
        }
        return new Review(card, contradictions, unreviewed, signals);
    }

    /** Settles a contradiction by following one document: its rule stays, the others are dropped. */
    public synchronized Review followDocument(String fundName, int version, Isin shareClass, String kind, String document, String by) {
        requireValidator(by);
        RuleCard card = draft(fundName, version);
        List<Rule> kept = new ArrayList<>();
        boolean found = false;
        for (Rule rule : card.rules()) {
            if (!inContradiction(rule, shareClass, kind)) {
                kept.add(rule);
            } else if (document.equals(documentOf(rule))) {
                kept.add(rule.chosenOverContradiction(document + " (dropped: " + dropped(card, shareClass, kind, document)
                        + ")", by, clock.instant()));
                found = true;
            }
        }
        if (!found) {
            throw new NoSuchElementException("No contradicting rule of " + kind + " cites " + document);
        }
        return save(card, kept);
    }

    public synchronized Review accept(String fundName, int version, int rule, String by) {
        requireValidator(by);
        return replace(fundName, version, rule, r -> r.accepted(by, clock.instant()));
    }

    public synchronized Review correct(String fundName, int version, int rule, Condition corrected, String by) {
        requireValidator(by);
        return replace(fundName, version, rule, r -> r.corrected(corrected, by, clock.instant()));
    }

    /** Marks an Unmodelled Restriction Blocking, or Noted with the reason it sets no condition on the client. */
    public synchronized Review mark(String fundName, int version, int rule, Condition.RestrictionStatus status, String reason,
            String by) {
        requireValidator(by);
        return replace(fundName, version, rule, r -> r.markedUnmodelled(status, reason, by, clock.instant()));
    }

    /** Validates the card once every contradiction is settled and every rule reviewed. */
    public synchronized RuleCard validate(String fundName, int version, String by) {
        Review review = review(fundName, version);
        if (!review.canValidate()) {
            throw new CannotValidate(review.blockers());
        }
        if (by == null || by.isBlank()) {
            throw new CannotValidate(List.of("the validator is not named"));
        }
        RuleCard card = review.card();
        RuleCard validated = new RuleCard(card.fundId(), card.fundName(), card.shareClasses(), card.version(),
                RuleCard.Status.VALIDATED, card.rules(), card.sources(), card.drafting(),
                new RuleCard.Validation(by, clock.instant()));
        store.save(validated);
        return validated;
    }

    private static String dropped(RuleCard card, Isin shareClass, String kind, String document) {
        return card.rules().stream().filter(r -> inContradiction(r, shareClass, kind))
                .filter(r -> !document.equals(documentOf(r))).map(r -> documentOf(r) + ": " + r.summary())
                .collect(java.util.stream.Collectors.joining("; "));
    }

    private static void requireValidator(String by) {
        if (by == null || by.isBlank()) {
            throw new IllegalArgumentException("The validator is not named");
        }
    }

    private Review replace(String fundName, int version, int index, java.util.function.UnaryOperator<Rule> change) {
        RuleCard card = draft(fundName, version);
        if (index < 0 || index >= card.rules().size()) {
            throw new NoSuchElementException("No rule " + index);
        }
        List<Rule> rules = new ArrayList<>(card.rules());
        rules.set(index, change.apply(rules.get(index)));
        return save(card, rules);
    }

    private Review save(RuleCard card, List<Rule> rules) {
        store.save(new RuleCard(card.fundId(), card.fundName(), card.shareClasses(), card.version(), card.status(),
                rules, card.sources(), card.drafting(), card.validation()));
        return review(card.fundName(), card.version());
    }

    private List<Review.Contradiction> contradictions(RuleCard card) {
        Map<String, List<Review.Option>> groups = new LinkedHashMap<>();
        Map<String, Isin> classes = new LinkedHashMap<>();
        for (int i = 0; i < card.rules().size(); i++) {
            Rule rule = card.rules().get(i);
            if (rule.contradiction()) {
                String key = key(rule.shareClass(), rule.condition().kind().name());
                groups.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(new Review.Option(i, documentOf(rule), rule.summary()));
                classes.put(key, rule.shareClass());
            }
        }
        List<Review.Contradiction> result = new ArrayList<>();
        groups.forEach((key, options) -> result.add(new Review.Contradiction(
                classes.get(key), key.substring(key.indexOf('|') + 1), options)));
        return result;
    }

    private static boolean inContradiction(Rule rule, Isin shareClass, String kind) {
        return rule.contradiction() && Objects.equals(rule.shareClass(), shareClass)
                && rule.condition().kind().name().equals(kind);
    }

    private static String key(Isin shareClass, String kind) {
        return (shareClass == null ? "" : shareClass.value()) + "|" + kind;
    }

    private static String documentOf(Rule rule) {
        return rule.citations().isEmpty() ? "" : rule.citations().get(0).document();
    }

    private RuleCard draft(String fundName, int version) {
        RuleCard card = find(fundName, version);
        if (card.status() != RuleCard.Status.DRAFT) {
            throw new IllegalStateException(
                    "Version " + version + " of " + fundName + " is Validated: a new edition is a new Draft version");
        }
        return card;
    }

    private RuleCard find(String fundName, int version) {
        return store.all().stream().filter(c -> c.fundName().equals(fundName) && c.version() == version)
                .findFirst().orElseThrow(() -> new NoSuchElementException("No card " + fundName + " v" + version));
    }
}
