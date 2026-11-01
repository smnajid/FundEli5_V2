package com.fundgate.fundeli5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;
import org.junit.platform.testkit.engine.Events;

/**
 * Runs the whole specification inside the default build. A scenario with an undefined step is a
 * module's to-do list and does not fail the build; a scenario that really fails or errors does.
 */
class SpecificationRunsTest {

    @Test
    void noScenarioReallyFails() {
        Events finished = EngineTestKit.engine("junit-platform-suite")
                .selectors(selectClass(SpecificationSuite.class))
                .execute()
                .testEvents()
                .finished();

        assertThat(finished.count()).as("scenarios found under ../specs").isPositive();

        List<String> realFailures = finished.stream()
                .filter(event -> event.getRequiredPayload(TestExecutionResult.class).getStatus()
                        == TestExecutionResult.Status.FAILED)
                .filter(event -> !isUndefinedStep(event))
                .map(event -> event.getTestDescriptor().getDisplayName() + ": "
                        + event.getRequiredPayload(TestExecutionResult.class).getThrowable())
                .toList();

        assertThat(realFailures).isEmpty();
    }

    private static boolean isUndefinedStep(Event event) {
        return event.getRequiredPayload(TestExecutionResult.class).getThrowable()
                .filter(t -> t.getClass().getSimpleName().equals("UndefinedStepException"))
                .isPresent();
    }
}
