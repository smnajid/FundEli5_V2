package com.fundgate.fundeli5.fxrates;

import com.fundgate.fundeli5.bankdata.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One unit of {@code base} is worth {@code rate} units of {@code quote}. */
public record FxRate(Currency base, Currency quote, BigDecimal rate, LocalDate date, String source) {

    public FxRate {
        java.util.Objects.requireNonNull(base);
        java.util.Objects.requireNonNull(quote);
        java.util.Objects.requireNonNull(rate);
        java.util.Objects.requireNonNull(date);
    }
}
