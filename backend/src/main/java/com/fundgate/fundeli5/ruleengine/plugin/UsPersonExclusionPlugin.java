package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.bankdata.Country;
import com.fundgate.fundeli5.clientprofiles.ClientProfile;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.RuleKind;
import java.util.Optional;

/**
 * US Person is judged by the fund's own definition: one US citizen can pass one fund and fail another. The bank's
 * FATCA status counts only for a fund that uses the FATCA definition. A missing status gives Review, never Fail.
 */
public final class UsPersonExclusionPlugin implements RuleKindPlugin<Condition.UsPersonExclusion> {

    private static final Country US = new Country("US");

    @Override
    public RuleKind kind() {
        return RuleKind.US_PERSON_EXCLUSION;
    }

    @Override
    public Class<Condition.UsPersonExclusion> conditionType() {
        return Condition.UsPersonExclusion.class;
    }

    @Override
    public String draftingLine() {
        return "us_person_exclusion: definition is reg_s, fatca, irc_citizenship or mixed as the text defines a US "
                + "Person; excludesUsCitizens is true when the text names US citizens even if Reg S is the definition.";
    }

    @Override
    public Optional<Finding> evaluate(Condition.UsPersonExclusion condition, Facts facts, Lookups lookups) {
        var client = facts.profile().client();
        if (client == null) {
            return Optional.of(Finding.review("No client facts are on file, so US Person status is unknown."));
        }
        boolean citizenCounts = condition.excludesUsCitizens()
                || condition.definition() == Condition.UsPersonDefinition.IRC_CITIZENSHIP;
        boolean fatcaCounts = condition.definition() == Condition.UsPersonDefinition.FATCA
                || condition.definition() == Condition.UsPersonDefinition.MIXED;

        if (US.equals(client.residenceCountry())) {
            return Optional.of(Finding.fail("The client lives in the United States, a US Person under every definition."));
        }
        if (client.usPerson() != null && Boolean.TRUE.equals(client.usPerson().usPermanentResident())) {
            return Optional.of(Finding.fail("The client is a US permanent resident, a US Person under every definition."));
        }
        if (citizenCounts && client.nationalities() != null && client.nationalities().contains(US)) {
            return Optional.of(Finding.fail("The client holds US citizenship, which this fund's definition counts "
                    + "as a US Person."));
        }
        if (fatcaCounts && client.usPerson() != null
                && client.usPerson().fatcaStatus() == ClientProfile.FatcaStatus.US_PERSON) {
            return Optional.of(Finding.fail("The bank records the client as a FATCA US Person."));
        }
        if (client.usPerson() == null || client.usPerson().fatcaStatus() == null
                || client.usPerson().usPermanentResident() == null) {
            return Optional.of(Finding.review("The client's US Person status is missing."));
        }
        if (client.residenceCountry() == null || (citizenCounts && client.nationalities() == null)) {
            return Optional.of(Finding.review("A fact needed for this fund's US Person definition is missing."));
        }
        return Optional.of(Finding.pass("The client is not a US Person under this fund's definition."));
    }

    @Override
    public boolean accumulates() {
        return false;
    }
}
