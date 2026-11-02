package com.fundgate.fundeli5.validation.web;

import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.Rule;
import com.fundgate.fundeli5.rulecard.RuleCard;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * A Rule Card as the validation screen reads it, mirroring rule-card.schema.json. The condition is one flat
 * record whose {@code kind} says which of the other fields apply, so the generated client needs no
 * discriminator. Wire format is the same as the card's JSON.
 */
record RuleCardView(String fundId, String fundName, List<String> shareClasses, int version,
        @Schema(allowableValues = {"draft", "validated"}, requiredMode = Schema.RequiredMode.REQUIRED) String status,
        List<RuleView> rules, List<SourceView> sources, DraftingView drafting, ValidationStampView validation) {

    record RuleView(@Schema(allowableValues = {"fund", "share_class"}, requiredMode = Schema.RequiredMode.REQUIRED) String scope,
            String shareClass, ConditionView condition, List<CitationView> citations, String summary, double confidence,
            boolean contradiction, List<HistoryView> history) {
    }

    /** Fields by kind: investor_category: allowed, excluded; us_person_exclusion: definition, excludesUsCitizens;
     * minimum_initial_subscription: amount, currency; mandate_only_class: manager; kid_required_for_retail: kidAvailable;
     * target_market: dimension, value; unmodelled_restriction: status, notedReason. */
    record ConditionView(
            @Schema(allowableValues = {"investor_category", "us_person_exclusion", "minimum_initial_subscription",
                    "mandate_only_class", "kid_required_for_retail", "target_market", "unmodelled_restriction"},
                    requiredMode = Schema.RequiredMode.REQUIRED) String kind,
            List<String> allowed, List<String> excluded, String definition, Boolean excludesUsCitizens, BigDecimal amount,
            String currency, String manager, Boolean kidAvailable, String dimension, String value, String status,
            String notedReason) {
    }

    record CitationView(String document, int page, String quote, boolean quoteFoundOnPage) {
    }

    record HistoryView(String action, String by, String at, ConditionView condition) {
    }

    record SourceView(String documentId, String contentSha256, String fmdEditionDate, String pdfEditionDate,
            String confidentiality) {
    }

    record DraftingView(String model, String endpoint, String draftedAt) {
    }

    record ValidationStampView(String validator, String validatedAt) {
    }

    static RuleCardView of(RuleCard c) {
        return new RuleCardView(c.fundId(), c.fundName(), c.shareClasses().stream().map(i -> i.value()).toList(),
                c.version(), lower(c.status()), c.rules().stream().map(RuleCardView::rule).toList(),
                c.sources().stream().map(s -> new SourceView(s.documentId(), s.contentSha256(),
                        s.fmdEditionDate() == null ? null : s.fmdEditionDate().value(),
                        s.pdfEditionDate() == null ? null : s.pdfEditionDate().value(),
                        s.confidentiality() == null ? null : lower(s.confidentiality()))).toList(),
                c.drafting() == null ? null : new DraftingView(c.drafting().model(), c.drafting().endpoint(),
                        str(c.drafting().draftedAt())),
                c.validation() == null ? null : new ValidationStampView(c.validation().validator(),
                        str(c.validation().validatedAt())));
    }

    private static RuleView rule(Rule r) {
        return new RuleView(lower(r.scope()), r.shareClass() == null ? null : r.shareClass().value(),
                condition(r.condition()),
                r.citations().stream().map(x -> new CitationView(x.document(), x.page(), x.quote(), x.quoteFoundOnPage()))
                        .toList(),
                r.summary(), r.confidence(), r.contradiction(),
                r.history().stream().map(h -> new HistoryView(h.action(), h.by(), str(h.at()), condition(h.condition())))
                        .toList());
    }

    static ConditionView condition(Condition condition) {
        if (condition == null) {
            return null;
        }
        String kind = lower(condition.kind());
        return switch (condition) {
            case Condition.InvestorCategory c -> new ConditionView(kind,
                    c.allowed().stream().map(RuleCardView::lower).sorted().toList(), c.excluded(), null, null, null, null,
                    null, null, null, null, null, null);
            case Condition.UsPersonExclusion c -> new ConditionView(kind, null, null, lower(c.definition()),
                    c.excludesUsCitizens(), null, null, null, null, null, null, null, null);
            case Condition.MinimumInitialSubscription c -> new ConditionView(kind, null, null, null, null, c.amount(),
                    c.currency().code(), null, null, null, null, null, null);
            case Condition.MandateOnlyClass c -> new ConditionView(kind, null, null, null, null, null, null, c.manager(),
                    null, null, null, null, null);
            case Condition.KidRequiredForRetail c -> new ConditionView(kind, null, null, null, null, null, null, null,
                    c.kidAvailable(), null, null, null, null);
            case Condition.TargetMarket c -> new ConditionView(kind, null, null, null, null, null, null, null, null,
                    lower(c.dimension()), c.value(), null, null);
            case Condition.UnmodelledRestriction c -> new ConditionView(kind, null, null, null, null, null, null, null,
                    null, null, null, lower(c.status()), c.notedReason());
        };
    }

    /** The condition JSON of the Rule Card schema, without the fields this kind does not use. */
    static tools.jackson.databind.JsonNode toJson(ConditionView condition) {
        tools.jackson.databind.node.ObjectNode node = MAPPER.valueToTree(condition);
        node.values().removeIf(tools.jackson.databind.JsonNode::isNull);
        return node;
    }

    private static final tools.jackson.databind.json.JsonMapper MAPPER = tools.jackson.databind.json.JsonMapper.builder().build();

    private static String str(java.time.Instant at) {
        return at == null ? null : at.toString();
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
