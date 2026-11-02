package com.fundgate.fundeli5.requestparser.replay;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import com.fundgate.fundeli5.llm.GatedLlmEgress;
import com.fundgate.fundeli5.llm.GatedLlmRequest;
import com.fundgate.fundeli5.privacygate.GatedEmail;
import com.fundgate.fundeli5.requestparser.ParseResult;
import com.fundgate.fundeli5.requestparser.ParsedRequest;
import com.fundgate.fundeli5.requestparser.RequestParser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Asks the model through the egress client (replay in tests) and reads its JSON answer. */
public class ReplayRequestParser implements RequestParser {

    public static final String MODEL = "claude-sonnet-5-5";

    public static final String SYSTEM = """
            You read an email asking whether portfolios may subscribe to funds. Portfolios appear as UUIDs.
            Answer with a JSON array only, one object per portfolio and share class asked about:
            {"portfolioId": <uuid>, "shareClass": <text as written>, "amount": <number or null>,
             "currency": <ISO code or null>, "clientInitiated": <true or false>}.
            Use null for what the email does not say. Never guess.""";

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final GatedLlmEgress egress;

    public ReplayRequestParser(GatedLlmEgress egress) {
        this.egress = egress;
    }

    @Override
    public ParseResult parse(GatedEmail email) {
        String answer = egress.complete(new GatedLlmRequest(MODEL, SYSTEM, email)).text();
        List<ParsedRequest> requests = new ArrayList<>();
        for (JsonNode node : MAPPER.readTree(answer)) {
            requests.add(new ParsedRequest(
                    new PortfolioId(UUID.fromString(node.get("portfolioId").asString())),
                    node.get("shareClass").asString(),
                    node.hasNonNull("amount") ? new BigDecimal(node.get("amount").asString()) : null,
                    node.hasNonNull("currency") ? new Currency(node.get("currency").asString()) : null,
                    node.path("clientInitiated").asBoolean(false)));
        }
        return new ParseResult(requests, MODEL, email.text(), answer, email.fieldsSent());
    }
}
