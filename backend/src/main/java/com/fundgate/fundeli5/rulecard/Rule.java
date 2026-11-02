package com.fundgate.fundeli5.rulecard;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.citation.Citation;
import java.time.Instant;
import java.util.List;

/** One rule, scoped to the whole fund or to one Share Class, with its Citations and history. */
public record Rule(
        Scope scope,
        Isin shareClass,
        Condition condition,
        List<Citation> citations,
        String summary,
        double confidence,
        boolean contradiction,
        List<HistoryEntry> history) {

    public enum Scope { FUND, SHARE_CLASS }

    /** What happened to the rule: drafted, or added/edited by the validator. */
    public record HistoryEntry(String action, String by, Instant at, Condition condition) {
    }

    public Rule {
        java.util.Objects.requireNonNull(scope);
        java.util.Objects.requireNonNull(condition);
        citations = List.copyOf(citations);
        history = List.copyOf(history);
        if (scope == Scope.SHARE_CLASS && shareClass == null) {
            throw new IllegalArgumentException("A share-class rule names its Share Class");
        }
    }

    /** True once a person has acted on the rule: anything in its history besides the pipeline's drafting. */
    public boolean reviewed() {
        return history.stream().anyMatch(h -> !DRAFTED.equals(h.action()));
    }

    public static final String DRAFTED = "drafted";
    public static final String ACCEPTED = "accepted";
    public static final String CORRECTED = "corrected";
    public static final String SOURCE_CHOSEN = "source-chosen";
    public static final String MARKED_NOTED = "marked-noted";
    public static final String MARKED_BLOCKING = "marked-blocking";

    /** The rule as the validator found it right: kept, with who accepted it and when. */
    public Rule accepted(String by, Instant at) {
        return recorded(ACCEPTED, condition, by, at, contradiction);
    }

    /** The rule with the validator's corrected condition. */
    public Rule corrected(Condition corrected, String by, Instant at) {
        return recorded(CORRECTED, java.util.Objects.requireNonNull(corrected), by, at, contradiction);
    }

    /** The winner of a contradiction the validator settled by following this rule's document. */
    public Rule chosenOverContradiction(String document, String by, Instant at) {
        return new Rule(scope, shareClass, condition, citations, summary, confidence, false,
                with(new HistoryEntry(SOURCE_CHOSEN + ": " + document, by, at, condition)));
    }

    /** An Unmodelled Restriction marked Blocking or Noted, with the reason for Noted. */
    public Rule markedUnmodelled(Condition.RestrictionStatus status, String reason, String by, Instant at) {
        if (!(condition instanceof Condition.UnmodelledRestriction)) {
            throw new IllegalStateException("Only an Unmodelled Restriction is marked Blocking or Noted");
        }
        if (status == Condition.RestrictionStatus.NOTED && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Marking a restriction Noted needs a reason");
        }
        Condition marked = new Condition.UnmodelledRestriction(status,
                status == Condition.RestrictionStatus.NOTED ? reason : null);
        return recorded(status == Condition.RestrictionStatus.NOTED ? MARKED_NOTED : MARKED_BLOCKING, marked, by, at,
                contradiction);
    }

    private Rule recorded(String action, Condition newCondition, String by, Instant at, boolean stillContradicting) {
        return new Rule(scope, shareClass, newCondition, citations, summary, confidence, stillContradicting,
                with(new HistoryEntry(action, by, at, newCondition)));
    }

    private List<HistoryEntry> with(HistoryEntry entry) {
        List<HistoryEntry> all = new java.util.ArrayList<>(history);
        all.add(entry);
        return all;
    }
}
