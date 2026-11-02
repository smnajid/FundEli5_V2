package com.fundgate.fundeli5.rulecardpipeline;

import com.fundgate.fundeli5.rulecard.RuleCard;
import com.fundgate.fundeli5.rulecard.json.RuleCardJson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import tools.jackson.databind.json.JsonMapper;

/** Writes a Draft Rule Card as JSON into the pipeline's output folder ({@code fundeli5.pipeline.out}). */
public class DraftCardWriter {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final Path out;

    public DraftCardWriter(Path out) {
        this.out = out;
    }

    /** The file written. A Draft is never allowed to be Validated here, and a card with no rule is an error. */
    public Path write(RuleCard card) {
        if (card.status() != RuleCard.Status.DRAFT) {
            throw new IllegalArgumentException("The pipeline writes Draft cards only");
        }
        if (card.rules().isEmpty()) {
            throw new DraftingFailed("A Rule Card with 0 rules is an error, not a draft");
        }
        String slug = card.fundName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (slug.isEmpty()) {
            throw new DraftingFailed("The card's fund name gives no file name");
        }
        Path target = out.resolve(slug + "-v" + card.version() + "-draft.json");
        try {
            Files.createDirectories(out);
            Path temp = Files.createTempFile(out, slug, ".tmp");
            try {
                Files.writeString(temp, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(RuleCardJson.write(card)));
                return Files.move(temp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + target, e);
        }
    }
}
