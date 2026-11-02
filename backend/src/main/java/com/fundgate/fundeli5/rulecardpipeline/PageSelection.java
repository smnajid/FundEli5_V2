package com.fundgate.fundeli5.rulecardpipeline;

import java.util.List;
import java.util.Set;

/** The pages to send to drafting (ascending, 1-based) and the topics no selected page scores well for. */
public record PageSelection(List<Integer> pages, Set<PageTopic> possiblyMissed) {

    public PageSelection {
        pages = List.copyOf(pages);
        possiblyMissed = Set.copyOf(possiblyMissed);
    }
}
