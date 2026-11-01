package com.fundgate.fundeli5;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectDirectories;
import org.junit.platform.suite.api.Suite;

/**
 * The executable specification. Not named *Test, so the default build skips it; run it by name:
 * {@code ./mvnw test -Dtest=SpecificationSuite -Dcucumber.filter.tags="@rule-engine"}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectDirectories("../specs")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.fundgate.fundeli5.specs")
public class SpecificationSuite {
}
