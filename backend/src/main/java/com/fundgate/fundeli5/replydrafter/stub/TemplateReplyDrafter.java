package com.fundgate.fundeli5.replydrafter.stub;

import com.fundgate.fundeli5.eligibility.Verdict;
import com.fundgate.fundeli5.replydrafter.ReplyDrafter;
import com.fundgate.fundeli5.replydrafter.ReplyLine;
import java.util.List;

/** Template per Verdict, no model: the fallback of the real drafter and the walking skeleton's reply. */
public class TemplateReplyDrafter implements ReplyDrafter {

    @Override
    public String draft(List<ReplyLine> lines) {
        StringBuilder reply = new StringBuilder("Hello,\n\n");
        for (ReplyLine line : lines) {
            reply.append("- ").append(line.portfolio()).append(", ").append(line.shareClassName()).append(": ")
                    .append(sentence(line.verdict())).append('\n');
        }
        return reply.append("\nBest regards,\nFund dealing desk").toString();
    }

    private static String sentence(Verdict verdict) {
        return switch (verdict) {
            case ELIGIBLE -> "Eligible. The portfolio may subscribe.";
            case NOT_ELIGIBLE -> "Not Eligible. The portfolio may not subscribe.";
            case NEEDS_REVIEW -> "Needs Review. We come back to you once we have checked.";
        };
    }
}
