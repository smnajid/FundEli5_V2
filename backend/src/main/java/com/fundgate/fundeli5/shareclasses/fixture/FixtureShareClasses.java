package com.fundgate.fundeli5.shareclasses.fixture;

import static com.fundgate.fundeli5.bankdata.json.Json.bankCode;
import static com.fundgate.fundeli5.bankdata.json.Json.country;
import static com.fundgate.fundeli5.bankdata.json.Json.currency;
import static com.fundgate.fundeli5.bankdata.json.Json.instant;
import static com.fundgate.fundeli5.bankdata.json.Json.isin;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredString;
import static com.fundgate.fundeli5.bankdata.json.Json.string;

import com.fundgate.fundeli5.bankdata.FundDataUnavailable;
import com.fundgate.fundeli5.bankdata.FundType;
import com.fundgate.fundeli5.bankdata.json.Json;
import com.fundgate.fundeli5.shareclasses.ShareClass;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/** Reads {@code share-classes.json} under the fixtures directory. */
public class FixtureShareClasses implements ShareClasses {

    private final Path file;

    public FixtureShareClasses(Path fixturesDir) {
        this.file = fixturesDir.resolve("share-classes.json");
    }

    @Override
    public List<ShareClass> all() {
        try {
            List<ShareClass> classes = new ArrayList<>();
            for (JsonNode node : Json.read(file)) {
                classes.add(shareClass(node));
            }
            return List.copyOf(classes);
        } catch (IOException | JacksonException e) {
            throw new FundDataUnavailable("Cannot read " + file.getFileName(), e);
        }
    }

    private static ShareClass shareClass(JsonNode node) {
        FundType fundType = Json.enumOrNull(node, "fundType", FundType.class);
        if (fundType == null && string(node, "fundType") != null) {
            fundType = FundType.OTHER;
        }
        return new ShareClass(
                isin(node, "isin"),
                string(node, "fundId"),
                requiredString(node, "fundName"),
                string(node, "umbrellaName"),
                requiredString(node, "shareClassName"),
                currency(node, "currency"),
                country(node, "domicile"),
                fundType,
                bankCode(node, "productType"),
                string(node, "valor"),
                string(node, "internalCode"),
                instant(node, "retrievedAt"));
    }
}
