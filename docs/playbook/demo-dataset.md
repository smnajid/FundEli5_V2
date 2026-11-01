# Demo dataset

Resolves [Which synthetic Client Profiles make the demo?](https://github.com/smnajid/fund-gate/issues/8).

The synthetic data the team needs for rehearsal and the demo: the inputs, not the expected answers. This is a **spec**, not fixture files. Mathilde turns these rows into fixtures before the event. The bank's data shape is only known at the event, where she and Mohamed map them onto it in about 30 minutes.

It has two tiers:
- **Demo cast**: 3 named clients the 5-minute story shows.
- **Coverage set**: every check the demo data must produce, cast included. Its expected Verdicts and per-rule outcomes, the answer key, live in the executable specification: each row as an example under the rule it proves in [`specs/01-check-eligibility.feature`](../../specs/01-check-eligibility.feature), and all 26 rows in [`specs/99-answer-key.feature`](../../specs/99-answer-key.feature). The emails live in [`specs/02-answer-a-subscription-request.feature`](../../specs/02-answer-a-subscription-request.feature).

This page keeps the conventions, the Share Classes and their short names, the bank scales, Fund Registration, FX and the Client Profiles the specification refers to.

Vocabulary: [GLOSSARY.md](../../GLOSSARY.md). Rule kinds and outcomes: [What is the Rule Card model?](https://github.com/smnajid/fund-gate/issues/6). Fund facts: [sample funds research](https://github.com/smnajid/fund-gate/blob/research/sample-funds/docs/research/sample-funds.md).

## Conventions

- **Synthetic client numbers** come from the reserved range **900001–900099**, so they can never be mistaken for a real client. Portfolio Reference = Legal Entity + client number + dossier number, e.g. `LOC90000101`.
- **Portfolio Ids** are random UUIDs, generated with the fixtures. Only Portfolio Ids reach the LLM.
- Values use the **bank scales** below. The Scale Mapping is the only thing that translates them.
- *Assumed* marks a fact we chose, not one read from a source. Replace it with the bank's data at the event.

## Share Classes used

| Short name | Fund / class | ISIN | Class currency | Regime of fund |
|---|---|---|---|---|
| GCE-BP | Nordea 1 – Global Climate and Environment, BP-EUR | `LU0348926287` | EUR | LU UCITS |
| GCE-BI | Nordea 1 – Global Climate and Environment, BI-EUR | `LU0348927095` | EUR | LU UCITS |
| SIF-W | Nordea SIF – Global Private Equity, BW-SEK | `LU2607415275` | SEK (minimums in EUR, fixed 1 EUR = 10 SEK) | LU SIF |
| BNP-A | BNP PARIBAS (CH) Bonds CHF, A | `CH0112537516` | CHF | CH |
| BNP-AAC | BNP PARIBAS (CH) Bonds CHF, A AC | `CH0114292532` | CHF | CH |
| BNP-I | BNP PARIBAS (CH) Bonds CHF, I | `CH0112537557` | CHF | CH |
| SL-ICAP | Swiss Life iFunds (CH), I Cap | *ISIN to confirm* | CHF | CH |
| SL-AMCAP | Swiss Life iFunds (CH), AM Cap | *ISIN to confirm* | CHF | CH |
| SL-IA1 | Swiss Life iFunds (CH), I-A 1 | *ISIN to confirm* | CHF | CH |

Swiss Life class↔ISIN pairs are unverified (see the research). Pick one sub-fund, e.g. Bond Swiss Francs Domestic, and confirm the ISINs before the event.

## Bank scales and Scale Mapping

The bank scales are invented and deliberately differ from the Target Market scales, so the Scale Mapping does real work. At the event the bank's real scales replace this one table.

| Bank field | Bank value | Target Market value |
|---|---|---|
| Risk profile | 1 Conservative | max SRI 2 |
| | 2 Income | max SRI 3 |
| | 3 Balanced | max SRI 4 |
| | 4 Growth | max SRI 5 |
| | 5 Dynamic | max SRI 7 |
| Knowledge & experience (per product type) | none | below basic |
| | basic | basic |
| | informed | informed |
| | expert | advanced |
| Ability to bear losses | no loss | no capital loss |
| | limited loss | limited capital loss |
| | full loss | can bear loss of entire capital |
| Investment horizon | years | compared with the KID's recommended holding period (pass if ≥ RHP) |

## Fund Registration

| Share Class | Country | Investor Classification | Registered | Source |
|---|---|---|---|---|
| GCE-BP, GCE-BI | CH | all | yes | *assumed* (Swiss representative named, prospectus p.146) |
| GCE-BP, GCE-BI | LU | all | yes | home jurisdiction |
| SIF-W | LU | well-informed | yes | home jurisdiction |
| SIF-W | CH | any | **no** | no Swiss representative named |
| BNP-A, BNP-AAC, BNP-I | CH | all (I: qualified only) | yes | home jurisdiction |
| BNP-A, BNP-AAC, BNP-I | LU | any | **no** | "no issue or redemption abroad" (prospectus p.16) |
| SL-* | CH | qualified | yes | "distribution approved: Switzerland" (appendix p.45) |
| SL-* | LU | any | **no** | same |

Fixture: [`contracts/fixtures/fund-registrations.json`](contracts/fixtures/fund-registrations.json). DEV FMD carries registration data, so at the event the engine reads FMD instead ([contract](contracts/fund-registrations.md)). Compare this table with FMD before the event if DEV can be reached, otherwise at the event; where they differ, FMD wins and the answer-key rows in `specs/` that depend on it change.

## FX

| Pair | Rate | Date | Source |
|---|---|---|---|
| EUR/CHF | 1 EUR = 0.9350 CHF | 2026-11-02 | *assumed*, "bank FX table" |

The planned SEK rate is not needed. SIF minimums are stated in EUR, and the prospectus fixes 1 EUR = 10 SEK for converting them.

## Client Profiles

Fields follow the Rule Card model: client (client number) / client × Legal Entity / portfolio (dossier).

### Client level

| Client | Name | Residence | Nationalities | Tax residence | Place of birth | US Person / FATCA | Knowledge & experience |
|---|---|---|---|---|---|---|---|
| 900001 | Anna Steinmann | CH | CH | CH | Zürich | not a US Person / non-US | funds: informed |
| 900002 | Tom Whitfield | CH | CH, US | CH, US | Boston, US | US citizen / specified US person | funds: expert |
| 900003 | Claire Dupont | LU | FR | LU | Lyon, FR | not a US Person / non-US | funds: basic |
| 900004 | (coverage) | LU | LU | LU | Luxembourg | not a US Person | funds: expert, private equity: expert |
| 900005 | (coverage) | LU | BE | LU | Liège, BE | not a US Person | funds: informed, private equity: informed |
| 900006 | (coverage) | LU | DE | LU | Trier, DE | not a US Person | funds: informed, private equity: **none** |
| 900007 | (coverage) | CH | CH | CH | Bern | not a US Person | funds: expert |
| 900008 | (coverage) | LU | LU | LU | Luxembourg | not a US Person | funds: basic |
| 900009 | (coverage) | CH | CH | CH | Geneva | not a US Person | funds: expert |
| 900010 | (coverage) | CH | CH | CH | Basel | not a US Person | funds: basic |
| 900011 | (coverage) | CH | CH | CH | Lausanne | not a US Person | funds: informed |
| 900012 | (coverage) | CH | CH | CH | Lugano | **missing** | funds: basic |
| 900013 | (coverage) | SG | SG | SG | Singapore | not a US Person | funds: informed |
| 900014 | (coverage) | CH | CH | CH | Zürich | not a US Person | funds: basic |

### Client × Legal Entity

| Client | Legal Entity | Classification | Declarations |
|---|---|---|---|
| 900001 | LOC | FinSA retail | none |
| 900002 | LOC | FinSA professional | HNWI opt-out (FinSA art. 5(1)), 2024-05-14 |
| 900003 | LUX | MiFID retail | none |
| 900004 | LUX | MiFID professional (per se) | none |
| 900005 | LUX | MiFID retail | well-informed written confirmation, 2026-09-15 |
| 900006 | LUX | MiFID retail | well-informed written confirmation, 2026-09-20 |
| 900007 | LOC | FinSA professional | HNWI opt-out, 2023-02-01 |
| 900008 | LUX | MiFID retail | none |
| 900009 | LOC | FinSA professional | HNWI opt-out, 2022-11-10 |
| 900010 | LOC | FinSA retail | none |
| 900011 | LOC | FinSA retail | none |
| 900012 | LOC | FinSA retail | none |
| 900013 | SIN | (not covered) | none |
| 900014 | LOC | FinSA retail | none |

### Portfolio level

| Portfolio Reference | Mandate type (start) | Risk profile | Ability to bear losses | Horizon | Ref. currency |
|---|---|---|---|---|---|
| `LOC90000101` | advisory | 3 Balanced | full loss | 7y | CHF |
| `LOC90000102` | discretionary mandate (2021-03-01) | 4 Growth | full loss | 10y | EUR |
| `LOC90000201` | advisory | 4 Growth | full loss | 10y | CHF |
| `LUX90000301` | advisory | 2 Income | limited loss | 2y | EUR |
| `LUX90000401` | advisory | 5 Dynamic | full loss | 12y | EUR |
| `LUX90000501` | advisory | 2 Income | full loss | 5y | EUR |
| `LUX90000601` | advisory | 5 Dynamic | full loss | 12y | EUR |
| `LOC90000701` | advisory | 5 Dynamic | full loss | 12y | CHF |
| `LUX90000801` | advisory | 2 Income | limited loss | 5y | EUR |
| `LOC90000901` | advisory | 3 Balanced | full loss | 5y | CHF |
| `LOC90001001` | discretionary mandate (2019-06-01), not with Swiss Life | 3 Balanced | full loss | 7y | CHF |
| `LOC90001101` | discretionary mandate (2020-01-15) | 3 Balanced | full loss | 7y | CHF |
| `LOC90001201` | advisory | 2 Income | limited loss | 5y | CHF |
| `SIN90001301` | advisory | 3 Balanced | full loss | 5y | USD |
| `LOC90001401` | advisory | 2 Income | limited loss | 5y | CHF |

## Demo cast

The emails and the expected behaviour are scenarios in [`specs/02-answer-a-subscription-request.feature`](../../specs/02-answer-a-subscription-request.feature) and [`specs/04-find-what-a-portfolio-can-buy.feature`](../../specs/04-find-what-a-portfolio-can-buy.feature). Row numbers refer to the [answer key](../../specs/99-answer-key.feature).

- **Anna Steinmann, the hero: same client, same fund, two portfolios.** Marc Aebi's email lists `LOC90000101` and `LOC90000102` and no client data. One check per reference gives Not Eligible and Eligible on GCE-BI (rows 1 and 2), and the reply suggests GCE-BP for portfolio 01 (row 3).
- **Tom Whitfield, the closing flourish: one client, three US Person definitions.** No email: the dealer runs "what can this portfolio buy?" on `LOC90000201` with CHF 2,000,000, deterministic and with no LLM. The same US citizen passes Nordea's Reg S test and fails BNP's and Swiss Life's (rows 4–9).
- **Claire Dupont: the Target Market binds in Luxembourg.** Sophie Lentz's email is in French and names the fund but no Share Class, so every class of the fund is checked (rows 10 and 11). The reply, in French, recommends BP-EUR.

Two more emails exercise the edges: the **guard email** names Anna, so the Privacy Gate makes no LLM call and the dealer enters the request by hand; the **amount-to-follow email** gives row 26.

The emails are drafts. Revise them once [How does the fund dealing desk answer subscription requests today?](https://github.com/smnajid/fund-gate/issues/14) brings real samples.

## At the event

1. Map the canonical fields above onto the bank's export, and swap the Scale Mapping for the bank's scales.
2. Generate fixtures in the bank's shape, with fresh Portfolio Ids.
3. Compare the Fund Registration table with DEV FMD and update the answer-key rows in `specs/` where they differ; replace the FX table with the bank's if it provides one.
4. Run the answer key (`@answer-key`); every row must pass before the demo is rehearsed.
