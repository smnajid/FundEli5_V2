package com.fundgate.fundeli5.ports;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.folder.FolderRuleCardStore;
import com.fundgate.fundeli5.rulecard.json.RuleCardJson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuleCardStoreTest {

    @Test
    void everyFixtureCardSurvivesWriteAndRead() throws Exception {
        try (Stream<Path> files = Files.list(RuleCardFixturesTest.CARDS)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                RuleCard card = RuleCardJson.read(file);
                assertThat(RuleCardJson.parse(RuleCardJson.write(card))).as(file.getFileName().toString())
                        .isEqualTo(card);
            }
        }
    }

    @Test
    void aSavedCardSupersedesTheReadOnlyCardOfTheSameVersionAndNeverTouchesTheReadOnlyFolder(@TempDir Path work) {
        Path drafts = FixturePortsTest.FIXTURES.resolve("draft-rule-cards");
        FolderRuleCardStore store = new FolderRuleCardStore(List.of(drafts), work);
        RuleCard draft = store.all().stream().filter(c -> c.fundName().startsWith("Nordea 1")).findFirst().orElseThrow();
        assertThat(draft.status()).isEqualTo(RuleCard.Status.DRAFT);

        store.save(new RuleCard(draft.fundId(), draft.fundName(), draft.shareClasses(), draft.version(),
                RuleCard.Status.VALIDATED, draft.rules(), draft.sources(), draft.drafting(),
                new RuleCard.Validation("Validator", java.time.Instant.parse("2026-11-02T10:00:00Z"))));

        assertThat(store.all()).hasSize(4).filteredOn(c -> c.fundName().equals(draft.fundName())).singleElement()
                .satisfies(c -> assertThat(c.status()).isEqualTo(RuleCard.Status.VALIDATED));
        assertThat(RuleCardJson.read(drafts.resolve("nordea1-global-climate-and-environment-v1-draft.json")).status())
                .isEqualTo(RuleCard.Status.DRAFT);
    }
}
