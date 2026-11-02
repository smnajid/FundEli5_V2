package com.fundgate.fundeli5.rulecardpipeline;

import static com.fundgate.fundeli5.bankdata.json.Json.requiredArray;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredBool;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredDouble;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredEnum;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredInt;
import static com.fundgate.fundeli5.bankdata.json.Json.requiredString;
import static com.fundgate.fundeli5.bankdata.json.Json.child;
import static com.fundgate.fundeli5.bankdata.json.Json.string;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.citation.Citation;
import com.fundgate.fundeli5.rulecard.Rule;
import com.fundgate.fundeli5.rulecard.json.RuleCardJson;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * The rules a model answered with. The answer is searched for every JSON object that holds a
 * {@code rules} array (a model may think aloud, fence its JSON, or answer more than once); the first
 * generation that parses into at least one rule wins. When none does, drafting fails loudly: a
 * silently saved card with 0 rules would look like a fund with no restriction.
 */
record DraftAnswer(String fundName, List<Rule> rules) {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    DraftAnswer {
        rules = List.copyOf(rules);
    }

    static DraftAnswer parse(String text, java.util.function.Consumer<DraftAnswer> accept) {
        List<String> problems = new ArrayList<>();
        int generations = 0;
        for (int start = text.indexOf('{'); start >= 0; start = text.indexOf('{', start + 1)) {
            JsonNode node = objectAt(text, start);
            if (node == null || !node.has("rules")) {
                continue;
            }
            generations++;
            try {
                DraftAnswer answer = read(node);
                if (!answer.rules().isEmpty()) {
                    accept.accept(answer);
                }
                if (answer.rules().isEmpty()) {
                    problems.add("generation " + generations + ": 0 rules");
                    continue;
                }
                return answer;
            } catch (RuntimeException e) {
                problems.add("generation " + generations + ": " + e.getMessage());
            }
        }
        throw new DraftingFailed(generations == 0
                ? "The answer holds no JSON object with a rules array"
                : "No generation of the answer parses into a Rule Card with rules (" + String.join("; ", problems) + ")");
    }

    /** The JSON object starting at {@code start}, or null when the text there is not one. */
    private static JsonNode objectAt(String text, int start) {
        int end = closingBrace(text, start);
        if (end < 0) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(text.substring(start, end + 1));
            return node != null && node.isObject() ? node : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Index of the brace closing the one at {@code start}, skipping strings; -1 when it never closes. */
    private static int closingBrace(String text, int start) {
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static DraftAnswer read(JsonNode node) {
        List<Rule> rules = new ArrayList<>();
        for (JsonNode r : requiredArray(node, "rules")) {
            Rule.Scope scope = requiredEnum(r, "scope", Rule.Scope.class);
            String shareClass = string(r, "shareClass");
            List<Citation> citations = new ArrayList<>();
            for (JsonNode c : requiredArray(r, "citations")) {
                citations.add(new Citation(requiredString(c, "document"), requiredInt(c, "page"),
                        requiredString(c, "quote"), false));
            }
            if (citations.isEmpty()) {
                throw new IllegalArgumentException("A rule without a citation");
            }
            rules.add(new Rule(scope, scope == Rule.Scope.SHARE_CLASS ? new Isin(shareClass) : null,
                    RuleCardJson.condition(proposed(child(r, "condition"))), citations, string(r, "summary"),
                    requiredDouble(r, "confidence"), requiredBool(r, "contradiction"),
                    List.of()));
        }
        return new DraftAnswer(string(node, "fundName"), rules);
    }

    /**
     * An unreviewed clause never passes silently (ADR 0003): whatever status the model wrote, an
     * unmodelled restriction is Blocking until a person decides, and carries no Noted reason.
     */
    private static JsonNode proposed(JsonNode condition) {
        if (condition != null && "unmodelled_restriction".equals(string(condition, "kind"))) {
            ObjectNode blocking = (ObjectNode) condition.deepCopy();
            blocking.put("status", "blocking");
            blocking.putNull("notedReason");
            return blocking;
        }
        if (condition != null && "target_market".equals(string(condition, "kind"))
                && condition.get("value") != null && condition.get("value").isNumber()) {
            // a model writes the SRI or the holding period as a number; the card keeps the value as text
            ObjectNode asText = (ObjectNode) condition.deepCopy();
            asText.put("value", condition.get("value").asText());
            return asText;
        }
        return condition;
    }
}
