package com.fundgate.fundeli5.answering;

import com.fundgate.fundeli5.clientprofiles.PortfolioId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the reply's bracketed passages ("[Prospectus, p. 12]") into numbered markers ("[1]") that point to the
 * evidence rail. Which check a marker belongs to is read from the nearest preceding mention of its Share Class
 * or Portfolio Id. Pure text work on the reply as drafted, so the model's prompt and its recordings stay as they are.
 */
final class CitationMarkers {

    private static final Pattern BRACKET = Pattern.compile("\\[([^\\[\\]]*, p\\. \\d+[^\\[\\]]*)\\]");
    private static final String UNVERIFIED = ", quote not verified";

    record Numbered(String text, List<Integer> cited) {
    }

    private CitationMarkers() {
    }

    static Numbered number(String reply, List<Answer.Check> checks, List<PortfolioId> ids, Evidence.Numbering numbering) {
        List<Map<String, List<Integer>>> passages = checks.stream().map(numbering::numbersByPassage).toList();
        List<Integer> cited = new ArrayList<>();
        StringBuilder out = new StringBuilder();
        Matcher matcher = BRACKET.matcher(reply);
        while (matcher.find()) {
            List<Integer> numbers = new ArrayList<>();
            boolean allResolved = true;
            for (String part : matcher.group(1).split("; ")) {
                String key = part.endsWith(UNVERIFIED) ? part.substring(0, part.length() - UNVERIFIED.length()) : part;
                int line = lineOf(reply, matcher.start(), key, checks, ids, passages);
                if (line < 0) {
                    allResolved = false;
                    break;
                }
                passages.get(line).get(key).stream().filter(n -> !numbers.contains(n)).forEach(numbers::add);
            }
            if (!allResolved) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            StringBuilder marker = new StringBuilder();
            numbers.forEach(n -> marker.append('[').append(n).append(']'));
            numbers.stream().filter(n -> !cited.contains(n)).forEach(cited::add);
            matcher.appendReplacement(out, Matcher.quoteReplacement(marker.toString()));
        }
        matcher.appendTail(out);
        return new Numbered(out.toString(), cited.stream().sorted().toList());
    }

    /** The check the marker at {@code at} belongs to: the one named last before it that cites this passage. */
    private static int lineOf(String reply, int at, String key, List<Answer.Check> checks, List<PortfolioId> ids,
            List<Map<String, List<Integer>>> passages) {
        String before = reply.substring(0, at);
        int best = -1;
        long bestScore = Long.MIN_VALUE;
        for (int i = 0; i < checks.size(); i++) {
            if (!passages.get(i).containsKey(key)) {
                continue;
            }
            // The Portfolio Id decides first (it opens each line of the reply), the Share Class name second.
            long score = before.lastIndexOf(ids.get(i).toString()) * 1_000_000L
                    + before.lastIndexOf(checks.get(i).shareClassName());
            if (score > bestScore) {
                best = i;
                bestScore = score;
            }
        }
        return best;
    }
}
