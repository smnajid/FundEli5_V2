package com.fundgate.fundeli5.rulecard;

import com.fundgate.fundeli5.bankdata.Isin;
import java.util.Optional;

/** Port: where the Rule Engine finds the Rule Card of a Share Class. */
public interface RuleCards {

    /** The card that covers the class: its newest Validated version, else its newest Draft; empty when none. */
    Optional<RuleCard> forShareClass(Isin isin);
}
