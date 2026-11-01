package com.fundgate.fundeli5.rulecard;

import com.fundgate.fundeli5.bankdata.Confidentiality;
import com.fundgate.fundeli5.bankdata.EditionDate;
import com.fundgate.fundeli5.bankdata.Isin;
import java.time.Instant;
import java.util.List;

/**
 * The structured set of rules for one fund. Only a {@link Status#VALIDATED} card decides a Verdict.
 * {@code validation} is {@code null} on a draft.
 */
public record RuleCard(
        String fundId,
        String fundName,
        List<Isin> shareClasses,
        int version,
        Status status,
        List<Rule> rules,
        List<Source> sources,
        Drafting drafting,
        Validation validation) {

    public enum Status { DRAFT, VALIDATED }

    /** A document the card was drafted from, pinned by its hash. */
    public record Source(
            String documentId,
            String contentSha256,
            EditionDate fmdEditionDate,
            EditionDate pdfEditionDate,
            Confidentiality confidentiality) {
    }

    public record Drafting(String model, String endpoint, Instant draftedAt) {
    }

    public record Validation(String validator, Instant validatedAt) {
    }

    public RuleCard {
        java.util.Objects.requireNonNull(fundName);
        java.util.Objects.requireNonNull(status);
        shareClasses = List.copyOf(shareClasses);
        rules = List.copyOf(rules);
        sources = List.copyOf(sources);
    }
}
