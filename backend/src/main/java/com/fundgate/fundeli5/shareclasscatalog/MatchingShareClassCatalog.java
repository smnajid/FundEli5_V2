package com.fundgate.fundeli5.shareclasscatalog;

import com.fundgate.fundeli5.shareclasses.ShareClass;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministic matcher over the {@link ShareClasses} port. A fund is matched when every content word of the
 * mention belongs to the fund's or umbrella's name; a class is matched when its name appears in the mention
 * as a run of words. A fund named without a class gives all its classes.
 */
public class MatchingShareClassCatalog implements ShareClassCatalog {

    private static final Set<String> FILLER = Set.of("fund", "funds", "sicav", "the", "and", "of", "et", "de", "la",
            "le", "les", "du", "des", "class", "classe", "share", "part", "in", "dans", "into", "a", "to", "for");
    private static final Pattern ISIN = Pattern.compile("\\b[A-Z]{2}[A-Z0-9]{9}\\d\\b");

    private final ShareClasses shareClasses;

    public MatchingShareClassCatalog(ShareClasses shareClasses) {
        this.shareClasses = shareClasses;
    }

    @Override
    public List<ShareClass> resolve(String mention) {
        if (mention == null || mention.isBlank()) {
            return List.of();
        }
        List<ShareClass> catalog = shareClasses.all();
        var isin = ISIN.matcher(mention.toUpperCase(Locale.ROOT).replace('-', ' '));
        while (isin.find()) {
            List<ShareClass> byIsin = catalog.stream().filter(c -> c.isin().value().equals(isin.group())).toList();
            if (!byIsin.isEmpty()) {
                return byIsin;
            }
        }
        List<String> words = words(mention);
        Map<String, List<ShareClass>> funds = new LinkedHashMap<>();
        for (ShareClass c : catalog) {
            funds.computeIfAbsent(c.fundName() + "|" + c.umbrellaName(), k -> new ArrayList<>()).add(c);
        }
        List<ShareClass> result = new ArrayList<>();
        for (List<ShareClass> classes : funds.values()) {
            result.addAll(matchFund(words, classes));
        }
        return result;
    }

    private List<ShareClass> matchFund(List<String> mention, List<ShareClass> classes) {
        ShareClass first = classes.get(0);
        Set<String> fundWords = new LinkedHashSet<>(words(first.fundName()));
        if (first.umbrellaName() != null) {
            fundWords.addAll(words(first.umbrellaName()));
        }
        List<ShareClass> named = namedClasses(mention, classes);
        List<String> remaining = new ArrayList<>(mention);
        for (ShareClass c : named) {
            remaining.removeAll(words(c.shareClassName()));
        }
        remaining.removeIf(FILLER::contains);
        if (remaining.isEmpty() || !fundWords.containsAll(remaining)) {
            return List.of();
        }
        return named.isEmpty() ? classes : named;
    }

    /** Classes whose name is a run of words in the mention; a longer name hides the shorter one it contains. */
    private List<ShareClass> namedClasses(List<String> mention, List<ShareClass> classes) {
        List<ShareClass> hits = classes.stream()
                .filter(c -> Collections.indexOfSubList(mention, words(c.shareClassName())) >= 0).toList();
        return hits.stream().filter(c -> hits.stream().noneMatch(other -> {
            List<String> mine = words(c.shareClassName());
            List<String> theirs = words(other.shareClassName());
            return theirs.size() > mine.size() && Collections.indexOfSubList(theirs, mine) >= 0;
        })).toList();
    }

    @Override
    public List<ShareClass> search(String query, List<ShareClass> rows) {
        List<String> typed = words(query == null ? "" : query);
        return rows.stream().filter(row -> {
            List<String> haystack = new ArrayList<>();
            haystack.addAll(words(row.fundName()));
            haystack.addAll(words(row.shareClassName()));
            haystack.add(row.isin().value().toLowerCase(Locale.ROOT));
            for (String optional : new String[] {row.umbrellaName(), row.valor(), row.internalCode()}) {
                if (optional != null) {
                    haystack.addAll(words(optional));
                }
            }
            return typed.stream().allMatch(t -> haystack.stream().anyMatch(h -> h.startsWith(t)));
        }).toList();
    }

    /** Lower-case words without accents; anything that is not a letter or digit separates words. */
    static List<String> words(String text) {
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        List<String> words = new ArrayList<>();
        for (String w : plain.split("[^a-z0-9]+")) {
            if (!w.isEmpty()) {
                words.add(w);
            }
        }
        return words;
    }
}
