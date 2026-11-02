package com.fundgate.fundeli5.answering;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.citation.Citation;
import com.fundgate.fundeli5.eligibility.RuleOutcome;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One numbered card of the evidence rail: a rule outcome of a Share Class and the passage of a public Fund
 * Document it cites. Holds no client data.
 */
public record Evidence(int number, RuleKind kind, RuleOutcome.Result result, String shareClassName, Citation citation,
        boolean citedInReply) {

    /** The numbering of one answer: first appearance across the checks gives the next number. */
    static final class Numbering {

        private record Key(Isin shareClass, RuleKind kind, RuleOutcome.Result result, Citation citation) {
        }

        private final Map<Key, Integer> numbers = new LinkedHashMap<>();
        private final List<Evidence> cards = new ArrayList<>();

        Numbering(List<Answer.Check> checks) {
            for (Answer.Check check : checks) {
                for (RuleOutcome outcome : check.justification().outcomes()) {
                    for (Citation citation : outcome.citations()) {
                        Key key = new Key(check.shareClass(), outcome.kind(), outcome.result(), citation);
                        if (!numbers.containsKey(key)) {
                            numbers.put(key, numbers.size() + 1);
                            cards.add(new Evidence(numbers.size(), outcome.kind(), outcome.result(),
                                    check.shareClassName(), citation, false));
                        }
                    }
                }
            }
        }

        int number(Isin shareClass, RuleOutcome outcome, Citation citation) {
            return numbers.get(new Key(shareClass, outcome.kind(), outcome.result(), citation));
        }

        /** The numbers a check's outcomes cite, by the "document, p. N" text the explanation prints. */
        Map<String, List<Integer>> numbersByPassage(Answer.Check check) {
            Map<String, List<Integer>> byPassage = new LinkedHashMap<>();
            for (RuleOutcome outcome : check.justification().outcomes()) {
                for (Citation citation : outcome.citations()) {
                    List<Integer> list = byPassage.computeIfAbsent(citation.document() + ", p. " + citation.page(),
                            k -> new ArrayList<>());
                    int n = number(check.shareClass(), outcome, citation);
                    if (!list.contains(n)) {
                        list.add(n);
                    }
                }
            }
            return byPassage;
        }

        List<Evidence> cards(List<Integer> citedInReply) {
            return cards.stream().map(e -> new Evidence(e.number(), e.kind(), e.result(), e.shareClassName(),
                    e.citation(), citedInReply.contains(e.number()))).toList();
        }
    }
}
