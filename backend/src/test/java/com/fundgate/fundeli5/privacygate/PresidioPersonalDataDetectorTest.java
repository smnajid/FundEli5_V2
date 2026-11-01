package com.fundgate.fundeli5.privacygate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fundgate.fundeli5.privacygate.presidio.PresidioPersonalDataDetector;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The Presidio adapter against a stubbed HTTP server: no Docker. */
class PresidioPersonalDataDetectorTest {

    private HttpServer server;
    private final List<String> bodies = new ArrayList<>();
    private volatile int status = 200;
    private volatile String reply = "[]";

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/analyze", exchange -> {
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = reply.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private PresidioPersonalDataDetector detector(String... languages) {
        return new PresidioPersonalDataDetector("http://127.0.0.1:" + server.getAddress().getPort(),
                List.of(languages), Duration.ofSeconds(2));
    }

    @Test
    void sendsTextLanguageAndPersonAndReadsTheSpans() {
        reply = "[{\"entity_type\":\"PERSON\",\"start\":4,\"end\":10,\"score\":0.85}]";
        var spans = detector("en").detect("Hi, Anna.");
        assertThat(spans).containsExactly(new DetectedSpan("PERSON", 4, 10, 0.85));
        assertThat(bodies).singleElement().satisfies(b -> assertThat(b)
                .contains("\"language\":\"en\"").contains("\"text\":\"Hi, Anna.\"").contains("PERSON"));
    }

    @Test
    void asksOncePerLanguageAndUnitesTheAnswers() {
        reply = "[{\"entity_type\":\"PERSON\",\"start\":0,\"end\":3,\"score\":0.5}]";
        assertThat(detector("en", "fr", "de").detect("Tom")).hasSize(3);
        assertThat(bodies).hasSize(3);
    }

    @Test
    void anErrorStatusFailsClosed() {
        status = 500;
        assertThatThrownBy(() -> detector("en").detect("x")).isInstanceOf(PersonalDataDetectorUnavailable.class);
    }

    @Test
    void anUnsupportedLanguageFailsClosed() {
        status = 400;
        assertThatThrownBy(() -> detector("fr").detect("x")).isInstanceOf(PersonalDataDetectorUnavailable.class);
    }

    @Test
    void anItemOfUnknownShapeFailsClosed() {
        reply = "[{\"type\":\"PERSON\"}]";
        assertThatThrownBy(() -> detector("en").detect("x")).isInstanceOf(PersonalDataDetectorUnavailable.class);
    }

    @Test
    void garbageFailsClosed() {
        reply = "not json";
        assertThatThrownBy(() -> detector("en").detect("x")).isInstanceOf(PersonalDataDetectorUnavailable.class);
    }

    @Test
    void unreachableFailsClosed() {
        server.stop(0);
        assertThatThrownBy(() -> detector("en").detect("x")).isInstanceOf(PersonalDataDetectorUnavailable.class);
    }
}
