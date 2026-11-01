package com.fundgate.fundeli5.fundregistrations;

import com.fundgate.fundeli5.bankdata.InvestorType;
import java.util.Set;

/**
 * The bank's record. {@link NotRegistered} ("the country is missing from a list that exists") and
 * {@link Unknown} ("there is no list") differ on purpose.
 */
public sealed interface Registration {

    record Registered(Set<InvestorType> investorTypes) implements Registration {
        public Registered {
            investorTypes = Set.copyOf(investorTypes);
        }
    }

    record NotRegistered() implements Registration {
    }

    record Unknown() implements Registration {
    }
}
