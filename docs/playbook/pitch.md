# FundEli5

**Fund eligibility a five-year-old could follow, and an auditor could check.**
Can this client buy this fund? A cited answer in seconds, explained in plain words.

<!-- Resolves https://github.com/smnajid/fund-gate/issues/12. One A4 page when exported to PDF: keep each section to 2–5 lines. -->

## The problem

Every subscription request sends the fund dealing desk through a prospectus of about 400 pages, two regulators' rules (Swiss FinSA/CISA, Luxembourg MiFID II/UCITS/AIF) and the client file, to answer one question: may this portfolio buy this Share Class? Then the relationship manager still has to explain the "no" to the client.
The desk handles about 10,000 subscriptions a year, roughly 40 a working day (sponsor, 2026-10-15).
<!-- TODO #14: minutes per request, from the desk interview. Never invent it. -->

## What you'll see

A relationship manager emails the desk: put EUR 80,000 from two of Anna's portfolios into Nordea Global Climate, institutional class. FundEli5 answers each portfolio in seconds:

- **Not Eligible** for the advisory portfolio: it is not an institutional investor. **Eligible** for the discretionary mandate. Every reason cites the prospectus page.
- **Explained like you're five**: a short paragraph the RM can forward to the client, with a suggested class she *can* buy.
- **Form guided**: a checklist of the declarations and documents the subscription needs, pre-filled from the client's facts.
- **Questions answered**: "why not the I class?", "what would make her eligible?"

- **No email needed**: the same check from a structured request (portfolio, ISIN, amount), no AI involved. Every subscription the bank executes passes through a GX order to Negoce, so the next step is Negoce checking each fund Order with FundEli5 ([order flow](order-flow-integration.md)).

Closing: "which funds can Tom buy?" One US citizen, three funds, three different US Person definitions.

## How it works

```
 Fund docs ─► LLM drafts   ─► human       ─► engine     ─► reply, explained
              Rule Cards      validates      decides       like you're five
              (cited)                        (no LLM)
 ─────────────────────────────────────────────────────────────────────────
 Privacy Gate: client identity never reaches the LLM. Every check audited.
```

The AI drafts, a person validates, deterministic code decides. The explanation and the questions never change a Verdict.

## Already de-risked

- **Extraction on a real prospectus** (Nordea 1): 18/18 rules found, every quote checked on its page, 1–2 min and $0.2–1.2 per fund.
- **Claude on Microsoft Foundry** confirmed for PDF, citations and structured output.
- **A 26-case answer key** across Switzerland and Luxembourg, plus screens and architecture decided and recorded as ADRs.
- The Must tier is built before the event on synthetic data; the 20 hours move it onto the bank's data and add the rest.

## The team

Five people: one builder and four specialists from the business side. The desk that will use it advises every day.

| Who | Profile | Brings |
|---|---|---|
| Mohamed | Builder | The whole app: pipeline, engine, Privacy Gate, screens, bank adapters, audit trail |
| Frederic | Banking Relation Specialist | The relationship manager's voice: the requests and whether the explanation can go to a client |
| Cynthia | Fund Custodian Bank Specialist | Fund rules and registrations: validates the Rule Cards, writes the form checklist |
| Mathilde | Data and Reporting Specialist | The client and fund data, and the 26-case answer key run in the app |
| Xavier | Business Development Manager | The business case and the pitch |
| Samuel and Tania | Fund dealing desk, advisers | Answer domain questions 30 minutes a day |

Stack: Spring Boot 4 and Angular, Claude on Microsoft Foundry.
