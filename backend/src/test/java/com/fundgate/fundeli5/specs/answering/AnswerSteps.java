package com.fundgate.fundeli5.specs.answering;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.llm.LlmEgress;
import com.fundgate.fundeli5.llm.LlmRequest;
import com.fundgate.fundeli5.specs.SpyLlmEgress;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Steps of 02-answer-a-subscription-request.feature, driving the app over HTTP under /api. */
public class AnswerSteps {

    private static final String ANNA = """
            From: Marc Aebi (relationship manager)
            To: Fund dealing desk
            Subject: Subscription Nordea Global Climate - LOC90000101, LOC90000102

            Hi desk,
            The client would like to put EUR 80,000 into the Nordea Global Climate fund, institutional class,
            from portfolios LOC90000101 and LOC90000102. Can you confirm they can subscribe?
            Thanks, Marc""";

    private static final String CLAIRE = """
            De : Sophie Lentz (conseillère)
            À : Fund dealing desk
            Objet : Souscription Nordea Global Climate - LUX90000301

            Bonjour,
            Ma cliente (LUX90000301) souhaite investir 20 000 EUR dans Nordea 1 Global Climate and Environment.
            Pouvez-vous valider ?
            Merci, Sophie""";

    private static final String ANNA_NAMED = """
            From: Marc Aebi
            Subject: Nordea Global Climate for Anna Steinmann

            Hi desk, can Anna Steinmann (LOC90000101) subscribe EUR 80,000 to Nordea Global Climate BP-EUR?""";

    /** Demo short names (demo-dataset.md) and the portfolio ids of the fixtures. */
    private static final Map<String, String> ISIN = Map.of("GCE-BP", "LU0348926287", "GCE-BI", "LU0348927095");
    private static final Map<String, String> PORTFOLIO_ID = Map.of(
            "LOC90000101", "9ae403db-1754-4033-9f06-a99edf92a574",
            "LOC90000102", "216c5cc3-990a-4bec-adb7-212cbea7f259",
            "LUX90000301", "e1d33ef1-fa84-4778-ad55-8a13c0b9fa50");
    private static final Pattern REFERENCE = Pattern.compile("\\b[A-Z]{3}\\d{8}\\b");
    private static final Pattern UUID = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    @LocalServerPort
    int port;

    @Autowired
    LlmEgress egress;

    private RestClient http;
    private JsonNode answer;

    private SpyLlmEgress spy() {
        return (SpyLlmEgress) egress;
    }

    @Before
    public void freshScenario() {
        spy().reset();
        http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        answer = null;
    }

    // ---- Given

    @Given("the Requester's email:")
    public void theRequestersEmail(String email) {
        post(email);
    }

    @Given("Anna's email")
    public void annasEmail() {
        post(ANNA);
    }

    @Given("Claire's email")
    public void clairesEmail() {
        post(CLAIRE);
    }

    @Given("Anna's email has been answered")
    public void annasEmailHasBeenAnswered() {
        post(ANNA);
    }

    @Given("the Privacy Gate refused the email naming Anna Steinmann")
    public void theGateRefusedTheEmailNamingAnna() {
        post(ANNA_NAMED);
    }

    @Given("the LLM egress client cannot reach the model")
    public void theModelIsUnreachable() {
        spy().unreachable(true);
    }

    // ---- When

    @When("the Fund Dealer asks FundEli5 to answer it")
    public void theDealerAsksToAnswer() {
        // The Given stored the email's answer: the request is sent once, in the Given.
        assertThat(answer).as("an email was given").isNotNull();
    }

    @When("the Reply Drafter drafts the reply to Anna's email")
    public void theReplyDrafterDraftsAnnasReply() {
        post(ANNA);
    }

    @When("the Fund Dealer answers Anna's email entered by hand")
    public void theDealerAnswersAnnasEmailByHand() {
        spy().unreachable(false);
        JsonNode withLlm = postEmail(ANNA);
        spy().unreachable(true);
        spy().sent();
        JsonNode first = byHand("LOC90000101", "GCE-BI", 80000, false);
        JsonNode second = byHand("LOC90000102", "GCE-BI", 80000, false);
        verdictsWithLlm = verdicts(withLlm);
        answer = merge(first, second);
    }

    private List<String> verdictsWithLlm = List.of();

    @When("^the Fund Dealer enters ([A-Z0-9]+), ([A-Z0-9-]+), EUR ([\\d,]+), not client initiated$")
    public void theDealerEntersTheRequest(String portfolio, String shareClass, String amount) {
        answer = byHand(portfolio, shareClass, Integer.parseInt(amount.replace(",", "")), false);
    }

    @When("^a structured Subscription Request arrives for ([A-Z0-9]+), ISIN ([A-Z0-9]+), EUR ([\\d,]+)$")
    public void aStructuredRequestArrives(String portfolio, String isin, String amountText) {
        int amount = Integer.parseInt(amountText.replace(",", ""));
        spy().reset();
        answer = http.post().uri("/api/answers/by-hand").body(Map.of("portfolioReference", portfolio,
                "shareClassIsin", isin, "amount", amount, "currency", "EUR", "clientInitiated", false))
                .retrieve().body(JsonNode.class);
    }

    // ---- Then

    @Then("the Privacy Gate replaces {word} and {word} by their Portfolio Ids")
    public void theGateReplacesReferences(String first, String second) {
        String sent = String.join("\n", sentPrompts());
        assertThat(sent).contains(PORTFOLIO_ID.get(first), PORTFOLIO_ID.get(second)).doesNotContain(first, second);
    }

    @Then("no text sent to the LLM contains a Portfolio Reference or a client name")
    public void noTextSentContainsClientData() {
        assertThat(spy().sent()).as("the LLM was called").isNotEmpty();
        for (String text : sentTexts()) {
            assertThat(REFERENCE.matcher(text).find()).as("a Portfolio Reference in: " + text).isFalse();
            assertThat(text).doesNotContain("Steinmann", "Anna");
        }
    }

    @Then("the screen shows {string}")
    public void theScreenShows(String text) {
        assertThat(answer.path("screenNote").asString()).isEqualTo(text);
    }

    @Then("no LLM call is made")
    public void noLlmCallIsMade() {
        assertThat(spy().sent()).isEmpty();
    }

    @Then("the screen says no AI was used for this email, and shows the client name it found")
    public void theScreenSaysNoAiWasUsed() {
        assertThat(answer.path("aiUsed").asBoolean()).isFalse();
        assertThat(answer.path("screenNote").asString()).containsIgnoringCase("no AI was used");
        List<String> names = new ArrayList<>();
        answer.path("refusal").path("namesFound").forEach(n -> names.add(n.asString()));
        assertThat(names).contains("Anna Steinmann");
    }

    @Then("the Fund Dealer gets the request form to fill by hand")
    public void theDealerGetsTheFormByHand() {
        assertThat(answer.path("refusal").isObject()).as("the Gate refused the email").isTrue();
    }

    @Then("the Verdict is {}")
    public void theVerdictIs(String verdict) {
        String expected = verdict.toUpperCase().replace(' ', '_');
        assertThat(verdicts(answer)).isNotEmpty().allMatch(expected::equals);
    }

    @Then("the reply is the template for Eligible, with no LLM call")
    public void theReplyIsTheTemplateForEligible() {
        assertThat(spy().sent()).isEmpty();
        assertThat(answer.path("aiUsed").asBoolean()).isFalse();
        assertThat(answer.path("reply").asString()).contains("Eligible. The portfolio may subscribe.");
    }

    @Then("every client fact sent is on the allow-list")
    public void everyClientFactSentIsOnTheAllowList() {
        // The Request Parser sends the pseudonymised email: Portfolio Ids and the Requester's free text, no profile fact.
        for (JsonNode call : lastCheckRecord().path("llmCalls")) {
            for (JsonNode field : call.path("fieldsSent")) {
                assertThat(field.asString()).isIn("portfolioId", "freeText");
            }
        }
    }

    @Then("the Check Record lists exactly the fields sent")
    public void theCheckRecordListsTheFieldsSent() {
        JsonNode record = lastCheckRecord();
        assertThat(record.path("llmCalls")).isNotEmpty();
        assertThat(record.path("llmCalls").get(0).path("gatedPrompt").asString())
                .isEqualTo(spy().sent().get(0).prompt());
        assertThat(record.path("llmCalls").get(0).path("fieldsSent").toString())
                .isEqualTo("[\"portfolioId\",\"freeText\"]");
    }

    @Then("these Eligibility Checks run:")
    public void theseChecksRun(DataTable table) {
        List<String> actual = new ArrayList<>();
        answer.path("checks").forEach(c -> actual.add(c.path("portfolioReference").asString() + "|"
                + c.path("shareClassName").asString() + "|" + c.path("amount").asString() + "|"
                + verdictLabel(c.path("verdict").asString())));
        List<String> expected = new ArrayList<>();
        for (Map<String, String> row : table.asMaps()) {
            expected.add(row.get("portfolio") + "|" + shareClassName(row.get("Share Class")) + "|"
                    + row.get("amount").replaceAll("[^0-9]", "") + "|" + row.get("Verdict"));
        }
        assertThat(actual.stream().map(AnswerSteps::normaliseAmount).toList())
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Then("the Eligibility Check of {word} on {word} has no amount")
    public void theCheckHasNoAmount(String portfolio, String shareClass) {
        JsonNode check = findCheck(portfolio, shareClass);
        assertThat(check.path("amount").isNull() || check.path("amount").isMissingNode()).isTrue();
    }

    @Then("the {} rule gives {word}")
    public void theRuleGives(String rule, String result) {
        String kind = rule.toUpperCase().replace(' ', '_').replace('-', '_');
        List<String> results = new ArrayList<>();
        answer.path("checks").forEach(c -> c.path("outcomes").forEach(o -> {
            if (o.path("kind").asString().equals(kind)) {
                results.add(o.path("result").asString());
            }
        }));
        String expected = result.equals("Fail") ? "FAIL" : result.equals("Review") ? "REVIEW" : "PASS";
        assertThat(results).as("outcomes of the " + rule + " rule").contains(expected);
    }

    @Then("in the parsed request form these fields are editable:")
    public void theseFieldsAreEditable(DataTable table) {
        List<String> editable = new ArrayList<>();
        answer.path("editableFields").forEach(f -> editable.add(f.asString()));
        for (Map<String, String> row : table.asMaps()) {
            String key = switch (row.get("field")) {
                case "Portfolio Reference" -> "portfolioReference";
                case "Share Class" -> "shareClass";
                case "client initiated" -> "clientInitiated";
                default -> row.get("field");
            };
            assertThat(editable.contains(key)).as(row.get("field")).isEqualTo(row.get("editable").equals("yes"));
        }
    }

    @Then("the drafted reply names {word} and {word} and contains no Portfolio Id")
    public void theReplyNamesPortfolios(String first, String second) {
        String reply = answer.path("reply").asString();
        assertThat(reply).contains(first, second);
        assertThat(UUID.matcher(reply).find()).as("a Portfolio Id in the reply").isFalse();
    }

    @Then("the drafted reply gives each portfolio its Verdict")
    public void theReplyGivesEachItsVerdict() {
        String reply = answer.path("reply").asString();
        answer.path("checks").forEach(c -> assertThat(reply).contains(c.path("portfolioReference").asString())
                .contains(verdictLabel(c.path("verdict").asString())));
    }

    @Then("the drafted reply cites the investor category rule for {word}, with its page")
    public void theReplyCitesTheInvestorCategoryRule(String portfolio) {
        assertThat(answer.path("reply").asString()).containsIgnoringCase("investor category")
                .containsPattern("(?i)(p\\.|page)\\s*\\d+");
    }

    @Then("the drafted reply suggests {word} for {word}")
    public void theReplySuggests(String shareClass, String portfolio) {
        assertThat(answer.path("reply").asString()).contains(shareClassName(shareClass));
    }

    @Then("the drafted reply recommends {word}")
    public void theReplyRecommends(String shareClass) {
        assertThat(answer.path("reply").asString()).contains(shareClassName(shareClass));
    }

    @Then("the drafted reply is in French")
    public void theReplyIsInFrench() {
        assertThat(answer.path("reply").asString()).contains("Bonjour");
    }

    @Then("the Verdicts are the same as with the LLM")
    public void theVerdictsAreTheSame() {
        assertThat(verdicts(answer)).isEqualTo(verdictsWithLlm);
    }

    @Then("the reply is the template for each Verdict")
    public void theReplyIsTheTemplateForEachVerdict() {
        assertThat(spy().sent()).as("calls to the unreachable model after the first").hasSizeLessThanOrEqualTo(1);
        assertThat(answer.path("aiUsed").asBoolean()).isFalse();
        assertThat(answer.path("reply").asString()).isNotBlank();
    }

    // ---- helpers

    private void post(String email) {
        answer = postEmail(email);
    }

    private JsonNode postEmail(String email) {
        return http.post().uri("/api/answers").body(Map.of("email", email)).retrieve().body(JsonNode.class);
    }

    private JsonNode byHand(String portfolio, String shareClass, int amount, boolean clientInitiated) {
        return http.post().uri("/api/answers/by-hand").body(Map.of("portfolioReference", portfolio,
                "shareClassIsin", ISIN.get(shareClass), "amount", amount, "currency", "EUR",
                "clientInitiated", clientInitiated)).retrieve().body(JsonNode.class);
    }

    /** Two by-hand answers read as one: their checks, and the first reply. */
    private static JsonNode merge(JsonNode first, JsonNode second) {
        var mapper = JsonMapper.builder().build();
        var merged = (tools.jackson.databind.node.ObjectNode) first.deepCopy();
        ((tools.jackson.databind.node.ArrayNode) merged.withArray("checks")).addAll(
                (tools.jackson.databind.node.ArrayNode) second.path("checks"));
        merged.put("reply", first.path("reply").asString() + "\n" + second.path("reply").asString());
        return mapper.readTree(mapper.writeValueAsString(merged));
    }

    private List<String> sentPrompts() {
        return spy().sent().stream().map(LlmRequest::prompt).toList();
    }

    private List<String> sentTexts() {
        return spy().sent().stream().flatMap(r -> java.util.stream.Stream.of(r.system(), r.prompt())).toList();
    }

    private static List<String> verdicts(JsonNode answer) {
        List<String> verdicts = new ArrayList<>();
        answer.path("checks").forEach(c -> verdicts.add(c.path("verdict").asString()));
        return verdicts;
    }

    private JsonNode lastCheckRecord() {
        JsonNode records = http.get().uri("/api/check-records").retrieve().body(JsonNode.class);
        assertThat(records).as("Check Records").isNotEmpty();
        return records.get(records.size() - 1);
    }

    private JsonNode findCheck(String portfolio, String shareClass) {
        for (JsonNode c : answer.path("checks")) {
            if (c.path("portfolioReference").asString().equals(portfolio)
                    && c.path("shareClassName").asString().equals(shareClassName(shareClass))) {
                return c;
            }
        }
        throw new AssertionError("No Eligibility Check of " + portfolio + " on " + shareClass
                + "; unclear: " + answer.path("unclear"));
    }

    private static String shareClassName(String shortName) {
        return shortName.replaceFirst("^GCE-", "") + "-EUR";
    }

    private static String normaliseAmount(String row) {
        String[] parts = row.split("\\|");
        return parts[0] + "|" + parts[1] + "|" + parts[2].replaceAll("\\..*", "") + "|" + parts[3];
    }

    private static String verdictLabel(String verdict) {
        return switch (verdict) {
            case "ELIGIBLE" -> "Eligible";
            case "NOT_ELIGIBLE" -> "Not Eligible";
            default -> "Needs Review";
        };
    }
}
