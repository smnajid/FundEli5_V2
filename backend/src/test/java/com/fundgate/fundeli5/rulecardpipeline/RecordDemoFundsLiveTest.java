package com.fundgate.fundeli5.rulecardpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.AnsweringConfigurationAccess;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.funddocuments.folder.FolderFundDocuments;
import com.fundgate.fundeli5.llm.RecordingLlmEgress;
import com.fundgate.fundeli5.llm.claudeplan.ClaudePlanLlmEgress;
import com.fundgate.fundeli5.rulecardpipeline.jev.CachedPageRanking;
import com.fundgate.fundeli5.rulecardpipeline.jev.JevClient;
import com.fundgate.fundeli5.rulecardpipeline.jev.JevPageRanking;
import com.fundgate.fundeli5.rulecardpipeline.pdf.PdfBoxPdfText;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Records the drafting answers of the four demo funds with the live Jev ranking and the Claude plan, and
 * writes the Draft cards to the validation screen's draft folder. Public Fund Documents only.
 * Run: {@code RECORD_DEMO_FUNDS=1 ./mvnw test -Dtest=RecordDemoFundsLiveTest} (needs TYPESAFE_API_KEY and a logged-in claude).
 */
@Tag("live-llm")
@EnabledIfEnvironmentVariable(named = "RECORD_DEMO_FUNDS", matches = "1")
class RecordDemoFundsLiveTest {

    private static final String[] FUNDS = {"LU0348926287", "LU2607415275", "CH0112537516", "XXSLICAP0001"};

    @Test
    void recordsTheFourDemoFunds() {
        Path fixtures = Path.of("../docs/playbook/contracts/fixtures");
        var reader = new SourceReader(new FolderFundDocuments(fixtures, Path.of("../fund-documents")), new PdfBoxPdfText());
        var ranking = new CachedPageRanking(new JevPageRanking(new JevClient("https://api.typesafe.ai",
                System.getenv("TYPESAFE_API_KEY"), "jev-latest", Duration.ofSeconds(120))),
                Path.of("target/jev-rankings"), "jev-latest|questions-v1");
        var egress = new RecordingLlmEgress(
                new ClaudePlanLlmEgress("claude", Optional.empty(), Duration.ofMinutes(15)),
                Path.of("src/main/resources/llm-recordings"));
        var drafter = new CardDrafter(new PageSelector(ranking, 25), egress, AnsweringConfigurationAccess.catalog(),
                "claude-opus-5-5", "claude-plan", Clock.systemUTC());
        var writer = new DraftCardWriter(fixtures.resolve("draft-rule-cards"));
        for (String isin : FUNDS) {
            SourceReading reading = reader.read(new Isin(isin));
            assertThat(reading.canDraft()).as(isin).isTrue();
            var drafted = drafter.draft(reading);
            System.out.println("DRAFT " + isin + " " + writer.write(drafted.card()) + " rules="
                    + drafted.card().rules().size() + " missed=" + drafted.possiblyMissed());
        }
    }
}
