package com.fundgate.fundeli5.rulecard.json;

import static com.fundgate.fundeli5.bankdata.json.Json.array;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredArray;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredBool;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredDouble;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredInt;
import static com.fundgate.fundeli5.bankdata.json.Json.child;
import static com.fundgate.fundeli5.bankdata.json.Json.currency;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredDecimal;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredStrings;
import static com.fundgate.fundeli5.bankdata.json.Json.editionDate;
import static com.fundgate.fundeli5.bankdata.json.Json.instant;
import static com.fundgate.fundeli5.bankdata.json.Json.isin;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredEnum;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredString;
import static com.fundgate.fundeli5.bankdata.json.Json.string;

import com.fundgate.fundeli5.bankdata.Confidentiality;
import com.fundgate.fundeli5.bankdata.InvestorType;
import com.fundgate.fundeli5.bankdata.json.Json;
import com.fundgate.fundeli5.citation.Citation;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.Rule;
import com.fundgate.fundeli5.rulecard.RuleCard;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reads a Rule Card from its JSON. Fails loudly on a missing required field, an unknown rule kind
 * or an unknown enum value: a card that quietly drops a rule would let a Verdict skip it.
 */
public final class RuleCardJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private RuleCardJson() {
    }

    public static RuleCard read(Path file) {
        try {
            return parse(Json.read(file));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read Rule Card " + file, e);
        }
    }

    public static RuleCard parse(JsonNode card) {
        return new RuleCard(
                string(card, "fundId"),
                requiredString(card, "fundName"),
                requiredStrings(card, "shareClasses").stream().map(com.fundgate.fundeli5.bankdata.Isin::new).toList(),
                requiredInt(card, "version"),
                requiredEnum(card, "status", RuleCard.Status.class),
                requiredArray(card, "rules").stream().map(RuleCardJson::rule).toList(),
                requiredArray(card, "sources").stream().map(RuleCardJson::source).toList(),
                drafting(child(card, "drafting")),
                validation(child(card, "validation")));
    }

    private static Rule rule(JsonNode rule) {
        return new Rule(
                requiredEnum(rule, "scope", Rule.Scope.class),
                isin(rule, "shareClass"),
                condition(child(rule, "condition")),
                requiredArray(rule, "citations").stream().map(RuleCardJson::citation).toList(),
                string(rule, "summary"),
                requiredDouble(rule, "confidence"),
                requiredBool(rule, "contradiction"),
                history(array(rule, "history")));
    }

    private static List<Rule.HistoryEntry> history(List<JsonNode> entries) {
        return entries == null ? List.of() : entries.stream()
                .map(h -> new Rule.HistoryEntry(
                        requiredString(h, "action"), string(h, "by"), instant(h, "at"), condition(child(h, "condition"))))
                .toList();
    }

    private static Citation citation(JsonNode citation) {
        return new Citation(
                requiredString(citation, "document"),
                requiredInt(citation, "page"),
                requiredString(citation, "quote"),
                requiredBool(citation, "quoteFoundOnPage"));
    }

    /** A condition from its JSON; fails loudly on an unknown kind or a missing field. */
    public static Condition condition(JsonNode condition) {
        String kind = requiredString(condition, "kind");
        return switch (kind) {
            case "investor_category" -> new Condition.InvestorCategory(investorTypes(condition), requiredStrings(condition, "excluded"));
            case "us_person_exclusion" -> new Condition.UsPersonExclusion(
                    requiredEnum(condition, "definition", Condition.UsPersonDefinition.class),
                    requiredBool(condition, "excludesUsCitizens"));
            case "minimum_initial_subscription" -> new Condition.MinimumInitialSubscription(
                    requiredDecimal(condition, "amount"), currency(condition, "currency"));
            case "mandate_only_class" -> new Condition.MandateOnlyClass(string(condition, "manager"));
            case "kid_required_for_retail" -> new Condition.KidRequiredForRetail(requiredBool(condition, "kidAvailable"));
            case "target_market" -> new Condition.TargetMarket(
                    requiredEnum(condition, "dimension", Condition.Dimension.class), requiredString(condition, "value"));
            case "unmodelled_restriction" -> new Condition.UnmodelledRestriction(
                    requiredEnum(condition, "status", Condition.RestrictionStatus.class), string(condition, "notedReason"));
            default -> throw new IllegalArgumentException("Unknown rule kind: " + kind);
        };
    }

    private static Set<InvestorType> investorTypes(JsonNode condition) {
        Set<InvestorType> allowed = new HashSet<>();
        for (String value : requiredStrings(condition, "allowed")) {
            InvestorType type = Json.enumOrNull(InvestorType.class, value);
            if (type == null) {
                throw new IllegalArgumentException("Unknown investor type: " + value);
            }
            allowed.add(type);
        }
        return allowed;
    }

    private static RuleCard.Source source(JsonNode source) {
        return new RuleCard.Source(
                requiredString(source, "documentId"),
                requiredString(source, "contentSha256"),
                editionDate(source, "fmdEditionDate"),
                editionDate(source, "pdfEditionDate"),
                Json.enumOrNull(source, "confidentiality", Confidentiality.class));
    }

    private static RuleCard.Drafting drafting(JsonNode drafting) {
        return drafting == null ? null
                : new RuleCard.Drafting(string(drafting, "model"), string(drafting, "endpoint"), instant(drafting, "draftedAt"));
    }

    private static RuleCard.Validation validation(JsonNode validation) {
        return validation == null ? null
                : new RuleCard.Validation(string(validation, "validator"), instant(validation, "validatedAt"));
    }

    /** The card as JSON, in the shape {@link #parse} reads back. */
    public static ObjectNode write(RuleCard card) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("fundId", card.fundId());
        node.put("fundName", card.fundName());
        ArrayNode classes = node.putArray("shareClasses");
        card.shareClasses().forEach(c -> classes.add(c.value()));
        node.put("version", card.version());
        node.put("status", lower(card.status()));
        ArrayNode rules = node.putArray("rules");
        card.rules().forEach(r -> rules.add(writeRule(r)));
        ArrayNode sources = node.putArray("sources");
        card.sources().forEach(src -> {
            ObjectNode o = sources.addObject();
            o.put("documentId", src.documentId());
            o.put("contentSha256", src.contentSha256());
            o.put("fmdEditionDate", src.fmdEditionDate() == null ? null : src.fmdEditionDate().value());
            o.put("pdfEditionDate", src.pdfEditionDate() == null ? null : src.pdfEditionDate().value());
            o.put("confidentiality", src.confidentiality() == null ? null : lower(src.confidentiality()));
        });
        if (card.drafting() == null) {
            node.putNull("drafting");
        } else {
            ObjectNode o = node.putObject("drafting");
            o.put("model", card.drafting().model());
            o.put("endpoint", card.drafting().endpoint());
            o.put("draftedAt", card.drafting().draftedAt() == null ? null : card.drafting().draftedAt().toString());
        }
        if (card.validation() == null) {
            node.putNull("validation");
        } else {
            ObjectNode o = node.putObject("validation");
            o.put("validator", card.validation().validator());
            o.put("validatedAt", card.validation().validatedAt() == null ? null : card.validation().validatedAt().toString());
        }
        return node;
    }

    private static ObjectNode writeRule(Rule rule) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("scope", lower(rule.scope()));
        node.put("shareClass", rule.shareClass() == null ? null : rule.shareClass().value());
        node.set("condition", writeCondition(rule.condition()));
        ArrayNode citations = node.putArray("citations");
        rule.citations().forEach(c -> {
            ObjectNode o = citations.addObject();
            o.put("document", c.document());
            o.put("page", c.page());
            o.put("quote", c.quote());
            o.put("quoteFoundOnPage", c.quoteFoundOnPage());
        });
        node.put("summary", rule.summary());
        node.put("confidence", rule.confidence());
        node.put("contradiction", rule.contradiction());
        ArrayNode history = node.putArray("history");
        rule.history().forEach(h -> {
            ObjectNode o = history.addObject();
            o.put("action", h.action());
            o.put("by", h.by());
            o.put("at", h.at() == null ? null : h.at().toString());
            o.set("condition", writeCondition(h.condition()));
        });
        return node;
    }

    public static JsonNode writeCondition(Condition condition) {
        if (condition == null) {
            return MAPPER.nullNode();
        }
        ObjectNode node = MAPPER.createObjectNode();
        node.put("kind", lower(condition.kind()));
        switch (condition) {
            case Condition.InvestorCategory c -> {
                ArrayNode allowed = node.putArray("allowed");
                c.allowed().stream().map(RuleCardJson::lower).sorted().forEach(allowed::add);
                ArrayNode excluded = node.putArray("excluded");
                c.excluded().forEach(excluded::add);
            }
            case Condition.UsPersonExclusion c -> {
                node.put("definition", lower(c.definition()));
                node.put("excludesUsCitizens", c.excludesUsCitizens());
            }
            case Condition.MinimumInitialSubscription c -> {
                node.put("amount", c.amount());
                node.put("currency", c.currency().code());
            }
            case Condition.MandateOnlyClass c -> node.put("manager", c.manager());
            case Condition.KidRequiredForRetail c -> node.put("kidAvailable", c.kidAvailable());
            case Condition.TargetMarket c -> {
                node.put("dimension", lower(c.dimension()));
                node.put("value", c.value());
            }
            case Condition.UnmodelledRestriction c -> {
                node.put("status", lower(c.status()));
                node.put("notedReason", c.notedReason());
            }
        }
        return node;
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(java.util.Locale.ROOT);
    }
}
