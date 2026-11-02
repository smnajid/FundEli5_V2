package com.fundgate.fundeli5.privacygate;

import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import com.fundgate.fundeli5.eligibility.Verdict;
import java.util.List;
import java.util.Objects;

/**
 * What a reply call may carry, structured so that the Gate decides what text reaches the model: per check a
 * Portfolio Id, the Share Class name, the Verdict and the desk's own explanation sentences (templates and
 * Citations), the classes to suggest, and the Requester Questions the dealer accepted.
 */
public record ReplyFacts(String language, List<Line> lines, List<String> requesterQuestions) {

    public ReplyFacts {
        Objects.requireNonNull(language);
        lines = List.copyOf(lines);
        requesterQuestions = List.copyOf(requesterQuestions);
    }

    public record Line(PortfolioId portfolio, String shareClassName, Verdict verdict, List<String> explanation,
            List<String> alternatives) {

        public Line {
            Objects.requireNonNull(portfolio);
            Objects.requireNonNull(shareClassName);
            Objects.requireNonNull(verdict);
            explanation = List.copyOf(explanation);
            alternatives = List.copyOf(alternatives);
        }
    }
}
