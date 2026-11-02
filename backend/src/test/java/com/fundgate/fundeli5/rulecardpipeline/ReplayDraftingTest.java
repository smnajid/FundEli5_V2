package com.fundgate.fundeli5.rulecardpipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fundgate.fundeli5.AnsweringConfigurationAccess;
import com.fundgate.fundeli5.bankdata.Confidentiality;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.funddocuments.DocumentId;
import com.fundgate.fundeli5.funddocuments.FundDocument;
import com.fundgate.fundeli5.llm.LlmRequest;
import com.fundgate.fundeli5.llm.UnrecordedLlmRequest;
import com.fundgate.fundeli5.llm.replay.ReplayLlmEgress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/** Drafting in replay mode: one small recorded answer in the egress's own file format, no model called. */
class ReplayDraftingTest {

    private static final Isin ISIN = new Isin("LU0348926287");
    private static final String ANSWER = """
            {"fundName": "Test Fund", "rules": [{"scope": "fund", "shareClass": null,
             "condition": {"kind": "us_person_exclusion", "definition": "reg_s", "excludesUsCitizens": false},
             "citations": [{"document": "prospectus", "page": 1, "quote": "US persons may not subscribe"}],
             "summary": "US persons excluded", "confidence": 0.9, "contradiction": false}]}""";

    private static final PageRanking NO_RANKING = new PageRanking() {
        @Override
        public Map<PageTopic, List<Integer>> rank(List<String> pages) {
            return Map.of();
        }

        @Override
        public Map<PageTopic, Double> bestScores(List<String> pages, List<Integer> pageNumbers) {
            return Map.of();
        }
    };

    private static SourceReading reading(String pageText) {
        FundDocument document = new FundDocument(new DocumentId("prospectus"), List.of(ISIN),
                FundDocument.DocumentType.PROSPECTUS, "en", null, null, Confidentiality.PUBLIC, "abc", null, "p.pdf");
        return new SourceReading(List.of(new SourceDocument(document, List.of(pageText))), List.of());
    }

    private static CardDrafter drafter(com.fundgate.fundeli5.llm.LlmEgress egress) {
        return new CardDrafter(new PageSelector(NO_RANKING, 25), egress, AnsweringConfigurationAccess.catalog(),
                "test-model", "replay", Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void draftsFromARecordedAnswerAndRefusesAnUnrecordedRequest(@TempDir Path recordings) throws Exception {
        SourceReading reading = reading("US persons may not subscribe to this fund.");
        List<LlmRequest> seen = new ArrayList<>();
        try {
            drafter(request -> {
                seen.add(request);
                throw new UnrecordedLlmRequest(request);
            }).draft(reading);
        } catch (UnrecordedLlmRequest expected) {
            // the request is what gets recorded
        }
        JsonMapper mapper = JsonMapper.builder().build();
        Files.writeString(recordings.resolve("us-person.json"), mapper.writeValueAsString(Map.of(
                "request", seen.getFirst(), "response", Map.of("text", ANSWER))));

        CardDrafter replaying = drafter(new ReplayLlmEgress(recordings));

        assertThat(replaying.draft(reading).card().rules()).hasSize(1);
        assertThatThrownBy(() -> replaying.draft(reading("A different page.")))
                .isInstanceOf(UnrecordedLlmRequest.class);
    }
}
