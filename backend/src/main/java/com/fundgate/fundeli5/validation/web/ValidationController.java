package com.fundgate.fundeli5.validation.web;

import com.fundgate.fundeli5.Runtimes;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.json.RuleCardJson;
import com.fundgate.fundeli5.validation.CannotValidate;
import com.fundgate.fundeli5.validation.Review;
import com.fundgate.fundeli5.validation.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The Rule Card Validator's screen over HTTP. Every route lives under {@code /api/validation}. */
@RestController
@Profile("!" + Runtimes.PIPELINE)
@RequestMapping("/api/validation")
class ValidationController {

    private final ValidationService validation;

    ValidationController(ValidationService validation) {
        this.validation = validation;
    }

    @GetMapping("/drafts")
    List<DraftSummary> drafts() {
        return validation.drafts().stream()
                .map(c -> new DraftSummary(c.fundName(), c.version(), c.shareClasses().stream().map(Isin::value).toList()))
                .toList();
    }

    @GetMapping("/review")
    ReviewView review(@RequestParam String fundName, @RequestParam int version) {
        return view(validation.review(fundName, version));
    }

    /** Settles a contradiction by following one document. */
    @PostMapping("/follow")
    ReviewView follow(@RequestBody FollowBody b) {
        return view(validation.followDocument(b.fundName(), b.version(), new Isin(b.shareClass()), b.kind(), b.document(),
                b.validator()));
    }

    @PostMapping("/accept")
    ReviewView accept(@RequestBody RuleBody b) {
        return view(validation.accept(b.fundName(), b.version(), b.rule(), b.validator()));
    }

    /** Replaces a rule's condition; {@code condition} is the condition JSON of the Rule Card schema. */
    @Operation(operationId = "correctRule")
    @PostMapping("/correct")
    ReviewView correct(@RequestBody CorrectBody b) {
        if (b.condition() == null) {
            throw new IllegalArgumentException("condition is required");
        }
        return view(validation.correct(b.fundName(), b.version(), b.rule(), RuleCardJson.condition(RuleCardView.toJson(b.condition())),
                b.validator()));
    }

    /** Marks an Unmodelled Restriction {@code blocking} or {@code noted} (the reason is required for noted). */
    @PostMapping("/mark")
    ReviewView mark(@RequestBody MarkBody b) {
        if (b.status() == null) {
            throw new IllegalArgumentException("status is required");
        }
        Condition.RestrictionStatus status = Condition.RestrictionStatus.valueOf(b.status().toUpperCase(java.util.Locale.ROOT));
        return view(validation.mark(b.fundName(), b.version(), b.rule(), status, b.reason(), b.validator()));
    }

    @PostMapping("/validate")
    RuleCardView validate(@RequestBody CardBody b) {
        RuleCard card = validation.validate(b.fundName(), b.version(), b.validator());
        return RuleCardView.of(card);
    }

    private static ReviewView view(Review r) {
        return new ReviewView(RuleCardView.of(r.card()), r.contradictions().stream()
                .map(c -> new ContradictionView(c.shareClass() == null ? null : c.shareClass().value(), c.kind(),
                        c.options().stream().map(o -> new OptionView(o.rule(), o.document(), o.summary())).toList()))
                .toList(), r.unreviewedRules(),
                r.signals().stream().map(s -> new SignalView(s.rule(), s.type().name())).toList(), r.canValidate(),
                r.blockers());
    }

    @ExceptionHandler(CannotValidate.class)
    ResponseEntity<List<String>> cannotValidate(CannotValidate e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.reasons());
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<String> notFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<String> refused(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<String> badRequest(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    record DraftSummary(String fundName, int version, List<String> shareClasses) {
    }

    record ReviewView(RuleCardView card, List<ContradictionView> contradictions, List<Integer> unreviewedRules,
            List<SignalView> signals, boolean canValidate, List<String> blockers) {
    }

    record ContradictionView(String shareClass, String kind, List<OptionView> options) {
    }

    record OptionView(int rule, String document, String summary) {
    }

    record SignalView(int rule, String type) {
    }

    record CardBody(String fundName, int version, String validator) {
    }

    record RuleBody(String fundName, int version, int rule, String validator) {
    }

    record FollowBody(String fundName, int version, String shareClass, String kind, String document, String validator) {
    }

    record CorrectBody(String fundName, int version, int rule, RuleCardView.ConditionView condition, String validator) {
    }

    record MarkBody(String fundName, int version, int rule, String status, String reason, String validator) {
    }
}
