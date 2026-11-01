package com.fundgate.fundeli5.replydrafter;

import java.util.List;

/** Writes the reply to the Requester. Sees Portfolio Ids only; the caller swaps references back. */
public interface ReplyDrafter {

    String draft(List<ReplyLine> lines);
}
