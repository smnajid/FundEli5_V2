package com.fundgate.fundeli5;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.funddocuments.FundDocuments;
import com.fundgate.fundeli5.fundregistrations.FundRegistrations;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/** The two runtimes of ADR 0004: the dealer app holds client data, the pipeline never does. */
class RuntimeProfilesTest {

    @Test
    void dealerAppStartsAWebServerAndReadsClientProfilesShareClassesAndFundRegistrations() {
        try (ConfigurableApplicationContext context = start("--spring.profiles.active=default")) {
            assertThat(context).isInstanceOf(WebServerApplicationContext.class);
            assertThat(((WebServerApplicationContext) context).getWebServer().getPort()).isPositive();
            assertThat(context.getBeansOfType(ClientProfiles.class)).hasSize(1);
            assertThat(context.getBeansOfType(ShareClasses.class)).hasSize(1);
            assertThat(context.getBeansOfType(FundRegistrations.class)).hasSize(1);
        }
    }

    @Test
    void pipelineStartsNoWebServerAndReadsFundDocumentsAndNeverAClientProfile() {
        try (ConfigurableApplicationContext context = start("--spring.profiles.active=pipeline")) {
            assertThat(context).isNotInstanceOf(WebServerApplicationContext.class);
            assertThat(context.getBeansOfType(FundDocuments.class)).hasSize(1);
            assertThat(context.getBeansOfType(ClientProfiles.class)).isEmpty();
        }
    }

    @Test
    void dealerAppNeverReadsFundDocuments() {
        try (ConfigurableApplicationContext context = start("--spring.profiles.active=default")) {
            assertThat(context.getBeansOfType(FundDocuments.class)).isEmpty();
        }
    }

    @Test
    void refusesToStartWhenTheFixtureDetectorMeetsALiveEndpoint() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> start("--spring.profiles.active=default",
                        "--fundeli5.privacygate.detector=fixture", "--fundeli5.llm.endpoint=claude-plan"))
                .hasStackTraceContaining("fixture")
                .hasStackTraceContaining("replay only");
    }

    private static ConfigurableApplicationContext start(String... args) {
        String[] withRandomPort = new String[args.length + 1];
        System.arraycopy(args, 0, withRandomPort, 0, args.length);
        withRandomPort[args.length] = "--server.port=0";
        return new SpringApplicationBuilder(FundEli5Application.class)
                .run(withRandomPort);
    }
}
