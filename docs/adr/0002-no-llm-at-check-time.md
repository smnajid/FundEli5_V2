---
status: accepted, amended by ADR-0005 (advisory LLM lane after the engine); ELI5 note added with #12
---

# No LLM at check time; Target Market matched by fixed rules

Amends [ADR 0001](0001-llm-extracts-rules-engine-decides.md), which allowed an LLM at check time for fuzzy Target Market matching. Target Market data comes as structured EMT/KID fields. So the Client Profile is kept in the bank's own scales, and a validated Scale Mapping translates it onto the Target Market scales. The Target Market match is then done entirely by fixed rules, and an Eligibility Check involves no LLM at all. We chose this for two reasons. Every Verdict becomes reproducible end to end. And the organisers forbid sending client-identifying data to an external LLM, which a check-time LLM reading the Client Profile would risk. The LLM still drafts Rule Cards from Fund Documentation and may draft the reply to the dealer, but only from pseudonymised text carrying Portfolio Ids.

The plain-language ("explain like I'm five") explanation FundEli5 adds is presentation, not decision. It is rendered from the Justification by a fixed template per rule kind, and the LLM may only rephrase or translate it after the Verdict, like the Reply Drafter.

## Considered Options

- **LLM matches Target Market at check time** (ADR 0001 as written): tolerant of messy client data, but sends client facts to an external model and makes part of the Verdict non-reproducible.
