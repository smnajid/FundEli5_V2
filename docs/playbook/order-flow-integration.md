# Order flow integration: from desk tool to the order flow

Not built at the hackathon: this note maps where FundEli5 fits in the bank's order flow, for the pitch and later brainstorming. Sources: exchanges with Samuel (fund dealing desk), 2026-10-20, a grilling session the same day, and a correction of the bank's architecture on 2026-10-24. Vocabulary: [GLOSSARY.md](../../GLOSSARY.md).

Diagrams: the [architecture map](https://claude.ai/artifact/UwWpZvZGcQ3WxY1zfpTXUM) (current architecture and this roadmap, interactive) and the [roadmap slides](https://claude.ai/artifact/Pbkn19Z7SmPQ3Z5Q4nMNXD) shown at the end of the 5-minute demo. Both are private artifacts: ask Mohamed for access.

## The bank's order flow

```
 PM ─► GX (portfolio management) ─────────────► Negoce (dealing desk) ─► booking
        │ validate                                 transmits the Order directly
        ▼
      ValidationEngine ─► Kafka (async) ─► sub-validators
                                            ├─ client constraints (suitability, cash)
                                            ├─ credit risk
                                            └─ OrderRules (market: min amount, round lot)
```

- GX is the next version of G2, the portfolio management system.
- Portfolio managers often skip the email and create the **Order** directly in GX. **100% of executed subscriptions pass through GX → Negoce**: no Order in Negoce, no execution.
- GX transmits the Order to Negoce directly; the transmission does not pass through the ValidationEngine.
- GX asks the **ValidationEngine** to validate the Order. The sub-validators are components outside the ValidationEngine, which delegates to them asynchronously over Kafka.
- Today the fund dealing desk re-checks eligibility by hand on each fund Order it receives in Negoce: the same work as an emailed request, triggered differently.

So email covers the question asked *before* an Order; the Order flow covers *every* execution.

## Roadmap

1. **Standalone, on real bank data.** FundEli5 is used as at the hackathon: the dealer pastes an emailed request, or a structured Subscription Request (portfolio, ISIN, amount, currency) gets its Verdict through the same OpenAPI endpoint the dealer form uses, with no LLM call. What changes is behind the ports ([ADR 0008](../adr/0008-bank-data-enters-through-canonical-contracts.md)): FMD supplies Share Classes and Fund Registrations, the CRM Gateway supplies Client Profiles and Portfolio Ids. The order flow is untouched.
2. **Negoce calls FundEli5** (REST) on each incoming fund Order and shows the Verdict and answer sheet beside it. The email path stays for questions asked before an Order exists.

Stage 2 is possible because the Verdict involves no LLM ([ADR 0002](../adr/0002-no-llm-at-check-time.md)): Negoce gets the same answer, fast, every time it asks about the same Order, and the Check Record makes it auditable.

## What the desk does with the Verdict in Negoce

| Verdict | The desk |
|---|---|
| Eligible | lets the Order through, with the Justification on file |
| Not Eligible | rejects the Order, citing the rule and its page in the Fund Documentation |
| Needs Review | checks by hand, as today, starting from the answer sheet |

The desk's process and its decision stay the same; FundEli5 removes the manual re-check where the rules already answer. The Reply Drafter has no role here: there is no email to answer.

## Not on the roadmap: FundEli5 as a sub-validator

FundEli5 could become a distinct sub-validator for fund Orders, consuming the ValidationEngine's Kafka topics like the others, so every fund Order is checked before it reaches Negoce. It is left out of the roadmap because it implies process changes and raises questions the pitch cannot answer yet:

- **Needs Review.** Either the ValidationEngine holds the Order in a manual workflow for the desk, or the PM gets a warning that the desk will review the Order and may reject it. A warning today means the PM must justify; a Needs Review warning would only inform.
- **Who validates.** The check moves upstream of the desk, which goes from validating every Order to validating the rules as Rule Card Validators ([ADR 0001](../adr/0001-llm-extracts-rules-engine-decides.md), [ADR 0003](../adr/0003-validator-marks-unmodelled-restrictions-noted.md)).
- **Overlap with existing sub-validators.** OrderRules checks a minimum amount and could delegate the per-Share-Class minimum to FundEli5; that is the desk's decision. It is unknown whether the client-constraints sub-validator already covers the MiFID Target Market.

## Open questions

- Does the client-constraints sub-validator cover the MiFID Target Market? Only matters for the sub-validator idea. (owner: Samuel)
- For discretionary mandates, is Target Market checked at mandate level or at client level? Affects the Target Market rules.
