package com.fundgate.fundeli5.rulecardpipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fundgate.fundeli5.AnsweringConfigurationAccess;
import com.fundgate.fundeli5.bankdata.Confidentiality;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.funddocuments.DocumentId;
import com.fundgate.fundeli5.funddocuments.FundDocument;
import com.fundgate.fundeli5.llm.LlmEgress;
import com.fundgate.fundeli5.llm.LlmRequest;
import com.fundgate.fundeli5.llm.LlmResponse;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.Rule;
import com.fundgate.fundeli5.rulecard.RuleCard;
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

class CardDrafterTest {

    private static final Isin BP = new Isin("LU0348926287");
    private static final String PAGE_2 = "US persons may not subscribe for shares in the SICAV.\nThe Board may reject or close accounts at its discretion.";

    private final List<LlmRequest> requests = new ArrayList<>();

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

    private SourceReading reading() {
        FundDocument document = new FundDocument(new DocumentId("prospectus"), List.of(BP),
                FundDocument.DocumentType.PROSPECTUS, "en", null, null, Confidentiality.PUBLIC, "abc", null, "p.pdf");
        return new SourceReading(List.of(new SourceDocument(document, List.of("Front page", PAGE_2))), List.of());
    }

    private CardDrafter drafter(String answer) {
        LlmEgress egress = request -> {
            requests.add(request);
            return new LlmResponse(answer);
        };
        return new CardDrafter(new PageSelector(NO_RANKING, 25), egress, AnsweringConfigurationAccess.catalog(),
                "test-model", "replay", Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC));
    }

    private static String answer(String rules) {
        return "{\"fundName\": \"Nordea 1 - Global Climate\", \"rules\": [" + rules + "]}";
    }

    private static final String US_RULE = """
            {"scope": "fund", "shareClass": null,
             "condition": {"kind": "us_person_exclusion", "definition": "reg_s", "excludesUsCitizens": false},
             "citations": [{"document": "prospectus", "page": 2, "quote": "US persons may not subscribe for shares in the SICAV"}],
             "summary": "US persons excluded", "confidence": 0.9, "contradiction": false}""";

    private static final String RESTRICTION = """
            {"scope": "fund", "shareClass": null,
             "condition": {"kind": "unmodelled_restriction", "status": "noted", "notedReason": "fine"},
             "citations": [{"document": "prospectus", "page": 2, "quote": "reject or close accounts"}],
             "summary": "Board may reject or close", "confidence": 0.7, "contradiction": false}""";

    @Test
    void draftsADraftCardWithCheckedQuotes() {
        RuleCard card = drafter(answer(US_RULE)).draft(reading()).card();

        assertThat(card.status()).isEqualTo(RuleCard.Status.DRAFT);
        assertThat(card.version()).isEqualTo(1);
        assertThat(card.fundName()).isEqualTo("Nordea 1 - Global Climate");
        assertThat(card.drafting().model()).isEqualTo("test-model");
        assertThat(card.sources()).singleElement().satisfies(s -> assertThat(s.contentSha256()).isEqualTo("abc"));
        assertThat(card.rules()).singleElement().satisfies(rule -> {
            assertThat(rule.condition()).isInstanceOf(Condition.UsPersonExclusion.class);
            assertThat(rule.citations()).singleElement().satisfies(c -> assertThat(c.quoteFoundOnPage()).isTrue());
            assertThat(rule.history()).singleElement().satisfies(h -> assertThat(h.action()).isEqualTo(Rule.DRAFTED));
        });
    }

    @Test
    void flagsAQuoteThatIsNotOnItsPage() {
        String invented = US_RULE.replace("\"page\": 2", "\"page\": 1");
        RuleCard card = drafter(answer(invented)).draft(reading()).card();

        assertThat(card.rules().getFirst().citations().getFirst().quoteFoundOnPage()).isFalse();
    }

    @Test
    void proposesEveryUnmodelledRestrictionAsBlocking() {
        RuleCard card = drafter(answer(RESTRICTION)).draft(reading()).card();

        assertThat(card.rules().getFirst().condition())
                .isEqualTo(new Condition.UnmodelledRestriction(Condition.RestrictionStatus.BLOCKING, null));
    }

    @Test
    void theDraftingPromptHasOneLinePerKindAndNamesNoClient() {
        drafter(answer(US_RULE)).draft(reading());

        LlmRequest request = requests.getFirst();
        AnsweringConfigurationAccess.catalog().draftingLines()
                .forEach(line -> assertThat(request.system()).contains(line));
        assertThat(request.prompt()).contains("=== document prospectus", "--- page 2 ---", BP.value());
    }

    @Test
    void searchesEveryGenerationOfTheAnswerForTheCard() {
        String text = "Let me think {not json}. First try: {\"fundName\": \"X\", \"rules\": []}\n"
                + "Second try:\n```json\n" + answer(US_RULE) + "\n```";

        assertThat(drafter(text).draft(reading()).card().rules()).hasSize(1);
    }

    @Test
    void failsLoudlyWhenNoGenerationParses() {
        assertThatThrownBy(() -> drafter("I could not find any rule.").draft(reading()))
                .isInstanceOf(DraftingFailed.class).hasMessageContaining("no JSON object");
    }

    @Test
    void aCardWithZeroRulesIsAnError() {
        assertThatThrownBy(() -> drafter(answer("")).draft(reading()))
                .isInstanceOf(DraftingFailed.class).hasMessageContaining("0 rules");
    }

    @Test
    void aGenerationWithAnUnknownKindIsRejected() {
        String bad = US_RULE.replace("us_person_exclusion", "made_up_kind");
        assertThatThrownBy(() -> drafter(answer(bad)).draft(reading()))
                .isInstanceOf(DraftingFailed.class).hasMessageContaining("made_up_kind");
    }

    @Test
    void aCitationOfADocumentThatWasNotReadFails() {
        String bad = US_RULE.replace("\"prospectus\"", "\"other\"");
        assertThatThrownBy(() -> drafter(answer(bad)).draft(reading()))
                .isInstanceOf(DraftingFailed.class).hasMessageContaining("other");
    }

    @Test
    void writesTheDraftToTheOutputFolderAndReadsBack(@TempDir Path out) throws Exception {
        RuleCard card = drafter(answer(US_RULE)).draft(reading()).card();

        Path file = new DraftCardWriter(out).write(card);

        assertThat(file.getFileName().toString()).isEqualTo("nordea-1-global-climate-v1-draft.json");
        assertThat(com.fundgate.fundeli5.rulecard.json.RuleCardJson.read(file)).isEqualTo(card);
        try (var files = Files.list(out)) {
            assertThat(files).hasSize(1);
        }
    }

    @Test
    void refusesToWriteAnEmptyCard(@TempDir Path out) {
        RuleCard empty = new RuleCard(null, "X", List.of(BP), 1, RuleCard.Status.DRAFT, List.of(), List.of(), null, null);
        assertThatThrownBy(() -> new DraftCardWriter(out).write(empty)).isInstanceOf(DraftingFailed.class);
    }
}
