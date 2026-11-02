package com.fundgate.fundeli5;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"fundeli5.llm.endpoint=replay", "fundeli5.privacygate.detector=fixture"})
class ScreensServingTest {

    @TempDir
    static Path screens;

    @DynamicPropertySource
    static void built(DynamicPropertyRegistry registry) throws Exception {
        Files.writeString(screens.resolve("index.html"), "<app-root>screens</app-root>");
        registry.add("fundeli5.screens.dir", screens::toString);
    }

    @LocalServerPort
    int port;

    @Test
    void bothScreensLoadTheSinglePageAppFromTheSameOriginAsTheApi() throws Exception {
        assertThat(get("/dealer-screen").body()).contains("<app-root>");
        assertThat(get("/validation-screen").body()).contains("<app-root>");
        assertThat(get("/").body()).contains("<app-root>");
        assertThat(get("/api/validation/drafts").statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
