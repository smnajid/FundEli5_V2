package com.fundgate.fundeli5.rulecardpipeline;

import com.fundgate.fundeli5.funddocuments.DocumentId;

/** The pipeline will not draft from this document, and says why. */
public class DocumentRefused extends RuntimeException {

    private final DocumentId document;

    public DocumentRefused(DocumentId document, String reason) {
        super("Refused " + document + ": " + reason);
        this.document = document;
    }

    public DocumentId document() {
        return document;
    }
}
