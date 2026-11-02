package com.fundgate.fundeli5.rulecardpipeline.jev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The Jev client against a stubbed local HTTP server: never api.typesafe.ai, never a real key. */
class JevClientTest {

    private HttpServer server;
    private final List<String> bodies = new ArrayList<>();
    private final List<String> auth = new ArrayList<>();
    private final List<String> paths = new ArrayList<>();
    private volatile int status = 200;
    private volatile String reply = "{}";

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            auth.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
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

    private JevClient client(String key) {
        return new JevClient("http://127.0.0.1:" + server.getAddress().getPort() + "/", key, "jev-latest",
                Duration.ofSeconds(2));
    }

    @Test
    void postsStateModelAndQuestionsWithTheBearerKeyAndParsesTheProbabilities() {
        reply = "{\"answers\":{\"q\":{\"probabilities\":{\"page 1\":0.7,\"none\":0.3},\"confidence\":0.9}}}";
        var answers = client("secret").ask("[Page 1] text",
                Map.of("q", JevQuestion.choice("Which?", List.of("page 1", "none"))));
        assertThat(paths).containsExactly("POST /v1/systemone");
        assertThat(auth).containsExactly("Bearer secret");
        assertThat(bodies).singleElement().satisfies(b -> assertThat(b)
                .contains("\"state\":\"[Page 1] text\"").contains("\"model\":\"jev-latest\"")
                .contains("\"type\":\"choice\"").contains("\"instructions\":\"Which?\"")
                .contains("\"criteria\":{\"page 1\":\"page 1\",\"none\":\"none\"}"));
        assertThat(answers.get("q").probability("page 1")).isEqualTo(0.7);
        assertThat(answers.get("q").confidence()).isEqualTo(0.9);
    }

    @Test
    void aNoulAnswerIsTheProbabilityOfYes() {
        reply = "{\"answers\":{\"q\":{\"type\":\"noul\",\"noul\":0.99}}}";
        var answers = client("k").ask("s", Map.of("q", JevQuestion.yesNo("?")));
        assertThat(answers.get("q").probability("yes")).isEqualTo(0.99);
    }

    @Test
    void aBadKeyIs403AndSaysSo() {
        status = 403;
        assertThatThrownBy(() -> client("bad").ask("s", Map.of("q", JevQuestion.yesNo("?"))))
                .isInstanceOf(JevUnavailable.class).hasMessageContaining("403");
    }

    @Test
    void aMissingKeySendsNoAuthorizationHeaderAndFailsOn403() {
        status = 403;
        assertThatThrownBy(() -> client("").ask("s", Map.of("q", JevQuestion.yesNo("?"))))
                .isInstanceOf(JevUnavailable.class);
        assertThat(auth).containsExactly("null");
    }

    @Test
    void anotherStatusOrAnUnknownShapeIsUnavailable() {
        status = 500;
        assertThatThrownBy(() -> client("k").ask("s", Map.of("q", JevQuestion.yesNo("?"))))
                .isInstanceOf(JevUnavailable.class).hasMessageContaining("500");
        status = 200;
        reply = "{\"answers\":{}}";
        assertThatThrownBy(() -> client("k").ask("s", Map.of("q", JevQuestion.yesNo("?"))))
                .isInstanceOf(JevUnavailable.class).hasMessageContaining("unknown shape");
    }
}
