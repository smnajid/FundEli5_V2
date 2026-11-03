package com.fundgate.fundeli5.privacygate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.privacygate.fixture.FixturePersonalDataDetector;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequesterSignatureTest {

    @Test
    void dropsTheSenderLineAndTheNameAfterTheClosing() {
        String email = "From: Marc Aebi (relationship manager)\nSubject: LOC90000101\n\nHi desk,\nPlease check LOC90000101.\nThanks, Marc";
        assertThat(RequesterSignature.strip(email))
                .isEqualTo("Subject: LOC90000101\n\nHi desk,\nPlease check LOC90000101.\nThanks,");
    }

    @Test
    void worksInFrenchAndGerman() {
        assertThat(RequesterSignature.strip("De : Sophie Lentz\nBonjour,\nLUX90000301 ?\nMerci, Sophie"))
                .doesNotContain("Sophie");
        assertThat(RequesterSignature.strip("Von: Hans Meier\nHallo,\nLOC90000101 ?\nMit freundlichen Grüssen\nHans Meier\nBank AG"))
                .doesNotContain("Hans").doesNotContain("Bank AG");
    }

    @Test
    void dropsTheLinesOfASignatureBlock() {
        assertThat(RequesterSignature.strip("LOC90000101 EUR 5,000?\n-- \nMarc Aebi\nRelationship Manager\n+41 79 000 00 00"))
                .isEqualTo("LOC90000101 EUR 5,000?");
    }

    @Test
    void keepsTheSubjectAndTheBodySoAClientNamedThereStillRefuses() {
        String email = "From: Marc Aebi\nSubject: Nordea for Anna Steinmann\n\nCan Anna Steinmann (LOC90000101) subscribe?";
        assertThat(RequesterSignature.strip(email)).contains("Subject: Nordea for Anna Steinmann")
                .contains("Can Anna Steinmann (LOC90000101) subscribe?");
    }

    @Test
    void doesNotCutWhenALineAfterTheClosingHoldsAReference() {
        String email = "Hi desk,\nBest regards\nMarc\nAlso LOC90000102 wants EUR 5,000";
        assertThat(RequesterSignature.strip(email)).isEqualTo(email);
    }

    @Test
    void doesNotCutAClosingPhraseInTheMiddleOfTheBody() {
        String email = "Thanks, that helps.\nNow LOC90000101 wants EUR 5,000 in the Nordea fund.\nCan you check the minimum and the fees and the class and the currency?\nOne more thing.\nA second thing.\nA third thing.\nA fourth thing.";
        assertThat(RequesterSignature.strip(email)).isEqualTo(email);
    }

    @Test
    void keepsRequestTextThatFollowsAClosingWord() {
        String ps = "LOC90000101 wants EUR 50,000 in Fund X.\nThanks,\nPS also switch 20k into the Y class\nPlease make it EUR";
        assertThat(RequesterSignature.strip(ps)).isEqualTo(ps);
        String merci = "Merci de souscrire 50k EUR pour LOC90000101";
        assertThat(RequesterSignature.strip(merci)).isEqualTo(merci);
        String best = "Best effort on the 50k please";
        assertThat(RequesterSignature.strip(best)).isEqualTo(best);
    }

    @Test
    void dropsASenderLineOnlyInTheLeadingHeader() {
        String body = "Subject: switch\n\nFrom: Fund A\nTo: Fund B\nLOC90000101 EUR 50,000";
        assertThat(RequesterSignature.strip(body)).isEqualTo(body);
        String frenchAmount = "Objet: x\n\nDe: 50 000 EUR\nLOC90000101";
        assertThat(RequesterSignature.strip(frenchAmount)).isEqualTo(frenchAmount);
    }

    @Test
    void worksOnWindowsLineEndings() {
        assertThat(RequesterSignature.strip("From: Marc Aebi\r\nSubject: x\r\n\r\nLOC90000101 EUR 5,000?\r\nThanks, Marc"))
                .isEqualTo("Subject: x\n\nLOC90000101 EUR 5,000?\nThanks,");
    }

    @Test
    void theGateStillRefusesAClientNameAfterTheSignatureIsGone() {
        var gate = new DefaultPrivacyGate(PrivacyGateTest.PROFILES, new FixturePersonalDataDetector());
        var outcome = (GateOutcome.Refused) gate.gate(
                "From: Marc Aebi\nSubject: x\n\nCan Anna Steinmann (LOC90000101) subscribe?\nThanks, Marc");
        assertThat(outcome.namesFound()).containsExactly("Anna Steinmann");
    }

    @Test
    void aDetectorThatNamesTheRequesterNoLongerRefusesAnEmailWithOnlyASignature() {
        PersonalDataDetector detector = text -> {
            int at = text.indexOf("Marc");
            return at < 0 ? List.of() : List.of(new DetectedSpan("PERSON", at, at + 4, 0.9));
        };
        var gate = new DefaultPrivacyGate(PrivacyGateTest.PROFILES, detector);
        assertThat(gate.gate("From: Marc Aebi\n\nLOC90000101 wants EUR 5,000.\nThanks, Marc"))
                .isInstanceOf(GateOutcome.Passed.class);
    }
}
