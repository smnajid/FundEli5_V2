package com.fundgate.fundeli5;

import com.fundgate.fundeli5.rulecard.RuleCardStore;
import com.fundgate.fundeli5.rulecard.folder.FolderRuleCardStore;
import com.fundgate.fundeli5.validation.ValidationService;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Wires the Validation module: Drafts read from the drafts folders, work saved to the output folder. */
@Configuration
@Profile("!" + Runtimes.PIPELINE)
class ValidationConfiguration {

    @Bean
    RuleCardStore validationRuleCardStore(@Value("${fundeli5.validation.drafts}") String drafts,
            @Value("${fundeli5.validation.out:target/validated-rule-cards}") String out) {
        List<Path> folders = Arrays.stream(drafts.split(",")).map(String::trim).filter(f -> !f.isEmpty()).map(Path::of)
                .toList();
        return new FolderRuleCardStore(folders, Path.of(out));
    }

    @Bean
    ValidationService validationService(RuleCardStore store) {
        return new ValidationService(store, Clock.systemUTC());
    }
}
