package com.fundgate.fundeli5.ruleengine.plugin;

import com.fundgate.fundeli5.clientprofiles.ClientProfile;
import com.fundgate.fundeli5.eligibility.SubscriptionRequest;
import com.fundgate.fundeli5.shareclasses.ShareClass;

/** What a check knows: the request, the Client Profile and the Share Class. Unknown facts are null in them. */
public record Facts(SubscriptionRequest request, ClientProfile profile, ShareClass shareClass) {
}
