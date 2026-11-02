package com.fundgate.fundeli5.rulecardpipeline;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chooses the pages to draft from: the front matter, then round-robin over the topic rankings, with
 * the page before each topic's top pages, up to a budget. A document no longer than the budget is
 * sent whole. A per-page check then flags the topics whose best selected page scores low.
 */
public final class PageSelector {

    public static final int DEFAULT_BUDGET = 25;
    public static final int FRONT_PAGES = 5;
    public static final double POSSIBLY_MISSED_BELOW = 0.5;
    private static final int TOP_WITH_PREVIOUS = 2;

    private final PageRanking ranking;
    private final int budget;

    public PageSelector(PageRanking ranking, int budget) {
        if (budget < FRONT_PAGES) {
            throw new IllegalArgumentException("The budget must be at least " + FRONT_PAGES);
        }
        this.ranking = ranking;
        this.budget = budget;
    }

    public PageSelection select(List<String> pages) {
        if (pages.isEmpty()) {
            return new PageSelection(List.of(), Set.of());
        }
        Set<Integer> chosen = new LinkedHashSet<>();
        if (pages.size() <= budget) {
            for (int p = 1; p <= pages.size(); p++) {
                chosen.add(p);
            }
        } else {
            chosen.addAll(roundRobin(pages));
        }
        List<Integer> sorted = chosen.stream().sorted().toList();
        Map<PageTopic, Double> scores = ranking.bestScores(pages, sorted);
        Set<PageTopic> missed = EnumSet.noneOf(PageTopic.class);
        for (PageTopic topic : PageTopic.values()) {
            if (scores.getOrDefault(topic, 0.0) < POSSIBLY_MISSED_BELOW) {
                missed.add(topic);
            }
        }
        return new PageSelection(sorted, missed);
    }

    private Set<Integer> roundRobin(List<String> pages) {
        Set<Integer> chosen = new LinkedHashSet<>();
        for (int p = 1; p <= FRONT_PAGES && p <= pages.size(); p++) {
            chosen.add(p);
        }
        Map<PageTopic, List<Integer>> ranked = ranking.rank(pages);
        List<List<Integer>> queues = new ArrayList<>();
        for (PageTopic topic : PageTopic.values()) {
            List<Integer> queue = new ArrayList<>();
            List<Integer> top = ranked.getOrDefault(topic, List.of());
            for (int i = 0; i < top.size(); i++) {
                int page = top.get(i);
                if (page < 1 || page > pages.size()) {
                    continue;
                }
                queue.add(page);
                if (i < TOP_WITH_PREVIOUS && page > 1) {
                    queue.add(page - 1);
                }
            }
            queues.add(queue);
        }
        int[] next = new int[queues.size()];
        boolean progress = true;
        while (chosen.size() < budget && progress) {
            progress = false;
            for (int q = 0; q < queues.size() && chosen.size() < budget; q++) {
                List<Integer> queue = queues.get(q);
                while (next[q] < queue.size()) {
                    boolean added = chosen.add(queue.get(next[q]++));
                    progress = true;
                    if (added) {
                        break;
                    }
                }
            }
        }
        return chosen;
    }
}
