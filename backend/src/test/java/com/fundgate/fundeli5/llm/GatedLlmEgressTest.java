package com.fundgate.fundeli5.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.privacygate.GatedEmail;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/** The online egress cannot be called with a raw String, and only the Privacy Gate can build its input (ADR 0004). */
class GatedLlmEgressTest {

    @Test
    void theOnlineEgressTakesGatedRequestsOnly() {
        for (Method m : GatedLlmEgress.class.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            assertThat(m.getParameterTypes()).containsExactly(GatedLlmRequest.class);
        }
        assertThat(GatedLlmRequest.class.getRecordComponents()[2].getType()).isEqualTo(com.fundgate.fundeli5.privacygate.GatedText.class);
    }

    @Test
    void aGatedEmailCannotBeBuiltOutsideThePrivacyGate() {
        for (Constructor<?> c : java.util.stream.Stream.concat(
                java.util.Arrays.stream(GatedEmail.class.getDeclaredConstructors()),
                java.util.Arrays.stream(com.fundgate.fundeli5.privacygate.GatedPrompt.class.getDeclaredConstructors())).toList()) {
            assertThat(Modifier.isPublic(c.getModifiers()) || Modifier.isProtected(c.getModifiers())).isFalse();
        }
    }

    @Test
    void theGatedEntryPassesOnlyTheGatedText() {
        var gate = new com.fundgate.fundeli5.privacygate.DefaultPrivacyGate(
                new com.fundgate.fundeli5.clientprofiles.fixture.FixtureClientProfiles(
                        java.nio.file.Path.of("../docs/playbook/contracts/fixtures")),
                text -> java.util.List.of());
        var email = ((com.fundgate.fundeli5.privacygate.GateOutcome.Passed) gate.gate("Can LOC90000101 hold X?")).email();
        var seen = new java.util.ArrayList<LlmRequest>();
        GatedLlmEgress.over(r -> {
            seen.add(r);
            return new LlmResponse("ok");
        }).complete(new GatedLlmRequest("m", "s", email));
        assertThat(seen).singleElement().satisfies(r -> assertThat(r.prompt()).doesNotContain("LOC9000"));
    }
}
