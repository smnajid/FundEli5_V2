package com.fundgate.fundeli5.replydrafter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import com.fundgate.fundeli5.clientprofiles.fixture.FixtureClientProfiles;
import com.fundgate.fundeli5.eligibility.Justification;
import com.fundgate.fundeli5.eligibility.Verdict;
import com.fundgate.fundeli5.explanation.ExplanationRenderer;
import com.fundgate.fundeli5.explanation.ExplanationTemplates;
import com.fundgate.fundeli5.explanation.Language;
import com.fundgate.fundeli5.llm.GatedLlmEgress;
import com.fundgate.fundeli5.llm.LlmRequest;
import com.fundgate.fundeli5.llm.LlmResponse;
import com.fundgate.fundeli5.privacygate.DefaultPrivacyGate;
import com.fundgate.fundeli5.privacygate.ReplyFacts;
import com.fundgate.fundeli5.privacygate.fixture.FixturePersonalDataDetector;
import com.fundgate.fundeli5.replydrafter.gated.GatedReplyDrafter;
import com.fundgate.fundeli5.replydrafter.stub.TemplateReplyDrafter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class GatedReplyDrafterTest {

    private static final PortfolioId ID = new PortfolioId(UUID.fromString("9ae403db-1754-4033-9f06-a99edf92a574"));
    private static final ReplyLine LINE = new ReplyLine(ID, new Isin("LU0348927095"), "BI-EUR", Verdict.ELIGIBLE,
            new Justification(List.of()), List.of());

    private final DefaultPrivacyGate gate = new DefaultPrivacyGate(
            new FixtureClientProfiles(Path.of("../docs/playbook/contracts/fixtures")), new FixturePersonalDataDetector());
    private final ExplanationRenderer renderer = new ExplanationRenderer(ExplanationTemplates.load());
    private final List<LlmRequest> sent = new ArrayList<>();

    private GatedReplyDrafter drafter(Function<LlmRequest, String> model) {
        GatedLlmEgress egress = GatedLlmEgress.over(request -> {
            sent.add(request);
            return new LlmResponse(model.apply(request));
        });
        return new GatedReplyDrafter(gate, egress, renderer, new TemplateReplyDrafter(renderer));
    }

    @Test
    void usesTheModelsReplyAndSendsTheIdAndTheVerdictOnly() {
        String reply = drafter(r -> "Hello, " + ID + " is Eligible.").draft(Language.EN, List.of(LINE), List.of()).text();

        assertThat(reply).isEqualTo("Hello, " + ID + " is Eligible.");
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).prompt()).contains("Language: en", ID.toString(), "ELIGIBLE").doesNotContain("LOC9");
    }

    @Test
    void fallsBackToTheTemplateWhenTheModelIsUnreachable() {
        String reply = drafter(r -> {
            throw new IllegalStateException("unreachable");
        }).draft(Language.FR, List.of(LINE), List.of("Quel est le montant ?")).text();

        assertThat(reply).startsWith("Bonjour,").contains(ID.toString(), "Éligible", "Quel est le montant ?");
    }

    @Test
    void fallsBackWhenTheModelDropsAPortfolio() {
        String reply = drafter(r -> "Hello, all is well.").draft(Language.EN, List.of(LINE), List.of()).text();

        assertThat(reply).startsWith("Hello,\n\n- " + ID).contains("Eligible. The portfolio may subscribe.");
    }

    @Test
    void theGateRefusesAPromptThatWouldCarryAReference() {
        var facts = new ReplyFacts("en", List.of(new ReplyFacts.Line(ID, "BI-EUR", Verdict.ELIGIBLE,
                List.of("Ask LOC90000101"), List.of())));

        assertThatThrownBy(() -> gate.gateReply(facts)).isInstanceOf(IllegalArgumentException.class);
    }
}
