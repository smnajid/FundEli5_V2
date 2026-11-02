package com.fundgate.fundeli5.rulecardpipeline;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.ruleengine.RuleCatalog;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The drafting request, assembled from the {@link RuleCatalog}: one line per rule kind, so a new kind
 * reaches the prompt with its plug-in. The documents are public Fund Documentation, page by page.
 */
final class DraftingPrompt {

    private static final String INTRO = """
            You extract a Rule Card from fund documentation for a private bank's fund dealing desk.
            A Rule Card is the set of Hard Restrictions and Target Market criteria deciding whether a client may subscribe to one Share Class.
            Every rule must be supported by a quote copied word for word from the pages; give its document, page number and quote.
            Mark a rule's scope as fund (applies to every class) or share_class.
            When two documents contradict each other on a rule, never resolve it: write each document's rule with the value that document literally states, set contradiction to true on both, and cite both passages.
            Ignore fees, investment policy, tax and operations unless they restrict who may subscribe.
            Any restriction on who may subscribe that no kind below expresses is an unmodelled_restriction.
            """;

    private static final String FORMAT = """

            Answer with one JSON object and nothing else:
            {"fundName": "...", "rules": [{"scope": "fund" | "share_class", "shareClass": "<ISIN>" (share_class scope only, else null),
              "condition": {"kind": "<kind>", ...fields of that kind as described above},
              "citations": [{"document": "<document id>", "page": <page number>, "quote": "<exact words from that page>"}],
              "summary": "<one sentence>", "confidence": <0..1>, "contradiction": true | false}]}
            """;

    private DraftingPrompt() {
    }

    static String system(RuleCatalog catalog) {
        String kinds = catalog.draftingLines().stream().map(l -> "- " + l).collect(Collectors.joining("\n"));
        return INTRO + "\nRule kinds (use only these; the condition's kind is the word before the colon):\n" + kinds
                + "\n" + FORMAT;
    }

    /** The share classes and the selected pages of each document; {@code pages} maps a document to its 1-based pages. */
    static String documents(List<Isin> shareClasses, List<SourceDocument> sources, Map<SourceDocument, List<Integer>> pages) {
        StringBuilder out = new StringBuilder("Share classes: ");
        out.append(shareClasses.stream().map(Isin::value).collect(Collectors.joining(", "))).append("\n");
        for (SourceDocument source : sources) {
            out.append("\n=== document ").append(source.document().documentId()).append(" (")
                    .append(source.document().type().name().toLowerCase(java.util.Locale.ROOT)).append(") ===\n");
            for (int page : pages.get(source)) {
                out.append("--- page ").append(page).append(" ---\n").append(source.pages().get(page - 1)).append("\n");
            }
        }
        return out.toString();
    }
}
