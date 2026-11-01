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
}
