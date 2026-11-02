package com.fundgate.fundeli5.answering;

import com.fundgate.fundeli5.explanation.Language;
import java.text.Normalizer;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The language of a request, read locally from the gated text by counting French and English function words:
 * deterministic, no model, and the reply templates only exist in French and English. English on a tie.
 */
final class RequestLanguage {

    private static final Pattern WORDS = Pattern.compile("[a-z]+");
    private static final Set<String> FRENCH = Set.of("bonjour", "merci", "pouvez", "vous", "souhaite", "ma", "mon",
            "cliente", "client", "investir", "souscription", "objet", "dans", "pour", "nous", "est", "une", "des", "les",
            "conseillere", "cordialement", "peut", "peuvent", "valider", "de", "la", "le", "du", "et");
    private static final Set<String> ENGLISH = Set.of("hi", "hello", "thanks", "can", "you", "the", "would", "like",
            "into", "from", "they", "subscribe", "confirm", "please", "to", "of", "and", "fund", "is", "are", "for",
            "check", "whether", "with", "this", "that");

    private RequestLanguage() {
    }

    static Language of(String text) {
        String plain = Normalizer.normalize(text.toLowerCase(java.util.Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        int french = 0;
        int english = 0;
        var matcher = WORDS.matcher(plain);
        while (matcher.find()) {
            String word = matcher.group();
            if (FRENCH.contains(word)) {
                french++;
            }
            if (ENGLISH.contains(word)) {
                english++;
            }
        }
        return french > english ? Language.FR : Language.EN;
    }
}
