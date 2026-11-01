package com.fundgate.fundeli5.ruleengine.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.eligibility.RuleOutcome.Result;
import com.fundgate.fundeli5.rulecard.Condition;
import com.fundgate.fundeli5.rulecard.Condition.RestrictionStatus;
import org.junit.jupiter.api.Test;

class UnmodelledRestrictionPluginTest {

    private final UnmodelledRestrictionPlugin plugin = new UnmodelledRestrictionPlugin();

    @Test
    void aBlockingRestrictionGivesReview() {
        var finding = plugin.evaluate(new Condition.UnmodelledRestriction(RestrictionStatus.BLOCKING, null), null, null)
                .orElseThrow();
        assertThat(finding.result()).isEqualTo(Result.REVIEW);
    }

    @Test
    void aNotedRestrictionPassesAndKeepsItsReason() {
        var finding = plugin.evaluate(
                new Condition.UnmodelledRestriction(RestrictionStatus.NOTED, "The board may reject an application"),
                null, null).orElseThrow();
        assertThat(finding.result()).isEqualTo(Result.PASS);
        assertThat(finding.explanation()).contains("Noted").contains("The board may reject an application");
    }

    @Test
    void everyRestrictionIsShown() {
        assertThat(plugin.accumulates()).isTrue();
    }
}
