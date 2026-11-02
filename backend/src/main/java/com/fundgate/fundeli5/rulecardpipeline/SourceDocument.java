package com.fundgate.fundeli5.rulecardpipeline;

import com.fundgate.fundeli5.funddocuments.FundDocument;
import java.util.List;

/** An admitted Fund Document with the text of each page (element 0 is page 1). */
public record SourceDocument(FundDocument document, List<String> pages) {

    public SourceDocument {
        pages = List.copyOf(pages);
    }
}
