package com.fundgate.fundeli5.validation;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.rulecard.RuleCard;
import java.util.List;

/**
 * What the validation screen shows about a Draft card. Only signals the code checks (hypotheses.md):
 * a quote not found on its page, a value with no supporting quote, a contradiction between documents.
 * The model's self-rated confidence stays in the card and is not shown.
 */
public record Review(RuleCard card, List<Contradiction> contradictions, List<Integer> unreviewedRules,
        List<Signal> signals) {

    /** Rules that disagree on one kind for one Share Class; {@code options} are the rule indexes. */
    public record Contradiction(Isin shareClass, String kind, List<Option> options) {
    }

    /** One side of a contradiction: the rule and the document it cites. */
    public record Option(int rule, String document, String summary) {
    }

    public enum SignalType { QUOTE_NOT_FOUND_ON_PAGE, NO_SUPPORTING_QUOTE }

    public record Signal(int rule, SignalType type) {
    }

    public boolean canValidate() {
        return card.status() == RuleCard.Status.DRAFT && contradictions.isEmpty() && unreviewedRules.isEmpty();
    }

    /** What stands in the way of validating, in words. */
    public List<String> blockers() {
        List<String> blockers = new java.util.ArrayList<>();
        if (card.status() != RuleCard.Status.DRAFT) {
            blockers.add("only a Draft card can be validated");
        }
        contradictions.forEach(c -> blockers.add(
                "contradiction on " + c.kind() + " of " + c.shareClass().value() + ": choose a document"));
        unreviewedRules.forEach(i -> blockers.add("rule " + i + " is not reviewed"));
        return blockers;
    }
}
