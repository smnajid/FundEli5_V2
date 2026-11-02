package com.fundgate.fundeli5.rulecardpipeline.jev;

import java.util.Map;

/** Jev's answer to one question: a probability per option and the confidence. */
public record JevAnswer(Map<String, Double> probabilities, double confidence) {

    public JevAnswer {
        probabilities = Map.copyOf(probabilities);
    }

    public double probability(String option) {
        return probabilities.getOrDefault(option, 0.0);
    }
}
