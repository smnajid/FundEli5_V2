package com.fundgate.fundeli5.privacygate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.clientprofiles.fixture.FixtureClientProfiles;
import com.fundgate.fundeli5.clientprofiles.PortfolioReference;
import com.fundgate.fundeli5.privacygate.fixture.FixturePersonalDataDetector;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PrivacyGateTest {

    static final Path FIXTURES = Path.of("../docs/playbook/contracts/fixtures");
    static final FixtureClientProfiles PROFILES = new FixtureClientProfiles(FIXTURES);

    private final PrivacyGate gate = new DefaultPrivacyGate(PROFILES, new FixturePersonalDataDetector());

    @Test
    void swapsEveryReferenceForItsIdAndKeepsTheMapLocal() {
        var outcome = gate.gate("Marc Aebi asks: LOC90000101 and LOC90000102, EUR 80,000?");
        var email = ((GateOutcome.Passed) outcome).email();
        assertThat(email.text()).doesNotContain("LOC9000").contains("9ae403db-1754-4033-9f06-a99edf92a574");
        assertThat(email.references()).hasSize(2);
    }

    @Test
    void refusesAClientNameAndShowsItOnce() {
        var outcome = (GateOutcome.Refused) gate.gate("Can Anna Steinmann (LOC90000101) subscribe?");
        assertThat(outcome.namesFound()).containsExactly("Anna Steinmann");
    }

    @Test
    void refusesASurnameAlone() {
        assertThat(gate.gate("Mrs steinmann, LOC90000101, wants EUR 5,000")).isInstanceOf(GateOutcome.Refused.class);
    }

    @Test
    void refusesAnUnknownReferenceAndAnEmailWithNone() {
        assertThat(gate.gate("LOC90009999 wants EUR 5,000")).isInstanceOf(GateOutcome.Refused.class);
        assertThat(gate.gate("Please check a subscription")).isInstanceOf(GateOutcome.Refused.class);
    }

    @Test
    void refusesAReferenceShapedTextItCannotSwap() {
        for (String odd : List.of("loc90000199", "LOC 90000199", "LOC-90000199", "LOC900001999", "x_LOC90000199")) {
            assertThat(gate.gate("LOC90000101 and " + odd + " want EUR 5,000"))
                    .as(odd).isInstanceOf(GateOutcome.Refused.class);
        }
    }

    @Test
    void failsClosedWhenTheDetectorIsDown() {
        PersonalDataDetector down = text -> {
            throw new PersonalDataDetectorUnavailable("down");
        };
        var outcome = new DefaultPrivacyGate(PROFILES, down).gate("LOC90000101 wants EUR 5,000");
        assertThat(outcome).isInstanceOf(GateOutcome.Refused.class);
    }

    @Test
    void anyPersonLeftRefusesWhateverTheDetector() {
        PersonalDataDetector everyoneIsNamed = text -> List.of(new DetectedSpan("PERSON", 0, 4, 0.9));
        var outcome = (GateOutcome.Refused) new DefaultPrivacyGate(PROFILES, everyoneIsNamed)
                .gate("Marc LOC90000101");
        assertThat(outcome.namesFound()).containsExactly("Marc");
    }

    @Test
    void theDetectorSeesThePseudonymisedTextOnly() {
        var seen = new java.util.ArrayList<String>();
        PersonalDataDetector spy = text -> {
            seen.add(text);
            return List.of();
        };
        new DefaultPrivacyGate(PROFILES, spy).gate("LOC90000101 wants EUR 5,000");
        assertThat(seen).hasSize(1);
        assertThat(seen.get(0)).doesNotContain("LOC90000101");
    }

    @Test
    void theAllowListReleasesOnlyWhatIsAskedAndNothingIdentifying() {
        var facts = new FactAllowList(PROFILES).facts(new PortfolioReference("LOC90000201"),
                Set.of(Fact.RESIDENCE_COUNTRY, Fact.US_BORN, Fact.HORIZON_YEARS));
        assertThat(facts.values()).containsEntry(Fact.RESIDENCE_COUNTRY, "CH").containsEntry(Fact.US_BORN, "yes")
                .hasSize(3);
        assertThat(facts.values().values()).doesNotContain("LOC90000201");
        assertThat(facts.fieldsSent()).isSorted();
        assertThat(java.util.Arrays.stream(Fact.values()).map(Enum::name))
                .noneMatch(n -> n.contains("NAME") || n.contains("ADDRESS") || n.contains("BIRTH_DATE")
                        || n.contains("BALANCE"));
    }
}
