package com.fundgate.fundeli5.requestparser;

import com.fundgate.fundeli5.privacygate.GatedEmail;

/** Turns a gated email into Subscription Requests, with a model. Sees the text with Portfolio Ids only (ADR 0004). */
public interface RequestParser {

    ParseResult parse(GatedEmail email);
}
