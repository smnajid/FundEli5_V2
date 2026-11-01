package com.fundgate.fundeli5.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fundgate.fundeli5.llm.replay.ReplayLlmEgress;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReplayLlmEgressTest {

    @TempDir
    Path dir;

    private void recording(String name, String json) throws IOException {
        Files.writeString(dir.resolve(name), json);
    }

    private static final String HELLO = """
            {"request": {"model": "m", "system": "s", "prompt": "hello"},
             "response": {"text": "world"}}""";

    @Test
    void servesTheRecordedAnswerForAnIdenticalRequest() throws IOException {
        recording("hello.json", HELLO);

        var response = new ReplayLlmEgress(dir).complete(new LlmRequest("m", "s", "hello"));

        assertThat(response).isEqualTo(new LlmResponse("world"));
    }

    @Test
    void anUnrecordedRequestThrows() throws IOException {
        recording("hello.json", HELLO);

        assertThatThrownBy(() -> new ReplayLlmEgress(dir).complete(new LlmRequest("m", "s", "other")))
                .isInstanceOf(UnrecordedLlmRequest.class);
    }

    @Test
    void theModelIsPartOfTheMatch() throws IOException {
        recording("hello.json", HELLO);

        assertThatThrownBy(() -> new ReplayLlmEgress(dir).complete(new LlmRequest("other-model", "s", "hello")))
                .isInstanceOf(UnrecordedLlmRequest.class);
    }

    @Test
    void theSystemPromptIsPartOfTheMatch() throws IOException {
        recording("hello.json", HELLO);

        assertThatThrownBy(() -> new ReplayLlmEgress(dir).complete(new LlmRequest("m", "other", "hello")))
                .isInstanceOf(UnrecordedLlmRequest.class);
    }

    @Test
    void anUnknownPropertyFailsLoudly() throws IOException {
        recording("typo.json", """
                {"request": {"model": "m", "sytem": "s", "prompt": "hello"},
                 "response": {"text": "world"}}""");

        assertThatThrownBy(() -> new ReplayLlmEgress(dir)).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("typo.json");
    }

    @Test
    void anUnknownPropertyOnTheResponseFailsLoudly() throws IOException {
        recording("typo.json", """
                {"request": {"model": "m", "system": "s", "prompt": "hello"},
                 "response": {"txt": "world"}}""");

        assertThatThrownBy(() -> new ReplayLlmEgress(dir)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void aMissingPartFailsLoudly() throws IOException {
        recording("partial.json", """
                {"request": {"model": "m", "prompt": "hello"}, "response": {"text": "world"}}""");

        assertThatThrownBy(() -> new ReplayLlmEgress(dir)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void twoRecordingsOfTheSameRequestFailLoudly() throws IOException {
        recording("a.json", HELLO);
        recording("b.json", HELLO);

        assertThatThrownBy(() -> new ReplayLlmEgress(dir)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void aMissingFolderFailsLoudly() {
        assertThatThrownBy(() -> new ReplayLlmEgress(dir.resolve("nope"))).isInstanceOf(RuntimeException.class);
    }
}
