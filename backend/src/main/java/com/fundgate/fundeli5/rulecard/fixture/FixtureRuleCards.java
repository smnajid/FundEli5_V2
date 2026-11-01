package com.fundgate.fundeli5.rulecard.fixture;

import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.RuleCards;
import com.fundgate.fundeli5.rulecard.json.RuleCardJson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Reads the fixtures' {@code rule-cards} folder on every call, so a card edited on disk is seen at once. */
public class FixtureRuleCards implements RuleCards {

    private static final Comparator<RuleCard> NEWEST_VALIDATED_FIRST = Comparator
            .comparing((RuleCard c) -> c.status() == RuleCard.Status.VALIDATED).thenComparingInt(RuleCard::version)
            .reversed();

    private final Path folder;

    public FixtureRuleCards(Path fixturesDir) {
        this.folder = fixturesDir.resolve("rule-cards");
    }

    @Override
    public Optional<RuleCard> forShareClass(Isin isin) {
        try (Stream<Path> files = Files.list(folder)) {
            List<RuleCard> cards = files.filter(f -> f.toString().endsWith(".json")).sorted()
                    .map(RuleCardJson::read).filter(c -> c.shareClasses().contains(isin)).toList();
            return cards.stream().min(NEWEST_VALIDATED_FIRST);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read Rule Cards in " + folder, e);
        }
    }
}
