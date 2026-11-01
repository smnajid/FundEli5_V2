---
status: accepted, amends ADR-0002 (no LLM at check time); amended by ADR-0007 (suggestions never change the Verdict)
---

# An advisory LLM lane runs after the engine: downgrade-only, allow-listed facts

The bank allows pseudonymised client facts (no name, Portfolio Reference, address or birth date) to reach the LLM, pending written confirmation from the sponsor. So an advisory lane runs after the Rule Engine. On a Needs Review it drafts what the dealer should check and which evidence to file, from the Justification's Citations and allow-listed facts. The Verdict itself stays deterministic and reproducible as ADR 0002 requires. The lane may only downgrade a Verdict, never move it toward Eligible. Its suggestions are labelled AI-generated, stored in the Check Record with the exact fields sent, and a human accepts them. A wrong suggestion can therefore cost the dealer time but can never let through a subscription the rules forbid.

## Considered Options

- **Suggest Blocking/Noted for an Unmodelled Restriction per client**: overlaps with the Rule Card Validator's once-per-card decision (ADR 0003); left for production.
- **Propose facts from free-text KYC notes**: the most powerful, but no data source exists yet; left for production.
- **No lane**: Needs Review would stay a bare list of Citations, the weakest moment of the demo.
