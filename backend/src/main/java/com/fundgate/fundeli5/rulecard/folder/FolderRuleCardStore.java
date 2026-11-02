package com.fundgate.fundeli5.rulecard.folder;

import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.RuleCardStore;
import com.fundgate.fundeli5.rulecard.json.RuleCardJson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cards as JSON files (the validator's store; the engine reads cards through {@code RuleCards}): read from the read-only folders and from the work folder, written to the work
 * folder only. The work folder wins over a read-only card of the same fund and version, so a validated
 * card supersedes the draft it came from. Reads the folders on every call.
 */
public class FolderRuleCardStore implements RuleCardStore {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final List<Path> readOnly;
    private final Path work;

    public FolderRuleCardStore(List<Path> readOnly, Path work) {
        this.readOnly = List.copyOf(readOnly);
        this.work = work;
    }

    @Override
    public synchronized List<RuleCard> all() {
        List<RuleCard> cards = new ArrayList<>();
        for (Path folder : readOnly) {
            cards.addAll(read(folder));
        }
        for (RuleCard own : read(work)) {
            cards.removeIf(c -> sameCard(c, own));
            cards.add(own);
        }
        return List.copyOf(cards);
    }

    @Override
    public synchronized void save(RuleCard card) {
        try {
            Files.createDirectories(work);
            String name = card.fundName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-") + "-v" + card.version()
                    + ".json";
            Path target = work.resolve(name);
            Path temp = Files.createTempFile(work, "card", ".tmp");
            Files.writeString(temp, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(RuleCardJson.write(card)));
            Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save Rule Card " + card.fundName() + " v" + card.version(), e);
        }
    }

    private static boolean sameCard(RuleCard a, RuleCard b) {
        return a.fundName().equals(b.fundName()) && a.version() == b.version();
    }

    private static List<RuleCard> read(Path folder) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(f -> f.toString().endsWith(".json")).sorted().map(RuleCardJson::read).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read Rule Cards in " + folder, e);
        }
    }
}
