package com.fundgate.fundeli5.explanation;

import com.fundgate.fundeli5.eligibility.Verdict;
import java.util.List;

/** The explanation under a Verdict: the Verdict stated, one sentence per Fail or Review rule, the classes to buy instead. */
public record Explanation(Verdict verdict, String verdictStatement, List<String> sentences, String alternatives) {

    public Explanation {
        java.util.Objects.requireNonNull(verdict);
        java.util.Objects.requireNonNull(verdictStatement);
        sentences = List.copyOf(sentences);
    }

    /** The whole text, as shown under the Verdict and pasted in a reply. */
    public String text() {
        StringBuilder text = new StringBuilder(verdictStatement);
        sentences.forEach(s -> text.append('\n').append(s));
        if (alternatives != null && !alternatives.isBlank()) {
            text.append('\n').append(alternatives);
        }
        return text.toString();
    }
}
