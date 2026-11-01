package com.fundgate.fundeli5.clientprofiles.fixture;

import static com.fundgate.fundeli5.bankdata.json.Json.array;
import static com.fundgate.fundeli5.bankdata.json.Json.bankCode;
import static com.fundgate.fundeli5.bankdata.json.Json.bool;
import static com.fundgate.fundeli5.bankdata.json.Json.child;
import static com.fundgate.fundeli5.bankdata.json.Json.country;
import static com.fundgate.fundeli5.bankdata.json.Json.currency;
import static com.fundgate.fundeli5.bankdata.json.Json.date;
import static com.fundgate.fundeli5.bankdata.json.Json.enumOrNull;
import static com.fundgate.fundeli5.bankdata.json.Json.instant;
import static com.fundgate.fundeli5.bankdata.json.Json.integer;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredString;
import static com.fundgate.fundeli5.bankdata.json.Json.string;
import static com.fundgate.fundeli5.bankdata.json.Json.strings;

import com.fundgate.fundeli5.bankdata.Country;
import com.fundgate.fundeli5.bankdata.InvestorCategory;
import com.fundgate.fundeli5.bankdata.json.Json;
import com.fundgate.fundeli5.clientprofiles.ClientDataUnavailable;
import com.fundgate.fundeli5.clientprofiles.ClientProfile;
import com.fundgate.fundeli5.clientprofiles.ClientProfile.DeclarationType;
import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import com.fundgate.fundeli5.clientprofiles.PortfolioNotFound;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * Reads {@code client-profiles/<Portfolio Reference>.json} under the fixtures directory. Absent
 * fields stay absent; a legal category matching no value is left unknown (declarations: {@code OTHER}).
 */
public class FixtureClientProfiles implements ClientProfiles {

    private final Path directory;

    public FixtureClientProfiles(Path fixturesDir) {
        this.directory = fixturesDir.resolve("client-profiles");
    }

    @Override
    public Optional<PortfolioId> portfolioId(PortfolioReference ref) {
        return load(ref).map(json -> portfolioId(json));
    }

    @Override
    public ClientProfile clientProfile(PortfolioReference ref) {
        JsonNode json = load(ref).orElseThrow(() -> new PortfolioNotFound(ref));
        return new ClientProfile(
                new PortfolioReference(requiredString(json, "portfolioRef")),
                portfolioId(json),
                requiredString(json, "legalEntity"),
                instant(json, "retrievedAt"),
                client(child(json, "client")),
                classification(child(json, "classification")),
                portfolio(child(json, "portfolio")));
    }

    private Optional<JsonNode> load(PortfolioReference ref) {
        Path file = directory.resolve(ref.value() + ".json");
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Json.read(file));
        } catch (IOException | JacksonException e) {
            throw new ClientDataUnavailable("Cannot read client fixture " + file.getFileName(), e);
        }
    }

    private static PortfolioId portfolioId(JsonNode json) {
        return new PortfolioId(UUID.fromString(requiredString(json, "portfolioId")));
    }

    private static List<Country> countries(JsonNode node, String field) {
        List<String> codes = strings(node, field);
        return codes == null ? null : codes.stream().map(Country::new).toList();
    }

    private static ClientProfile.Client client(JsonNode client) {
        if (client == null) {
            return null;
        }
        JsonNode usPerson = child(client, "usPerson");
        List<JsonNode> knowledge = array(client, "knowledgeAndExperience");
        return new ClientProfile.Client(
                country(client, "residenceCountry"),
                countries(client, "nationalities"),
                countries(client, "taxResidences"),
                country(client, "birthCountry"),
                usPerson == null ? null : new ClientProfile.UsPerson(
                        enumOrNull(usPerson, "fatcaStatus", ClientProfile.FatcaStatus.class),
                        bool(usPerson, "usPermanentResident")),
                knowledge == null ? null : knowledge.stream()
                        .map(k -> new ClientProfile.KnowledgeAndExperience(bankCode(k, "productType"), bankCode(k, "level")))
                        .toList());
    }

    private static ClientProfile.Classification classification(JsonNode classification) {
        if (classification == null) {
            return null;
        }
        List<JsonNode> declarations = array(classification, "declarations");
        return new ClientProfile.Classification(
                enumOrNull(classification, "regime", ClientProfile.Regime.class),
                enumOrNull(classification, "category", InvestorCategory.class),
                date(classification, "classifiedOn"),
                declarations == null ? null : declarations.stream()
                        .map(d -> new ClientProfile.Declaration(
                                orOther(enumOrNull(d, "type", DeclarationType.class)), date(d, "date"), string(d, "label")))
                        .toList());
    }

    private static DeclarationType orOther(DeclarationType type) {
        return type == null ? DeclarationType.OTHER : type;
    }

    private static ClientProfile.PortfolioFacts portfolio(JsonNode portfolio) {
        if (portfolio == null) {
            return null;
        }
        JsonNode mandate = child(portfolio, "mandate");
        JsonNode manager = child(mandate, "manager");
        JsonNode profile = child(portfolio, "investmentProfile");
        return new ClientProfile.PortfolioFacts(
                currency(portfolio, "referenceCurrency"),
                mandate == null ? null : new ClientProfile.Mandate(
                        enumOrNull(mandate, "type", ClientProfile.MandateType.class),
                        date(mandate, "startDate"),
                        manager == null ? null : new ClientProfile.Manager(
                                enumOrNull(manager, "kind", ClientProfile.ManagerKind.class), string(manager, "name"))),
                profile == null ? null : new ClientProfile.InvestmentProfile(
                        bankCode(profile, "riskProfile"),
                        bankCode(profile, "abilityToBearLosses"),
                        integer(profile, "horizonYears"),
                        date(profile, "assessedOn")));
    }
}
