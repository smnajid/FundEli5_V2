package com.fundgate.fundeli5.explanation;

import com.fundgate.fundeli5.eligibility.RuleOutcome;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

/**
 * The desk's wording, keyed {@code <RULE_KIND>.<RESULT>.<lang>} in {@code explanation/templates.properties}.
 * Keys are text so that the wording is data, not code that names a rule kind.
 */
public final class ExplanationTemplates {

    private static final String RESOURCE = "/explanation/templates.properties";

    private final Properties templates;

    private ExplanationTemplates(Properties templates) {
        this.templates = templates;
    }

    public static ExplanationTemplates load() {
        Properties properties = new Properties();
        try (InputStream in = ExplanationTemplates.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + RESOURCE);
            }
            properties.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new ExplanationTemplates(properties);
    }

    /** The sentence for a rule kind and a Fail or Review outcome; empty for a Pass, which gets none. */
    public Optional<String> sentence(String ruleKind, RuleOutcome.Result result, Language language) {
        if (result == RuleOutcome.Result.PASS) {
            return Optional.empty();
        }
        return Optional.of(get(ruleKind + "." + result.name() + "." + language.code()));
    }

    public String get(String key) {
        String value = templates.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("No explanation template " + key);
        }
        return value;
    }
}
