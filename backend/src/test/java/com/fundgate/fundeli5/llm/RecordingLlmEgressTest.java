package com.fundgate.fundeli5.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.llm.replay.ReplayLlmEgress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RecordingLlmEgressTest {

    @TempDir
    Path dir;

    @Test
    void whatItRecordsReplaysAsTheSameAnswer() {
        List<LlmRequest> seen = new ArrayList<>();
        LlmEgress live = request -> {
            seen.add(request);
            return new LlmResponse("live answer");
        };
        var request = new LlmRequest("m", "s", "a \"quoted\"\nprompt é");

        var answer = new RecordingLlmEgress(live, dir).complete(request);

        assertThat(answer).isEqualTo(new LlmResponse("live answer"));
        assertThat(seen).containsExactly(request);
        assertThat(new ReplayLlmEgress(dir).complete(request)).isEqualTo(answer);
    }

    @Test
    void recordingTheSameRequestTwiceKeepsOneFile() {
        var request = new LlmRequest("m", "s", "p");
        var recorder = new RecordingLlmEgress(r -> new LlmResponse("x"), dir);

        recorder.complete(request);
        recorder.complete(request);

        assertThat(new ReplayLlmEgress(dir).complete(request)).isEqualTo(new LlmResponse("x"));
    }
}
