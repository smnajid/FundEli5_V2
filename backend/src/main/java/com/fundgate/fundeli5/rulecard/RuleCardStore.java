package com.fundgate.fundeli5.rulecard;

import java.util.List;

/**
 * Port: where the Rule Card Validator's cards live. Keyed by fund name and version; a Validated card is
 * never replaced (versions are superseded, never edited), a Draft is replaced by its own edits and by its
 * validated self.
 */
public interface RuleCardStore {

    /** Every card held, any status. */
    List<RuleCard> all();

    /** Saves the card under its fund name and version. */
    void save(RuleCard card);
}
