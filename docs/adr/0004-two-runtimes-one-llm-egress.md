# Two runtimes split on client data; one LLM egress behind a Privacy Gate

The system is split into an offline Rule Card pipeline, which only ever reads public Fund Documentation, and an online dealer app, which holds client data; they share one store. Every LLM call from the dealer app goes through a single egress client that only accepts Privacy Gate output. The Gate swaps Portfolio References for Portfolio Ids through the CRM, sends client facts only from a per-call allow-list, and fails closed: if a known client name or an unknown reference remains, no LLM call is made and the dealer enters the request by hand. Requesters identify portfolios by Portfolio Reference only, so the Gate stays deterministic. We chose this because the organisers forbid client-identifying data reaching an external LLM, and a boundary drawn in the deployment and enforced in one place is something a bank's DEV, OPS and INFRA heads can verify.

## Considered Options

- **One app, LLM called wherever needed**: simpler, but the privacy guarantee would rest on every module's discipline instead of one checkpoint.
- **NER-based redaction (e.g. Presidio) of free-text emails**: catches names we don't know, but it is probabilistic and needs tuning; unnecessary once Requesters send references only.
- **No LLM on emails at all (form only)**: safest, but drops the email-to-reply demo and the "rapid" half of the brief.
