package com.fundgate.fundeli5.answering;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.checkrecord.CheckRecords;
import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import com.fundgate.fundeli5.clientprofiles.PortfolioNotFound;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.eligibility.CheckRecord;
import com.fundgate.fundeli5.eligibility.SubscriptionRequest;
import com.fundgate.fundeli5.privacygate.GateOutcome;
import com.fundgate.fundeli5.privacygate.GatedEmail;
import com.fundgate.fundeli5.privacygate.PrivacyGate;
import com.fundgate.fundeli5.replydrafter.ReplyDrafter;
import com.fundgate.fundeli5.replydrafter.ReplyLine;
import com.fundgate.fundeli5.requestparser.ParseResult;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.RuleCards;
import com.fundgate.fundeli5.clientprofiles.ClientProfile;
import com.fundgate.fundeli5.requestparser.ParsedRequest;
import com.fundgate.fundeli5.requestparser.RequestParser;
import com.fundgate.fundeli5.ruleengine.EngineResult;
import com.fundgate.fundeli5.ruleengine.RuleEngine;
import com.fundgate.fundeli5.shareclasscatalog.ShareClassCatalog;
import com.fundgate.fundeli5.shareclasses.ShareClass;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the online flow (architecture.md, data flow): Gate, Parser, catalog, engine, reply,
 * Check Record. Knows no framework; the only code that holds a reference and an id side by side.
 */
public class AnswerService {

    private static final System.Logger LOG = System.getLogger(AnswerService.class.getName());

    private final PrivacyGate gate;
    private final RequestParser parser;
    private final ShareClassCatalog catalog;
    private final RuleEngine engine;
    private final ReplyDrafter drafter;
    private final CheckRecords checkRecords;
    private final ClientProfiles clientProfiles;
    private final Clock clock;
    private final String engineVersion;
    private final RuleCards ruleCards;
    private final String scaleMappingVersion;

    public AnswerService(PrivacyGate gate, RequestParser parser, ShareClassCatalog catalog, RuleEngine engine,
            ReplyDrafter drafter, CheckRecords checkRecords, ClientProfiles clientProfiles, Clock clock,
            String engineVersion, RuleCards ruleCards, String scaleMappingVersion) {
        this.gate = gate;
        this.parser = parser;
        this.catalog = catalog;
        this.engine = engine;
        this.drafter = drafter;
        this.checkRecords = checkRecords;
        this.clientProfiles = clientProfiles;
        this.clock = clock;
        this.engineVersion = engineVersion;
        this.ruleCards = ruleCards;
        this.scaleMappingVersion = scaleMappingVersion;
    }

    /** The email flow. A refused email makes no model call. */
    public Answer answerEmail(String rawEmail) {
        GateOutcome outcome = gate.gate(rawEmail);
        if (!(outcome instanceof GateOutcome.Passed(GatedEmail email))) {
            GateOutcome.Refused refused = (GateOutcome.Refused) outcome;
            return Answer.refused(refused.reason(), refused.namesFound());
        }
        ParseResult parsed;
        try {
            parsed = parser.parse(email);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "The Request Parser failed: " + e.getClass().getSimpleName());
            return Answer.refused("The AI could not read this email. Enter the request by hand.", List.of());
        }
        CheckRecord.LlmCall call = new CheckRecord.LlmCall(parsed.model(), parsed.prompt(), parsed.fieldsSent(), parsed.response());
        List<Answer.Check> checks = new ArrayList<>();
        List<PortfolioId> ids = new ArrayList<>();
        List<String> unclear = new ArrayList<>();
        for (ParsedRequest request : parsed.requests()) {
            PortfolioReference reference = email.references().get(request.portfolio());
            List<ShareClass> classes = catalog.resolve(request.shareClassMention());
            if (reference == null || classes.isEmpty() || request.amount() == null || request.currency() == null) {
                // A Subscription Request needs an amount; a request without one is flagged, never guessed.
                unclear.add("Request unclear: " + request.shareClassMention());
                continue;
            }
            for (ShareClass shareClass : classes) {
                checks.add(check(new SubscriptionRequest(reference, shareClass.isin(), request.amount(),
                        request.currency(), request.clientInitiated()), shareClass.shareClassName(), List.of(call)));
                ids.add(request.portfolio());
            }
        }
        return new Answer(null, true, checks, unclear, reply(checks, ids, email.references()));
    }

    /** The by-hand flow: no model, template reply. */
    public Answer answerByHand(SubscriptionRequest request, String shareClassName) {
        PortfolioId id = clientProfiles.portfolioId(request.portfolio()).orElseThrow(() -> new PortfolioNotFound(request.portfolio()));
        Answer.Check check = check(request, shareClassName, List.of());
        return new Answer(null, false, List.of(check), List.of(),
                reply(List.of(check), List.of(id), java.util.Map.of(id, request.portfolio())));
    }

    private Answer.Check check(SubscriptionRequest request, String shareClassName, List<CheckRecord.LlmCall> calls) {
        EngineResult result = engine.check(request);
        Integer cardVersion = ruleCards.forShareClass(request.shareClass()).map(RuleCard::version).orElse(null);
        ClientProfile profile = clientProfiles.clientProfile(request.portfolio());
        checkRecords.append(new CheckRecord(UUID.randomUUID(), clock.instant(), request, cardVersion,
                scaleMappingVersion, engineVersion, profile, result.verdict(), result.justification(), calls,
                List.of()));
        return new Answer.Check(request.portfolio(), request.shareClass(), shareClassName, request.amount(),
                request.currency(), request.clientInitiated(), result.verdict(), result.justification());
    }

    /** The drafter sees ids; the references are swapped back here, locally, after it. */
    private String reply(List<Answer.Check> checks, List<PortfolioId> ids,
            java.util.Map<PortfolioId, PortfolioReference> references) {
        List<ReplyLine> lines = new ArrayList<>();
        for (int i = 0; i < checks.size(); i++) {
            Answer.Check c = checks.get(i);
            lines.add(new ReplyLine(ids.get(i), c.shareClass(), c.shareClassName(), c.verdict(), c.justification()));
        }
        String reply = drafter.draft(lines);
        for (var entry : references.entrySet()) {
            reply = reply.replace(entry.getKey().toString(), entry.getValue().value());
        }
        return reply;
    }
}
