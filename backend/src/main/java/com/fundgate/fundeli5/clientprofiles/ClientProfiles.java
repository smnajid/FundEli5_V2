package com.fundgate.fundeli5.clientprofiles;

import java.util.Optional;

/** Out port (ADR 0008): the Client Profile contract. */
public interface ClientProfiles {

    /** For the Privacy Gate. Empty when the bank does not know the reference. */
    Optional<PortfolioId> portfolioId(PortfolioReference ref);

    /**
     * For the Rule Engine.
     *
     * @throws PortfolioNotFound when the bank does not know the reference
     * @throws ClientDataUnavailable when the bank's systems cannot be read
     */
    ClientProfile clientProfile(PortfolioReference ref);
}
