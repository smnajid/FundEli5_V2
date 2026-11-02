package com.fundgate.fundeli5;

import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.clientprofiles.fixture.FixtureClientProfiles;
import com.fundgate.fundeli5.funddocuments.FundDocuments;
import com.fundgate.fundeli5.funddocuments.folder.FolderFundDocuments;
import com.fundgate.fundeli5.fundregistrations.FundRegistrations;
import com.fundgate.fundeli5.fundregistrations.fixture.FixtureFundRegistrations;
import com.fundgate.fundeli5.fxrates.FxRates;
import com.fundgate.fundeli5.fxrates.fixture.FixtureFxRates;
import com.fundgate.fundeli5.llm.LlmEgress;
import com.fundgate.fundeli5.llm.claudeplan.ClaudePlanLlmEgress;
import com.fundgate.fundeli5.llm.replay.ReplayLlmEgress;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import com.fundgate.fundeli5.shareclasses.fixture.FixtureShareClasses;
import com.fundgate.fundeli5.privacygate.PersonalDataDetector;
import com.fundgate.fundeli5.privacygate.fixture.FixturePersonalDataDetector;
import com.fundgate.fundeli5.privacygate.presidio.PresidioPersonalDataDetector;
import com.fundgate.fundeli5.rulecardpipeline.CardDrafter;
import com.fundgate.fundeli5.rulecardpipeline.DraftCardWriter;
import com.fundgate.fundeli5.rulecardpipeline.PageRanking;
import com.fundgate.fundeli5.rulecardpipeline.PageSelector;
import com.fundgate.fundeli5.rulecardpipeline.SourceReader;
import com.fundgate.fundeli5.rulecardpipeline.jev.CachedPageRanking;
import com.fundgate.fundeli5.rulecardpipeline.jev.JevClient;
import com.fundgate.fundeli5.rulecardpipeline.jev.JevPageRanking;
import com.fundgate.fundeli5.rulecardpipeline.pdf.PdfBoxPdfText;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
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

    /** The only path to a model (ADR 0004), in both runtimes; {@code replay} serves recordings. */
    @Bean
    @ConditionalOnProperty(name = "fundeli5.llm.endpoint", havingValue = "replay", matchIfMissing = true)
    LlmEgress llmEgress(@Value("${fundeli5.llm.recordings}") String recordings) {
        return new ReplayLlmEgress(Path.of(recordings));
    }

    /** {@code claude-plan}: the Claude plan through the {@code claude -p} subprocess (V2 live mode). */
    @Bean
    @ConditionalOnProperty(name = "fundeli5.llm.endpoint", havingValue = "claude-plan")
    LlmEgress claudePlanLlmEgress(
            @Value("${fundeli5.llm.claude-plan.command:claude}") String command,
            @Value("${fundeli5.llm.claude-plan.schema:}") String schema,
            @Value("${fundeli5.llm.claude-plan.timeout:5m}") Duration timeout) {
        return new ClaudePlanLlmEgress(command, schema.isBlank() ? Optional.empty() : Optional.of(Path.of(schema)),
                timeout);
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("!" + Runtimes.PIPELINE)
    static class DealerApp {

        @Bean
        @ConditionalOnProperty(name = "fundeli5.clientprofiles.adapter", havingValue = "fixture", matchIfMissing = true)
        ClientProfiles clientProfiles(@Value("${fundeli5.fixtures.dir}") String fixturesDir) {
            return new FixtureClientProfiles(Path.of(fixturesDir));
        }

        /** ADR 0009: {@code presidio} calls the analyzer service; {@code fixture} (tests) needs no Docker and knows three names only, so Presidio is the default. */
        @Bean
        @ConditionalOnProperty(name = "fundeli5.privacygate.detector", havingValue = "fixture")
        PersonalDataDetector fixturePersonalDataDetector() {
            return new FixturePersonalDataDetector();
        }

        @Bean
        @ConditionalOnProperty(name = "fundeli5.privacygate.detector", havingValue = "presidio", matchIfMissing = true)
        PersonalDataDetector presidioPersonalDataDetector(
                @Value("${fundeli5.privacygate.presidio.url:http://localhost:5002}") String url,
                @Value("${fundeli5.privacygate.presidio.languages:en}") List<String> languages,
                @Value("${fundeli5.privacygate.presidio.timeout:5s}") Duration timeout) {
            return new PresidioPersonalDataDetector(url, languages, timeout);
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

        @Bean
        SourceReader sourceReader(FundDocuments fundDocuments) {
            return new SourceReader(fundDocuments, new PdfBoxPdfText());
        }

        /** Page selection by TypeSafe Jev (public documents only), rankings cached on disk. */
        @Bean
        PageRanking pageRanking(
                @Value("${fundeli5.jev.url:https://api.typesafe.ai}") String url,
                @Value("${fundeli5.jev.api-key:}") String apiKey,
                @Value("${fundeli5.jev.model:jev-latest}") String model,
                @Value("${fundeli5.jev.timeout:60s}") Duration timeout,
                @Value("${fundeli5.jev.cache-dir:target/jev-rankings}") String cacheDir) {
            return new CachedPageRanking(new JevPageRanking(new JevClient(url, apiKey, model, timeout)),
                    Path.of(cacheDir), model + "|questions-v1");
        }

        @Bean
        PageSelector pageSelector(PageRanking pageRanking,
                @Value("${fundeli5.pipeline.page-budget:25}") int budget) {
            return new PageSelector(pageRanking, budget);
        }

        @Bean
        CardDrafter cardDrafter(PageSelector pageSelector, LlmEgress llmEgress,
                @Value("${fundeli5.pipeline.model:claude-opus-5-5}") String model,
                @Value("${fundeli5.llm.endpoint:replay}") String endpoint) {
            return new CardDrafter(pageSelector, llmEgress, AnsweringConfiguration.standardCatalog(), model, endpoint, Clock.systemUTC());
        }

        @Bean
        DraftCardWriter draftCardWriter(@Value("${fundeli5.pipeline.out}") String out) {
            return new DraftCardWriter(Path.of(out));
        }

        @Bean
        PipelineRunner pipelineRunner(SourceReader sourceReader, CardDrafter cardDrafter, DraftCardWriter writer) {
            return new PipelineRunner(sourceReader, cardDrafter, writer);
        }
    }
}
