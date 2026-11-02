package com.fundgate.fundeli5.rulecardpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.AnsweringConfigurationAccess;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.funddocuments.folder.FolderFundDocuments;
import com.fundgate.fundeli5.llm.replay.ReplayLlmEgress;
import com.fundgate.fundeli5.rulecardpipeline.jev.CachedPageRanking;
import com.fundgate.fundeli5.rulecardpipeline.pdf.PdfBoxPdfText;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The four demo funds draft offline: committed Jev rankings (a copy of the live ones) and the committed
 * drafting recordings. A ranking that is not on disk or a request that is not recorded fails; nothing is called.
 */
class DemoFundsReplayTest {

    private static final Path FIXTURES = Path.of("../docs/playbook/contracts/fixtures");

    private static final PageRanking NO_NETWORK = new PageRanking() {
        @Override
        public Map<PageTopic, List<Integer>> rank(List<String> pages) {
            throw new AssertionError("Jev must not be called in replay: ranking missing from the committed cache");
        }

        @Override
        public Map<PageTopic, Double> bestScores(List<String> pages, List<Integer> pageNumbers) {
            throw new AssertionError("Jev must not be called in replay: scores missing from the committed cache");
        }
    };

    @ParameterizedTest
    @ValueSource(strings = {"LU0348926287", "LU2607415275", "CH0112537516", "XXSLICAP0001"})
    void draftsEachDemoFundFromTheRecordingWithNoNetwork(String isin) {
        var reader = new SourceReader(new FolderFundDocuments(FIXTURES, Path.of("../fund-documents")), new PdfBoxPdfText());
        var ranking = new CachedPageRanking(NO_NETWORK, Path.of("src/test/resources/jev-rankings"), "jev-latest|questions-v1");
        var drafter = new CardDrafter(new PageSelector(ranking, 25),
                new ReplayLlmEgress(Path.of("src/main/resources/llm-recordings")), AnsweringConfigurationAccess.catalog(),
                "claude-opus-5-5", "claude-plan", Clock.systemUTC());

        var drafted = drafter.draft(reader.read(new Isin(isin)));

        assertThat(drafted.card().rules()).isNotEmpty();
        assertThat(drafted.card().status().toString()).isEqualToIgnoringCase("draft");
    }

    @Test
    void theDraftFolderHoldsACardForEachDemoFund() throws Exception {
        try (var files = Files.list(FIXTURES.resolve("draft-rule-cards"))) {
            assertThat(files.map(p -> p.getFileName().toString()).toList()).hasSize(4);
        }
    }
}
