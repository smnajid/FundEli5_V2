package com.fundgate.fundeli5.specs.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.clientprofiles.ClientProfiles;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import io.cucumber.java.en.Given;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/** The Background every feature shares. */
public class DatasetSteps {

    @Autowired
    ClientProfiles clientProfiles;

    @Value("${fundeli5.fixtures.dir}")
    String fixturesDir;

    @Given("the demo dataset")
    public void theDemoDataset() {
        assertThat(clientProfiles.portfolioId(new PortfolioReference("LOC90000101"))).isPresent();
    }

    @Given("the Validated Rule Cards of the sample funds")
    public void theValidatedRuleCards() throws IOException {
        try (Stream<Path> cards = Files.list(Path.of(fixturesDir, "rule-cards"))) {
            assertThat(cards.filter(p -> p.toString().endsWith(".json")).count()).isPositive();
        }
    }
}
