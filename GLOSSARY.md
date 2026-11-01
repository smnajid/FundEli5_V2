# Fund Gate

Helping a private bank's fund dealing desk decide, quickly and defensibly, whether a given client may subscribe to a given fund. Regimes in view: Switzerland (FinSA/CISA) and Luxembourg (EU MiFID II, UCITS/AIF, PRIIPs).

## Language

### The decision

**Eligibility Check**:
The assessment of whether a given portfolio (by Portfolio Reference) may subscribe to a given Share Class, covering Hard Restrictions, Fund Registration and Target Market match. Excludes Suitability.
_Avoid_: Suitability check, compliance check, KYC check

**Verdict**:
The outcome of an Eligibility Check: Eligible, Not Eligible, or Needs Review.
_Avoid_: Result, answer, status

**Justification**:
The rule-by-rule explanation behind a Verdict, each rule outcome tied to a Citation.
_Avoid_: Reason, explanation, rationale

**Citation**:
A pointer from a rule outcome to the exact passage of Fund Documentation (or field of Client Profile) it relies on.
_Avoid_: Reference, source

**Advisory Suggestion**:
An AI-drafted hint attached to a Needs Review, telling the Fund Dealer what to check or which evidence to file; it never changes the Verdict. Each one is a Requester Question or a Dealer To-do, and the Fund Dealer accepts or dismisses it.
_Avoid_: Recommendation, AI verdict, advice

**Requester Question**:
An Advisory Suggestion addressed to the Requester; once accepted, it is asked in the reply.
_Avoid_: Clarification, query

**Dealer To-do**:
An Advisory Suggestion the Fund Dealer carries out themselves, such as filing evidence; it stays internal and never appears in the reply.
_Avoid_: Task, action item

### The rules

**Hard Restriction**:
A binary condition stated in Fund Documentation that excludes a Client outright, e.g. investor category, US Person exclusion, minimum initial subscription, mandate-only share class, KID required for retail.
_Avoid_: Constraint, criterion

**Unmodelled Restriction**:
A restriction found in Fund Documentation that fits no known kind of Hard Restriction; it is kept with its Citation and leads to Needs Review unless the Rule Card Validator marks it Noted.
_Avoid_: Other rule, free-text rule

**Noted Restriction**:
An Unmodelled Restriction the Rule Card Validator has judged to set no condition on the client (e.g. the board's right to reject applications or close a Share Class); it is shown in the Justification with its Citation but does not affect the Verdict. Its opposite is a Blocking Restriction.
_Avoid_: Ignored rule, informational rule

**Blocking Restriction**:
An Unmodelled Restriction that sets a condition the client may not meet (e.g. "subscribing through Nordea Group"); it leads to Needs Review on every check. The default until a Rule Card Validator marks it Noted.
_Avoid_: Open rule, unresolved rule

**Fund Registration**:
The bank's record, from its fund master data or the desk's fund list, that a Share Class may be offered to a given Investor Classification in a given country. Not part of the Rule Card, and not what the Fund Documentation says about it (that is a Selling Restriction).
_Avoid_: Distribution jurisdiction, passport, country list

**Selling Restriction**:
A statement in Fund Documentation about where, or to whom, the fund may be offered, e.g. "not offered in Switzerland". It is kept with its Citation to cross-check the Fund Registration, and never decides a Verdict.
_Avoid_: Fund registration (for the document's claim), distribution restriction

**Rule Card**:
The validated, structured set of Hard Restrictions and Target Market criteria for one Fund, each rule scoped to the whole Fund or to one of its Share Classes and carrying its Citation.
_Avoid_: Rule set, fund profile, extraction

**Stale Rule Card**:
A Validated Rule Card drafted from an edition of Fund Documentation older than the one the bank's fund master data lists. It still decides Verdicts, with a warning, until it is re-drafted and validated.
_Avoid_: Outdated card, expired card

**Target Market**:
The manufacturer-defined profile of clients a Fund is designed for (client type, knowledge and experience, loss-bearing capacity, risk tolerance, objectives, horizon), as published in the EMT/KID.
_Avoid_: Target audience, investor profile

**Suitability**:
Whether an investment fits a client's overall portfolio and objectives; an advisory judgement outside the Eligibility Check.
_Avoid_: Appropriateness (a distinct, narrower MiFID test)

### The inputs

**Client Profile**:
Everything an Eligibility Check reads about one portfolio: client-level facts (residence, nationalities, US Person facts, knowledge and experience), the Investor Classification given by the portfolio's Legal Entity, and portfolio-level facts (mandate type, risk profile, ability to bear losses, horizon).
_Avoid_: Client data, customer record, KYC file

**Portfolio Reference**:
The bank's identifier of a portfolio, e.g. LOC12345678: a 3-letter legal entity code (LOC = Switzerland, LUX = Luxembourg, SIN = Singapore, …), the 6-digit client number (nocli) and the 2-digit dossier number (nodos), since one client can hold several portfolios. It identifies the client, so it is client-identifying data.
_Avoid_: Account number, PortfolioRef (in prose)

**Portfolio Id**:
The bank's opaque identifier (a UUID) for a portfolio. It maps one-to-one to a Portfolio Reference through the CRM, appears on no official document and reveals nothing about the client, so it may be shared with an LLM.
_Avoid_: Portfolio key, pseudonym

**Legal Entity**:
The bank entity that holds a portfolio, named by the 3-letter code opening its Portfolio Reference (LOC = Switzerland, LUX = Luxembourg, PAR = France, SIN = Singapore, …); it determines the regulatory regime of an Eligibility Check.
_Avoid_: Booking centre, booking entity

**Subscription Request**:
What a Fund Dealer is asked to check: a portfolio, a Share Class, an amount and currency, and whether the client initiated the order.
_Avoid_: Order, ticket, request email

**Order**:
An instruction to subscribe to a Share Class for a portfolio, created by a portfolio manager in GX (the next version of G2) and sent to Negoce for execution; no subscription is executed without one. An Eligibility Check on an Order runs on a Subscription Request derived from it, without an LLM.
_Avoid_: Trade, ticket

**Scale Mapping**:
The validated correspondence between the bank's own client scales (risk profile, knowledge and experience, …) and the Target Market scales.
_Avoid_: Conversion table, risk mapping

**Investor Classification**:
The regulatory category a Legal Entity gives a client: retail, professional or institutional/eligible counterparty. Qualified investor (CH) and well-informed investor (LU) are derived from it plus mandate and declarations, never stored as such.
_Avoid_: Client type, segment

**Fund Documentation**:
The published documents defining a Fund's restrictions and Target Market: prospectus, KID/KIID, EMT, fund factsheet.
_Avoid_: Fund docs, fund papers

**Share Class**:
A subscribable variant of a Fund with its own currency, fees, minimum subscription, and investor restrictions; the actual unit an Eligibility Check targets.
_Avoid_: Tranche

### The people

**Fund Dealer**:
A member of the fund dealing desk who receives subscription requests and must answer and justify them.
_Avoid_: Trader, operator, user

**Requester**:
The bank employee (relationship manager or portfolio manager) who asks the fund dealing desk whether one or more portfolios may subscribe to a fund, identifying them by Portfolio Reference only, never by client data.
_Avoid_: RM, PM, advisor, sender

**Rule Card Validator**:
The person who reviews a drafted Rule Card against its Citations, corrects it, marks each Unmodelled Restriction Blocking or Noted, and validates it; typically compliance or a senior Fund Dealer, distinct from the Fund Dealer who uses the card.
_Avoid_: Reviewer, approver, checker
