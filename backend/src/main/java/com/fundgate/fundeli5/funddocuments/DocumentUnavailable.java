package com.fundgate.fundeli5.funddocuments;

/** The PDF of a document cannot be read now. */
public class DocumentUnavailable extends RuntimeException {

    public DocumentUnavailable(DocumentId id, String reason, Throwable cause) {
        super("Document " + id + " unavailable: " + reason, cause);
    }
}
