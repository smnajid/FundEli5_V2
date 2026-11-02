package com.fundgate.fundeli5.requestparser;

import java.util.List;

/** The parsed requests, with the model call that produced them, for the Check Record. */
public record ParseResult(List<ParsedRequest> requests, String model, String prompt, String response,
        List<String> fieldsSent) {

    public ParseResult {
        requests = List.copyOf(requests);
        fieldsSent = List.copyOf(fieldsSent);
    }
}
