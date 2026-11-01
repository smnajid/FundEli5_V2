package com.fundgate.fundeli5.ruleengine;

import com.fundgate.fundeli5.bankdata.EditionDate;
import java.util.Optional;

/** The edition of a Fund Document that the bank's fund master data lists now; empty when it does not say. */
@FunctionalInterface
public interface EditionLookup {

    Optional<EditionDate> listedEdition(String documentId);

    static EditionLookup none() {
        return documentId -> Optional.empty();
    }
}
