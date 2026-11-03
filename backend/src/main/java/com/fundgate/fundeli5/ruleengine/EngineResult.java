package com.fundgate.fundeli5.ruleengine;

import com.fundgate.fundeli5.clientprofiles.ClientProfile;
import com.fundgate.fundeli5.eligibility.Justification;
import com.fundgate.fundeli5.eligibility.Verdict;

/**
 * A Verdict and the rule-by-rule explanation behind it, with the inputs that produced it, so the Check Record
 * shows what the engine used and never a second read. {@code ruleCardVersion} and {@code clientProfile} are null
 * when the engine decided before reading them.
 */
public record EngineResult(Verdict verdict, Justification justification, Integer ruleCardVersion,
        String scaleMappingVersion, ClientProfile clientProfile) {

    public EngineResult {
        java.util.Objects.requireNonNull(verdict);
        java.util.Objects.requireNonNull(justification);
        java.util.Objects.requireNonNull(scaleMappingVersion);
    }

    /** A result that used no card, profile or Scale Mapping. */
    public EngineResult(Verdict verdict, Justification justification) {
        this(verdict, justification, null, "none", null);
    }

    /** Never prints the client profile. */
    @Override
    public String toString() {
        return "EngineResult[verdict=" + verdict + ", ruleCardVersion=" + ruleCardVersion
                + ", scaleMappingVersion=" + scaleMappingVersion + "]";
    }
}
