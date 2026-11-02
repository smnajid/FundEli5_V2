package com.fundgate.fundeli5.rulecardpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.fundgate.fundeli5.rulecard.Condition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DraftAnswerTest {

    @Test
    void aNumberWrittenAsTheTargetMarketValueIsReadAsItsText() {
        String text = """
                ```json
                {"fundName": "F", "rules": [
                  {"scope": "fund", "shareClass": null,
                   "condition": {"kind": "target_market", "dimension": "risk_sri", "value": 4},
                   "citations": [{"document": "kid", "page": 1, "quote": "4 out of 7"}],
                   "summary": "SRI 4", "confidence": 0.9, "contradiction": false}]}
                ```""";
        List<DraftAnswer> accepted = new ArrayList<>();
        DraftAnswer answer = DraftAnswer.parse(text, accepted::add);
        var condition = (Condition.TargetMarket) answer.rules().get(0).condition();
        assertThat(condition.value()).isEqualTo("4");
    }
}
