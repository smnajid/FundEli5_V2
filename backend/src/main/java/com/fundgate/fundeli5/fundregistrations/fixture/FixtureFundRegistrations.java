package com.fundgate.fundeli5.fundregistrations.fixture;

import static com.fundgate.fundeli5.bankdata.json.Json.array;
import static com.fundgate.fundeli5.bankdata.json.Json.isin;
import static com.fundgate.fundeli5.bankdata.json.Json.strings;

import com.fundgate.fundeli5.bankdata.Country;
import com.fundgate.fundeli5.bankdata.FundDataUnavailable;
import com.fundgate.fundeli5.bankdata.InvestorType;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.bankdata.json.Json;
import com.fundgate.fundeli5.fundregistrations.FundRegistrations;
import com.fundgate.fundeli5.fundregistrations.Registration;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * Reads {@code fund-registrations.json}. No entry for the ISIN, no {@code registrations} list, or an
 * entry whose investor types are absent or match no value: {@code Unknown}. A list without the
 * country: {@code NotRegistered}.
 */
public class FixtureFundRegistrations implements FundRegistrations {

    private final Path file;

    public FixtureFundRegistrations(Path fixturesDir) {
        this.file = fixturesDir.resolve("fund-registrations.json");
    }

    @Override
    public Registration registration(Isin isin, Country country) {
        JsonNode entry = entryFor(isin);
        List<JsonNode> registrations = entry == null ? null : array(entry, "registrations");
        if (registrations == null) {
            return new Registration.Unknown();
        }
        for (JsonNode registration : registrations) {
            if (country.code().equals(Json.string(registration, "country"))) {
                return registered(strings(registration, "investorTypes"));
            }
        }
        return new Registration.NotRegistered();
    }

    private static Registration registered(List<String> codes) {
        if (codes == null) {
            return new Registration.Unknown();
        }
        Set<InvestorType> types = EnumSet.noneOf(InvestorType.class);
        for (String code : codes) {
            InvestorType type = Json.enumOrNull(InvestorType.class, code);
            if (type == null) {
                return new Registration.Unknown();
            }
            types.add(type);
        }
        return new Registration.Registered(types);
    }

    private JsonNode entryFor(Isin isin) {
        try {
            for (JsonNode entry : Json.read(file)) {
                if (isin.equals(isin(entry, "isin"))) {
                    return entry;
                }
            }
            return null;
        } catch (IOException | JacksonException e) {
            throw new FundDataUnavailable("Cannot read " + file.getFileName(), e);
        }
    }
}
