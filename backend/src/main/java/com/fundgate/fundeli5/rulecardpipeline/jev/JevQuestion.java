package com.fundgate.fundeli5.rulecardpipeline.jev;

import java.util.List;

/** One question (the type is spelled "noul" in the Jev API): {@code type} is choice, noul (yes/no) or score; {@code criteria} are the options or the criterion. */
public record JevQuestion(String type, String instructions, List<String> criteria) {

    public static JevQuestion choice(String instructions, List<String> options) {
        return new JevQuestion("choice", instructions, options);
    }

    public static JevQuestion yesNo(String instructions) {
        return new JevQuestion("noul", instructions, List.of("yes", "no"));
    }
}
