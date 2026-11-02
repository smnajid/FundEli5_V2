package com.fundgate.fundeli5.rulecardpipeline;

import com.fundgate.fundeli5.citation.Citation;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The quote check: every cited quote must be found on its cited page. Both sides are normalised the
 * same way, so only real differences in wording fail.
 *
 * <ul>
 *   <li>whitespace, including the no-break spaces of French typography, is matched with
 *       {@code [\s\p{Z}]} (plain {@code \s} misses U+00A0) and collapsed to one space;
 *   <li>a control character is read as a hyphen (the Citations API returns a line-break hyphen as
 *       U+0002);
 *   <li>soft hyphens (U+00AD) are ignored.
 * </ul>
 */
public final class QuoteCheck {

    private static final Pattern SOFT_HYPHEN = Pattern.compile("\u00AD");
    private static final Pattern CONTROL = Pattern.compile("\\p{Cc}");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\p{Z}\u0085]+");
    private static final Pattern HYPHEN_THEN_SPACE = Pattern.compile("- ");

    private QuoteCheck() {
    }

    /** Whether the quote occurs in the page text after normalisation. A blank quote is never found. */
    public static boolean found(String quote, String pageText) {
        String wanted = normalise(quote);
        return !wanted.isEmpty() && normalise(pageText).contains(wanted);
    }

    /** Whether the quote is on the given page (1-based) of the document's page texts. */
    public static boolean onItsPage(String quote, int page, List<String> pages) {
        return page >= 1 && page <= pages.size() && found(quote, pages.get(page - 1));
    }

    /** The citation with {@code quoteFoundOnPage} set to what the check finds, whatever it claimed. */
    public static Citation check(Citation citation, List<String> pages) {
        return new Citation(citation.document(), citation.page(), citation.quote(),
                onItsPage(citation.quote(), citation.page(), pages));
    }

    private static String normalise(String text) {
        // Soft hyphens go first: a soft hyphen is a Cf character, not a control one, but must vanish
        // before whitespace is collapsed.
        String s = SOFT_HYPHEN.matcher(text).replaceAll("");
        // Whitespace controls (tab, newline, ...) are whitespace; every other control is a hyphen.
        s = WHITESPACE.matcher(s).replaceAll(" ");
        s = CONTROL.matcher(s).replaceAll("-");
        // A line-break hyphen leaves "sub-\nfund" as "sub- fund": the space after a hyphen is dropped
        // on both sides, so it matches "sub-fund" and "subfund" alike only in that position.
        s = HYPHEN_THEN_SPACE.matcher(s).replaceAll("-");
        return s.strip();
    }
}
