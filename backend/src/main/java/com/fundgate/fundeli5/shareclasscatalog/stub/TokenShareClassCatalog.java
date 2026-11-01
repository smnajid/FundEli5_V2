package com.fundgate.fundeli5.shareclasscatalog.stub;

import com.fundgate.fundeli5.shareclasscatalog.ShareClassCatalog;
import com.fundgate.fundeli5.shareclasses.ShareClass;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Walking-skeleton matcher: a class matches when its name is in the mention and the fund shares two
 * words with it; a fund without a class named gives all its classes.
 */
public class TokenShareClassCatalog implements ShareClassCatalog {

    private final ShareClasses shareClasses;

    public TokenShareClassCatalog(ShareClasses shareClasses) {
        this.shareClasses = shareClasses;
    }

    @Override
    public List<ShareClass> resolve(String mention) {
        String text = normalise(mention);
        Set<String> words = new HashSet<>(Arrays.asList(text.split(" ")));
        List<ShareClass> sameFund = shareClasses.all().stream()
                .filter(c -> Arrays.stream(normalise(c.fundName()).split(" ")).filter(w -> w.length() > 2)
                        .filter(words::contains).count() >= 2)
                .toList();
        List<ShareClass> named = sameFund.stream()
                .filter(c -> (" " + text + " ").contains(" " + normalise(c.shareClassName()) + " "))
                .toList();
        return named.isEmpty() ? sameFund : named;
    }

    private static String normalise(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", " ").trim();
    }
}
