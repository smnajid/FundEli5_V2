package com.fundgate.fundeli5.ruleengine;

import com.fundgate.fundeli5.eligibility.SubscriptionRequest;

/** Decides a Subscription Request with no model, ever (ADR 0002). */
public interface RuleEngine {

    EngineResult check(SubscriptionRequest request);
}
