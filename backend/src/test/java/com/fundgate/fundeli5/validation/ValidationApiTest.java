package com.fundgate.fundeli5.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "fundeli5.llm.endpoint=replay")
class ValidationApiTest {

    @TempDir
    static Path out;

    @DynamicPropertySource
    static void work(DynamicPropertyRegistry registry) {
        registry.add("fundeli5.validation.out", () -> out.toString());
        registry.add("fundeli5.validation.drafts", () -> "../docs/playbook/contracts/fixtures/draft-rule-cards");
    }

    @LocalServerPort
    int port;

    static final String FUND = "Nordea%201%20%E2%80%93%20Global%20Climate%20and%20Environment%20Fund";
    final HttpClient http = HttpClient.newHttpClient();
    final JsonMapper json = JsonMapper.builder().build();

    @Test
    void aValidatorWalksADraftToValidated() throws Exception {
        JsonNode drafts = get("/api/validation/drafts");
        assertThat(drafts).hasSize(1);
        String fund = drafts.get(0).get("fundName").asString();

        JsonNode review = get("/api/validation/review?fundName=" + FUND + "&version=1");
        assertThat(review.get("canValidate").asBoolean()).isFalse();
        assertThat(review.get("contradictions")).hasSize(1);

        HttpResponse<String> refused = post("/api/validation/validate", body(fund, "\"validator\":\"V\""));
        assertThat(refused.statusCode()).isEqualTo(409);

        post("/api/validation/follow", body(fund, "\"shareClass\":\"LU0348927095\",\"kind\":\"INVESTOR_CATEGORY\","
                + "\"document\":\"nordea1-prospectus-2026-06\",\"validator\":\"V\""));
        post("/api/validation/mark", body(fund, "\"rule\":2,\"status\":\"noted\",\"reason\":\"no condition on client\","
                + "\"validator\":\"V\""));
        JsonNode now = get("/api/validation/review?fundName=" + FUND + "&version=1");
        for (JsonNode i : now.get("unreviewedRules")) {
            post("/api/validation/accept", body(fund, "\"rule\":" + i.asInt() + ",\"validator\":\"V\""));
        }

        HttpResponse<String> validated = post("/api/validation/validate", body(fund, "\"validator\":\"V\""));
        assertThat(validated.statusCode()).isEqualTo(200);
        assertThat(json.readTree(validated.body()).get("status").asString()).isEqualTo("validated");
        assertThat(get("/api/validation/drafts")).isEmpty();
    }

    @Test
    void theReviewIsTypedAndACorrectionTakesTheSameConditionShape() throws Exception {
        String fund = get("/api/validation/drafts").get(0).get("fundName").asString();
        JsonNode rules = get("/api/validation/review?fundName=" + FUND + "&version=1").get("card").get("rules");
        int minimum = -1;
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).get("condition").get("kind").asString().equals("minimum_initial_subscription")) {
                minimum = i;
            }
        }
        assertThat(minimum).isGreaterThanOrEqualTo(0);
        HttpResponse<String> corrected = post("/api/validation/correct", body(fund, "\"rule\":" + minimum
                + ",\"condition\":{\"kind\":\"minimum_initial_subscription\",\"amount\":123456,\"currency\":\"EUR\"},"
                + "\"validator\":\"V\""));
        assertThat(corrected.statusCode()).isEqualTo(200);
        JsonNode condition = json.readTree(corrected.body()).get("card").get("rules").get(minimum).get("condition");
        assertThat(condition.get("amount").asInt()).isEqualTo(123456);
        assertThat(condition.has("allowed")).isTrue();
    }

    private String body(String fund, String rest) {
        return "{\"fundName\":\"" + fund + "\",\"version\":1," + rest + "}";
    }

    private JsonNode get(String path) throws Exception {
        return json.readTree(http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString()).body());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
