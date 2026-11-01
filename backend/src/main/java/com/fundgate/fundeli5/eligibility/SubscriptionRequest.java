package com.fundgate.fundeli5.eligibility;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import java.math.BigDecimal;

/** What a Fund Dealer is asked to check. */
public record SubscriptionRequest(
        PortfolioReference portfolio, Isin shareClass, BigDecimal amount, Currency currency,
        boolean clientInitiated) {

    public SubscriptionRequest {
        java.util.Objects.requireNonNull(portfolio);
        java.util.Objects.requireNonNull(shareClass);
        java.util.Objects.requireNonNull(amount);
        java.util.Objects.requireNonNull(currency);
    }
}
