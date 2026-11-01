# Client Profile contract

Part of [What contracts let the app move from the external to the internal environment?](https://github.com/smnajid/fund-gate/issues/41). Why the app reads bank data only through contracts like this one: [ADR 0008](../../adr/0008-bank-data-enters-through-canonical-contracts.md).

The app reads a **Client Profile** (see `GLOSSARY.md`) through one out port, `ClientProfiles`. Two adapters implement it, chosen per port ([ADR 0008](../../adr/0008-bank-data-enters-through-canonical-contracts.md)). **At the event the fixture adapter runs inside the bank too**: DEV's CRM holds mock clients, and the fixtures are the coverage set's answer key. The CRM adapter below is the production path.

```
Privacy Gate ──┐
Rule Engine  ──┴─► ClientProfiles (out port)
                     ├── FixtureClientProfiles   external environment: reads fixtures/client-profiles/*.json
                     └── CrmClientProfiles       internal environment: CRM GraphQL portfolio + person → Client Profile
```

Nothing above the port knows which adapter runs or that the CRM speaks GraphQL. This page is a specification: the Java interface and records are written from it before the event.

## The port

```
interface ClientProfiles {

  /** For the Privacy Gate. Empty when the CRM does not know the reference. */
  Optional<PortfolioId> portfolioId(PortfolioReference ref);

  /** For the Rule Engine. Throws PortfolioNotFound or ClientDataUnavailable. */
  ClientProfile clientProfile(PortfolioReference ref);
}
```

- `PortfolioReference`: the bank's identifier, `^[A-Z]{3}\d{8}$` (Legal Entity code, 6-digit client number, 2-digit dossier number). Client-identifying: never sent to an LLM.
- `PortfolioId`: a UUID, one to one with the Portfolio Reference. May be sent to an LLM.
- `portfolioId` exists so the Privacy Gate never loads client facts it does not need. The CRM adapter selects only that field.

### Outcomes

| Case | Port | What the app does |
|---|---|---|
| Unknown Portfolio Reference | `portfolioId` empty; `clientProfile` throws `PortfolioNotFound` | Privacy Gate: no LLM call, the dealer enters the request by hand. Check: Needs Review "portfolio not found" |
| Portfolio found, some facts missing | `ClientProfile` with those fields absent | Each rule that reads a missing field gives Review; other rules run normally |
| No classification for the portfolio's Legal Entity | `classification` absent | Needs Review "no Investor Classification" |
| Bank system down or timing out | `clientProfile` throws `ClientDataUnavailable` | No Verdict; the screen says "client data unavailable, retry". Never a Verdict on partial data |

A profile with gaps is a normal answer, not an error. **The adapter never fills a default**: absent means unknown, and unknown leads to Review, never to a guess.

## The Client Profile

Three blocks, one per level of the glossary's Client Profile: the person (`client`), the classification the portfolio's Legal Entity gives them (`classification`), and the dossier (`portfolio`).

```json
{
  "portfolioRef": "LOC90000201",
  "portfolioId": "4b0f6f3e-2f7a-4c55-9a59-0c8f1d5b7e21",
  "legalEntity": "LOC",
  "retrievedAt": "2026-11-02T08:00:00Z",
  "client": {
    "residenceCountry": "CH",
    "nationalities": ["CH", "US"],
    "taxResidences": ["CH", "US"],
    "birthCountry": "US",
    "usPerson": { "fatcaStatus": "us_person", "usPermanentResident": false },
    "knowledgeAndExperience": [
      { "productType": { "code": "FUNDS", "label": "Investment funds" }, "level": { "code": "EXPERT", "label": "Expert" } }
    ]
  },
  "classification": {
    "regime": "finsa",
    "category": "professional",
    "classifiedOn": "2024-05-14",
    "declarations": [
      { "type": "finsa_hnwi_opt_out", "date": "2024-05-14", "label": "HNWI opt-out, FinSA art. 5(1)" }
    ]
  },
  "portfolio": {
    "referenceCurrency": "CHF",
    "mandate": { "type": "advisory" },
    "investmentProfile": {
      "riskProfile": { "code": "4", "label": "Growth" },
      "abilityToBearLosses": { "code": "FULL_LOSS", "label": "Full loss" },
      "horizonYears": 10,
      "assessedOn": "2026-03-10"
    }
  }
}
```

### Two kinds of values

- **Bank codes**, `{ "code": "...", "label": "..." }`: the bank's own scales, passed through unchanged. The validated **Scale Mapping** translates them to Target Market values; no code does. Inside the bank only the Scale Mapping table changes.
- **Canonical enums** (lower snake case): legal categories that are closed lists set by law. The CRM adapter maps the bank's codes onto them. Rules branch on them directly.

### Fields

Columns: **LLM** is whether the field may reach an LLM under the [allow-list](../architecture.md#privacy-boundary) (only the fields a call needs, through the Privacy Gate). **Unknown** is what an absent value leads to. Every field is optional unless marked *required*.

#### Top level

| Field | Type | Meaning | Read by | LLM | Unknown |
|---|---|---|---|---|---|
| `portfolioRef` | string, `^[A-Z]{3}\d{8}$`, *required* | The Portfolio Reference asked for, echoed back | Check Record, reply | **no** | — |
| `portfolioId` | UUID, *required* | The portfolio's Portfolio Id | Privacy Gate | yes | — |
| `legalEntity` | string, `^[A-Z]{3}$`, *required* | The Legal Entity holding the portfolio: the first 3 letters of the Portfolio Reference. Sets the regime: `LOC` → CH, `LUX` → LU, anything else → not covered | every rule (regime) | yes | — (other codes give Needs Review "regime not covered") |
| `retrievedAt` | date-time, *required* | When the adapter read the bank's systems (for fixtures: when they were written) | Check Record | no | — |
| `client` | object | Facts about the person; see below | | | the block's fields are all unknown |
| `classification` | object | The Investor Classification the portfolio's Legal Entity gives the client; see below | | | Needs Review "no Investor Classification" |
| `portfolio` | object | Facts about the dossier; see below | | | the block's fields are all unknown |

#### `client`: the person

| Field | Type | Meaning | Read by | LLM | Unknown |
|---|---|---|---|---|---|
| `residenceCountry` | ISO 3166-1 alpha-2 | Country of legal residence | Fund Registration (country of distribution); US Person under residence-based definitions (Reg S) | yes | Fund Registration: Review. US Person: Review when the card's definition looks at residence |
| `nationalities` | array of ISO 3166-1 alpha-2, every nationality held | Citizenships. `["CH","US"]` is a US citizen even if resident in Switzerland | US Person under citizenship-based definitions ("investors with U.S. citizenship", US tax code) | yes | US Person: Review when the card's definition looks at citizenship |
| `taxResidences` | array of ISO 3166-1 alpha-2 | Countries where the client is tax resident | US Person (tax-code definitions); shown in the Justification | yes | US Person: Review when the card's definition looks at tax residence |
| `birthCountry` | ISO 3166-1 alpha-2 | Country of birth only, never city or date | US Person: a US birthplace is a US indication | as "US-born yes/no" only | US Person: Review when needed and nothing else settles it |
| `usPerson.fatcaStatus` | `us_person` \| `non_us_person` | The bank's own FATCA determination for the client | US Person: shown in the Justification next to the card's own definition; never replaces it | yes | Review on every US Person rule ([answer-key row 23](../../../specs/99-answer-key.feature)) |
| `usPerson.usPermanentResident` | boolean | Holds a US green card | US Person (tax-code definitions count green card holders) | yes | taken from `fatcaStatus`: `non_us_person` means no green card; otherwise Review |
| `knowledgeAndExperience` | array of `{ productType, level }` | Knowledge and experience per product type, as the bank assesses it | Target Market (knowledge and experience), for the product type the Share Class catalog gives the Share Class | yes | no entry for the Share Class's product type: Target Market Review |
| `knowledgeAndExperience[].productType` | bank code | The bank's product type, e.g. `FUNDS`, `PRIVATE_EQUITY` | Share Class catalog → which entry applies | yes | entry ignored |
| `knowledgeAndExperience[].level` | bank code | The bank's level for that product type, e.g. `NONE`, `BASIC`, `INFORMED`, `EXPERT`. Translated by the Scale Mapping | Target Market | yes | entry ignored |

The engine derives each card's US Person answer from these raw facts: one flag cannot answer Reg S (residence), "US citizens" and the US tax code at once. Tom Whitfield is not a US Person under Reg S but is excluded as a US citizen elsewhere.

#### `classification`: given by the portfolio's Legal Entity

Only the portfolio's own Legal Entity: a client served by LOC and LUX has two classifications, and a `LUX` portfolio gets only the LUX one.

| Field | Type | Meaning | Read by | LLM | Unknown |
|---|---|---|---|---|---|
| `regime` | `finsa` \| `mifid` | The law under which the classification was made: FinSA for LOC, MiFID II for LUX | consistency check with `legalEntity` | yes | taken from `legalEntity` |
| `category` | `retail` \| `professional` \| `institutional` | The Investor Classification. `institutional` covers eligible counterparties (MiFID) and institutional clients (FinSA) | investor category; KID required for retail; Fund Registration (classification); deriving qualified (CH) and well-informed (LU) | yes | Needs Review "no Investor Classification" |
| `classifiedOn` | date | When the category was last set | Justification, Check Record | no | — |
| `declarations` | array | Opting declarations on file for this Legal Entity. Empty array: none on file. Absent: unknown | deriving qualified investor (CH) and well-informed investor (LU) | type only | rules that need a declaration: Review |
| `declarations[].type` | `finsa_hnwi_opt_out` \| `finsa_opt_in_retail` \| `mifid_professional_on_request` \| `lu_well_informed_confirmation` \| `other` | `finsa_hnwi_opt_out`: a high-net-worth retail client declared to be treated as professional (FinSA art. 5(1)). `finsa_opt_in_retail`: a professional asked to be treated as retail (FinSA art. 5(5)). `mifid_professional_on_request`: a retail client treated as professional on request (MiFID II Annex II.II). `lu_well_informed_confirmation`: written confirmation of well-informed investor status (LU SIF/RAIF laws). `other`: anything else, kept with its `label` | qualified / well-informed derivation; `other` gives Review where a declaration matters | yes | — |
| `declarations[].date` | date | When the declaration was signed | Justification | no | the declaration still counts; shown as undated |
| `declarations[].label` | string | The bank's own wording, for the dealer | dealer screen | no | — |

Qualified investor (CH) and well-informed investor (LU) are never fields: the engine derives them from `category`, `declarations`, `mandate` and the amount, as the glossary says.

#### `portfolio`: the dossier

| Field | Type | Meaning | Read by | LLM | Unknown |
|---|---|---|---|---|---|
| `referenceCurrency` | ISO 4217 | The portfolio's reference currency | not read by any rule; shown to the dealer. The Subscription Request carries its own amount and currency | no | — |
| `mandate.type` | `advisory` \| `discretionary` \| `execution_only` | How the portfolio is managed | mandate-only class; investor category (a discretionary mandate counts as institutional under art. 174 of the 2010 Law, coverage row 2); qualified investor through a mandate (CISA art. 10(3ter)) | yes | Review on those rules |
| `mandate.startDate` | date | When the mandate started | Justification | no | — |
| `mandate.manager.kind` | `bank` \| `external_supervised` \| `external_other` | Who manages a discretionary mandate: the bank itself, an external manager supervised by a regulator, or another external manager | CISA art. 10(3ter) (supervised manager); mandate-only classes | yes | Review on those rules |
| `mandate.manager.name` | string | The manager's name when external, e.g. a fund house for a class reserved to its own mandates | mandate-only class naming a manager (Swiss Life AM class, coverage row 18) | no | Review when the card names a manager |
| `investmentProfile.riskProfile` | bank code | The bank's risk profile, e.g. `3` "Balanced". Translated to a maximum SRI by the Scale Mapping | Target Market (risk tolerance) | yes | Target Market Review |
| `investmentProfile.abilityToBearLosses` | bank code | e.g. `NO_LOSS`, `LIMITED_LOSS`, `FULL_LOSS`. Translated by the Scale Mapping | Target Market (ability to bear losses) | yes | Target Market Review |
| `investmentProfile.horizonYears` | integer ≥ 0 | Investment horizon of the portfolio in years | Target Market: Pass if ≥ the KID's recommended holding period | yes | Target Market Review |
| `investmentProfile.assessedOn` | date | When the investment profile was last assessed | Justification; an Advisory Suggestion may ask whether it is current | no | — |

`mandate` absent: unknown. A portfolio without a mandate is `{ "type": "advisory" }` or `{ "type": "execution_only" }`, never an absent block.

**Not in the contract**, because no rule reads them: name, address, phone, email, exact birth date, place of birth below country, balances and positions, and any person identifier. The CRM adapter must not put them in the profile, so they never reach the Check Record snapshot or an LLM.

## Fixtures

[`fixtures/client-profiles/`](fixtures/client-profiles/): the 15 portfolios of [demo-dataset.md](../demo-dataset.md), one file per Portfolio Reference, in the invented bank scales of that page. The fixture adapter reads them as they are. Portfolio Ids are fixed random UUIDs; regenerate them at the event if the bank wants fresh ones.

## Inside the bank: the CRM adapter

The CRM is a GraphQL API with a `portfolio` query and a `person` query, both taking the Portfolio Reference. Only this adapter knows that. At the event:

1. Read the CRM schema and fill the mapping table below.
2. `portfolioId(ref)`: `portfolio(ref)` selecting only the Portfolio Id.
3. `clientProfile(ref)`: `portfolio(ref)` and `person(ref)`, mapped onto the Client Profile. Select only the mapped fields, so nothing outside the contract is fetched.
4. Map the bank's legal-category codes onto the canonical enums. Codes that match none: `other` for declarations; for `category` and `mandate.type`, leave the field absent (unknown) and log the code.
5. Replace the Scale Mapping's bank side with the bank's real scales (risk profile, knowledge and experience levels and product types, ability to bear losses). This is a validated table, not adapter code.
6. Run the coverage set against portfolios the desk picks.

Expected query shape, to adjust to the real schema:

```graphql
query ClientProfile($ref: String!) {
  portfolio(ref: $ref) { id legalEntity referenceCurrency mandate { type startDate manager { kind name } }
                         riskProfile { code label } lossCapacity { code label } horizon assessedOn }
  person(ref: $ref)    { residence nationalities taxResidences birthCountry fatcaStatus greenCard
                         knowledge { productType { code label } level { code label } } }
}
```

| Contract field | CRM field (fill at the event) | Notes |
|---|---|---|
| `portfolioId` | `portfolio.?` | |
| `client.residenceCountry` | `person.?` | ISO alpha-2 |
| `client.nationalities` | `person.?` | all of them |
| `client.taxResidences` | `person.?` | |
| `client.birthCountry` | `person.?` | country only |
| `client.usPerson.fatcaStatus` | `person.?` | map to `us_person` / `non_us_person` |
| `client.usPerson.usPermanentResident` | `person.?` | |
| `client.knowledgeAndExperience` | `person.?` | per product type |
| `classification.*` | `portfolio.?` or `person.?` | unknown which query holds it; one entry per Legal Entity, keep the portfolio's |
| `portfolio.referenceCurrency` | `portfolio.?` | |
| `portfolio.mandate.*` | `portfolio.?` | |
| `portfolio.investmentProfile.*` | `portfolio.?` or `person.?` | risk profile may be held per client |
