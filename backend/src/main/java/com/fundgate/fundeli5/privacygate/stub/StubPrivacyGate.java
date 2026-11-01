package com.fundgate.fundeli5.privacygate.stub;

import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.privacygate.GateOutcome;
import com.fundgate.fundeli5.privacygate.GatedEmail;
import com.fundgate.fundeli5.privacygate.PrivacyGate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Walking-skeleton Gate: swaps references for ids and refuses an unknown reference. It does not yet
 * look for client names or build fact allow-lists; the real Gate replaces it.
 */
public class StubPrivacyGate implements PrivacyGate {

    private static final Pattern REFERENCE = Pattern.compile("\\b[A-Z]{3}\\d{8}\\b");

    private final ClientProfiles clientProfiles;

    public StubPrivacyGate(ClientProfiles clientProfiles) {
        this.clientProfiles = clientProfiles;
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
        return new GateOutcome.Passed(new GatedEmail(text.toString(), references));
    }
}
