package com.fundgate.fundeli5.privacygate;

import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Privacy Gate (ADR 0004, ADR 0009). First swaps every Portfolio Reference for its Portfolio Id
 * through {@link ClientProfiles}; then scans the pseudonymised text with the {@link PersonalDataDetector}.
 * Refuses on an unknown reference, a person left in the text, or a detector that cannot answer.
 * Deterministic given the detector; never calls a model.
 */
public class DefaultPrivacyGate implements PrivacyGate {

    private static final Pattern REFERENCE = Pattern.compile("\\b[A-Z]{3}\\d{8}\\b");

    /** Anything shaped like a reference that the exact pattern missed: other case, a separator, more digits. */
    private static final Pattern LOOKS_LIKE_REFERENCE = Pattern.compile("(?i)[a-z]{3}[\\s_-]?\\d{8}");
    private static final Pattern PORTFOLIO_ID = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private static final System.Logger LOG = System.getLogger(DefaultPrivacyGate.class.getName());

    private final ClientProfiles clientProfiles;
    private final PersonalDataDetector detector;

    public DefaultPrivacyGate(ClientProfiles clientProfiles, PersonalDataDetector detector) {
        this.clientProfiles = clientProfiles;
        this.detector = detector;
    }

    @Override
    public GateOutcome gate(String rawEmail) {
        Map<PortfolioId, PortfolioReference> references = new LinkedHashMap<>();
        Matcher matcher = REFERENCE.matcher(rawEmail);
        StringBuilder text = new StringBuilder();
        while (matcher.find()) {
            PortfolioReference reference = new PortfolioReference(matcher.group());
            Optional<PortfolioId> id = clientProfiles.portfolioId(reference);
            if (id.isEmpty()) {
                return new GateOutcome.Refused("The email names a Portfolio Reference the bank does not know.", List.of());
            }
            references.put(id.get(), reference);
            matcher.appendReplacement(text, Matcher.quoteReplacement(id.get().toString()));
        }
        matcher.appendTail(text);
        if (references.isEmpty()) {
            return new GateOutcome.Refused("The email names no Portfolio Reference.", List.of());
        }
        String pseudonymised = text.toString();
        if (LOOKS_LIKE_REFERENCE.matcher(PORTFOLIO_ID.matcher(pseudonymised).replaceAll("")).find()) {
            return new GateOutcome.Refused("The email holds something that looks like a Portfolio Reference the Gate cannot swap.", List.of());
        }
        List<DetectedSpan> spans;
        try {
            spans = detector.detect(pseudonymised);
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "The personal-data detector failed: " + e.getClass().getSimpleName());
            return new GateOutcome.Refused(
                    "The personal-data check could not run, so the email is treated as naming a client.", List.of());
        }
        List<DetectedSpan> persons = spans.stream().filter(DetectedSpan::isPerson).toList();
        if (!persons.isEmpty()) {
            return new GateOutcome.Refused("The email names a person.", namesIn(pseudonymised, persons));
        }
        return new GateOutcome.Passed(new GatedEmail(pseudonymised, references));
    }

    /** The text of each span that no other span contains, for the dealer's screen. */
    private static List<String> namesIn(String text, List<DetectedSpan> persons) {
        return persons.stream()
                .filter(s -> s.end() <= text.length())
                .filter(s -> persons.stream().noneMatch(o -> o != s && o.start() <= s.start() && s.end() <= o.end()
                        && (o.start() < s.start() || o.end() > s.end())))
                .map(s -> text.substring(s.start(), s.end())).distinct().toList();
    }
}
