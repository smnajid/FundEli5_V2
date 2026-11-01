package com.fundgate.fundeli5;

import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.clientprofiles.stub.StubClientProfiles;
import com.fundgate.fundeli5.funddocuments.FundDocuments;
import com.fundgate.fundeli5.funddocuments.stub.StubFundDocuments;
import com.fundgate.fundeli5.fundregistrations.FundRegistrations;
import com.fundgate.fundeli5.fundregistrations.stub.StubFundRegistrations;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import com.fundgate.fundeli5.shareclasses.stub.StubShareClasses;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Wires the out ports per runtime (ADR 0004): the dealer app reads client data and never
 * Fund Documents; the {@code pipeline} profile reads Fund Documents and never a Client Profile.
 */
@Configuration(proxyBeanMethods = false)
public class PortsConfiguration {

    @Configuration(proxyBeanMethods = false)
    @Profile("!" + Runtimes.PIPELINE)
    static class DealerApp {

        @Bean
        ClientProfiles clientProfiles() {
            return new StubClientProfiles();
        }

        @Bean
        ShareClasses shareClasses() {
            return new StubShareClasses();
        }

        @Bean
        FundRegistrations fundRegistrations() {
            return new StubFundRegistrations();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Profile(Runtimes.PIPELINE)
    static class Pipeline {

        @Bean
        FundDocuments fundDocuments() {
            return new StubFundDocuments();
        }
    }
}
