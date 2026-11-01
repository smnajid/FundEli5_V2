package com.fundgate.fundeli5.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fundgate.fundeli5.llm.claudeplan.ClaudePlanLlmEgress;
import com.fundgate.fundeli5.llm.claudeplan.ClaudePlanException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs the adapter against a fake {@code claude} script that records its arguments and stdin. */
class ClaudePlanLlmEgressTest {

    @TempDir
    Path dir;

    private Path fake(String body) throws IOException {
        Path script = dir.resolve("claude");
        Files.writeString(script, "#!/bin/sh\n" + body);
        script.toFile().setExecutable(true);
        return script;
    }

    private static final String RECORD = """
            printf '%s\\n' "$@" > "$(dirname "$0")/args.txt"
            cat > "$(dirname "$0")/stdin.txt"
            """;

    @Test
    void sendsThePromptOnStdinAndReturnsTheResultText() throws IOException {
        Path claude = fake(RECORD + "echo '{\"type\":\"result\",\"is_error\":false,\"result\":\"hello back\"}'\n");

        var response = new ClaudePlanLlmEgress(claude.toString(), Optional.empty(), Duration.ofSeconds(10))
                .complete(new LlmRequest("sonnet", "be brief", "the prompt"));

        assertThat(response).isEqualTo(new LlmResponse("hello back"));
        assertThat(Files.readString(dir.resolve("stdin.txt"))).isEqualTo("the prompt");
        assertThat(Files.readAllLines(dir.resolve("args.txt")))
                .containsExactly("-p", "--output-format", "json", "--model", "sonnet", "--system-prompt", "be brief");
    }

    @Test
    void passesTheJsonSchemaAndReturnsTheStructuredOutput() throws IOException {
        Path claude = fake(RECORD
                + "echo '{\"is_error\":false,\"result\":\"ignored\",\"structured_output\":{\"a\":1}}'\n");
        Path schema = dir.resolve("schema.json");
        Files.writeString(schema, "{\"type\":\"object\"}");

        var response = new ClaudePlanLlmEgress(claude.toString(), Optional.of(schema), Duration.ofSeconds(10))
                .complete(new LlmRequest("m", "s", "p"));

        assertThat(response.text()).isEqualTo("{\"a\":1}");
        assertThat(Files.readAllLines(dir.resolve("args.txt")))
                .containsSubsequence("--json-schema", "{\"type\":\"object\"}");
    }

    @Test
    void aNonZeroExitFailsWithoutEchoingThePrompt() throws IOException {
        Path claude = fake("cat > /dev/null\necho 'boom' >&2\nexit 3\n");

        assertThatThrownBy(() -> new ClaudePlanLlmEgress(claude.toString(), Optional.empty(), Duration.ofSeconds(10))
                .complete(new LlmRequest("m", "s", "SECRET-PROMPT")))
                .isInstanceOf(ClaudePlanException.class)
                .hasMessageContaining("exit 3")
                .hasMessageNotContaining("SECRET-PROMPT");
    }

    @Test
    void anErrorResultFails() throws IOException {
        Path claude = fake("cat > /dev/null\necho '{\"is_error\":true,\"result\":\"rate limited\"}'\n");

        assertThatThrownBy(() -> new ClaudePlanLlmEgress(claude.toString(), Optional.empty(), Duration.ofSeconds(10))
                .complete(new LlmRequest("m", "s", "p")))
                .isInstanceOf(ClaudePlanException.class);
    }

    @Test
    void outputThatIsNotJsonFails() throws IOException {
        Path claude = fake("cat > /dev/null\necho 'not json'\n");

        assertThatThrownBy(() -> new ClaudePlanLlmEgress(claude.toString(), Optional.empty(), Duration.ofSeconds(10))
                .complete(new LlmRequest("m", "s", "p")))
                .isInstanceOf(ClaudePlanException.class);
    }

    @Test
    void aSlowCommandIsKilledAfterTheTimeout() throws IOException {
        Path claude = fake("cat > /dev/null\nsleep 30\n");

        assertThatThrownBy(() -> new ClaudePlanLlmEgress(claude.toString(), Optional.empty(), Duration.ofMillis(300))
                .complete(new LlmRequest("m", "s", "p")))
                .isInstanceOf(ClaudePlanException.class)
                .hasMessageContaining("timed out");
    }

    @Test
    void aMissingExecutableFails() {
        assertThatThrownBy(() -> new ClaudePlanLlmEgress(dir.resolve("nope").toString(), Optional.empty(),
                Duration.ofSeconds(1)).complete(new LlmRequest("m", "s", "p")))
                .isInstanceOf(ClaudePlanException.class);
    }
}
