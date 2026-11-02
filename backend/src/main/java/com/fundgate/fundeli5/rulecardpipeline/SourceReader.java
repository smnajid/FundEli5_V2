package com.fundgate.fundeli5.rulecardpipeline;

import com.fundgate.fundeli5.bankdata.Confidentiality;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.funddocuments.DocumentUnavailable;
import com.fundgate.fundeli5.funddocuments.FundDocument;
import com.fundgate.fundeli5.funddocuments.FundDocuments;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * The first step of the pipeline: admits only public Fund Documentation whose bytes match the manifest
 * (fund-documents.md "Outcomes"), then reads the text of each page. It knows the {@link FundDocuments}
 * port and nothing about clients (ADR 0004).
 */
public class SourceReader {

    private final FundDocuments documents;
    private final PdfText pdfText;

    public SourceReader(FundDocuments documents, PdfText pdfText) {
        this.documents = documents;
        this.pdfText = pdfText;
    }

    /**
     * Reads every document of the fund the Share Class belongs to. A refused document is reported, not
     * thrown. A document whose PDF is unavailable is reported too: no draft from it this run.
     */
    public SourceReading read(Isin anyShareClassOfTheFund) {
        List<SourceDocument> sources = new ArrayList<>();
        List<DocumentRefused> refusals = new ArrayList<>();
        for (FundDocument document : documents.documents(anyShareClassOfTheFund)) {
            try {
                sources.add(read(document));
            } catch (DocumentRefused refused) {
                refusals.add(refused);
            }
        }
        return new SourceReading(sources, refusals);
    }

    /** Admission checks that need no bytes run first, so a refused document is never fetched. */
    SourceDocument read(FundDocument document) {
        if (document.contentSha256() == null || document.contentSha256().isBlank()) {
            throw new DocumentRefused(document.documentId(),
                    "its metadata has no content hash, so the file cannot be trusted to be the real document");
        }
        if (document.confidentiality() != Confidentiality.PUBLIC) {
            throw new DocumentRefused(document.documentId(),
                    "it is not marked public; the pipeline drafts public Fund Documentation only");
        }
        byte[] pdf;
        try {
            pdf = documents.content(document.documentId());
        } catch (DocumentUnavailable unavailable) {
            throw new DocumentRefused(document.documentId(), "its PDF is unavailable: " + unavailable.getMessage());
        }
        if (!sha256(pdf).equalsIgnoreCase(document.contentSha256().strip())) {
            throw new DocumentRefused(document.documentId(), "its PDF does not match the content hash in the metadata");
        }
        try {
            return new SourceDocument(document, pdfText.pages(pdf));
        } catch (RuntimeException unreadable) {
            throw new DocumentRefused(document.documentId(), "its PDF cannot be read: " + unreadable.getMessage());
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
