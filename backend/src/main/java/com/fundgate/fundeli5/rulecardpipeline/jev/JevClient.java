package com.fundgate.fundeli5.rulecardpipeline.jev;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * TypeSafe Jev over HTTPS, no SDK: {@code POST <baseUrl>/v1/systemone} with a bearer key and
 * {@code {state, model, questions: {id: {type, instructions, criteria}}}}. The reply holds
 * {@code questions: {id: {probabilities: {option: p}, confidence}}}. Only public document text is
 * ever the state. A missing or bad key is a 403 here, not a 401.
 */
public class JevClient {

    private final URI endpoint;
    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final HttpClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public JevClient(String baseUrl, String apiKey, String model, Duration timeout) {
        URI base = URI.create(baseUrl);
        boolean loopback = "127.0.0.1".equals(base.getHost()) || "localhost".equals(base.getHost());
        if (!"https".equals(base.getScheme()) && !("http".equals(base.getScheme()) && loopback)) {
            throw new IllegalArgumentException("The Jev URL must be https (http only for localhost)");
        }
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/v1/systemone");
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(timeout).build();
    }

    /** Asks the questions about the state; the answers are keyed by question id. */
    public Map<String, JevAnswer> ask(String state, Map<String, JevQuestion> questions) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("state", state);
            body.put("model", model);
            body.put("questions", wire(questions));
            HttpRequest.Builder request = HttpRequest.newBuilder(endpoint).timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            if (!apiKey.isBlank()) {
                request.header("Authorization", "Bearer " + apiKey);
            }
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 403) {
                throw new JevUnavailable("Jev refused the key (403): set TYPESAFE_API_KEY");
            }
            if (response.statusCode() != 200) {
                throw new JevUnavailable("Jev answered " + response.statusCode());
            }
            return parse(json.readTree(response.body()), questions);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JevUnavailable("Interrupted while calling Jev");
        } catch (JevUnavailable e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new JevUnavailable("Jev cannot be used: " + e.getClass().getSimpleName());
        }
    }

    /** The API takes the options of a question as a dictionary (option to description), not a list. */
    private static Map<String, Object> wire(Map<String, JevQuestion> questions) {
        Map<String, Object> out = new LinkedHashMap<>();
        questions.forEach((id, q) -> {
            Map<String, String> criteria = new LinkedHashMap<>();
            q.criteria().forEach(option -> criteria.put(option, option));
            Map<String, Object> question = new LinkedHashMap<>();
            question.put("type", q.type());
            question.put("instructions", q.instructions());
            question.put("criteria", criteria);
            out.put(id, question);
        });
        return out;
    }

    /** Answers sit under {@code answers}: a choice gives probabilities and confidence; a noul gives P(yes) alone. */
    private static Map<String, JevAnswer> parse(JsonNode root, Map<String, JevQuestion> asked) {
        JsonNode answers = root.path("answers");
        Map<String, JevAnswer> out = new LinkedHashMap<>();
        for (String id : asked.keySet()) {
            JsonNode answer = answers.path(id);
            if ("noul".equals(asked.get(id).type()) && answer.path("noul").isNumber()) {
                double yes = answer.path("noul").asDouble();
                out.put(id, new JevAnswer(Map.of("yes", yes, "no", 1 - yes), Math.max(yes, 1 - yes)));
                continue;
            }
            JsonNode probabilities = answer.path("probabilities");
            if (!probabilities.isObject() || !answer.path("confidence").isNumber()) {
                throw new JevUnavailable("Jev answered question " + id + " in an unknown shape");
            }
            Map<String, Double> map = new LinkedHashMap<>();
            for (String option : probabilities.propertyNames()) {
                if (!probabilities.path(option).isNumber()) {
                    throw new JevUnavailable("Jev answered question " + id + " in an unknown shape");
                }
                map.put(option, probabilities.path(option).asDouble());
            }
            out.put(id, new JevAnswer(map, answer.path("confidence").asDouble()));
        }
        return out;
    }
}
