package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.fundregistrations.FundRegistrations;
import com.fundgate.fundeli5.fxrates.FxRates;

/** The bank data a plug-in may look up beyond the facts of the check. Grows with the plug-ins that need it. */
public record Lookups(FundRegistrations registrations, FxRates fxRates, ScaleMapping scaleMapping) {

    /** Lookups with the demo dataset's Scale Mapping. */
    public Lookups(FundRegistrations registrations, FxRates fxRates) {
        this(registrations, fxRates, ScaleMapping.demo());
    }
}
