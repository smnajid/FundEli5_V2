package com.fundgate.fundeli5.privacygate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Takes the Requester's own name out of an email before the Gate looks for persons: the sender line of the leading
 * header block (From, De, Von) and the sign-off (a bare closing phrase, a name after it, and the few signature lines
 * below). Only text is removed, so nothing new can reach the LLM. It cuts nothing that could be request text: no line
 * with something shaped like a Portfolio Reference, no sign-off line that carries lowercase words beyond a name, and
 * not the Subject or the body, so a client named there still refuses. When in doubt the text stays and the detector
 * decides.
 */
final class RequesterSignature {

    private static final Pattern SENDER_LINE = Pattern.compile("(?i)^\\s*(from|de|von)\\s*:.*$");

    private static final Pattern CLOSING = Pattern.compile(
            "(?iu)^\\s*(thank you|thanks|best regards|kind regards|warm regards|regards|best wishes|"
                    + "merci|cordialement|bien cordialement|salutations distinguées|salutations|bien à vous|"
                    + "danke|vielen dank|mit freundlichen gr(?:ü|ue)(?:ß|ss)en|freundliche gr(?:ü|ue)(?:ß|ss)e|"
                    + "beste gr(?:ü|ue)(?:ß|ss)e|viele gr(?:ü|ue)(?:ß|ss)e|liebe gr(?:ü|ue)(?:ß|ss)e)"
                    + "[\\s,.!]*([^\\n]*)$");

    private static final Pattern DASHES = Pattern.compile("^--\\s*$");
    private static final Pattern HAS_REFERENCE = Pattern.compile("(?i)[a-z]{3}[\\s_-]?\\d{8}");

    /** Lowercase words a name or a job title may hold. */
    private static final Set<String> NAME_PARTICLES = Set.of(
            "of", "and", "de", "du", "la", "le", "von", "van", "der", "den", "und", "&", "at", "bei", "et");

    /** A signature is a few short lines; more than that, or a long line, is body text. */
    private static final int MAX_SIGNATURE_LINES = 6;
    private static final int MAX_WORDS_PER_LINE = 5;
    private static final int MAX_NAME_WORDS = 3;

    private RequesterSignature() {
    }

    static String strip(String email) {
        List<String> lines = new ArrayList<>(List.of(email.split("\\R", -1)));
        removeSenderLines(lines);
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            var closing = CLOSING.matcher(line);
            boolean dashes = DASHES.matcher(line).matches();
            if (!dashes && !closing.matches()) {
                continue;
            }
            List<String> after = lines.subList(i + 1, lines.size());
            String sameLine = dashes ? "" : closing.group(2).strip();
            if (after.size() > MAX_SIGNATURE_LINES || !isName(sameLine, MAX_NAME_WORDS) || !allSignatureLines(after)) {
                continue;
            }
            List<String> kept = new ArrayList<>(lines.subList(0, i));
            if (!dashes) {
                kept.add(line.substring(0, closing.start(2)).stripTrailing());
            }
            return String.join("\n", kept);
        }
        return String.join("\n", lines);
    }

    /** Only the leading header block, up to the first blank line: a "From:" later in the body is request text. */
    private static void removeSenderLines(List<String> lines) {
        int end = 0;
        while (end < lines.size() && !lines.get(end).isBlank()) {
            end++;
        }
        for (int i = end - 1; i >= 0; i--) {
            String line = lines.get(i);
            if (SENDER_LINE.matcher(line).matches() && !HAS_REFERENCE.matcher(line).find()) {
                lines.remove(i);
            }
        }
    }

    private static boolean allSignatureLines(List<String> after) {
        return after.stream().allMatch(l -> l.isBlank() || isName(l.strip(), MAX_WORDS_PER_LINE));
    }

    /** Short, no Portfolio Reference, and no lowercase word other than a name particle: a name, a title, a phone. */
    private static boolean isName(String text, int maxWords) {
        if (text.isEmpty()) {
            return true;
        }
        if (HAS_REFERENCE.matcher(text).find()) {
            return false;
        }
        String[] words = text.split("\\s+");
        if (words.length > maxWords) {
            return false;
        }
        for (String word : words) {
            String letters = word.replaceAll("[^\\p{L}]", "");
            if (!letters.isEmpty() && Character.isLowerCase(letters.charAt(0))
                    && !NAME_PARTICLES.contains(letters.toLowerCase())) {
                return false;
            }
        }
        return true;
    }
}
