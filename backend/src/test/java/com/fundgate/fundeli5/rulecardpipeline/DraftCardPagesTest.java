package com.fundgate.fundeli5.rulecardpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.funddocuments.folder.FolderFundDocuments;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.folder.FolderSourcePages;
import com.fundgate.fundeli5.rulecard.json.RuleCardJson;
import com.fundgate.fundeli5.rulecardpipeline.pdf.PdfBoxPdfText;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Every committed Draft card has, beside it, the text of each page it cites, so the validation screen can show
 * the page around the quote with no Fund Document read at run time. Run with {@code -Dregenerate.pages=true}
 * to rewrite the committed texts from the PDFs (the pipeline does the same for new Drafts).
 */
class DraftCardPagesTest {

    private static final Path FIXTURES = Path.of("../docs/playbook/contracts/fixtures");
    private static final Path CARDS = FIXTURES.resolve("draft-rule-cards");
    private static final Path PAGES = FIXTURES.resolve("draft-rule-card-pages");

    @Test
    void everyCitedPageOfACommittedDraftIsKeptWithItsQuoteOnIt() throws Exception {
        if (Boolean.getBoolean("regenerate.pages")) {
            regenerate();
        }
        var pages = new FolderSourcePages(List.of(PAGES));
        try (var files = Files.list(CARDS)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                RuleCard card = RuleCardJson.read(file);
                for (var rule : card.rules()) {
                    for (var c : rule.citations()) {
                        String text = pages.text(card.fundName(), card.version(), c.document(), c.page()).orElse(null);
                        assertThat(text).as("%s p.%d of %s", c.document(), c.page(), card.fundName()).isNotBlank();
                        if (c.quoteFoundOnPage()) {
                            assertThat(QuoteCheck.check(c, java.util.stream.IntStream.rangeClosed(1, c.page()).mapToObj(i -> i == c.page() ? text : "").toList()).quoteFoundOnPage()).as("quote on kept page").isTrue();
                        }
                    }
                }
            }
        }
    }

    private void regenerate() throws Exception {
        var reader = new SourceReader(new FolderFundDocuments(FIXTURES, Path.of("../fund-documents")), new PdfBoxPdfText());
        try (var files = Files.list(CARDS)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                RuleCard card = RuleCardJson.read(file);
                var reading = reader.read(new Isin(card.shareClasses().get(0).value()));
                Map<String, Map<Integer, String>> cited = new TreeMap<>();
                for (var rule : card.rules()) {
                    for (var c : rule.citations()) {
                        reading.sources().stream().filter(s -> s.document().documentId().value().equals(c.document()))
                                .findFirst().ifPresent(s -> cited.computeIfAbsent(c.document(), d -> new TreeMap<>())
                                        .put(c.page(), s.pages().get(c.page() - 1)));
                    }
                }
                FolderSourcePages.write(PAGES, card.fundName(), card.version(), cited);
            }
        }
    }
}
