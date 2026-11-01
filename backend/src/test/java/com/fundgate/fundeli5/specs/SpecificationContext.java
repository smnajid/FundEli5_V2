package com.fundgate.fundeli5.specs;

import io.cucumber.spring.CucumberContextConfiguration;
import com.fundgate.fundeli5.FundEli5Application;
import org.springframework.boot.test.context.SpringBootTest;

/** Shared Spring context of every step-definition package; random port, replay egress. */
@CucumberContextConfiguration
@SpringBootTest(classes = FundEli5Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "fundeli5.llm.endpoint=replay")
public class SpecificationContext {
}
