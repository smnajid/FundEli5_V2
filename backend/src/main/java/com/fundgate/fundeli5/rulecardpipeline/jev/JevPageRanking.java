package com.fundgate.fundeli5.rulecardpipeline.jev;

import com.fundgate.fundeli5.rulecardpipeline.PageRanking;
import com.fundgate.fundeli5.rulecardpipeline.PageTopic;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link PageRanking} with Jev. Ranking is a knockout of choice questions: per topic the options are
 * the pages of a batch plus "none"; the top {@value #KEEP} of each batch go on until one batch is
 * left. Topics with the same batch share one request. The per-page check asks one yes/no question
 * per topic about each page. Pages over {@value #MAX_PAGE_CHARS} characters are cut.
 */
public class JevPageRanking implements PageRanking {

    static final int BATCH_PAGES = 20;
    static final int BATCH_CHARS = 128_000;
    static final int KEEP = 3;
    static final int MAX_PAGE_CHARS = 6_000;
    static final String NONE = "none";

    private final JevClient client;

    public JevPageRanking(JevClient client) {
        this.client = client;
    }

    @Override
    public Map<PageTopic, List<Integer>> rank(List<String> pages) {
        Map<PageTopic, List<Integer>> candidates = new EnumMap<>(PageTopic.class);
        Map<PageTopic, List<List<Integer>>> dropped = new EnumMap<>(PageTopic.class);
        Map<PageTopic, List<Integer>> finalOrder = new EnumMap<>(PageTopic.class);
        List<Integer> all = new ArrayList<>();
        for (int p = 1; p <= pages.size(); p++) {
            all.add(p);
        }
        for (PageTopic topic : PageTopic.values()) {
            candidates.put(topic, all);
            dropped.put(topic, new ArrayList<>());
        }
        List<PageTopic> active = new ArrayList<>(List.of(PageTopic.values()));
        while (!active.isEmpty()) {
            // one request per distinct batch, carrying the question of every topic that has that batch
            Map<List<Integer>, List<PageTopic>> requests = new LinkedHashMap<>();
            Map<PageTopic, List<List<Integer>>> batchesOf = new EnumMap<>(PageTopic.class);
            for (PageTopic topic : active) {
                List<List<Integer>> batches = batches(candidates.get(topic), pages);
                batchesOf.put(topic, batches);
                for (List<Integer> batch : batches) {
                    requests.computeIfAbsent(batch, b -> new ArrayList<>()).add(topic);
                }
            }
            Map<List<Integer>, Map<String, JevAnswer>> answers = new LinkedHashMap<>();
            requests.forEach((batch, topics) -> answers.put(batch, ask(batch, topics, pages)));
            List<PageTopic> still = new ArrayList<>();
            for (PageTopic topic : active) {
                List<List<Integer>> batches = batchesOf.get(topic);
                List<Integer> advancing = new ArrayList<>();
                List<Integer> out = new ArrayList<>();
                for (List<Integer> batch : batches) {
                    List<Integer> ordered = order(batch, answers.get(batch).get(topic.name()));
                    advancing.addAll(ordered.subList(0, Math.min(KEEP, ordered.size())));
                    out.addAll(ordered.subList(Math.min(KEEP, ordered.size()), ordered.size()));
                }
                if (batches.size() == 1) {
                    finalOrder.put(topic, order(batches.get(0), answers.get(batches.get(0)).get(topic.name())));
                } else {
                    dropped.get(topic).add(out);
                    candidates.put(topic, advancing);
                    still.add(topic);
                }
            }
            active = still;
        }
        Map<PageTopic, List<Integer>> result = new EnumMap<>(PageTopic.class);
        for (PageTopic topic : PageTopic.values()) {
            List<Integer> ranking = new ArrayList<>(finalOrder.get(topic));
            List<List<Integer>> rounds = dropped.get(topic);
            for (int r = rounds.size() - 1; r >= 0; r--) {
                ranking.addAll(rounds.get(r));
            }
            result.put(topic, List.copyOf(ranking));
        }
        return result;
    }

    @Override
    public Map<PageTopic, Double> bestScores(List<String> pages, List<Integer> pageNumbers) {
        Map<PageTopic, Double> best = new EnumMap<>(PageTopic.class);
        for (PageTopic topic : PageTopic.values()) {
            best.put(topic, 0.0);
        }
        Map<String, JevQuestion> questions = new LinkedHashMap<>();
        for (PageTopic topic : PageTopic.values()) {
            questions.put(topic.name(), JevQuestion.yesNo(topic.pageQuestion()));
        }
        for (int number : pageNumbers) {
            Map<String, JevAnswer> answers = client.ask(pageText(pages, number), questions);
            for (PageTopic topic : PageTopic.values()) {
                best.merge(topic, answers.get(topic.name()).probability("yes"), Math::max);
            }
        }
        return best;
    }

    private Map<String, JevAnswer> ask(List<Integer> batch, List<PageTopic> topics, List<String> pages) {
        StringBuilder state = new StringBuilder();
        List<String> options = new ArrayList<>();
        for (int number : batch) {
            state.append("[Page ").append(number).append("]\n").append(pageText(pages, number)).append("\n\n");
            options.add(optionOf(number));
        }
        options.add(NONE);
        Map<String, JevQuestion> questions = new LinkedHashMap<>();
        for (PageTopic topic : topics) {
            questions.put(topic.name(), JevQuestion.choice(topic.question(), options));
        }
        return client.ask(state.toString(), questions);
    }

    private static List<Integer> order(List<Integer> batch, JevAnswer answer) {
        return batch.stream()
                .sorted(Comparator.comparingDouble((Integer p) -> answer.probability(optionOf(p))).reversed()
                        .thenComparing(Comparator.naturalOrder()))
                .toList();
    }

    private static List<List<Integer>> batches(List<Integer> candidates, List<String> pages) {
        List<List<Integer>> batches = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        int chars = 0;
        for (int number : candidates) {
            int size = Math.min(pages.get(number - 1).length(), MAX_PAGE_CHARS);
            if (!current.isEmpty() && (current.size() >= BATCH_PAGES || chars + size > BATCH_CHARS)) {
                batches.add(current);
                current = new ArrayList<>();
                chars = 0;
            }
            current.add(number);
            chars += size;
        }
        if (!current.isEmpty()) {
            batches.add(current);
        }
        return batches;
    }

    private static String optionOf(int page) {
        return "page " + page;
    }

    private static String pageText(List<String> pages, int number) {
        String text = pages.get(number - 1);
        return text.length() > MAX_PAGE_CHARS ? text.substring(0, MAX_PAGE_CHARS) : text;
    }
}
