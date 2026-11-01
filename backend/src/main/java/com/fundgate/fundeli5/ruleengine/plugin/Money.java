package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.fxrates.FxRates;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** Conversion at the bank's FX table, in the stored direction or its inverse. Empty when the table has no rate. */
final class Money {

    private Money() {}

    static Optional<BigDecimal> convert(BigDecimal amount, Currency from, Currency to, FxRates rates) {
        if (from.equals(to)) {
            return Optional.of(amount);
        }
        if (rates == null) {
            return Optional.empty();
        }
        var direct = rates.rate(from, to);
        if (direct.isPresent()) {
            return Optional.of(amount.multiply(direct.get().rate()));
        }
        return rates.rate(to, from).filter(r -> r.rate().signum() > 0)
                .map(r -> amount.divide(r.rate(), 10, RoundingMode.HALF_EVEN));
    }
}
