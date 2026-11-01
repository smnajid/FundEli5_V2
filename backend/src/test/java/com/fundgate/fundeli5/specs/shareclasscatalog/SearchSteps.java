package com.fundgate.fundeli5.specs.shareclasscatalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.bankdata.Currency;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.eligibility.SubscriptionRequest;
import com.fundgate.fundeli5.ruleengine.RuleEngine;
import com.fundgate.fundeli5.shareclasscatalog.ShareClassCatalog;
import com.fundgate.fundeli5.shareclasses.ShareClass;
import com.fundgate.fundeli5.shareclasses.ShareClasses;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Steps of the catalog's search rule in 04-find-what-a-portfolio-can-buy.feature. The reverse lookup is
 * another module, so the rows are computed here the way it will: the engine once per catalog class.
 */
public class SearchSteps {

    /** Demo short names (demo-dataset.md). */
    private static final Map<String, String> ISIN = Map.of(
            "GCE-BP", "LU0348926287", "GCE-BI", "LU0348927095", "SIF-W", "LU2607415275",
            "BNP-A", "CH0112537516", "BNP-AAC", "CH0114292532", "BNP-I", "CH0112537557");

    @Autowired
    ShareClassCatalog catalog;

    @Autowired
    ShareClasses shareClasses;

    @Autowired
    RuleEngine engine;

    private final AtomicInteger engineRuns = new AtomicInteger();
    private List<ShareClass> rows;
    private List<ShareClass> shown;

    @Before
    public void reset() {
        engineRuns.set(0);
        rows = null;
        shown = null;
    }

    @Given("the Fund Dealer has asked what {word} can buy with CHF {double}")
    public void theDealerHasAskedWhatAPortfolioCanBuy(String reference, double amount) {
        rows = shareClasses.all();
        for (ShareClass row : rows) {
            engine.check(new SubscriptionRequest(new PortfolioReference(reference), row.isin(),
                    BigDecimal.valueOf(amount), new Currency("CHF"), false));
            engineRuns.incrementAndGet();
        }
        shown = rows;
    }

    @When("the Fund Dealer searches for {string}")
    public void theDealerSearchesFor(String query) {
        shown = catalog.search(query, rows);
    }

    @Then("only the rows of {} remain")
    public void onlyTheRowsOfRemain(String shortNames) {
        List<String> expected = Arrays.stream(shortNames.split(",\\s*")).map(ISIN::get).toList();
        assertThat(shown.stream().map(c -> c.isin().value()).toList()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Then("the engine did not run again")
    public void theEngineDidNotRunAgain() {
        assertThat(engineRuns.get()).isEqualTo(rows.size());
    }
}
