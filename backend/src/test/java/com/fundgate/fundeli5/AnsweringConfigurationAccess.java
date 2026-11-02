package com.fundgate.fundeli5;

import com.fundgate.fundeli5.ruleengine.RuleCatalog;

/** Lets tests outside the root package use the catalog both runtimes share. */
public final class AnsweringConfigurationAccess {

    private AnsweringConfigurationAccess() {
    }

    public static RuleCatalog catalog() {
        return AnsweringConfiguration.standardCatalog();
    }
}
