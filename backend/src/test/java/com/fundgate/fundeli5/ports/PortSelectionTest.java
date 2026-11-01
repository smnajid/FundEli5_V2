package com.fundgate.fundeli5.ports;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.FundEli5Application;
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
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** The adapter of each port is chosen by a Spring property (ADR 0008). */
class PortSelectionTest {

    @Test
    void theDealerAppWiresTheFixtureAdapterOfEachPortByDefault() {
        try (ConfigurableApplicationContext context = start("--spring.profiles.active=default")) {
            assertThat(context.getBean(ClientProfiles.class)).isInstanceOf(FixtureClientProfiles.class);
            assertThat(context.getBean(ShareClasses.class)).isInstanceOf(FixtureShareClasses.class);
            assertThat(context.getBean(FundRegistrations.class)).isInstanceOf(FixtureFundRegistrations.class);
            assertThat(context.getBean(FxRates.class)).isInstanceOf(FixtureFxRates.class);
        }
    }

    @Test
    void thePipelineWiresTheFolderAdapterForFundDocuments() {
        try (ConfigurableApplicationContext context = start("--spring.profiles.active=pipeline")) {
            assertThat(context.getBean(FundDocuments.class)).isInstanceOf(FolderFundDocuments.class);
        }
    }

    @Test
    void aPortWhosePropertyNamesNoAdapterIsLeftUnwiredWhileTheOthersStay() {
        try (ConfigurableApplicationContext context =
                start("--spring.profiles.active=default", "--fundeli5.shareclasses.adapter=fmd")) {
            assertThat(context.getBeansOfType(ShareClasses.class)).isEmpty();
            assertThat(context.getBeansOfType(ClientProfiles.class)).hasSize(1);
        }
    }

    @Test
    void thePropertiesOfOtherPortsDoNotChangeThisOne() {
        try (ConfigurableApplicationContext context =
                start("--spring.profiles.active=default", "--fundeli5.fundregistrations.adapter=fixture")) {
            assertThat(context.getBean(ShareClasses.class)).isInstanceOf(FixtureShareClasses.class);
        }
    }

    private static ConfigurableApplicationContext start(String... args) {
        String[] all = java.util.Arrays.copyOf(args, args.length + 1);
        all[args.length] = "--server.port=0";
        return new SpringApplicationBuilder(FundEli5Application.class).run(all);
    }
}
