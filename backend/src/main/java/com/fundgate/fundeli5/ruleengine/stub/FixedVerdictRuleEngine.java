package com.fundgate.fundeli5.ruleengine.stub;

import com.fundgate.fundeli5.eligibility.Justification;
import com.fundgate.fundeli5.eligibility.SubscriptionRequest;
import com.fundgate.fundeli5.eligibility.Verdict;
import com.fundgate.fundeli5.ruleengine.plugin.NoRuleCardRead;
import com.fundgate.fundeli5.ruleengine.EngineResult;
import com.fundgate.fundeli5.ruleengine.RuleEngine;
import java.util.List;

/**
 * Walking-skeleton engine: always Needs Review, because nothing here has read a Validated Rule Card
 * and no Verdict may move toward Eligible without one.
 */
public class FixedVerdictRuleEngine implements RuleEngine {

    @Override
    public EngineResult check(SubscriptionRequest request) {
        return new EngineResult(Verdict.NEEDS_REVIEW, new Justification(List.of(NoRuleCardRead.outcome())));
    }
}
