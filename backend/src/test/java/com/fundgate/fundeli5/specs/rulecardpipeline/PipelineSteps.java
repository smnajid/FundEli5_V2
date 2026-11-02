package com.fundgate.fundeli5.specs.rulecardpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.bankdata.Confidentiality;
import com.fundgate.fundeli5.bankdata.Isin;
import com.fundgate.fundeli5.funddocuments.DocumentId;
import com.fundgate.fundeli5.funddocuments.FundDocument;
import com.fundgate.fundeli5.funddocuments.FundDocuments;
import com.fundgate.fundeli5.rulecardpipeline.SourceReader;
import com.fundgate.fundeli5.rulecardpipeline.SourceReading;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import org.opentest4j.TestAbortedException;

/**
 * Steps of the pipeline rules in 06-validate-a-rule-card.feature. The pipeline is built here from the
 * {@link FundDocuments} port alone, as in its own runtime: it has no way to reach a Client Profile.
 * Drafting itself (page selection, the egress, the Draft card) belongs to later tickets: the steps
 * that need it are skipped (an aborted test), not undefined and not failed.
 */
public class PipelineSteps {

    private static final Isin SOME_ISIN = new Isin("LU0348926287");

    private SourceReading reading;

    @Given("a fund document whose metadata has no content hash")
    public void aFundDocumentWithoutAContentHash() {
        FundDocument document = new FundDocument(new DocumentId("no-hash"), List.of(SOME_ISIN),
                FundDocument.DocumentType.PROSPECTUS, "en", null, null, Confidentiality.PUBLIC, null, null,
                "no-hash.pdf");
        FundDocuments port = new FundDocuments() {
            @Override
            public List<FundDocument> documents(Isin isin) {
                return List.of(document);
            }

            @Override
            public byte[] content(DocumentId id) {
                throw new AssertionError("A document without a content hash must not be fetched");
            }
        };
        reader = new SourceReader(port, pdf -> List.of());
    }

    private SourceReader reader;

    @When("the pipeline is asked to draft from it")
    public void thePipelineIsAskedToDraftFromIt() {
        reading = reader.read(SOME_ISIN);
    }

    @Then("no Rule Card is drafted")
    public void noRuleCardIsDrafted() {
        assertThat(reading.canDraft()).as("something to draft from").isFalse();
    }

    @Then("the pipeline says why")
    public void thePipelineSaysWhy() {
        assertThat(reading.refusals()).singleElement()
                .satisfies(r -> assertThat(r.getMessage()).contains("no content hash"));
    }

    // ---- Waiting for drafting (Rule Card pipeline 3, and the recordings of ticket 25)

    @When("the pipeline drafts the Rule Card of Nordea 1 Global Climate and Environment")
    public void thePipelineDraftsNordea() {
        throw waiting("drafting needs the page ranking, the drafting prompt and the recorded answers");
    }

    @Then("the Draft Rule Card has a rule for each rule kind the prospectus and KID state")
    public void theDraftHasARuleForEachKind() {
        throw waiting("drafting");
    }

    @Then("every rule carries a quote found on its cited page")
    public void everyRuleCarriesAQuoteFoundOnItsPage() {
        throw waiting("drafting");
    }

    @Then("the pipeline read no Client Profile")
    public void thePipelineReadNoClientProfile() {
        throw waiting("drafting");
    }

    @Then("the {string} clause is an Unmodelled Restriction marked Blocking")
    public void theClauseIsAnUnmodelledRestrictionMarkedBlocking(String clause) {
        throw waiting("drafting");
    }

    private static TestAbortedException waiting(String why) {
        return new TestAbortedException("Waiting for drafting: " + why);
    }
}
