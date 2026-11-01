package com.fundgate.fundeli5.requestparser;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import java.math.BigDecimal;

/** One request as the model read it. {@code amount} and {@code currency} are null when the email gave none. */
public record ParsedRequest(
        PortfolioId portfolio, String shareClassMention, BigDecimal amount, Currency currency,
        boolean clientInitiated) {

    public ParsedRequest {
        java.util.Objects.requireNonNull(portfolio);
        java.util.Objects.requireNonNull(shareClassMention);
    }
}
