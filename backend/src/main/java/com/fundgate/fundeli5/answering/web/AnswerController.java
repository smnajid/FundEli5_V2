package com.fundgate.fundeli5.answering.web;

import com.fundgate.fundeli5.answering.Answer;
import com.fundgate.fundeli5.answering.AnswerService;
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
@Profile("!pipeline")
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
        return view(answers.answerEmail(body.email()));
    }

    /** The request form filled by hand: still checked, answered from a template, no model call. */
    @PostMapping("/answers/by-hand")
    AnswerView answerByHand(@RequestBody HandRequestBody body) {
        try {
            ShareClass shareClass = shareClasses.all().stream()
                    .filter(c -> c.isin().value().equals(body.shareClassIsin())).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown Share Class"));
            var request = new SubscriptionRequest(new PortfolioReference(body.portfolioReference()),
                    shareClass.isin(), body.amount(), new Currency(body.currency()), body.clientInitiated());
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
        return checkRecords.all().stream().map(AnswerController::view).toList();
    }

    private static AnswerView view(Answer a) {
        return new AnswerView(
                a.refusal() == null ? null : new RefusalView(a.refusal().reason(), a.refusal().namesFound()),
                a.aiUsed(),
                a.aiUsed() ? "the LLM saw Portfolio Ids only" : "no AI was used for this email",
                a.checks().stream().map(c -> new CheckView(c.portfolio().value(), c.shareClass().value(),
                        c.shareClassName(), c.amount(), c.currency().code(), c.clientInitiated(), c.verdict(),
                        c.justification().outcomes().stream().map(o -> new OutcomeView(o.kind().name(),
                                o.result().name(), o.explanation())).toList())).toList(),
                a.unclear(), EDITABLE_FIELDS, a.reply());
    }

    private static CheckRecordView view(CheckRecord r) {
        return new CheckRecordView(r.id(), r.checkedAt(), r.request().portfolio().value(),
                r.request().shareClass().value(), r.verdict(), r.engineVersion(),
                r.llmCalls().stream().map(c -> new LlmCallView(c.model(), c.gatedPrompt(), c.fieldsSent(),
                        c.response())).toList());
    }

    record EmailBody(String email) {
    }

    record HandRequestBody(String portfolioReference, String shareClassIsin, BigDecimal amount, String currency,
            boolean clientInitiated) {
    }

    record AnswerView(RefusalView refusal, boolean aiUsed, String screenNote, List<CheckView> checks,
            List<String> unclear, List<String> editableFields, String reply) {
    }

    record RefusalView(String reason, List<String> namesFound) {
    }

    record CheckView(String portfolioReference, String shareClassIsin, String shareClassName, BigDecimal amount,
            String currency, boolean clientInitiated, Verdict verdict, List<OutcomeView> outcomes) {
    }

    record OutcomeView(String kind, String result, String explanation) {
    }

    record CheckRecordView(UUID id, Instant checkedAt, String portfolioReference, String shareClassIsin,
            Verdict verdict, String engineVersion, List<LlmCallView> llmCalls) {
    }

    record LlmCallView(String model, String gatedPrompt, List<String> fieldsSent, String response) {
    }
}
