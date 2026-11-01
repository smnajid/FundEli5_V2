package com.fundgate.fundeli5.ports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fundgate.fundeli5.bankdata.Country;
import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.bankdata.Confidentiality;
import com.fundgate.fundeli5.bankdata.InvestorCategory;
import com.fundgate.fundeli5.bankdata.InvestorType;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.clientprofiles.ClientProfile;
import com.fundgate.fundeli5.clientprofiles.PortfolioNotFound;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.clientprofiles.fixture.FixtureClientProfiles;
import com.fundgate.fundeli5.funddocuments.DocumentId;
import com.fundgate.fundeli5.funddocuments.DocumentUnavailable;
import com.fundgate.fundeli5.funddocuments.FundDocument;
import com.fundgate.fundeli5.funddocuments.folder.FolderFundDocuments;
import com.fundgate.fundeli5.fundregistrations.Registration;
import com.fundgate.fundeli5.fundregistrations.fixture.FixtureFundRegistrations;
import com.fundgate.fundeli5.fxrates.fixture.FixtureFxRates;
import com.fundgate.fundeli5.shareclasses.ShareClass;
import com.fundgate.fundeli5.shareclasses.fixture.FixtureShareClasses;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Each fixture loads through its port, read as the demo-dataset fixtures are written. */
class FixturePortsTest {

    static final Path FIXTURES = Path.of("../docs/playbook/contracts/fixtures");
    static final Path DOCUMENTS = Path.of("../fund-documents");

    @Test
    void everyClientProfileFixtureLoadsUnderItsOwnReference() throws Exception {
        FixtureClientProfiles profiles = new FixtureClientProfiles(FIXTURES);
        List<Path> files;
        try (Stream<Path> stream = Files.list(FIXTURES.resolve("client-profiles"))) {
            files = stream.toList();
        }
        assertThat(files).hasSize(15);
        for (Path file : files) {
            String name = file.getFileName().toString().replace(".json", "");
            ClientProfile profile = profiles.clientProfile(new PortfolioReference(name));
            assertThat(profile.portfolioRef().value()).isEqualTo(name);
            assertThat(profile.legalEntity()).isEqualTo(name.substring(0, 3));
            assertThat(profiles.portfolioId(new PortfolioReference(name))).contains(profile.portfolioId());
        }
    }

    @Test
    void aUsCitizenResidentInSwitzerlandKeepsEveryFactSeparate() {
        ClientProfile profile = new FixtureClientProfiles(FIXTURES).clientProfile(new PortfolioReference("LOC90000201"));

        assertThat(profile.client().residenceCountry()).isEqualTo(new Country("CH"));
        assertThat(profile.client().nationalities()).containsExactly(new Country("CH"), new Country("US"));
        assertThat(profile.client().usPerson().fatcaStatus()).isEqualTo(ClientProfile.FatcaStatus.US_PERSON);
        assertThat(profile.client().knowledgeAndExperience().getFirst().level().code()).isEqualTo("EXPERT");
        assertThat(profile.classification().category()).isEqualTo(InvestorCategory.PROFESSIONAL);
        assertThat(profile.classification().declarations().getFirst().type())
                .isEqualTo(ClientProfile.DeclarationType.FINSA_HNWI_OPT_OUT);
        assertThat(profile.portfolio().mandate().type()).isEqualTo(ClientProfile.MandateType.ADVISORY);
        assertThat(profile.portfolio().investmentProfile().horizonYears()).isEqualTo(10);
        assertThat(profile.portfolio().investmentProfile().riskProfile().code()).isEqualTo("4");
    }

    @Test
    void anUnknownReferenceIsNotFoundAndHasNoPortfolioId() {
        FixtureClientProfiles profiles = new FixtureClientProfiles(FIXTURES);
        PortfolioReference unknown = new PortfolioReference("ZZZ99999999");

        assertThat(profiles.portfolioId(unknown)).isEmpty();
        assertThatThrownBy(() -> profiles.clientProfile(unknown)).isInstanceOf(PortfolioNotFound.class);
    }

    @Test
    void theShareClassCatalogHasTheNineDemoClassesAndTheTwoWithoutACard() {
        List<ShareClass> classes = new FixtureShareClasses(FIXTURES).all();

        assertThat(classes).hasSize(11);
        ShareClass bi = classes.stream().filter(c -> c.isin().value().equals("LU0348927095")).findFirst().orElseThrow();
        assertThat(bi.shareClassName()).isEqualTo("BI-EUR");
        assertThat(bi.currency()).isEqualTo(new Currency("EUR"));
        assertThat(bi.productType().code()).isEqualTo("FUNDS");
        assertThat(bi.fundId()).isNull();
    }

    @Test
    void aRegistrationIsRegisteredNotRegisteredOrUnknown() {
        FixtureFundRegistrations registrations = new FixtureFundRegistrations(FIXTURES);
        Isin sif = new Isin("LU2607415275");

        assertThat(registrations.registration(sif, new Country("LU")))
                .isEqualTo(new Registration.Registered(Set.of(InvestorType.WELL_INFORMED)));
        assertThat(registrations.registration(sif, new Country("CH"))).isInstanceOf(Registration.NotRegistered.class);
        assertThat(registrations.registration(new Isin("XX0000000000"), new Country("CH")))
                .isInstanceOf(Registration.Unknown.class);
    }

    @Test
    void theFxRateIsOnlyAnsweredInTheStoredDirection() {
        FixtureFxRates fx = new FixtureFxRates(FIXTURES);

        assertThat(fx.rate(new Currency("EUR"), new Currency("CHF")).orElseThrow().rate())
                .isEqualByComparingTo(new BigDecimal("0.9350"));
        assertThat(fx.rate(new Currency("CHF"), new Currency("EUR"))).isEmpty();
    }

    @Test
    void aFundsDocumentsAreItsProspectusAndEveryKid() {
        FolderFundDocuments documents = new FolderFundDocuments(FIXTURES, DOCUMENTS);

        List<FundDocument> forFund = documents.documents(new Isin("LU0348927095"));

        assertThat(forFund).extracting(d -> d.documentId().value())
                .containsExactlyInAnyOrder("nordea1-prospectus-2026-06", "nordea1-gce-kid-bp-eur", "nordea1-gce-kid-bi-eur");
        assertThat(forFund).allSatisfy(d -> assertThat(d.confidentiality()).isEqualTo(Confidentiality.PUBLIC));
        assertThat(documents.documents(new Isin("XX0000000000"))).isEmpty();
    }

    @Test
    void everyManifestPdfIsInTheFolderWithTheHashTheManifestStates() throws Exception {
        FolderFundDocuments documents = new FolderFundDocuments(FIXTURES, DOCUMENTS);
        List<FundDocument> all = Stream.of("LU0348926287", "LU2607415275", "CH0112537516", "XXSLICAP0001")
                .flatMap(isin -> documents.documents(new Isin(isin)).stream())
                .distinct()
                .toList();
        assertThat(all).hasSize(9);

        for (FundDocument document : all) {
            byte[] bytes = documents.content(document.documentId());
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            assertThat(sha).as(document.documentId().value()).isEqualTo(document.contentSha256());
        }
    }

    @Test
    void aDocumentOutsideTheManifestIsUnavailable() {
        FolderFundDocuments documents = new FolderFundDocuments(FIXTURES, DOCUMENTS);

        assertThatThrownBy(() -> documents.content(new DocumentId("nope"))).isInstanceOf(DocumentUnavailable.class);
    }
}
