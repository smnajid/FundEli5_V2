package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.fundregistrations.FundRegistrations;

/** The bank data a plug-in may look up beyond the facts of the check. Grows with the plug-ins that need it. */
public record Lookups(FundRegistrations registrations) {
}
