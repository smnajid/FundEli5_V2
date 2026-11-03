package com.fundgate.fundeli5.rulecard.folder;

import com.fundgate.fundeli5.rulecard.SourcePages;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cited page texts as one JSON file per Draft card ({@code {document: {page: text}}}), in folders of their own
 * so that the card folders hold cards only. Reads the folders on every call; the first folder with the file wins.
 */
public class FolderSourcePages implements SourcePages {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final TypeReference<Map<String, Map<String, String>>> SHAPE = new TypeReference<>() {
    };

    private final List<Path> folders;

    public FolderSourcePages(List<Path> folders) {
        this.folders = List.copyOf(folders);
    }

    /** The file name for a card: its Draft file name in the pipeline output. */
    public static String fileName(String fundName, int version) {
        String slug = fundName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return slug + "-v" + version + "-draft.json";
    }

    /** Writes the texts of the cited pages: document id, then page number, then text. */
    public static Path write(Path folder, String fundName, int version, Map<String, Map<Integer, String>> pages) {
        Map<String, Map<String, String>> byName = new TreeMap<>();
        pages.forEach((doc, perPage) -> {
            Map<String, String> m = new TreeMap<>(java.util.Comparator.comparingInt(Integer::parseInt));
            perPage.forEach((page, text) -> m.put(String.valueOf(page), text));
            byName.put(doc, m);
        });
        Path target = folder.resolve(fileName(fundName, version));
        try {
            Files.createDirectories(folder);
            Path temp = Files.createTempFile(folder, "pages", ".tmp");
            try {
                Files.writeString(temp, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(byName));
                return Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + target, e);
        }
    }

    @Override
    public Optional<String> text(String fundName, int version, String document, int page) {
        for (Path folder : folders) {
            Path file = folder.resolve(fileName(fundName, version));
            if (Files.isRegularFile(file)) {
                try {
                    Map<String, Map<String, String>> all = MAPPER.readValue(Files.readString(file), SHAPE);
                    Optional<String> text = Optional.ofNullable(all.get(document)).map(m -> m.get(String.valueOf(page)));
                    if (text.isPresent()) {
                        return text;
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException("Cannot read " + file, e);
                }
            }
        }
        return Optional.empty();
    }
}
