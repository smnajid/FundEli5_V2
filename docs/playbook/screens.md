# Screens

Resolves [What should the dealer screen and the Rule Card validation screen look like?](https://github.com/smnajid/fund-gate/issues/17).

Two screens, both Angular ([ADR 0006](../adr/0006-spring-boot-and-angular-python-only-for-real-added-value.md)), built by Mohamed ([architecture](architecture.md#ownership)). The throwaway prototype holds every variant we compared: [`prototype/screens`](https://github.com/smnajid/fund-gate/tree/prototype/screens/spikes/screens) (`open spikes/screens/index.html`). Rebuild from this page, not from the prototype's code. Vocabulary: [GLOSSARY.md](../../GLOSSARY.md).

## Dealer screen: the answer sheet

The screen leads with the answer and keeps the evidence one click away. Top to bottom:

1. **Paste.** A text area for the Requester's email (or a dropped `.eml`) and one button, **Answer it**. Afterwards the email folds into one line: "Request from Marc Aebi" plus the Privacy Gate line (`LOC90000101 → 3f6c…`, "the LLM saw Portfolio Ids only"). Unfolding it shows the email and the parsed Subscription Requests in an editable form.
2. **One sentence per check**, coloured by Verdict: "`LOC90000101` **cannot buy** Nordea 1 Global Climate BI-EUR. Advisory retail portfolio is not an institutional investor." An alternative the reply suggests is marked "(alternative)".
3. **Advisory Suggestions** on a Needs Review, grouped **Ask the Requester** / **Dealer to-do**, each with Accept and Dismiss (optional note), labelled AI-generated ([ADR 0007](../adr/0007-advisory-suggestions-never-change-the-verdict.md)). They sit above the reply because the dealer decides on them before drafting.
4. **The reply**, the largest element: drafted by AI, editable, **Copy reply**. Numbered markers `[1]` link to an **evidence rail** beside it: per marker, the rule outcome and the quoted passage with its page. On a check with suggestions the reply waits for **Draft reply**; a changed decision offers **Redraft**.
5. **What the AI saw** opens the Check Record in a side drawer: Rule Card versions, Verdicts, the dealer's decisions on suggestions, and for each LLM call the gated prompt, the fields sent and the response.
6. **Full Justification**, folded: every rule of every check with its outcome and Citation.

**Privacy Gate refusal.** A red panel replaces everything below the paste step ("No AI used for this email", what was found and where), followed by the manual Subscription Request form. The check runs as usual; a template reply replaces the drafted one.

### The request form: what the dealer can edit

**Rule: what the AI read is editable, what code found is not.** The AI drafts, the dealer corrects.

| Field | Editable | Source |
|---|---|---|
| Portfolio Reference | no | Privacy Gate: a fixed pattern match plus a CRM lookup, no AI |
| Share Class | yes, a dropdown of catalog classes | the AI's mention, matched by the Share Class catalog |
| Amount | yes | AI |
| Currency | yes | AI |
| Client initiated | yes | AI |

The Portfolio Reference stays fixed because it is the key to the Client Profile and the Check Record: an editable one would let a check run against a different client than the email names. A wrong reference means fixing the email and pasting it again. Currency is editable because a wrong one changes the Verdict (EUR 80,000 passes a EUR 75,000 minimum; CHF 80,000 may not). The prototype got this wrong and shows currency as plain text.

**Open:**
- **Does an edit rerun the check?** Either at once or with a **Re-check** button. In the prototype an edit changes nothing.
- **Are edits recorded?** The Check Record should keep both the AI's parse and the dealer's correction, the way the validation screen tells "corrected" from "accepted". Otherwise an audit cannot tell whether the AI or the dealer chose the Share Class.

### Reverse lookup: "what can this portfolio buy?"

Portfolio Reference and amount in, one engine run against **every Share Class in the catalog** out. A Share Class with only a Draft card gives Needs Review "Rule Card not validated", and one with no card gives "Fund not covered", exactly as a single Eligibility Check would.

- **Rows are Share Classes, columns are the eight rule kinds**, so the width is fixed and a long fund list only scrolls. The deciding cell is outlined (red Fail, amber Review) and opens its Citation.
- **Grouped by Verdict.** Can buy and Needs Review start open; **Cannot buy** starts collapsed and is **sub-grouped by the deciding rule**: for Tom, "US Person 17 · Fund Registration 7 · Investor category 6 …". At demo size Tom has 6 rows; the same screen holds the bank's whole fund list.
- A count line on top: Eligible n · Needs Review n · Not Eligible n.

**Search** narrows the rows already computed; it never reruns the engine.
- **Fields:** fund name, Share Class name, ISIN, currency and fund regime from the Share Class catalog, plus the Valor number and the bank's internal fund code if its fund list carries them (known at the event).
- **Matching** is deterministic, with no AI and no fuzzy guessing: case, accents and punctuation are ignored, every typed word must appear in any order, and an ISIN or Valor matches exactly or by prefix.
- **One matcher.** The search reuses the Share Class catalog's matcher, the one that turns an email's fund mention into Share Classes, so typing and parsing give the same answer.
- **Filters:** regime, currency, Verdict. Everything filters in the browser: a few thousand rows need no search server.

## Rule Card validation screen: the document beside the rules

Three columns, so the validator sees the rule, the passage and the decision at once:

1. **Rule list**: every rule of the Draft card with its kind, value, scope (class or all classes) and a status: open, accepted, corrected, or **!** for an unresolved contradiction.
2. **Document**: the cited page with the quote highlighted, one tab per Citation (e.g. prospectus p.123, p.5, KID p.1). Build the page as the **extracted page text with the quote highlighted** first; the rendered PDF page with the highlight drawn on it is an upgrade if time allows.
3. **Decision**: the extractor's summary and the signals the code checks (quote found on its page, value with no supporting quote, contradiction between documents; working choice in [hypotheses.md](hypotheses.md), the model's self-rated confidence is not shown), the editable value, and **Accept** or **Save correction**, which then jumps to the next open rule. An Unmodelled Restriction gets **Blocking / Noted** ([ADR 0003](../adr/0003-validator-marks-unmodelled-restrictions-noted.md)). A contradiction asks the validator to pick the governing source ("follow prospectus" / "follow KID").

Above the columns:
- **Card header**: fund, Share Class, ISIN, Draft version, extraction date, "every quote verified on its page", the count of rules ready, and **Validate card**, disabled until every rule is reviewed and every contradiction settled. Validating records who and when, and the header turns to "Validated v1 by … at … · in force for Eligibility Checks".
- **Contradiction banner**, from variant C: "2 contradictions need a decision: Investor category, Target Market client type. The prospectus says institutional only; the KID says all investors." It is the strongest moment of the validation demo, so it is the first thing on screen.
- **"Possibly missed" warning**, from [Does page selection find every needed page on fund documents it wasn't tuned on?](https://github.com/smnajid/fund-gate/issues/32): when a large prospectus was sent as selected pages, a Share Class with no rule for a core rule kind (investor classification, US Person, distribution, minimum) shows "No minimum found for BI-EUR: pages 6–122 and 149–944 were not read" with those page ranges to open. Until the validator adds the rule or confirms there is none, that Share Class gives Needs Review. A page the selection misses fails silently otherwise: the quote check only catches quotes that are wrong, not rules that are absent.

## Demo reading check

| Cast | Where | What reads in seconds |
|---|---|---|
| Anna | Dealer, answer sheet | Two sentences, red and green, plus the reply suggesting BP-EUR |
| Claire | Dealer, answer sheet | Needs Review sentence, two Requester Questions accepted, the reply asks them in French |
| Tom | Reverse lookup | "Can buy 1 · Cannot buy 5", US Person outlined on three Share Classes |
| Nordea 1 BI-EUR card | Validation | The banner, then the quote highlighted on its page |

## What we did not choose

- **Dealer, single column (A)**: complete but long; the Verdict sits below the request and the parsed form.
- **Dealer, three-pane workbench (B)**: an inbox is a production feature; its rules × Share Classes grid became the reverse lookup, turned on its side because one column per Share Class does not scale.
- **Validation, one rule at a time (B)**: calm but slow, and it hides the whole card.
- **Validation, dense table (C)**: fast across many cards, which a hackathon does not have; quotes show as text with no page. Its contradiction banner was kept.
