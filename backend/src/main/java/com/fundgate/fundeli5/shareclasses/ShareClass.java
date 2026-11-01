package com.fundgate.fundeli5.shareclasses;

import com.fundgate.fundeli5.bankdata.BankCode;
import com.fundgate.fundeli5.bankdata.Country;
import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.FundType;
import com.fundgate.fundeli5.bankdata.Isin;
import java.time.Instant;

/** A subscribable variant of a fund. Optional fields are {@code null} when unknown. */
public record ShareClass(
        Isin isin,
        String fundId,
        String fundName,
        String umbrellaName,
        String shareClassName,
        Currency currency,
        Country domicile,
        FundType fundType,
        BankCode productType,
        String valor,
        String internalCode,
        Instant retrievedAt) {

    public ShareClass {
        java.util.Objects.requireNonNull(isin);
        java.util.Objects.requireNonNull(fundName);
        java.util.Objects.requireNonNull(shareClassName);
        java.util.Objects.requireNonNull(currency);
        java.util.Objects.requireNonNull(retrievedAt);
    }
}
