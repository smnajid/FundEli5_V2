package com.fundgate.fundeli5;

import com.fundgate.fundeli5.answering.AnswerService;
import com.fundgate.fundeli5.checkrecord.CheckRecords;
import com.fundgate.fundeli5.checkrecord.memory.InMemoryCheckRecords;
import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.llm.LlmEgress;
import com.fundgate.fundeli5.privacygate.PrivacyGate;
import com.fundgate.fundeli5.privacygate.stub.StubPrivacyGate;
import com.fundgate.fundeli5.replydrafter.ReplyDrafter;
import com.fundgate.fundeli5.replydrafter.stub.TemplateReplyDrafter;
import com.fundgate.fundeli5.requestparser.RequestParser;
import com.fundgate.fundeli5.requestparser.replay.ReplayRequestParser;
import com.fundgate.fundeli5.ruleengine.RuleEngine;
import com.fundgate.fundeli5.ruleengine.stub.FixedVerdictRuleEngine;
import com.fundgate.fundeli5.shareclasscatalog.ShareClassCatalog;
import com.fundgate.fundeli5.shareclasscatalog.stub.TokenShareClassCatalog;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import java.time.Clock;
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
    PrivacyGate privacyGate(ClientProfiles clientProfiles) {
        return new StubPrivacyGate(clientProfiles);
    }

    @Bean
    RequestParser requestParser(LlmEgress egress) {
        return new ReplayRequestParser(egress);
    }

    @Bean
    @Lazy
    ShareClassCatalog shareClassCatalog(ShareClasses shareClasses) {
        return new TokenShareClassCatalog(shareClasses);
    }

    @Bean
    RuleEngine ruleEngine() {
        return new FixedVerdictRuleEngine();
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
            @Value("${fundeli5.engine.version:dev}") String engineVersion) {
        return new AnswerService(gate, parser, catalog, engine, drafter, checkRecords, clientProfiles,
                Clock.systemUTC(), engineVersion);
    }
}
