package com.fundgate.fundeli5.funddocuments;

import com.fundgate.fundeli5.bankdata.Confidentiality;
import com.fundgate.fundeli5.bankdata.EditionDate;
import com.fundgate.fundeli5.bankdata.Isin;
import java.util.List;

/**
 * One document of a fund. Optional fields are {@code null} when unknown. The pipeline refuses a
 * document without a {@code contentSha256} or whose confidentiality is not {@code PUBLIC}.
 */
public record FundDocument(
        DocumentId documentId,
        List<Isin> isins,
        DocumentType type,
        String language,
        EditionDate fmdEditionDate,
        EditionDate pdfEditionDate,
        Confidentiality confidentiality,
        String contentSha256,
        String sourceUrl,
        String file) {

    public enum DocumentType { PROSPECTUS, KID, SUPPLEMENT, FUND_CONTRACT, PPM, OTHER }

    public FundDocument {
        java.util.Objects.requireNonNull(documentId);
        isins = List.copyOf(isins);
        type = type == null ? DocumentType.OTHER : type;
    }
}
