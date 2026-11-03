package com.fundgate.fundeli5.answering.web;

import com.fundgate.fundeli5.Runtimes;
import com.fundgate.fundeli5.answering.Answer;
import com.fundgate.fundeli5.answering.AnswerService;
import com.fundgate.fundeli5.answering.Evidence;
import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.checkrecord.CheckRecords;
import com.fundgate.fundeli5.clientprofiles.PortfolioNotFound;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.eligibility.CheckRecord;
import com.fundgate.fundeli5.eligibility.SubscriptionRequest;
import com.fundgate.fundeli5.eligibility.Verdict;
import com.fundgate.fundeli5.shareclasses.ShareClass;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** The dealer's flow over HTTP. Every route lives under {@code /api}. */
@RestController
@Profile("!" + Runtimes.PIPELINE)
@RequestMapping("/api")
class AnswerController {

    /** The fields the Fund Dealer may correct in the parsed request form; the Portfolio Reference is not one. */
    private static final List<String> EDITABLE_FIELDS = List.of("shareClass", "amount", "currency", "clientInitiated");

    private final AnswerService answers;
    private final CheckRecords checkRecords;
    private final ShareClasses shareClasses;

    AnswerController(@Lazy AnswerService answers, CheckRecords checkRecords, @Lazy ShareClasses shareClasses) {
        this.answers = answers;
        this.checkRecords = checkRecords;
        this.shareClasses = shareClasses;
    }

    /** Intake: the Requester's email, pasted. */
    @PostMapping("/answers")
    AnswerView answer(@RequestBody EmailBody body) {
        if (body.email() == null || body.email().isBlank() || body.email().length() > 50_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An email of up to 50,000 characters is required");
        }
        return view(answers.answerEmail(body.email()));
    }

    /** The request form filled by hand: still checked, answered from a template, no model call. */
    @PostMapping("/answers/by-hand")
    AnswerView answerByHand(@RequestBody HandRequestBody body) {
        if (body.portfolioReference() == null || body.shareClassIsin() == null || (body.currency() == null) != (body.amount() == null)
                || (body.amount() != null && body.amount().signum() <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Portfolio and share class are required; an amount needs its currency, and must be positive");
        }
        try {
            ShareClass shareClass = shareClasses.all().stream()
                    .filter(c -> c.isin().value().equals(body.shareClassIsin())).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown Share Class"));
            var request = new SubscriptionRequest(new PortfolioReference(body.portfolioReference()),
                    shareClass.isin(), body.amount(),
                    body.currency() == null ? null : new Currency(body.currency()), body.clientInitiated());
            return view(answers.answerByHand(request, shareClass.shareClassName()));
        } catch (PortfolioNotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown Portfolio Reference");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** What each check read and sent to a model, oldest first. */
    @GetMapping("/check-records")
    List<CheckRecordView> checkRecords() {
        return checkRecords.all().stream().map(this::view).toList();
    }

    private static AnswerView view(Answer a) {
        return new AnswerView(
                a.refusal() == null ? null : new RefusalView(a.refusal().reason(), a.refusal().namesFound()),
                a.aiUsed(),
                a.aiUsed() ? "the LLM saw Portfolio Ids only" : "no AI was used for this email",
                a.checks().stream().map(c -> new CheckView(c.portfolio().value(), c.shareClass().value(),
                        c.shareClassName(), c.amount(), c.currency() == null ? null : c.currency().code(), c.clientInitiated(), c.verdict(),
                        c.justification().outcomes().stream().map(o -> new OutcomeView(o.kind().name(),
                                o.result().name(), o.explanation(), citations(a, c, o))).toList())).toList(),
                a.unclear(), EDITABLE_FIELDS, a.reply(),
                a.evidence().stream().map(e -> new EvidenceView(e.number(), e.kind().name(), e.result().name(),
                        e.shareClassName(), e.citation().document(), e.citation().page(), e.citation().quote(),
                        e.citation().quoteFoundOnPage(), e.citedInReply())).toList());
    }

    /** The numbers of the evidence cards an outcome cites, with the passages. */
    private static List<OutcomeCitationView> citations(Answer a, Answer.Check check, com.fundgate.fundeli5.eligibility.RuleOutcome outcome) {
        return outcome.citations().stream().map(c -> new OutcomeCitationView(
                a.evidence().stream().filter(e -> e.kind() == outcome.kind() && e.result() == outcome.result()
                        && e.shareClassName().equals(check.shareClassName()) && e.citation().equals(c))
                        .findFirst().map(Evidence::number).orElse(0),
                c.document(), c.page(), c.quote(), c.quoteFoundOnPage())).toList();
    }

    private CheckRecordView view(CheckRecord r) {
        return new CheckRecordView(r.id(), r.checkedAt(), r.request().portfolio().value(),
                r.request().shareClass().value(), r.request().amount(),
                r.request().currency() == null ? null : r.request().currency().code(),
                r.ruleCardVersion(), r.scaleMappingVersion(), r.engineVersion(), clientFieldsRead(r.clientProfile()),
                r.verdict(), r.justification().outcomes().stream().map(o -> new OutcomeView(o.kind().name(),
                        o.result().name(), o.explanation(), List.of())).toList(),
                r.llmCalls().stream().map(c -> new LlmCallView(c.model(), c.gatedPrompt(), c.fieldsSent(),
                        c.response())).toList(),
                checkRecords.correctionsOf(r.id()).stream().map(c -> new CorrectionView(c.field(), c.parsedValue(),
                        c.dealerValue(), c.by(), c.at())).toList());
    }

    /** The names of the Client Profile fields that held a value when the check read them; never the values. */
    private static List<String> clientFieldsRead(Object profile) {
        List<String> names = new java.util.ArrayList<>();
        collect("", profile, names);
        return names;
    }

    private static void collect(String prefix, Object value, List<String> names) {
        if (value == null) {
            return;
        }
        if (value instanceof java.util.Collection<?> c && c.isEmpty()) {
            return; // "none on file" is not a value read
        }
        if (!value.getClass().isRecord()) {
            names.add(prefix);
            return;
        }
        for (var component : value.getClass().getRecordComponents()) {
            try {
                String name = prefix.isEmpty() ? component.getName() : prefix + "." + component.getName();
                collect(name, component.getAccessor().invoke(value), names);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    record EmailBody(String email) {
    }

    record HandRequestBody(String portfolioReference, String shareClassIsin, BigDecimal amount, String currency,
            boolean clientInitiated) {
    }

    record AnswerView(RefusalView refusal, boolean aiUsed, String screenNote, List<CheckView> checks,
            List<String> unclear, List<String> editableFields, String reply, List<EvidenceView> evidence) {
    }

    record RefusalView(String reason, List<String> namesFound) {
    }

    record CheckView(String portfolioReference, String shareClassIsin, String shareClassName, BigDecimal amount,
            String currency, boolean clientInitiated, Verdict verdict, List<OutcomeView> outcomes) {
    }

    record OutcomeView(String kind, String result, String explanation, List<OutcomeCitationView> citations) {
    }

    /** A passage of a public Fund Document; {@code number} is its card in the evidence rail. */
    record OutcomeCitationView(int number, String document, int page, String quote, boolean quoteFoundOnPage) {
    }

    /** One card of the evidence rail; {@code citedInReply} when a [number] marker of the reply points to it. */
    record EvidenceView(int number, String kind, String result, String shareClassName, String document, int page,
            String quote, boolean quoteFoundOnPage, boolean citedInReply) {
    }

    record CheckRecordView(UUID id, Instant checkedAt, String portfolioReference, String shareClassIsin,
            BigDecimal amount, String currency, Integer ruleCardVersion, String scaleMappingVersion,
            String engineVersion, List<String> clientFieldsRead, Verdict verdict, List<OutcomeView> outcomes,
            List<LlmCallView> llmCalls, List<CorrectionView> corrections) {
    }

    record CorrectionView(String field, String parsedValue, String dealerValue, String by, Instant at) {
    }

    record LlmCallView(String model, String gatedPrompt, List<String> fieldsSent, String response) {
    }
}
