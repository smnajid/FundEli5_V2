package com.fundgate.fundeli5.answering;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.eligibility.Justification;
import com.fundgate.fundeli5.eligibility.Verdict;
import java.math.BigDecimal;
import java.util.List;

/**
 * What the Fund Dealer gets back. When the Privacy Gate refused, {@code refusal} is set, no model was
 * called and the dealer fills the request form by hand.
 */
public record Answer(Refusal refusal, boolean aiUsed, List<Check> checks, List<String> unclear, String reply) {

    public Answer {
        checks = List.copyOf(checks);
        unclear = List.copyOf(unclear);
    }

    public record Refusal(String reason, List<String> namesFound) {
    }

    /** One Eligibility Check. */
    public record Check(PortfolioReference portfolio, Isin shareClass, String shareClassName, BigDecimal amount,
            Currency currency, boolean clientInitiated, Verdict verdict, Justification justification) {
    }

    static Answer refused(String reason, List<String> namesFound) {
        return new Answer(new Refusal(reason, namesFound), false, List.of(), List.of(), null);
    }
}
