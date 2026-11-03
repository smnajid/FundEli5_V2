package com.fundgate.fundeli5.answering;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.citation.Citation;
import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.eligibility.Justification;
import com.fundgate.fundeli5.eligibility.RuleOutcome;
import com.fundgate.fundeli5.eligibility.Verdict;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CitationMarkersTest {

    private static final PortfolioId ID = new PortfolioId(UUID.fromString("9ae403db-1754-4033-9f06-a99edf92a574"));
    private static final Citation CATEGORY = new Citation("Nordea 1 prospectus", 123, "Institutional investors.", true);
    private static final Citation MINIMUM = new Citation("Nordea 1 prospectus", 123, "EUR 75,000", true);

    private static Answer.Check check(String isin, String name, RuleOutcome... outcomes) {
        return new Answer.Check(new PortfolioReference("LOC90000101"), new Isin(isin), name, BigDecimal.TEN,
                new Currency("EUR"), false, Verdict.NOT_ELIGIBLE, new Justification(List.of(outcomes)));
    }

    @Test
    void numbersTheMarkersOfEachLineByTheCardOfThatLine() {
        var bi = check("LU0000000001", "BI-EUR",
                new RuleOutcome(RuleKind.INVESTOR_CATEGORY, RuleOutcome.Result.FAIL, "x", List.of(CATEGORY)));
        var bp = check("LU0000000002", "BP-EUR",
                new RuleOutcome(RuleKind.INVESTOR_CATEGORY, RuleOutcome.Result.PASS, "y", List.of(CATEGORY)),
                new RuleOutcome(RuleKind.MINIMUM_INITIAL_SUBSCRIPTION, RuleOutcome.Result.PASS, "z", List.of(MINIMUM)));
        var numbering = new Evidence.Numbering(List.of(bi, bp));

        var numbered = CitationMarkers.number("Hi\n- " + ID + ", BI-EUR: no [Nordea 1 prospectus, p. 123]\n- " + ID
                + ", BP-EUR: yes [Nordea 1 prospectus, p. 123; Nordea 1 prospectus, p. 123, quote not verified]",
                List.of(bi, bp), List.of(ID, ID), numbering);

        assertThat(numbered.text()).contains("BI-EUR: no [Nordea 1 prospectus, p. 123][1]").contains("[2][3]");
        assertThat(numbered.cited()).containsExactly(1, 2, 3);
        assertThat(numbering.cards(numbered.cited())).hasSize(3).allMatch(Evidence::citedInReply);
    }

    @Test
    void leavesAnUnknownPassageAsWritten() {
        var bi = check("LU0000000001", "BI-EUR");
        var numbered = CitationMarkers.number(ID + " BI-EUR [Other, p. 1]", List.of(bi), List.of(ID),
                new Evidence.Numbering(List.of(bi)));
        assertThat(numbered.text()).endsWith("[Other, p. 1]");
        assertThat(numbered.cited()).isEmpty();
    }
}
