package com.fundgate.fundeli5.rulecardpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.citation.Citation;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuoteCheckTest {

    @Test
    void findsAQuoteVerbatim() {
        assertThat(QuoteCheck.found("institutional investors", "Shares are for institutional investors only.")).isTrue();
    }

    @Test
    void rejectsAQuoteThePageDoesNotContain() {
        assertThat(QuoteCheck.found("retail investors", "Shares are for institutional investors only.")).isFalse();
    }

    @Test
    void treatsAnyRunOfWhitespaceAsOneSpace() {
        assertThat(QuoteCheck.found("reject or close", "the Board may reject\n  or\tclose the account")).isTrue();
    }

    @Test
    void treatsNoBreakAndOtherUnicodeSpacesAsWhitespace() {
        // French typography: U+00A0 and U+202F before punctuation; U+2003 em space
        assertThat(QuoteCheck.found("minimum 1 000 EUR", "minimum\u00A01\u202F000\u2003EUR")).isTrue();
        assertThat(QuoteCheck.found("minimum\u00A01 000 EUR", "minimum 1\u00A0000 EUR")).isTrue();
    }

    @Test
    void readsAControlCharacterAsAHyphen() {
        // The Citations API returns a line-break hyphen as U+0002
        assertThat(QuoteCheck.found("sub\u0002fund", "the sub-fund holds")).isTrue();
        assertThat(QuoteCheck.found("sub-fund", "the sub\u0002fund holds")).isTrue();
    }

    @Test
    void ignoresSoftHyphens() {
        assertThat(QuoteCheck.found("investors", "inves\u00ADtors only")).isTrue();
        assertThat(QuoteCheck.found("inves\u00ADtors", "investors only")).isTrue();
    }

    @Test
    void aHyphenAtALineBreakMatchesTheQuoteWithoutTheBreak() {
        assertThat(QuoteCheck.found("sub-fund", "the sub-\nfund holds")).isTrue();
        assertThat(QuoteCheck.found("sub-fund", "the sub\u0002\nfund holds")).isTrue();
    }

    @Test
    void nextLineIsWhitespaceNotAHyphen() {
        assertThat(QuoteCheck.found("one two", "one\u0085two")).isTrue();
    }

    @Test
    void anEmptyQuoteIsNeverFound() {
        assertThat(QuoteCheck.found("  ", "any page")).isFalse();
    }

    @Test
    void checksACitationOnItsOwnPageOnly() {
        List<String> pages = List.of("front matter", "Shares are for institutional investors only.");
        assertThat(QuoteCheck.onItsPage("institutional investors", 2, pages)).isTrue();
        assertThat(QuoteCheck.onItsPage("institutional investors", 1, pages)).isFalse();
        assertThat(QuoteCheck.onItsPage("institutional investors", 3, pages)).isFalse();
        assertThat(QuoteCheck.onItsPage("institutional investors", 0, pages)).isFalse();
    }

    @Test
    void marksACitationWithTheResult() {
        List<String> pages = List.of("Shares are for institutional investors only.");
        Citation checked = QuoteCheck.check(new Citation("p.pdf", 1, "institutional investors", false), pages);
        assertThat(checked.quoteFoundOnPage()).isTrue();
        assertThat(QuoteCheck.check(new Citation("p.pdf", 1, "made up", true), pages).quoteFoundOnPage()).isFalse();
    }
}
