package com.fundgate.fundeli5.rulecardpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.bankdata.Confidentiality;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.funddocuments.DocumentId;
import com.fundgate.fundeli5.funddocuments.DocumentUnavailable;
import com.fundgate.fundeli5.funddocuments.FundDocument;
import com.fundgate.fundeli5.funddocuments.FundDocuments;
import com.fundgate.fundeli5.funddocuments.folder.FolderFundDocuments;
import com.fundgate.fundeli5.rulecardpipeline.pdf.PdfBoxPdfText;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourceReaderTest {

    private static final Isin ISIN = new Isin("LU0348926287");
    private static final byte[] BYTES = "%PDF fake".getBytes(StandardCharsets.UTF_8);

    private static String sha(byte[] b) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
    }

    private static FundDocument doc(String hash, Confidentiality confidentiality) {
        return new FundDocument(new DocumentId("d1"), List.of(ISIN), FundDocument.DocumentType.PROSPECTUS, "en",
                null, null, confidentiality, hash, null, "d1.pdf");
    }

    private static SourceReader reader(FundDocument document, byte[] bytes, List<String> pages) {
        FundDocuments port = new FundDocuments() {
            @Override
            public List<FundDocument> documents(Isin isin) {
                return List.of(document);
            }

            @Override
            public byte[] content(DocumentId id) {
                if (bytes == null) {
                    throw new DocumentUnavailable(id, "gone", null);
                }
                return bytes;
            }
        };
        return new SourceReader(port, pdf -> pages);
    }

    @Test
    void readsAPublicDocumentWhoseHashMatches() throws Exception {
        var reading = reader(doc(sha(BYTES), Confidentiality.PUBLIC), BYTES, List.of("p1", "p2")).read(ISIN);
        assertThat(reading.canDraft()).isTrue();
        assertThat(reading.sources().getFirst().pages()).containsExactly("p1", "p2");
        assertThat(reading.refusals()).isEmpty();
    }

    @Test
    void refusesADocumentWithoutAHashAndFetchesNoBytes() {
        var reading = reader(doc(null, Confidentiality.PUBLIC), null, List.of()).read(ISIN);
        assertThat(reading.canDraft()).isFalse();
        assertThat(reading.refusals()).singleElement().satisfies(r -> assertThat(r.getMessage()).contains("content hash"));
    }

    @Test
    void refusesADocumentWhoseBytesDoNotMatchTheHash() throws Exception {
        var reading = reader(doc(sha("other".getBytes(StandardCharsets.UTF_8)), Confidentiality.PUBLIC), BYTES,
                List.of("p")).read(ISIN);
        assertThat(reading.canDraft()).isFalse();
        assertThat(reading.refusals().getFirst().getMessage()).contains("does not match");
    }

    @Test
    void refusesAConfidentialOrUnmarkedDocument() throws Exception {
        for (Confidentiality c : new Confidentiality[] {Confidentiality.CONFIDENTIAL, null}) {
            var reading = reader(doc(sha(BYTES), c), BYTES, List.of("p")).read(ISIN);
            assertThat(reading.canDraft()).isFalse();
            assertThat(reading.refusals().getFirst().getMessage()).contains("public");
        }
    }

    @Test
    void reportsAnUnavailablePdfAsRefused() throws Exception {
        var reading = reader(doc(sha(BYTES), Confidentiality.PUBLIC), null, List.of()).read(ISIN);
        assertThat(reading.refusals().getFirst().getMessage()).contains("unavailable");
    }

    @Test
    void readsPageTextOfADemoProspectusAndFindsAQuoteOnItsPage() {
        var port = new FolderFundDocuments(Path.of("../docs/playbook/contracts/fixtures"), Path.of("../fund-documents"));
        var reading = new SourceReader(port, new PdfBoxPdfText()).read(ISIN);
        assertThat(reading.refusals()).isEmpty();
        var prospectus = reading.sources().stream()
                .filter(s -> s.document().type() == FundDocument.DocumentType.PROSPECTUS).findFirst().orElseThrow();
        assertThat(prospectus.pages().size()).isGreaterThan(10);
        String text = prospectus.pages().stream().filter(p -> p.length() > 200).findFirst().orElseThrow();
        String quote = text.strip().substring(0, 120);
        int onPage = prospectus.pages().indexOf(text) + 1;
        assertThat(QuoteCheck.onItsPage(quote, onPage, prospectus.pages())).isTrue();
        assertThat(QuoteCheck.onItsPage(quote, onPage == 1 ? 2 : 1, prospectus.pages())).isFalse();
    }
}
