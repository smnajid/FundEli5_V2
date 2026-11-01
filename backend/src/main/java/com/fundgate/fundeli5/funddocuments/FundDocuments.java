package com.fundgate.fundeli5.funddocuments;

import com.fundgate.fundeli5.bankdata.Isin;
import java.util.List;

/** Out port (ADR 0008): the documents of a fund, for the offline pipeline only (ADR 0004). */
public interface FundDocuments {

    /** Every document of the fund the Share Class belongs to: its prospectus and every KID. */
    List<FundDocument> documents(Isin anyShareClassOfTheFund);

    /**
     * The PDF.
     *
     * @throws DocumentUnavailable when the bytes cannot be read
     */
    byte[] content(DocumentId id);
}
