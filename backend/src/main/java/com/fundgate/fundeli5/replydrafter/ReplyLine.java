package com.fundgate.fundeli5.replydrafter;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import com.fundgate.fundeli5.eligibility.Justification;
import com.fundgate.fundeli5.eligibility.Verdict;

/** One Eligibility Check as the reply needs it: a Portfolio Id, never a Portfolio Reference. */
public record ReplyLine(PortfolioId portfolio, Isin shareClass, String shareClassName, Verdict verdict,
        Justification justification) {
}
