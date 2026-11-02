package com.fundgate.fundeli5.replydrafter.stub;

import com.fundgate.fundeli5.eligibility.Verdict;
import com.fundgate.fundeli5.explanation.Explanation;
import com.fundgate.fundeli5.explanation.ExplanationRenderer;
import com.fundgate.fundeli5.explanation.Language;
import com.fundgate.fundeli5.replydrafter.ReplyDrafter;
import com.fundgate.fundeli5.replydrafter.ReplyLine;
import java.util.List;

/**
 * Template per Verdict, no model: the fallback of the model-backed drafter and the by-hand reply. The deciding
 * rules and their Citations come from the explanation templates; accepted Requester Questions are listed verbatim.
 */
public class TemplateReplyDrafter implements ReplyDrafter {

    private final ExplanationRenderer renderer;

    public TemplateReplyDrafter(ExplanationRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public String draft(Language language, List<ReplyLine> lines, List<String> requesterQuestions) {
        boolean fr = language == Language.FR;
        StringBuilder reply = new StringBuilder(fr ? "Bonjour,\n\n" : "Hello,\n\n");
        for (ReplyLine line : lines) {
            reply.append("- ").append(line.portfolio()).append(", ").append(line.shareClassName()).append(": ")
                    .append(sentence(line.verdict(), fr)).append('\n');
            Explanation explanation = renderer.render(line.verdict(), line.justification(), language, line.alternatives());
            explanation.sentences().forEach(s -> reply.append("  ").append(s).append('\n'));
            if (explanation.alternatives() != null) {
                reply.append("  ").append(explanation.alternatives()).append('\n');
            }
        }
        if (!requesterQuestions.isEmpty()) {
            reply.append(fr ? "\nPourriez-vous nous préciser :\n" : "\nCould you tell us:\n");
            requesterQuestions.forEach(q -> reply.append("- ").append(q).append('\n'));
        }
        return reply.append(fr ? "\nCordialement,\nLe desk de souscription" : "\nBest regards,\nFund dealing desk")
                .toString();
    }

    private static String sentence(Verdict verdict, boolean fr) {
        return fr ? switch (verdict) {
            case ELIGIBLE -> "Éligible. Le portefeuille peut souscrire.";
            case NOT_ELIGIBLE -> "Non éligible. Le portefeuille ne peut pas souscrire.";
            case NEEDS_REVIEW -> "À examiner. Nous revenons vers vous après vérification.";
        } : switch (verdict) {
            case ELIGIBLE -> "Eligible. The portfolio may subscribe.";
            case NOT_ELIGIBLE -> "Not Eligible. The portfolio may not subscribe.";
            case NEEDS_REVIEW -> "Needs Review. We come back to you once we have checked.";
        };
    }
}
