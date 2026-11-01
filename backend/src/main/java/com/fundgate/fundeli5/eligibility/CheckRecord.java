package com.fundgate.fundeli5.eligibility;

import com.fundgate.fundeli5.clientprofiles.ClientProfile;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Everything one Eligibility Check read and produced. Append-only. The Check Record module owns
 * storing it; the shape sits here so every module can name it. {@code ruleCardVersion} and
 * {@code clientProfile} are {@code null} when the check ended before reading them.
 */
public record CheckRecord(
        UUID id,
        Instant checkedAt,
        SubscriptionRequest request,
        Integer ruleCardVersion,
        String scaleMappingVersion,
        String engineVersion,
        ClientProfile clientProfile,
        Verdict verdict,
        Justification justification,
        List<LlmCall> llmCalls,
        List<SuggestionDecision> suggestionDecisions) {

    /** One call to a model: what was sent after the Privacy Gate, and what came back. */
    public record LlmCall(String model, String gatedPrompt, List<String> fieldsSent, String response) {
        public LlmCall {
            fieldsSent = List.copyOf(fieldsSent);
        }
    }

    public enum Decision { ACCEPTED, DISMISSED }

    /** The Fund Dealer's decision on one Advisory Suggestion. */
    public record SuggestionDecision(String suggestion, Decision decision, String by, Instant at, String note) {
    }

    public CheckRecord {
        java.util.Objects.requireNonNull(id);
        java.util.Objects.requireNonNull(checkedAt);
        java.util.Objects.requireNonNull(request);
        java.util.Objects.requireNonNull(verdict);
        java.util.Objects.requireNonNull(justification);
        llmCalls = List.copyOf(llmCalls);
        suggestionDecisions = List.copyOf(suggestionDecisions);
    }
}
