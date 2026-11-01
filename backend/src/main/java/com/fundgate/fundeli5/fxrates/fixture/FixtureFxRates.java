package com.fundgate.fundeli5.fxrates.fixture;

import static com.fundgate.fundeli5.bankdata.json.Json.currency;
import static com.fundgate.fundeli5.bankdata.json.Json.date;
import static com.fundgate.fundeli5.bankdata.json.Json.decimal;
import static com.fundgate.fundeli5.bankdata.json.Json.string;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.FundDataUnavailable;
import com.fundgate.fundeli5.bankdata.json.Json;
import com.fundgate.fundeli5.fxrates.FxRate;
import com.fundgate.fundeli5.fxrates.FxRates;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/** Reads {@code fx-rates.json}; only the stored direction is answered, nothing is derived. */
public class FixtureFxRates implements FxRates {

    private final Path file;

    public FixtureFxRates(Path fixturesDir) {
        this.file = fixturesDir.resolve("fx-rates.json");
    }

    @Override
    public Optional<FxRate> rate(Currency base, Currency quote) {
        try {
            for (JsonNode node : Json.read(file)) {
                if (base.equals(currency(node, "base")) && quote.equals(currency(node, "quote"))) {
                    return Optional.of(new FxRate(base, quote, decimal(node, "rate"), date(node, "date"), string(node, "source")));
                }
            }
            return Optional.empty();
        } catch (IOException | JacksonException e) {
            throw new FundDataUnavailable("Cannot read " + file.getFileName(), e);
        }
    }
}
