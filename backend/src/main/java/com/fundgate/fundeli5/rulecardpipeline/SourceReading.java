package com.fundgate.fundeli5.rulecardpipeline;

import java.util.List;

/** What the pipeline could read for a fund: admitted documents and the refusals with their reasons. */
public record SourceReading(List<SourceDocument> sources, List<DocumentRefused> refusals) {

    public SourceReading {
        sources = List.copyOf(sources);
        refusals = List.copyOf(refusals);
    }

    /** There is something to draft from. */
    public boolean canDraft() {
        return !sources.isEmpty();
    }
}
