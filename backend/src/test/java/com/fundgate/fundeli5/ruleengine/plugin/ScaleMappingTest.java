package com.fundgate.fundeli5.ruleengine.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.bankdata.BankCode;
import org.junit.jupiter.api.Test;

/** The demo dataset's bank scales (demo-dataset.md, "Bank scales and Scale Mapping"). */
class ScaleMappingTest {

    private final ScaleMapping mapping = ScaleMapping.demo();

    private static BankCode code(String code) {
        return new BankCode(code, code);
    }

    @Test
    void aRiskProfileBecomesAMaximumSri() {
        assertThat(mapping.maxSri(code("1"))).contains(2);
        assertThat(mapping.maxSri(code("2"))).contains(3);
        assertThat(mapping.maxSri(code("3"))).contains(4);
        assertThat(mapping.maxSri(code("4"))).contains(5);
        assertThat(mapping.maxSri(code("5"))).contains(7);
    }

    @Test
    void aRiskProfileOffTheScaleHasNoMapping() {
        assertThat(mapping.maxSri(code("9"))).isEmpty();
    }

    @Test
    void knowledgeLevelsAreOrderedAndExpertIsAdvanced() {
        assertThat(mapping.knowledge(code("NONE"))).contains(ScaleMapping.Knowledge.BELOW_BASIC);
        assertThat(mapping.knowledge(code("BASIC"))).contains(ScaleMapping.Knowledge.BASIC);
        assertThat(mapping.knowledge(code("INFORMED"))).contains(ScaleMapping.Knowledge.INFORMED);
        assertThat(mapping.knowledge(code("EXPERT"))).contains(ScaleMapping.Knowledge.ADVANCED);
        assertThat(ScaleMapping.Knowledge.INFORMED).isGreaterThan(ScaleMapping.Knowledge.BASIC);
        assertThat(mapping.knowledge(code("GURU"))).isEmpty();
    }

    @Test
    void abilityToBearLossesIsOrdered() {
        assertThat(mapping.lossBearing(code("NO_LOSS"))).contains(ScaleMapping.LossBearing.NO_CAPITAL_LOSS);
        assertThat(mapping.lossBearing(code("LIMITED_LOSS"))).contains(ScaleMapping.LossBearing.LIMITED_CAPITAL_LOSS);
        assertThat(mapping.lossBearing(code("FULL_LOSS"))).contains(ScaleMapping.LossBearing.ENTIRE_CAPITAL);
        assertThat(ScaleMapping.LossBearing.ENTIRE_CAPITAL).isGreaterThan(ScaleMapping.LossBearing.LIMITED_CAPITAL_LOSS);
    }

    @Test
    void theCardsWordsForTheTargetMarketScalesAreRead() {
        assertThat(ScaleMapping.Knowledge.fromCard("basic")).contains(ScaleMapping.Knowledge.BASIC);
        assertThat(ScaleMapping.Knowledge.fromCard("advanced")).contains(ScaleMapping.Knowledge.ADVANCED);
        assertThat(ScaleMapping.Knowledge.fromCard("whatever")).isEmpty();
        assertThat(ScaleMapping.LossBearing.fromCard("full_loss")).contains(ScaleMapping.LossBearing.ENTIRE_CAPITAL);
        assertThat(ScaleMapping.LossBearing.fromCard("limited_loss"))
                .contains(ScaleMapping.LossBearing.LIMITED_CAPITAL_LOSS);
        assertThat(ScaleMapping.LossBearing.fromCard("no_loss")).contains(ScaleMapping.LossBearing.NO_CAPITAL_LOSS);
    }

    @Test
    void theMappingHasAVersionForTheCheckRecord() {
        assertThat(mapping.version()).isNotBlank();
    }
}
