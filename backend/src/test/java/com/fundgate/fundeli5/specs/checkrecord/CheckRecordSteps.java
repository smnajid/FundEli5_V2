package com.fundgate.fundeli5.specs.checkrecord;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.specs.SpyLlmEgress;
import com.fundgate.fundeli5.llm.LlmEgress;
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

/** Steps of 07-audit-a-check.feature, driving the app over HTTP under /api. */
public class CheckRecordSteps {

    private static final String ANNA = """
            From: Marc Aebi (relationship manager)
            To: Fund dealing desk
            Subject: Subscription Nordea Global Climate - LOC90000101, LOC90000102

            Hi desk,
            The client would like to put EUR 80,000 into the Nordea Global Climate fund, institutional class,
            from portfolios LOC90000101 and LOC90000102. Can you confirm they can subscribe?
            Thanks, Marc""";

    private static final String ANNA_NAMED = """
            From: Marc Aebi
            Subject: Nordea Global Climate for Anna Steinmann

            Hi desk, can Anna Steinmann (LOC90000101) subscribe EUR 80,000 to Nordea Global Climate BP-EUR?""";

    private static final Map<String, String> ISIN = Map.of("GCE-BP", "LU0348926287", "GCE-BI", "LU0348927095");
    private static final Pattern REFERENCE = Pattern.compile("\\b[A-Z]{3}\\d{8}\\b");

    @LocalServerPort
    int port;

    @Autowired
    LlmEgress egress;

    private RestClient http;
    /** Check Records present before the scenario acted: only the ones it wrote are looked at. */
    private int before;
    private JsonNode firstRecord;

    @Before
    public void freshScenario() {
        ((SpyLlmEgress) egress).reset();
        http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        before = records().size();
        firstRecord = null;
    }

    // ---- Given

    @Given("a Check Record for LOC90000101 on GCE-BI")
    public void aCheckRecordExists() {
        byHand("LOC90000101", "GCE-BI");
        firstRecord = written().get(0);
    }

    // ---- When

    @When("the Fund Dealer answers Anna's email")
    public void theDealerAnswersAnnasEmail() {
        email(ANNA);
    }

    @When("the Privacy Gate refuses the email naming Anna Steinmann")
    public void theGateRefuses() {
        JsonNode answer = email(ANNA_NAMED);
        assertThat(answer.path("refusal").isObject()).as("the Gate refused").isTrue();
    }

    @When("the same check is run again")
    public void theSameCheckRunsAgain() {
        byHand("LOC90000101", "GCE-BI");
    }

    @When("the Fund Dealer changes the currency of LOC90000101's request from EUR to CHF")
    public void theDealerChangesTheCurrency() {
        JsonNode record = written().stream().filter(r -> r.path("portfolioReference").asString().equals("LOC90000101"))
                .findFirst().orElseThrow();
        http.post().uri("/api/check-records/{id}/corrections", record.path("id").asString())
                .body(Map.of("field", "currency", "parsedValue", "EUR", "dealerValue", "CHF", "by", "fund-dealer"))
                .retrieve().toBodilessEntity();
    }

    // ---- Then

    @Then("two Check Records are written, one for LOC90000101 and one for LOC90000102")
    public void twoRecordsAreWritten() {
        assertThat(written().stream().map(r -> r.path("portfolioReference").asString()))
                .containsExactlyInAnyOrder("LOC90000101", "LOC90000102");
    }

    @Then("each Check Record holds:")
    public void eachRecordHolds(DataTable table) {
        assertThat(written()).isNotEmpty();
        for (JsonNode r : written()) {
            for (List<String> row : table.asLists()) {
                switch (row.get(0)) {
                    case "the Subscription Request" -> {
                        assertThat(r.path("shareClassIsin").asString()).isNotBlank();
                        assertThat(r.path("amount").isNumber()).isTrue();
                        assertThat(r.path("currency").asString()).isNotBlank();
                    }
                    case "the Rule Card version" -> assertThat(r.path("ruleCardVersion").isInt()).isTrue();
                    case "the Scale Mapping version" -> assertThat(r.path("scaleMappingVersion").asString()).isNotBlank();
                    case "the Client Profile fields the check read" -> assertThat(stringList(r.path("clientFieldsRead")))
                            .contains("portfolioRef.value", "legalEntity");
                    case "the engine version" -> assertThat(r.path("engineVersion").asString()).isNotBlank();
                    case "the Verdict and the Justification" -> {
                        assertThat(r.path("verdict").asString()).isNotBlank();
                        assertThat(r.path("outcomes")).isNotEmpty();
                    }
                    default -> throw new AssertionError("Unknown Check Record content: " + row.get(0));
                }
            }
        }
    }

    @Then("the Check Records hold, for each LLM call, the gated prompt, the client fields sent, the response and the model id")
    public void theRecordsHoldEachLlmCall() {
        assertThat(written()).isNotEmpty();
        for (JsonNode r : written()) {
            assertThat(r.path("llmCalls")).isNotEmpty();
            for (JsonNode call : r.path("llmCalls")) {
                assertThat(call.path("gatedPrompt").asString()).isNotBlank();
                assertThat(call.path("fieldsSent").isArray()).isTrue();
                assertThat(call.path("response").asString()).isNotBlank();
                assertThat(call.path("model").asString()).isNotBlank();
            }
        }
        List<String> sent = ((SpyLlmEgress) egress).sent().stream().map(q -> q.prompt()).toList();
        for (JsonNode call : written().get(0).path("llmCalls")) {
            assertThat(sent).contains(call.path("gatedPrompt").asString());
        }
    }

    @Then("no recorded prompt contains a Portfolio Reference or a client name")
    public void noRecordedPromptHoldsClientData() {
        for (JsonNode r : written()) {
            for (JsonNode call : r.path("llmCalls")) {
                String prompt = call.path("gatedPrompt").asString();
                assertThat(REFERENCE.matcher(prompt).find()).as("a Portfolio Reference in: " + prompt).isFalse();
                assertThat(prompt).doesNotContain("Steinmann", "Anna");
            }
        }
    }

    @Then("the Check Record of the request entered by hand holds no LLM call")
    public void theHandRecordHoldsNoLlmCall() {
        assertThat(written()).as("a refused email writes no record").isEmpty();
        byHand("LOC90000101", "GCE-BI");
        assertThat(written()).hasSize(1);
        assertThat(written().get(0).path("llmCalls")).isEmpty();
        assertThat(((SpyLlmEgress) egress).sent()).isEmpty();
    }

    @Then("a second Check Record is written")
    public void aSecondRecordIsWritten() {
        assertThat(written()).hasSize(2);
        assertThat(written().get(1).path("id").asString()).isNotEqualTo(written().get(0).path("id").asString());
    }

    @Then("the first one is unchanged")
    public void theFirstIsUnchanged() {
        assertThat(written().get(0)).isEqualTo(firstRecord);
    }

    @Then("the Check Record keeps both the parsed currency EUR and the dealer's CHF, with who and when")
    public void theRecordKeepsBothCurrencies() {
        JsonNode record = written().stream().filter(r -> r.path("portfolioReference").asString().equals("LOC90000101"))
                .findFirst().orElseThrow();
        assertThat(record.path("currency").asString()).as("what the AI read is untouched").isEqualTo("EUR");
        JsonNode correction = record.path("corrections").get(0);
        assertThat(correction.path("field").asString()).isEqualTo("currency");
        assertThat(correction.path("parsedValue").asString()).isEqualTo("EUR");
        assertThat(correction.path("dealerValue").asString()).isEqualTo("CHF");
        assertThat(correction.path("by").asString()).isNotBlank();
        assertThat(correction.path("at").asString()).isNotBlank();
    }

    // ---- helpers

    private JsonNode email(String text) {
        return http.post().uri("/api/answers").body(Map.of("email", text)).retrieve().body(JsonNode.class);
    }

    private void byHand(String portfolio, String shareClass) {
        http.post().uri("/api/answers/by-hand").body(Map.of("portfolioReference", portfolio,
                "shareClassIsin", ISIN.get(shareClass), "amount", 80000, "currency", "EUR", "clientInitiated", false))
                .retrieve().toBodilessEntity();
    }

    private JsonNode records() {
        return http.get().uri("/api/check-records").retrieve().body(JsonNode.class);
    }

    /** The records written since the scenario began. */
    private List<JsonNode> written() {
        List<JsonNode> all = new ArrayList<>();
        records().forEach(all::add);
        return all.subList(before, all.size());
    }

    private static List<String> stringList(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.asString()));
        return out;
    }
}
