package com.fundgate.fundeli5.shareclasscatalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.shareclasses.ShareClass;
import com.fundgate.fundeli5.shareclasses.fixture.FixtureShareClasses;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchingShareClassCatalogTest {

    private final FixtureShareClasses source = new FixtureShareClasses(Path.of("../docs/playbook/contracts/fixtures"));
    private final MatchingShareClassCatalog catalog = new MatchingShareClassCatalog(source);

    private List<String> resolved(String mention) {
        return catalog.resolve(mention).stream().map(ShareClass::shareClassName).toList();
    }

    private List<String> found(String query) {
        return catalog.search(query, source.all()).stream().map(c -> c.isin().value()).toList();
    }

    @Test
    void aFundWithItsClassGivesThatClass() {
        assertThat(resolved("Nordea Global Climate BI-EUR")).containsExactly("BI-EUR");
    }

    @Test
    void aFundWithoutAClassFansOutToItsClassesOnly() {
        assertThat(resolved("Nordea 1 Global Climate and Environment")).containsExactlyInAnyOrder("BP-EUR", "BI-EUR");
    }

    @Test
    void aSisterFundSharingWordsIsNotPulledIn() {
        assertThat(catalog.resolve("Nordea Global").stream().map(c -> c.fundName()).distinct()).hasSizeGreaterThan(1);
        assertThat(resolved("Nordea Global Climate")).doesNotContain("BW-SEK");
    }

    @Test
    void accentsCaseAndPunctuationAreIgnored() {
        assertThat(resolved("NORDEA 1 – global climate & environment, bp-eur")).containsExactly("BP-EUR");
    }

    @Test
    void theLongerClassNameWins() {
        assertThat(resolved("BNP PARIBAS (CH) Bonds CHF A AC")).containsExactly("A AC");
        assertThat(resolved("BNP Paribas Bonds CHF A")).containsExactly("A");
    }

    @Test
    void anIsinInTheMentionResolvesToItsClass() {
        assertThat(resolved("please buy LU0348927095 for the client")).containsExactly("BI-EUR");
    }

    @Test
    void anIsinOutsideTheCatalogResolvesToNothing() {
        assertThat(catalog.resolve("LU0000000000")).isEmpty();
    }

    @Test
    void anUnknownFundResolvesToNothing() {
        assertThat(catalog.resolve("Fidelity World")).isEmpty();
        assertThat(catalog.resolve("")).isEmpty();
    }

    @Test
    void searchNeedsEveryWordInAnyOrder() {
        assertThat(found("climate nordea BI")).containsExactly("LU0348927095");
        assertThat(found("Bnp-Paribas  Bonds")).containsExactly("CH0112537516", "CH0114292532", "CH0112537557");
        assertThat(found("bnp chf")).hasSize(3);
    }

    @Test
    void searchMatchesAnIsinByPrefix() {
        assertThat(found("LU03489")).containsExactly("LU0348926287", "LU0348927095");
        assertThat(found("lu0348927095")).containsExactly("LU0348927095");
    }

    @Test
    void anEmptySearchKeepsEveryRow() {
        assertThat(found("  ")).hasSize(11);
    }
}
