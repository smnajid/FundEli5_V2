package com.fundgate.fundeli5.privacygate.presidio;

import com.fundgate.fundeli5.privacygate.DetectedSpan;
import com.fundgate.fundeli5.privacygate.PersonalDataDetector;
import com.fundgate.fundeli5.privacygate.PersonalDataDetectorUnavailable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Presidio analyzer over HTTP ({@code POST <baseUrl>/analyze}, ADR 0009). Asks for PERSON in each
 * configured language and returns the union. Any failure (unreachable, non-200, unreadable body,
 * a language the analyzer does not support) is {@link PersonalDataDetectorUnavailable}: the Gate fails closed.
 */
public class PresidioPersonalDataDetector implements PersonalDataDetector {

    private final URI analyze;
    private final List<String> languages;
    private final HttpClient http;
    private final Duration timeout;
    private final JsonMapper json = JsonMapper.builder().build();

    public PresidioPersonalDataDetector(String baseUrl, List<String> languages, Duration timeout) {
        if (languages.isEmpty()) {
            throw new IllegalArgumentException("At least one language");
        }
        URI base = URI.create(baseUrl);
        if (!"http".equals(base.getScheme()) && !"https".equals(base.getScheme())) {
            throw new IllegalArgumentException("The Presidio URL must be http or https");
        }
        this.analyze = URI.create(baseUrl.replaceAll("/+$", "") + "/analyze");
        this.languages = List.copyOf(languages);
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(timeout).build();
    }

    @Override
    public List<DetectedSpan> detect(String text) {
        List<DetectedSpan> spans = new ArrayList<>();
        for (String language : languages) {
            spans.addAll(analyze(text, language));
        }
        return List.copyOf(spans);
    }

    private List<DetectedSpan> analyze(String text, String language) {
        try {
            String body = json.writeValueAsString(
                    java.util.Map.of("text", text, "language", language, "entities", List.of(DetectedSpan.PERSON)));
            HttpRequest request = HttpRequest.newBuilder(analyze).timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new PersonalDataDetectorUnavailable("Presidio answered " + response.statusCode()
                        + " for language " + language);
            }
            JsonNode array = json.readTree(response.body());
            if (!array.isArray()) {
                throw new PersonalDataDetectorUnavailable("Presidio answered something other than a list");
            }
            List<DetectedSpan> spans = new ArrayList<>();
            for (JsonNode item : array) {
                if (!item.path("entity_type").isString() || !item.path("start").isInt()
                        || !item.path("end").isInt() || !item.path("score").isNumber()) {
                    throw new PersonalDataDetectorUnavailable("Presidio answered an item of an unknown shape");
                }
                spans.add(new DetectedSpan(item.path("entity_type").asString(), item.path("start").asInt(),
                        item.path("end").asInt(), item.path("score").asDouble()));
            }
            return spans;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PersonalDataDetectorUnavailable("Interrupted while calling Presidio");
        } catch (IOException | RuntimeException e) {
            if (e instanceof PersonalDataDetectorUnavailable unavailable) {
                throw unavailable;
            }
            throw new PersonalDataDetectorUnavailable("Presidio cannot be used: " + e.getClass().getSimpleName());
        }
    }
}
