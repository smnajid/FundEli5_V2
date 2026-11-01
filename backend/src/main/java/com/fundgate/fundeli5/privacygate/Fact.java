package com.fundgate.fundeli5.privacygate;

/**
 * The client facts a call to a model may carry (architecture.md, "Privacy boundary"). A fact that is
 * not here cannot be asked for: name, address, balances, free-text labels and the exact birth date
 * have no constant.
 */
public enum Fact {
    LEGAL_ENTITY,
    INVESTOR_CLASSIFICATION,
    MANDATE_TYPE,
    RESIDENCE_COUNTRY,
    NATIONALITIES,
    TAX_RESIDENCES,
    /** Derived: "US-born yes/no"; the birth country itself is never sent. */
    US_BORN,
    FATCA_STATUS,
    US_PERMANENT_RESIDENT,
    KNOWLEDGE_AND_EXPERIENCE,
    RISK_PROFILE,
    ABILITY_TO_BEAR_LOSSES,
    HORIZON_YEARS
}
