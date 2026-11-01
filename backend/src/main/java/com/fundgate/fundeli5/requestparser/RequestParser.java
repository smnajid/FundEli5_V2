package com.fundgate.fundeli5.requestparser;

/** Turns a pseudonymised email into Subscription Requests, with a model. Sees Portfolio Ids only (ADR 0004). */
public interface RequestParser {

    ParseResult parse(String pseudonymisedText);
}
