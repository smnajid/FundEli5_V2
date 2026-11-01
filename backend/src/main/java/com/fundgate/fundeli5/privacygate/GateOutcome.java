package com.fundgate.fundeli5.privacygate;

import java.util.List;

/** Either an email safe to send to a model, or the reason it is not (and the dealer enters it by hand). */
public sealed interface GateOutcome {

    record Passed(GatedEmail email) implements GateOutcome {
    }

    /** {@code namesFound} are the client names that stopped the email, for the dealer's screen. */
    record Refused(String reason, List<String> namesFound) implements GateOutcome {
        public Refused {
            namesFound = List.copyOf(namesFound);
        }
    }
}
