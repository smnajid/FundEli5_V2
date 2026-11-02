package com.fundgate.fundeli5.replydrafter;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import com.fundgate.fundeli5.eligibility.Justification;
import com.fundgate.fundeli5.eligibility.Verdict;
import java.util.List;

/**
 * One Eligibility Check as the reply needs it: a Portfolio Id, never a Portfolio Reference, and the names of
 * the Share Classes the engine found Eligible for the same portfolio and amount, to suggest instead.
 */
public record ReplyLine(PortfolioId portfolio, Isin shareClass, String shareClassName, Verdict verdict,
        Justification justification, List<String> alternatives) {

    public ReplyLine {
        alternatives = List.copyOf(alternatives);
    }
}
