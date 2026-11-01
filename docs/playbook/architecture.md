# Reference architecture

Resolves [What is the reference architecture and its module boundaries?](https://github.com/smnajid/fund-gate/issues/9).

Stack: Spring Boot 4 for both runtimes (the pipeline is a command-line mode of the same app, run as its own process) and Angular for both screens, with the Angular client generated from the backend's OpenAPI spec; Python only where it adds real value, which today is nowhere ([ADR 0006](../adr/0006-spring-boot-and-angular-python-only-for-real-added-value.md)). Screens: [screens.md](screens.md). Diagrams: the interactive [architecture map](https://claude.ai/artifact/UwWpZvZGcQ3WxY1zfpTXUM) and the [roadmap slides](https://claude.ai/artifact/Pbkn19Z7SmPQ3Z5Q4nMNXD), both private artifacts (ask Mohamed for access). Every module below is a seam with a typed interface, so one person can own it. Vocabulary: [GLOSSARY.md](../../GLOSSARY.md). Decisions: [ADR 0004](../adr/0004-two-runtimes-one-llm-egress.md) (shape and privacy boundary), [ADR 0005](../adr/0005-advisory-llm-lane-after-the-engine.md) and [ADR 0007](../adr/0007-advisory-suggestions-never-change-the-verdict.md) (advisory lane), [ADR 0002](../adr/0002-no-llm-at-check-time.md) (the Verdict involves no LLM).

## Shape

Two runtimes, split on whether they touch client data, sharing one store.

```
 OFFLINE: public data only                 ONLINE: dealer app, client data stays inside
 ┌───────────────────────────┐             ┌─────────────────────────────────────────────────┐
 │ Rule Card pipeline (CLI)  │             │ Intake (paste / .eml)                           │
 │  page selection           │             │  → Privacy Gate (Portfolio Ref → Portfolio Id,  │
 │  extract (LLM, Citations) │             │     fail-closed guard, fact allow-list)         │
 │  quote-on-page check      │             │  → Request Parser (LLM) → Share Class catalog   │
 └─────────────┬─────────────┘             │  → Rule Engine (deterministic)                  │
               │ Draft cards               │      ← Client Profile, CRM, Fund Registration,  │
               ▼                           │        Scale Mapping (adapters)                 │
        ┌──────────────┐                   │  → Advisory lane (LLM, Needs Review only)       │
        │ Store        │◄─────────────────►│  → Reply Drafter (LLM, template fallback)       │
        │ Rule Cards,  │                   │  → Check Record (append-only)                   │
        │ Scale Mapping│                   │ Validation UI (Draft → Validated)               │
        │ Check Records│                   │ Reverse lookup (engine × every catalog class)   │
        └──────────────┘                   │ LLM egress client: the only path to Foundry     │
                                           └─────────────────────────────────────────────────┘
```

## Modules

| Module | Runtime | Input → output | LLM? |
|---|---|---|---|
| **Rule Card pipeline** | offline CLI | Fund Documentation PDF → Draft Rule Card (cited), quotes checked on their page. Ported from the [extraction spike](https://github.com/smnajid/fund-gate/tree/prototype/rule-card-extraction/spikes/rule-card-extraction) | yes, public documents only |
| **Share Class catalog** | online | fund/class mention or ISIN → Share Class(es); a fund named without a class fans out to all its classes. The same matcher serves the reverse-lookup search ([screens](screens.md#reverse-lookup-what-can-this-portfolio-buy)) | no |
| **Validation UI** | online | Draft card → corrected, Validated card; Unmodelled Restrictions marked Blocking/Noted (ADR 0003); who and when kept per version | no |
| **Intake** | online | pasted text or `.eml` → raw request text | no |
| **Privacy Gate** | online | raw text → pseudonymised text + local Portfolio Reference ↔ Portfolio Id map; client facts → allow-listed fields for one call | no |
| **Request Parser** | online | pseudonymised text → editable list of Subscription Requests (Portfolio Id, Share Class mention, amount, currency, client-initiated) | yes |
| **Rule Engine** | online | Subscription Request + Validated Rule Card + Client Profile + Fund Registration + Scale Mapping → Verdict + Justification | **never** |
| **Reverse lookup** | online | portfolio + amount → engine run against every Share Class in the catalog; Draft-only or card-less classes give Needs Review, as a single check would | no |
| **Advisory lane** | online | Needs Review Justification + allow-listed facts → Advisory Suggestions, each a Requester Question or a Dealer To-do; never changes the Verdict | yes |
| **Reply Drafter** | online | Verdict + Justification + accepted Requester Questions + allow-listed facts → reply in the request's language; Portfolio Ids swapped back afterwards; template per Verdict as fallback, listing accepted Requester Questions verbatim | yes |
| **Check Record** | online | everything a check read and produced → append-only record | no |
| **LLM egress client** | both | the only code that calls Foundry; the online side accepts only Privacy Gate output | — |
| **Adapters** | online | Behind out ports with canonical models ([ADR 0008](../adr/0008-bank-data-enters-through-canonical-contracts.md)): `ClientProfiles` gives the Client Profile and the Portfolio Id ([contract](contracts/client-profile.md)); `ShareClasses` the Share Class catalog ([contract](contracts/share-classes.md)); `FundDocuments` the pipeline's documents ([contract](contracts/fund-documents.md)); `FundRegistrations` the bank's Fund Registration ([contract](contracts/fund-registrations.md)). The adapter is chosen per port: at the event, FMD for Share Classes and Fund Registration, a folder of real PDFs for documents, fixtures for Client Profiles (DEV's CRM and FMD PDFs are mocks) | no |

The **Client Profile**, **Share Classes**, **Fund Documents** and **Fund Registrations** contracts are written before the event ([contracts/](contracts/), with fixtures); the **Rule Card**, **Subscription Request** and **Check Record** schemas are fixed with the skeleton on Wed 28 Oct. Everything is built against fixtures from [demo-dataset.md](demo-dataset.md); the behaviour it must produce is the executable specification in [`specs/`](../../specs/00-fundeli5.feature).

## Data flow of one request

1. The Fund Dealer pastes the Requester's email. Requesters identify portfolios by **Portfolio Reference only, never client data** (*provisional: to confirm with the business partners and the desk*).
2. The Privacy Gate swaps every `[A-Z]{3}\d{8}` match for its Portfolio Id through the CRM, then re-scans. If a known client name or a reference the CRM does not know is still there, **no LLM call is made** and the dealer fills the request form by hand.
3. The Request Parser turns the pseudonymised text into Subscription Requests. The dealer sees them in an editable form; several references give several checks; anything unresolved becomes Needs Review "request unclear", never a guess.
4. The Rule Engine runs each check, with no LLM. A Share Class with only a Draft card, or no card, gives Needs Review ("Rule Card not validated" / "Fund not covered"); a Draft card never gives Eligible or Not Eligible.
5. On Needs Review, the advisory lane drafts Advisory Suggestions: Requester Questions and Dealer To-dos. They are labelled AI-generated and **never change the Verdict** ([ADR 0007](../adr/0007-advisory-suggestions-never-change-the-verdict.md)). The dealer accepts or dismisses each one, optionally noting why on a dismissal.
6. The Reply Drafter writes the reply: at once on checks without suggestions, and when the dealer clicks **Draft reply** on checks with suggestions, so accepted Requester Questions are asked in it. Undecided suggestions are left out with a warning; changing a decision afterwards offers a redraft that never overwrites the dealer's edits. The Portfolio Ids are swapped back to Portfolio References locally.
7. A Check Record is written. The check ends when the reply is sent; the follow-up of a Needs Review happens outside the app.

## Privacy boundary

- **Never sent to an LLM**: client name, Portfolio Reference, address, phone, email, exact birth date, place of birth, balances.
- **Allow-listed pseudonymised facts**, only the fields each call needs: Portfolio Id, Legal Entity code, Investor Classification, mandate type, residence country, nationalities, tax residence, US Person / FATCA flags, knowledge and experience, risk profile, ability to bear losses, horizon. Derived values when needed (age band, "US-born yes/no"). *Permitted pending written confirmation from the sponsor.*
- Minimum per call, because combinations of quasi-identifiers can re-identify private-bank clients.
- Each Check Record logs exactly which fields went out.

## Pre-computed vs live

| Pre-computed | Live |
|---|---|
| Rule Cards for the 4 sample funds (1–2 min, $0.2–1.2 each) | Validating one Draft card on stage |
| Share Class catalog | Intake → Gate → Parser → Engine → advisory lane → Reply |
| Scale Mapping, Fund Registration, fixtures | Reverse lookup |

**Versioning.** A new Fund Documentation edition gives a new Draft version. The previous Validated version stays in force until the new one is validated. Versions are never edited, only superseded.

## Audit

One append-only **Check Record** per Eligibility Check: the Subscription Request, Rule Card version, Scale Mapping version, snapshot of the Client Profile fields read, engine version, Verdict, Justification, every accept or dismiss of an Advisory Suggestion (who, when, optional note), and for every LLM call the gated prompt, the response and the model id. The dealer UI has a "View Check Record" panel showing exactly what the LLM saw. Replaying a past Verdict (deterministic engine + pinned versions) is mentioned on the production slide, not built.

## Roles

- **Fund Dealer** runs checks and sends replies.
- **Rule Card Validator** (compliance or a senior dealer) validates cards. Separation of duties in production; one person plays both on stage.

## Runtime

- **Two environments**: we build in an external cloud environment that cannot reach the bank, with the fixture adapters. The app is then cloned into the bank's internal environment, the bank's DEV environment. There each port keeps its fixture adapter or switches to its bank adapter on its own, as a Spring property per port: FMD's instrument and registration data are real, its PDFs and the CRM's clients are mocks. The bank adapters are the only code written there ([ADR 0008](../adr/0008-bank-data-enters-through-canonical-contracts.md)).
- **Demo**: runs locally on our laptops (one compose file: dealer app + store; the pipeline as a CLI against the same store). Claude on Microsoft Foundry through the egress client. Fallbacks by config switch: our own Anthropic key, then a recorded-responses mode for offline rehearsal.
- **Production path (slide only)**: Container Apps, Entra ID SSO for dealers and validators, Key Vault, Foundry over a private endpoint, logs to Log Analytics, Check Record store as the audit source. Next lanes: NER-based redaction is not needed while Requesters send references only; advisory options (b) suggest Blocking/Noted per client and (c) propose facts from free-text KYC notes; a required reason list on dismissing an Advisory Suggestion (to measure how often the lane is wrong) and tracking a Needs Review to its final answer; Microsoft Graph mailbox intake.

## Ownership

Mohamed is the only builder: he owns every module above, builds the Must tier on fixtures before the event, and writes the bank adapters in the internal environment at the event. The other four own the content the modules run on.

| Seat | Owns |
|---|---|
| Mohamed, builder | Every module, the executable specification in `specs/`, merging, the production slide |
| Frederic, Banking Relation Specialist | Request emails in real RM wording, plain-language wording of templates and replies |
| Cynthia, Fund Custodian Bank Specialist | Rule Card validation (first pass), Fund Registration facts, form checklist content |
| Mathilde, Data and Reporting Specialist | Client Profile fixtures, mapping CRM and FMD fields onto the [contracts](contracts/), running the answer key in the app |
| Xavier, Business Development Manager | Pitch, business case, roadmap slides |

Samuel and Tania (fund dealing desk) answer domain questions 30 minutes a day, before and during the event. The advisory lane, free-text Q&A and LLM polish on ELI5 stay cut and shown on the production slide only; "what would make it eligible" stays as a thin Q&A that re-runs the engine. Scope tiers, the day-by-day plan and merge rules: [team-plan.md](team-plan.md).
