# Fund Registrations contract

Part of [What contracts let the app move from the external to the internal environment?](https://github.com/smnajid/fund-gate/issues/41). Why the app reads bank data only through contracts like this one: [ADR 0008](../../adr/0008-bank-data-enters-through-canonical-contracts.md).

The Rule Engine reads **Fund Registration** (see `GLOSSARY.md`) through one out port, `FundRegistrations`: whether the bank's records say a Share Class may be offered in a country, and to which investor types. Two adapters implement it:

```
Rule Engine ──► FundRegistrations (out port)
                  ├── FixtureFundRegistrations   reads fixtures/fund-registrations.json
                  └── FmdFundRegistrations       FMD registration data → Registration
```

**At the event `FmdFundRegistrations` runs inside the bank**: DEV FMD carries registration data per Share Class and country. This page is a specification; the Java interface and records are written from it before the event.

The port returns **the bank's record only**. What the Fund Documentation says about where the fund may be sold is a **Selling Restriction**. The pipeline extracts it with its Citation and shows it on the validation screen beside the bank's record, with a banner when the two disagree. It is never an input to the engine.

## The port

```
interface FundRegistrations {

  /** The bank's record for one Share Class in one country. Throws FundDataUnavailable. */
  Registration registration(Isin isin, Country country);
}

sealed interface Registration {
  record Registered(Set<InvestorType> investorTypes) implements Registration {}
  record NotRegistered() implements Registration {}
  record Unknown() implements Registration {}
}
```

- `country` is the Client Profile's `client.residenceCountry`: the country the class would be offered in. An absent residence gives Review before the port is called.
- `NotRegistered` and `Unknown` differ on purpose. "The country is missing from a list that exists" is a fact. "There is no list" is unknown.

### What the engine does with it

| Registration | Client initiated the order? | Fund Registration outcome |
|---|---|---|
| `Registered`, the client's investor type covered | either | Pass |
| `Registered`, the client's investor type not covered | no | Fail |
| `NotRegistered` | no | Fail (coverage rows 9, 15, 17) |
| `Registered` but type not covered, or `NotRegistered` | **yes** | Review: reverse solicitation, the dealer records evidence (row 16) |
| `Unknown` | either | Review |
| port throws `FundDataUnavailable` | either | no Verdict; the screen says "fund data unavailable, retry" |

"Covered" compares the client's Investor Classification, and the qualified (CH) or well-informed (LU) status the engine derives, with `investorTypes`. `all` covers everyone.

## The Registration

```json
{
  "isin": "LU2607415275",
  "registrations": [
    { "country": "LU", "investorTypes": ["well_informed"] }
  ],
  "retrievedAt": "2026-11-02T08:00:00Z"
}
```

### Fields

Columns as in the [Client Profile contract](client-profile.md#fields). Nothing here is client data, so every field may reach an LLM, but only when the call needs it, per the allow-list: a Justification built from a registration result also names the client's country. The adapter never logs the country together with a portfolio identifier. Every field is optional unless marked *required*.

| Field | Type | Meaning | Read by | LLM | Unknown |
|---|---|---|---|---|---|
| `isin` | ISIN, *required* | The Share Class | | yes | — |
| `registrations` | array | One entry per country the class is registered in. Present but without the client's country: `NotRegistered`. Absent: `Unknown` | Fund Registration | yes | Review |
| `registrations[].country` | ISO 3166-1 alpha-2, *required* | The country of distribution | Fund Registration | yes | — |
| `registrations[].investorTypes` | array of `all` \| `retail` \| `professional` \| `institutional` \| `qualified` \| `well_informed` | Who the class may be offered to in that country. A canonical enum the adapter maps (a legal category, ADR 0008). If FMD carries countries only: `["all"]`. **Ask Samuel whether FMD carries investor types** | Fund Registration | yes | `["all"]` only if FMD has no such field at all; an entry whose type code matches no value gives Review |
| `retrievedAt` | date-time, *required* | When the adapter read FMD | Check Record | no | — |

FMD may also carry a status or a date per entry (registered since, withdrawn on). Ask Samuel. If it does, a withdrawn entry counts as absent, and the date goes in the Justification.

## Fixtures

[`fixtures/fund-registrations.json`](fixtures/fund-registrations.json): the [demo dataset's Fund Registration table](../demo-dataset.md#fund-registration), one entry per Share Class. Several rows in it are *assumed*.

## Inside the bank: the FMD adapter

1. Find FMD's registration fields on one real ISIN, e.g. Nordea 1 GCE BP-EUR (`LU0348926287`): the list should include LU, CH and many EU countries.
2. Fill the mapping table below, and map FMD's investor-type codes onto the enum.
3. **Compare the fixture table with FMD for the demo funds**, before the event if DEV can be reached, or else at the event. Where they differ, FMD wins: update the demo dataset's table and the coverage set rows that depend on it, then rerun the coverage set.

| Contract field | FMD field (fill at the event) | Notes |
|---|---|---|
| `registrations[].country` | `?` | e.g. `registeredCountries`, `distributionCountries`, `countriesOfSale`, `Vertriebszulassung` |
| `registrations[].investorTypes` | `?` | ask Samuel |
| status or date per entry | `?` | ask Samuel |
