package com.fundgate.fundeli5.specs;

import com.fundgate.fundeli5.FundEli5Application;
import com.fundgate.fundeli5.llm.LlmEgress;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Shared Spring context of every step-definition package; random port, replay egress behind a spy. */
@CucumberContextConfiguration
@SpringBootTest(classes = FundEli5Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "fundeli5.llm.endpoint=replay")
@Import(SpecificationContext.SpyConfiguration.class)
public class SpecificationContext {

    @TestConfiguration(proxyBeanMethods = false)
    static class SpyConfiguration {

        @Bean
        static BeanPostProcessor spyOnTheEgress() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    return bean instanceof LlmEgress egress && !(bean instanceof SpyLlmEgress)
                            ? new SpyLlmEgress(egress) : bean;
                }
            };
        }
    }
}
