package com.fundgate.fundeli5.replydrafter;

import com.fundgate.fundeli5.explanation.Language;
import java.util.List;

/** Writes the reply to the Requester. Sees Portfolio Ids only; the caller swaps references back. */
public interface ReplyDrafter {

    /**
     * @param language the language of the request, which the reply is written in
     * @param lines one per Eligibility Check
     * @param requesterQuestions the Requester Questions the dealer accepted, to ask in the reply
     */
    Reply draft(Language language, List<ReplyLine> lines, List<String> requesterQuestions);
}
