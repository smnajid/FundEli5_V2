# Client Profile API: what to look for in the bank's catalogue

**Found (2026-10-18):** the bank's CRM is a GraphQL API with a `portfolio` query and a `person` query, both taking the Portfolio Reference; the Portfolio Id comes back with the other portfolio fields. The app's side is fixed in the [Client Profile contract](contracts/client-profile.md); this page stays as the checklist for finding each field in the CRM.

The Eligibility Check reads one **Client Profile** per portfolio (see `GLOSSARY.md` and [demo-dataset.md](demo-dataset.md)). This page describes the API (or APIs) we need from the bank, so we can find it in their catalogue. In most banks the data is spread over **2–4 APIs**: look for each block separately rather than for one "client profile" endpoint.

## What we're looking for

An API that takes a **portfolio identifier** (Portfolio Reference, e.g. `LOC12345678`: Legal Entity code + 6-digit client number + 2-digit dossier number) or the client number, and returns the facts below. Read-only, one portfolio at a time.

## The fields, by level

### 1. Client level

The person. Usually in client master data / KYC.

| Field | Used by | Example |
|---|---|---|
| Residence country | Fund Registration, US Person | CH |
| Nationalities (all of them) | US Person exclusion | CH, US |
| Tax residences | US Person / FATCA | CH, US |
| Place of birth (country is enough) | US Person ("US-born") | US |
| US Person status / FATCA classification | US Person exclusion | specified US person |
| Knowledge & experience per product type | Target Market | funds: informed; private equity: none |

### 2. Client × Legal Entity

The classification a Legal Entity gives the client. Usually in the MiFID/FinSA classification or compliance system.

| Field | Used by | Example |
|---|---|---|
| Investor Classification | investor category rule | FinSA retail / professional / institutional |
| Opting declarations, with date | qualified investor (CH) / well-informed investor (LU) | HNWI opt-out (FinSA art. 5(1)), 2024-05-14; well-informed written confirmation |

### 3. Portfolio level

The dossier. Usually in the portfolio / mandate system or the suitability profile.

| Field | Used by | Example |
|---|---|---|
| Mandate type, with start date | mandate-only classes, qualified investor (CH) | advisory / discretionary mandate since 2021-03-01 |
| Risk profile | Target Market | 3 Balanced |
| Ability to bear losses | Target Market | full loss / limited loss |
| Investment horizon | Target Market | 7 years |
| Reference currency | minimum subscription (FX) | CHF |

### CRM mapping

Portfolio Reference ↔ Portfolio Id (an opaque UUID), one to one. The Privacy Gate needs it so the LLM never sees the Portfolio Reference.

## Keywords to search the catalogue

- **Client data:** client master data, party, KYC, client onboarding, customer profile
- **Classification:** MiFID classification, FinSA / LSFin classification, client categorisation, investor type, opting-out, qualified investor
- **US Person:** FATCA, CRS, tax status, US indicia, W-9 / W-8BEN
- **Portfolio:** portfolio, dossier, mandate, contract, management type, investment profile
- **Target Market:** suitability profile, risk profile, investor profile, appropriateness, knowledge and experience, loss capacity, investment horizon

## What we don't need (and shouldn't pull)

Name, address, phone, email, exact birth date, balances and positions. None of them is used by a rule, and asking only for the fields above makes the data-access request easier to approve ([ADR 0004](../adr/0004-two-runtimes-one-llm-egress.md)). If an API returns them anyway, the adapter drops them at the boundary.

## Questions for the API owner

1. Is the input the Portfolio Reference, the client number, or an internal party id? Which API maps between them?
2. Is the classification returned **per Legal Entity** (LOC vs LUX), or one value per client?
3. Are knowledge & experience given per product type (funds, private equity, structured products)? On what scale?
4. Are risk profile and horizon held per portfolio or per client?
5. How fresh is the data: real time or a nightly copy? Is there a test environment with anonymised data for the hackathon?
6. Does the Investor Classification come from the `portfolio` query or the `person` query? Is there one entry per Legal Entity?
