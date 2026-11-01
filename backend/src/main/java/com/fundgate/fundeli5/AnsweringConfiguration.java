package com.fundgate.fundeli5;

import com.fundgate.fundeli5.answering.AnswerService;
import com.fundgate.fundeli5.checkrecord.CheckRecords;
import com.fundgate.fundeli5.checkrecord.memory.InMemoryCheckRecords;
import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.llm.LlmEgress;
import com.fundgate.fundeli5.privacygate.PrivacyGate;
import com.fundgate.fundeli5.privacygate.DefaultPrivacyGate;
import com.fundgate.fundeli5.privacygate.PersonalDataDetector;
import com.fundgate.fundeli5.replydrafter.ReplyDrafter;
import com.fundgate.fundeli5.replydrafter.stub.TemplateReplyDrafter;
import com.fundgate.fundeli5.requestparser.RequestParser;
import com.fundgate.fundeli5.requestparser.replay.ReplayRequestParser;
import com.fundgate.fundeli5.ruleengine.RuleEngine;
import com.fundgate.fundeli5.fundregistrations.FundRegistrations;
import com.fundgate.fundeli5.fxrates.FxRates;
import com.fundgate.fundeli5.rulecard.RuleCards;
import com.fundgate.fundeli5.rulecard.fixture.FixtureRuleCards;
import com.fundgate.fundeli5.ruleengine.EditionLookup;
import com.fundgate.fundeli5.ruleengine.RuleCatalog;
import com.fundgate.fundeli5.ruleengine.RuleCatalogEngine;
import com.fundgate.fundeli5.ruleengine.plugin.InvestorCategoryPlugin;
import com.fundgate.fundeli5.ruleengine.plugin.MandateOnlyClassPlugin;
import com.fundgate.fundeli5.ruleengine.plugin.MinimumInitialSubscriptionPlugin;
import com.fundgate.fundeli5.ruleengine.plugin.UsPersonExclusionPlugin;
import com.fundgate.fundeli5.ruleengine.plugin.KidRequiredForRetailPlugin;
import com.fundgate.fundeli5.ruleengine.plugin.Lookups;
import java.nio.file.Path;
import com.fundgate.fundeli5.shareclasscatalog.ShareClassCatalog;
import com.fundgate.fundeli5.shareclasscatalog.MatchingShareClassCatalog;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

/**
 * Wires the online flow with the walking skeleton's stubs; each ticket after S3a replaces one.
 * Dealer app only: the pipeline profile has no Client Profiles and answers no email (ADR 0004).
 */
@Configuration(proxyBeanMethods = false)
@Profile("!" + Runtimes.PIPELINE)
public class AnsweringConfiguration {

    @Bean
    PrivacyGate privacyGate(ClientProfiles clientProfiles, PersonalDataDetector detector) {
        return new DefaultPrivacyGate(clientProfiles, detector);
    }

    @Bean
    RequestParser requestParser(LlmEgress egress) {
        return new ReplayRequestParser(egress);
    }

    @Bean
    @Lazy
    ShareClassCatalog shareClassCatalog(ShareClasses shareClasses) {
        return new MatchingShareClassCatalog(shareClasses);
    }

    @Bean
    RuleCards ruleCards(@Value("${fundeli5.fixtures.dir}") String fixturesDir) {
        return new FixtureRuleCards(Path.of(fixturesDir));
    }

    /** One plug-in per rule kind, in the order their outcomes are shown; each later ticket adds its kinds here. */
    @Bean
    RuleCatalog ruleCatalog() {
        return new RuleCatalog(List.of(new InvestorCategoryPlugin(), new UsPersonExclusionPlugin(),
                new MinimumInitialSubscriptionPlugin(), new MandateOnlyClassPlugin(), new KidRequiredForRetailPlugin()));
    }

    /** No edition check until the bank's fund master data is read online: the contract's "absent means none". */
    @Bean
    EditionLookup editionLookup() {
        return EditionLookup.none();
    }

    @Bean
    @Lazy
    RuleEngine ruleEngine(RuleCards ruleCards, ClientProfiles clientProfiles, ShareClasses shareClasses,
            FundRegistrations fundRegistrations, FxRates fxRates, RuleCatalog catalog, EditionLookup editions) {
        return new RuleCatalogEngine(ruleCards, clientProfiles, shareClasses, new Lookups(fundRegistrations, fxRates), catalog,
                editions);
    }

    @Bean
    ReplyDrafter replyDrafter() {
        return new TemplateReplyDrafter();
    }

    @Bean
    CheckRecords checkRecords() {
        return new InMemoryCheckRecords();
    }

    @Bean
    @Lazy
    AnswerService answerService(PrivacyGate gate, RequestParser parser, ShareClassCatalog catalog, RuleEngine engine,
            ReplyDrafter drafter, CheckRecords checkRecords, ClientProfiles clientProfiles,
            RuleCards ruleCards, @Value("${fundeli5.engine.version:dev}") String engineVersion,
            @Value("${fundeli5.scalemapping.version:none}") String scaleMappingVersion) {
        return new AnswerService(gate, parser, catalog, engine, drafter, checkRecords, clientProfiles,
                Clock.systemUTC(), engineVersion, ruleCards, scaleMappingVersion);
    }
}
