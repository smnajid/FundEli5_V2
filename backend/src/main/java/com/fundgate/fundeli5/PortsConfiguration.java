package com.fundgate.fundeli5;

import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.clientprofiles.fixture.FixtureClientProfiles;
import com.fundgate.fundeli5.funddocuments.FundDocuments;
import com.fundgate.fundeli5.funddocuments.folder.FolderFundDocuments;
import com.fundgate.fundeli5.fundregistrations.FundRegistrations;
import com.fundgate.fundeli5.fundregistrations.fixture.FixtureFundRegistrations;
import com.fundgate.fundeli5.fxrates.FxRates;
import com.fundgate.fundeli5.fxrates.fixture.FixtureFxRates;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import com.fundgate.fundeli5.shareclasses.fixture.FixtureShareClasses;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Wires the out ports per runtime (ADR 0004): the dealer app reads client data and never
 * Fund Documents; the {@code pipeline} profile reads Fund Documents and never a Client Profile.
 *
 * <p>Each port's adapter is chosen by its own property, {@code fundeli5.<port>.adapter}, so a bank
 * adapter can replace a fixture adapter port by port (ADR 0008). The fixture or folder adapter is the
 * default; a value no adapter answers to leaves the port unwired, so whatever needs it fails to start.
 */
@Configuration(proxyBeanMethods = false)
public class PortsConfiguration {

    @Configuration(proxyBeanMethods = false)
    @Profile("!" + Runtimes.PIPELINE)
    static class DealerApp {

        @Bean
        @ConditionalOnProperty(name = "fundeli5.clientprofiles.adapter", havingValue = "fixture", matchIfMissing = true)
        ClientProfiles clientProfiles(@Value("${fundeli5.fixtures.dir}") String fixturesDir) {
            return new FixtureClientProfiles(Path.of(fixturesDir));
        }

        @Bean
        @ConditionalOnProperty(name = "fundeli5.shareclasses.adapter", havingValue = "fixture", matchIfMissing = true)
        ShareClasses shareClasses(@Value("${fundeli5.fixtures.dir}") String fixturesDir) {
            return new FixtureShareClasses(Path.of(fixturesDir));
        }

        @Bean
        @ConditionalOnProperty(name = "fundeli5.fundregistrations.adapter", havingValue = "fixture", matchIfMissing = true)
        FundRegistrations fundRegistrations(@Value("${fundeli5.fixtures.dir}") String fixturesDir) {
            return new FixtureFundRegistrations(Path.of(fixturesDir));
        }

        @Bean
        @ConditionalOnProperty(name = "fundeli5.fxrates.adapter", havingValue = "fixture", matchIfMissing = true)
        FxRates fxRates(@Value("${fundeli5.fixtures.dir}") String fixturesDir) {
            return new FixtureFxRates(Path.of(fixturesDir));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Profile(Runtimes.PIPELINE)
    static class Pipeline {

        @Bean
        @ConditionalOnProperty(name = "fundeli5.funddocuments.adapter", havingValue = "folder", matchIfMissing = true)
        FundDocuments fundDocuments(
                @Value("${fundeli5.fixtures.dir}") String fixturesDir,
                @Value("${fundeli5.documents.folder}") String documentsFolder) {
            return new FolderFundDocuments(Path.of(fixturesDir), Path.of(documentsFolder));
        }
    }
}
