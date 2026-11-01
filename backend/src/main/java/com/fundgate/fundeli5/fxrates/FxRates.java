package com.fundgate.fundeli5.fxrates;

import com.fundgate.fundeli5.bankdata.Currency;
import java.util.Optional;

/** Out port: the bank's FX table, for the minimum initial subscription across currencies. */
public interface FxRates {

    /** The stored rate for one unit of {@code base} in {@code quote}; empty when the table has none. */
    Optional<FxRate> rate(Currency base, Currency quote);
}
