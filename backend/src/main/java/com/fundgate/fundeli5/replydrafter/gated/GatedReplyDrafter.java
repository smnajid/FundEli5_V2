package com.fundgate.fundeli5.replydrafter.gated;

import com.fundgate.fundeli5.explanation.ExplanationRenderer;
import com.fundgate.fundeli5.explanation.Language;
import com.fundgate.fundeli5.llm.GatedLlmEgress;
import com.fundgate.fundeli5.llm.GatedLlmRequest;
import com.fundgate.fundeli5.privacygate.PrivacyGate;
import com.fundgate.fundeli5.privacygate.ReplyFacts;
import com.fundgate.fundeli5.replydrafter.Reply;
import com.fundgate.fundeli5.replydrafter.ReplyDrafter;
import com.fundgate.fundeli5.replydrafter.ReplyLine;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the reply with a model, in the request's language, through the Privacy Gate and the gated egress. The
 * explanation sentences and Citations come from the desk's templates, so the model words them and decides nothing.
 * Falls back to the template when the model cannot be reached, answers nothing, or drops a portfolio.
 */
public class GatedReplyDrafter implements ReplyDrafter {

    public static final String MODEL = "claude-sonnet-5-5";

    public static final String SYSTEM = """
            You write the reply of a bank's fund dealing desk to a colleague who asked whether portfolios may
            subscribe to funds. Portfolios appear as UUIDs: write each UUID exactly as given, never invent one.
            Write in the language named on the first line. Give every portfolio its Verdict (Eligible, Not Eligible
            or Needs Review; in French Éligible, Non éligible or À examiner), the reasons you are given, and keep
            each bracketed citation such as [Prospectus, p. 12] unchanged. Recommend the classes listed after
            "Eligible instead". Add nothing else: no other
            facts, no promise. Answer with the reply text only, greeting and sign-off included.""";

    private static final System.Logger LOG = System.getLogger(GatedReplyDrafter.class.getName());

    private final PrivacyGate gate;
    private final GatedLlmEgress egress;
    private final ExplanationRenderer renderer;
    private final ReplyDrafter fallback;

    public GatedReplyDrafter(PrivacyGate gate, GatedLlmEgress egress, ExplanationRenderer renderer,
            ReplyDrafter fallback) {
        this.gate = gate;
        this.egress = egress;
        this.renderer = renderer;
        this.fallback = fallback;
    }

    @Override
    public Reply draft(Language language, List<ReplyLine> lines, List<String> requesterQuestions) {
        if (!requesterQuestions.isEmpty()) {
            // Accepted Requester Questions are the dealer's free text: they are listed verbatim by the template, never sent.
            return fallback.draft(language, lines, requesterQuestions);
        }
        Reply.ModelCall sent = null;
        try {
            List<ReplyFacts.Line> facts = new ArrayList<>();
            for (ReplyLine line : lines) {
                var explanation = renderer.render(line.verdict(), line.justification(), language, List.of());
                List<String> sentences = new ArrayList<>();
                sentences.add(explanation.verdictStatement());
                explanation.sentences().stream().filter(x -> !sentences.contains(x)).forEach(sentences::add);
                facts.add(new ReplyFacts.Line(line.portfolio(), line.shareClassName(), line.verdict(), sentences,
                        line.verdict() == com.fundgate.fundeli5.eligibility.Verdict.ELIGIBLE ? List.of() : line.alternatives()));
            }
            var prompt = gate.gateReply(new ReplyFacts(language.code(), facts));
            // From here the prompt may have left: whatever happens next, the Check Record shows the call.
            sent = new Reply.ModelCall(MODEL, prompt.text(), prompt.fieldsSent(), null);
            String reply = egress.complete(new GatedLlmRequest(MODEL, SYSTEM, prompt)).text();
            var call = new Reply.ModelCall(MODEL, prompt.text(), prompt.fieldsSent(), reply);
            if (reply == null || reply.isBlank() || !namesEveryPortfolio(reply, lines)) {
                LOG.log(System.Logger.Level.WARNING, "The model's reply was unusable; using the template.");
                return new Reply(fallback.draft(language, lines, requesterQuestions).text(), call);
            }
            return new Reply(reply.strip(), call);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "No model reply (" + e.getClass().getSimpleName() + "); using the template.");
            return new Reply(fallback.draft(language, lines, requesterQuestions).text(), sent);
        }
    }

    /** The caller swaps ids back to references: a reply without an id would lose its portfolio. */
    private static boolean namesEveryPortfolio(String reply, List<ReplyLine> lines) {
        return lines.stream().allMatch(l -> reply.contains(l.portfolio().toString()));
    }
}
