package com.fundgate.fundeli5.privacygate.fixture;

import com.fundgate.fundeli5.privacygate.DetectedSpan;
import com.fundgate.fundeli5.privacygate.PersonalDataDetector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic detector for tests and the specification: finds the demo clients' names (full name,
 * or surname alone) and nothing else. The other demo clients are "(coverage)" rows with no name in
 * demo-dataset.md. Needs no Docker.
 */
public class FixturePersonalDataDetector implements PersonalDataDetector {

    /** The named clients of demo-dataset.md. */
    public static final List<String> DEMO_CLIENTS = List.of("Anna Steinmann", "Tom Whitfield", "Claire Dupont");

    private final List<Pattern> patterns = new ArrayList<>();

    public FixturePersonalDataDetector() {
        this(DEMO_CLIENTS);
    }

    public FixturePersonalDataDetector(List<String> fullNames) {
        for (String fullName : fullNames) {
            String[] words = fullName.strip().split("\\s+");
            patterns.add(word(String.join("\\s+", Arrays.stream(words).map(Pattern::quote).toList())));
            patterns.add(word(Pattern.quote(words[words.length - 1])));
        }
    }

    private static Pattern word(String regex) {
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + regex + "(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Override
    public List<DetectedSpan> detect(String text) {
        List<DetectedSpan> found = new ArrayList<>();
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                found.add(new DetectedSpan(DetectedSpan.PERSON, matcher.start(), matcher.end(), 1.0));
            }
        }
        found.sort(Comparator.comparingInt(DetectedSpan::start).thenComparing(s -> -s.end()));
        return List.copyOf(found);
    }
}
