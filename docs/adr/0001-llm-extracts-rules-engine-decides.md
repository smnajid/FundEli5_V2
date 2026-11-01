---
status: accepted, amended by ADR-0002 (no LLM at check time)
---

# The LLM drafts Rule Cards; a deterministic engine decides

An LLM reads Fund Documentation once per Share Class and drafts a Rule Card (each rule carrying its Citation); a human validates it; a deterministic rule engine then evaluates Hard Restrictions against the Client Profile to produce the Verdict. The LLM is used at check time only for fuzzy Target Market matching and for flagging Needs Review, never to decide a Hard Restriction. We chose this over asking an LLM for a Verdict per check (RAG-style) because a bank must be able to reproduce, audit, and defend every Verdict, and "the AI drafts, a human approves, the engine decides with citations" is the answer to "what if the AI is wrong?".

## Considered Options

- **LLM at check time (retrieve passages, ask for a Verdict)**: fastest to build, but non-reproducible, hallucination-prone, and unauditable.
- **Fully deterministic, rules hand-encoded**: auditable but doesn't scale to new funds and throws away the "rapid" half of the brief.
