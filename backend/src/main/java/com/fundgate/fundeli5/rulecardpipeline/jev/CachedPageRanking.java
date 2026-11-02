package com.fundgate.fundeli5.rulecardpipeline.jev;

import com.fundgate.fundeli5.rulecardpipeline.PageRanking;
import com.fundgate.fundeli5.rulecardpipeline.PageTopic;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps rankings and page scores on disk, keyed by a hash of the page texts, so a rerun costs nothing.
 * A cache file that cannot be read is ignored and written again.
 */
public class CachedPageRanking implements PageRanking {

    private final PageRanking delegate;
    private final Path dir;
    private final String salt;
    private final JsonMapper json = JsonMapper.builder().build();

    /** {@code salt} names what else the answers depend on (the Jev model, a questions version). */
    public CachedPageRanking(PageRanking delegate, Path dir, String salt) {
        this.delegate = delegate;
        this.dir = dir;
        this.salt = salt;
    }

    @Override
    public Map<PageTopic, List<Integer>> rank(List<String> pages) {
        Path file = dir.resolve(hash(pages, List.of(), salt) + "-rank.json");
        Map<PageTopic, List<Integer>> cached = read(file, new TypeReference<Map<PageTopic, List<Integer>>>() { });
        if (cached != null && cached.size() == PageTopic.values().length) {
            return cached;
        }
        Map<PageTopic, List<Integer>> ranked = delegate.rank(pages);
        write(file, ranked);
        return Map.copyOf(ranked);
    }

    @Override
    public Map<PageTopic, Double> bestScores(List<String> pages, List<Integer> pageNumbers) {
        Path file = dir.resolve(hash(pages, pageNumbers, salt) + "-scores.json");
        Map<PageTopic, Double> cached = read(file, new TypeReference<Map<PageTopic, Double>>() { });
        if (cached != null && cached.size() == PageTopic.values().length) {
            return cached;
        }
        Map<PageTopic, Double> scores = new EnumMap<>(PageTopic.class);
        scores.putAll(delegate.bestScores(pages, pageNumbers));
        write(file, scores);
        return scores;
    }

    private <T> T read(Path file, TypeReference<T> type) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return json.readValue(Files.readString(file, StandardCharsets.UTF_8), type);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private void write(Path file, Object value) {
        try {
            Files.createDirectories(dir);
            Path temp = Files.createTempFile(dir, "ranking", ".tmp");
            Files.writeString(temp, json.writeValueAsString(value), StandardCharsets.UTF_8);
            Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // the answer is still good; only the next run pays again
        }
    }

    private static String hash(List<String> pages, List<Integer> numbers, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String page : pages) {
                digest.update(page.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            digest.update(numbers.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
