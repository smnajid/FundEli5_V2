package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.bankdata.BankCode;
import java.util.Map;
import java.util.Optional;

/**
 * The validated correspondence between the bank's own client scales and the Target Market scales (ADR 0002).
 * A code the table does not know has no mapping: the rule then gives Review, never a guess. At the event
 * the bank's real scales replace the table; nothing else changes.
 */
public final class ScaleMapping {

    /** What the client knows, on the Target Market scale. */
    public enum Knowledge {
        BELOW_BASIC, BASIC, INFORMED, ADVANCED;

        /** The card's word for a level; empty when the card words it some other way. */
        public static Optional<Knowledge> fromCard(String value) {
            return switch (value.trim().toLowerCase()) {
                case "below_basic", "none" -> Optional.of(BELOW_BASIC);
                case "basic" -> Optional.of(BASIC);
                case "informed" -> Optional.of(INFORMED);
                case "advanced" -> Optional.of(ADVANCED);
                default -> Optional.empty();
            };
        }
    }

    /** How much capital loss the client can bear, on the Target Market scale. */
    public enum LossBearing {
        NO_CAPITAL_LOSS, LIMITED_CAPITAL_LOSS, ENTIRE_CAPITAL;

        public static Optional<LossBearing> fromCard(String value) {
            return switch (value.trim().toLowerCase()) {
                case "no_loss", "no_capital_loss" -> Optional.of(NO_CAPITAL_LOSS);
                case "limited_loss", "limited_capital_loss" -> Optional.of(LIMITED_CAPITAL_LOSS);
                case "full_loss", "entire_capital" -> Optional.of(ENTIRE_CAPITAL);
                default -> Optional.empty();
            };
        }
    }

    private final String version;
    private final Map<String, Integer> maxSri;
    private final Map<String, Knowledge> knowledge;
    private final Map<String, LossBearing> lossBearing;

    public ScaleMapping(String version, Map<String, Integer> maxSri, Map<String, Knowledge> knowledge,
            Map<String, LossBearing> lossBearing) {
        this.version = java.util.Objects.requireNonNull(version);
        this.maxSri = Map.copyOf(maxSri);
        this.knowledge = Map.copyOf(knowledge);
        this.lossBearing = Map.copyOf(lossBearing);
    }

    /** The demo dataset's invented bank scales (demo-dataset.md, "Bank scales and Scale Mapping"). */
    public static ScaleMapping demo() {
        return new ScaleMapping("demo-1",
                Map.of("1", 2, "2", 3, "3", 4, "4", 5, "5", 7),
                Map.of("NONE", Knowledge.BELOW_BASIC, "BASIC", Knowledge.BASIC, "INFORMED", Knowledge.INFORMED,
                        "EXPERT", Knowledge.ADVANCED),
                Map.of("NO_LOSS", LossBearing.NO_CAPITAL_LOSS, "LIMITED_LOSS", LossBearing.LIMITED_CAPITAL_LOSS,
                        "FULL_LOSS", LossBearing.ENTIRE_CAPITAL));
    }

    /** The version the Check Record keeps. */
    public String version() {
        return version;
    }

    /** The highest SRI a bank risk profile accepts. */
    public Optional<Integer> maxSri(BankCode riskProfile) {
        return Optional.ofNullable(maxSri.get(riskProfile.code()));
    }

    public Optional<Knowledge> knowledge(BankCode level) {
        return Optional.ofNullable(knowledge.get(level.code()));
    }

    public Optional<LossBearing> lossBearing(BankCode ability) {
        return Optional.ofNullable(lossBearing.get(ability.code()));
    }
}
