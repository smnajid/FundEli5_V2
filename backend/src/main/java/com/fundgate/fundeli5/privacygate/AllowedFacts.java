package com.fundgate.fundeli5.privacygate;

import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import java.util.List;
import java.util.Map;

/**
 * The client facts of one portfolio that one call may carry, keyed by Portfolio Id. Only the
 * Privacy Gate builds one. Unknown facts are left out, never defaulted.
 */
public record AllowedFacts(PortfolioId portfolio, Map<Fact, String> values) {

    public AllowedFacts {
        values = Map.copyOf(values);
    }

    /** What the Check Record logs as "the client fields sent". */
    public List<Fact> fieldsSent() {
        return values.keySet().stream().sorted().toList();
    }
}
