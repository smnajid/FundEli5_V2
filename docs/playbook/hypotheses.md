# Working hypotheses

Decided on 2026-10-28. The business questions below stay open until the specialists and the desk answer them at the hackathon. Mohamed builds before the event, so each one gets a working hypothesis drawn from the architecture, the ADRs and the specs already written. The build follows the hypothesis. When an answer arrives, change the hypothesis and the code that depends on it.

Each ticket is open, labelled `hackathon` and no longer on the [playbook map](https://github.com/smnajid/fund-gate/issues/1). The ticket keeps the question; this file keeps what the build assumes in the meantime.

## How to change a hypothesis

1. Record the answer on the ticket and close it.
2. Edit the scenarios listed under **Encoded in**. For a confirmed `@assumed` scenario, remove the tag. For a corrected one, change its example.
3. Run the module's tag. The failing scenarios show the code to change.
4. Strike the row here and link the ticket's answer.

A hypothesis written only in this file, with no scenario, is hidden from the build. Before its module is built, every hypothesis that changes behaviour gets an `@assumed` scenario; rows marked *to add* don't have one yet.

## The register

| Ticket | Working hypothesis | Encoded in | Confirms |
|---|---|---|---|
| [What is the 5-minute demo script?](https://github.com/smnajid/fund-gate/issues/10) | Anna's request email → cited Verdict → plain-language explanation → drafted reply, then Tom's reverse lookup as the closing scene. The trust story is told over the validation screen. The demo shows the `@must` scenarios and then the picked `@should` ones | `@must`, `@should`; `demo-flow-verifier` | Xavier, Frederic |
| [How does the fund dealing desk answer subscription requests today?](https://github.com/smnajid/fund-gate/issues/14) | Requests arrive by email and the dealer pastes them in. Requesters name portfolios by Portfolio Reference only; a client name stops the LLM call (fail-closed). With the advisory lane cut, only pseudonymised request text and allow-listed facts reach the LLM. Volume is about 40 requests a working day; the pitch leaves minutes per request as a placeholder | `02-answer-a-subscription-request.feature` (Privacy Gate rules); `pitch.md` | Samuel or Tania |
| [How does FundEli5 explain a Verdict like you're five?](https://github.com/smnajid/fund-gate/issues/22) | Templates are keyed by rule kind × outcome, and only Fail and Review rules get a sentence. The explanation sits under the Verdict, both in the reply and on the dealer screen. Each sentence keeps its Citation marker, and the suggested alternative class is named. Templates exist in English and French and are picked by the request's language; there is no LLM translation | `03-explain-a-verdict.feature` (`@explanation @assumed`) | Samuel writes the wording; Frederic checks it can be forwarded |
| [Which eligibility questions does FundEli5 answer, and from what?](https://github.com/smnajid/fund-gate/issues/23) | Only "what would make it eligible?" is built, as a button for the dealer on the dealer screen, with no free text and no LLM. A what-if may change the mandate and the amount. Its result is labelled *Hypothetical*, written to the Check Record as its own entry, and never changes the Verdict | `03-explain-a-verdict.feature` (`@what-if @assumed`) | Samuel or Tania |
| [How does FundEli5 guide the filling of the subscription form?](https://github.com/smnajid/fund-gate/issues/25) | A checklist for the dealer only. Each item comes from one rule of the Validated Rule Card and cites it, with values pre-filled locally and no LLM. Not Eligible gives no checklist. Needs Review gives the checklist with the unresolved items flagged. No adapter for a real bank form before the event | `05-prepare-the-subscription-form.feature` (`@form-checklist @assumed`) | Cynthia, then Samuel |
| [How does FundEli5 re-check that a fund's existing holders are still eligible?](https://github.com/smnajid/fund-gate/issues/36) | Not built. Production slide only. The engine stays a pure function of one portfolio and one Share Class, so the re-check is a loop over holders when it is wanted. Tom's reverse lookup stays the closing scene | none (nothing built) | Cynthia, with the Custodian Bank Regulatory Specialist |
| [Which FMD fields exist for Share Classes, Fund Documents and Fund Registrations?](https://github.com/smnajid/fund-gate/issues/49) | Every `?` field is treated as absent, and each contract's stated fallback is built (for example, joining classes to their fund through the Rule Card's ISIN list). Fixture adapters fill the rest | `contracts/*.md`; fixture adapters | Mathilde, at the event |
| [Do Samuel and Tania accept the eligibility rules and the 26-row answer key?](https://github.com/smnajid/fund-gate/issues/50) | The specs stand as written. `@assumed` rows run like any other, and the demo avoids any row still contested | `01-check-eligibility.feature`, `99-answer-key.feature` | Samuel or Tania |
| [For a discretionary mandate, is Target Market checked at mandate level or at client level?](https://github.com/smnajid/fund-gate/issues/51) | Risk, horizon and ability to bear losses are read from the portfolio, which is the mandate's profile under a discretionary mandate. Knowledge and experience are read from the client. This follows the Client Profile split in `GLOSSARY.md` | `01-check-eligibility.feature`: the `@assumed` Row 2 example under the Target Market rule (`@scale-mapping`) | Samuel or Tania |

## Technical choices taken while the business answers wait

These aren't business questions, but the build needs an answer before the event.

| Ticket | Working choice | Encoded in |
|---|---|---|
| [What should the validation screen show instead of the extractor's self-rated confidence?](https://github.com/smnajid/fund-gate/issues/34) | The screen shows only signals the code checks: quote found on its page, a value with no supporting quote, a contradiction between documents. The model's self-rated confidence is dropped from the screen and kept in the Draft card | `06-validate-a-rule-card.feature`; `screens.md` |
