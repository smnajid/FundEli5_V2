package com.fundgate.fundeli5.rulecardpipeline.jev;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.rulecardpipeline.PageRanking;
import com.fundgate.fundeli5.rulecardpipeline.PageSelection;
import com.fundgate.fundeli5.rulecardpipeline.PageSelector;
import com.fundgate.fundeli5.rulecardpipeline.PageTopic;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Ranking and selection against a stub Jev that "knows" which page answers each topic: a page whose text
 * contains the topic's name is the answer for that choice question and scores 0.9 on the yes/no one.
 */
class JevPageRankingTest {

    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    private final List<String> bodies = new ArrayList<>();
    private static final Pattern PAGE = Pattern.compile("\\[Page (\\d+)]\\n(.*?)\\n\\n");

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/systemone", exchange -> {
            requests.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            bodies.add(body);
            byte[] out = answer(body).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    /** Page text "TOPIC" marks the pages that answer that topic. */
    private static String answer(String body) {
        StringBuilder out = new StringBuilder("{\"answers\":{");
        boolean first = true;
        for (PageTopic topic : PageTopic.values()) {
            if (!body.contains("\"" + topic.name() + "\":")) {
                continue;
            }
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append('"').append(topic.name()).append("\":{");
            if (body.contains("\"type\":\"choice\"")) {
                Matcher m = PAGE.matcher(body.replace("\\n", "\n"));
                List<String> parts = new ArrayList<>();
                while (m.find()) {
                    parts.add("\"page " + m.group(1) + "\":" + (m.group(2).contains(topic.name()) ? "0.8" : "0.01"));
                }
                parts.add("\"none\":0.1");
                out.append("\"probabilities\":{").append(String.join(",", parts)).append("},\"confidence\":0.9}");
            } else {
                String yes = body.contains(topic.name() + " here") ? "0.9" : "0.1";
                out.append("\"type\":\"noul\",\"noul\":").append(yes).append('}');
            }
        }
        return out.append("}}").toString();
    }

    private PageRanking ranking() {
        return new JevPageRanking(new JevClient("http://127.0.0.1:" + server.getAddress().getPort(), "k",
                "jev-latest", Duration.ofSeconds(5)));
    }

    private static List<String> document(int size, Map<Integer, PageTopic> answers) {
        List<String> pages = new ArrayList<>();
        for (int p = 1; p <= size; p++) {
            PageTopic t = answers.get(p);
            pages.add("text of page " + p + (t == null ? "" : " " + t.name() + " here"));
        }
        return pages;
    }

    @Test
    void theKnockoutFindsTheAnswerPageOfEachTopicAcrossBatches() {
        var pages = document(95, Map.of(77, PageTopic.MINIMUM, 12, PageTopic.US_PERSON, 90, PageTopic.TARGET_MARKET));
        var ranked = ranking().rank(pages);
        assertThat(ranked.get(PageTopic.MINIMUM).get(0)).isEqualTo(77);
        assertThat(ranked.get(PageTopic.US_PERSON).get(0)).isEqualTo(12);
        assertThat(ranked.get(PageTopic.TARGET_MARKET).get(0)).isEqualTo(90);
        assertThat(ranked.get(PageTopic.MINIMUM)).doesNotHaveDuplicates().hasSize(95);
    }

    @Test
    void roundOneSharesOneRequestPerBatchBetweenTheTopics() {
        ranking().rank(document(40, Map.of()));
        // two batches of 20 answer all six topics at once; the 6 pages that go on are one shared final batch
        assertThat(bodies.get(0)).contains("US_PERSON").contains("MINIMUM");
        assertThat(requests.get()).isEqualTo(2 + 1);
    }

    @Test
    void aSmallDocumentIsSentWholeAndNothingIsRanked() {
        var pages = document(10, Map.of());
        PageSelection selection = new PageSelector(ranking(), 25).select(pages);
        assertThat(selection.pages()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(bodies).allSatisfy(b -> assertThat(b).contains("\"type\":\"noul\""));
    }

    @Test
    void aBigDocumentGetsFrontPagesTheTopPagesAndThePageBeforeWithinTheBudget() {
        var pages = document(300, Map.of(200, PageTopic.MINIMUM, 150, PageTopic.US_PERSON,
                250, PageTopic.TARGET_MARKET, 100, PageTopic.INVESTOR_ELIGIBILITY, 60, PageTopic.SUB_FUND,
                280, PageTopic.OTHER_RESTRICTION));
        PageSelection selection = new PageSelector(ranking(), 25).select(pages);
        assertThat(selection.pages()).hasSize(25).isSorted()
                .contains(1, 2, 3, 4, 5, 200, 150, 250, 100, 60, 280)
                .contains(199, 149, 249, 99, 59, 279);
        assertThat(selection.possiblyMissed()).isEmpty();
    }

    @Test
    void aTopicNoSelectedPageAnswersIsFlaggedPossiblyMissed() {
        var pages = document(300, Map.of(200, PageTopic.MINIMUM));
        PageSelection selection = new PageSelector(ranking(), 25).select(pages);
        assertThat(selection.possiblyMissed()).contains(PageTopic.US_PERSON).doesNotContain(PageTopic.MINIMUM);
    }

    @Test
    void anEmptyDocumentRanksNothingAndTerminates() {
        assertThat(ranking().rank(List.of()).get(PageTopic.MINIMUM)).isEmpty();
        assertThat(requests.get()).isZero();
    }

    @Test
    void aSecondRunReadsTheCacheAndCallsJevNoMore(@TempDir Path dir) {
        var pages = document(300, Map.of(200, PageTopic.MINIMUM));
        PageSelection first = new PageSelector(new CachedPageRanking(ranking(), dir, "t"), 25).select(pages);
        int after = requests.get();
        PageSelection second = new PageSelector(new CachedPageRanking(ranking(), dir, "t"), 25).select(pages);
        assertThat(requests.get()).isEqualTo(after);
        assertThat(second).isEqualTo(first);
    }
}
