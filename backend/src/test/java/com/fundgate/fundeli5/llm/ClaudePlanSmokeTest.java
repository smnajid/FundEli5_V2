package com.fundgate.fundeli5.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.llm.claudeplan.ClaudePlanLlmEgress;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Smoke check against the real {@code claude} CLI. Not run in the default build (it needs a logged-in
 * Claude plan and sends a public, non-client prompt): run it with
 * {@code CLAUDE_PLAN_SMOKE=1 ./mvnw test -Dtest=ClaudePlanSmokeTest}.
 */
@Tag("live-llm")
@EnabledIfEnvironmentVariable(named = "CLAUDE_PLAN_SMOKE", matches = "1")
class ClaudePlanSmokeTest {

    @Test
    void theRealCliAnswers() {
        var response = new ClaudePlanLlmEgress("claude", Optional.empty(), Duration.ofMinutes(2))
                .complete(new LlmRequest("sonnet", "Answer with one word.", "Say the word: pong"));

        assertThat(response.text()).isNotBlank();
    }
}
