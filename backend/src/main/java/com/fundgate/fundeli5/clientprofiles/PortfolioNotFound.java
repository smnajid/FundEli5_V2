package com.fundgate.fundeli5.clientprofiles;

/** The bank does not know the Portfolio Reference. */
public class PortfolioNotFound extends RuntimeException {

    public PortfolioNotFound(PortfolioReference ref) {
        super("Portfolio not found: " + ref /* masked by toString */);
    }
}
