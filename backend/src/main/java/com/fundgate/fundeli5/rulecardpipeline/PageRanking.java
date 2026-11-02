package com.fundgate.fundeli5.rulecardpipeline;

import java.util.List;
import java.util.Map;

/**
 * Ranks the pages of one public Fund Document per {@link PageTopic}. Only document text goes through
 * this port, never client data (ADR 0004). Page numbers are 1-based.
 */
public interface PageRanking {

    /** For each topic, the pages worth reading, best first. */
    Map<PageTopic, List<Integer>> rank(List<String> pages);

    /** For each topic, the best 0..1 score among the given pages: does one of them really state it? */
    Map<PageTopic, Double> bestScores(List<String> pages, List<Integer> pageNumbers);
}
