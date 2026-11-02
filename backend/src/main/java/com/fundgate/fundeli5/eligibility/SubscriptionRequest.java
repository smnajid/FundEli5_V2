package com.fundgate.fundeli5.eligibility;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import java.math.BigDecimal;

/** What a Fund Dealer is asked to check. The amount and its currency are null when the request names none. */
public record SubscriptionRequest(
        PortfolioReference portfolio, Isin shareClass, BigDecimal amount, Currency currency,
        boolean clientInitiated) {

    public SubscriptionRequest {
        java.util.Objects.requireNonNull(portfolio);
        java.util.Objects.requireNonNull(shareClass);
        if ((amount == null) != (currency == null)) {
            throw new IllegalArgumentException("An amount and its currency are given together or not at all");
        }
    }
}
