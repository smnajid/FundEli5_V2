# Share Classes contract

Part of [What contracts let the app move from the external to the internal environment?](https://github.com/smnajid/fund-gate/issues/41). Why the app reads bank data only through contracts like this one: [ADR 0008](../../adr/0008-bank-data-enters-through-canonical-contracts.md).

The online runtime reads the **Share Class catalog** through one out port, `ShareClasses`. The catalog feeds the matcher (an email's fund mention or an ISIN → Share Classes), the dealer form's dropdown and the reverse lookup. Two adapters implement it:

```
Share Class catalog ──► ShareClasses (out port)
(matcher, reverse lookup)  ├── FixtureShareClasses   reads fixtures/share-classes.json
                           └── FmdShareClasses       FMD instrument data → Share Classes
```

**At the event `FmdShareClasses` runs inside the bank**: DEV FMD's instrument data is real, so real ISINs resolve. This page is a specification; the Java interface and records are written from it before the event.

## The port

```
interface ShareClasses {

  /** Every Share Class the bank distributes. Throws FundDataUnavailable. */
  List<ShareClass> all();
}
```

- One operation. The catalog loads it at startup, keeps it in memory, and reloads it on demand (an admin action, not a schedule).
- There is no lookup by ISIN. An ISIN outside `all()` is a fund the bank does not distribute, and gets the same answer as any other uncovered fund.

### What is in the catalog

Only the Share Classes **the bank distributes**, not FMD's whole universe. Otherwise Tom's reverse lookup lists thousands of "Fund not covered" rows. The bank adapter applies the filter: FMD's distribution or approved-list flag if it has one, or else a list of ISINs from the desk, each fetched from FMD. Nothing above the port knows which.

The fixture adapter serves the 9 Share Classes of the [demo dataset](../demo-dataset.md#share-classes-used) and 2 classes with no Rule Card, so the reverse lookup shows "Fund not covered".

### Outcomes

| Case | Port | What the app does |
|---|---|---|
| ISIN or fund mention not in the catalog | not in `all()` | Needs Review "fund not covered", with no LLM call for that class |
| Share Class in the catalog, no Rule Card | in `all()` | Needs Review "fund not covered" ([screens](../screens.md#reverse-lookup-what-can-this-portfolio-buy)) |
| Share Class in the catalog, Draft card only | in `all()` | Needs Review "Rule Card not validated" (coverage row 25) |
| An FMD instrument with no ISIN | left out by the adapter, logged | not offered |
| FMD down at startup | `all()` throws `FundDataUnavailable` | The online runtime does not start. An empty catalog would turn every check into "fund not covered" |
| FMD down on reload | `all()` throws `FundDataUnavailable` | Keep the loaded catalog; the screen shows when it was loaded |

## The Share Class

```json
{
  "isin": "LU0348927095",
  "fundId": null,
  "fundName": "Nordea 1 – Global Climate and Environment Fund",
  "umbrellaName": "Nordea 1, SICAV",
  "shareClassName": "BI-EUR",
  "currency": "EUR",
  "domicile": "LU",
  "fundType": "ucits",
  "productType": { "code": "FUNDS", "label": "Investment funds" },
  "valor": null,
  "internalCode": null,
  "retrievedAt": "2026-11-02T08:00:00Z"
}
```

### Fields

Columns as in the [Client Profile contract](client-profile.md#fields). Nothing here is client data, so every field may reach an LLM when the call needs it, per the allow-list: the Request Parser needs the names to match a mention. Every field is optional unless marked *required*.

| Field | Type | Meaning | Read by | LLM | Unknown |
|---|---|---|---|---|---|
| `isin` | ISIN, `^[A-Z]{2}[A-Z0-9]{9}\d$`, *required* | The Share Class's key | everything: the Subscription Request, the Rule Card, Fund Registration, the Check Record | yes | — (no ISIN: not in the catalog) |
| `fundId` | string | FMD's id of the fund (sub-fund) the class belongs to, shared by its sister classes. **Unconfirmed: ask Samuel whether FMD has one** | the Rule Card's key (see below); fans a fund mention out to its classes | yes | the Rule Card's ISIN list joins instead |
| `fundName` | string, *required* | The fund (sub-fund) name as FMD holds it | matcher, screens, reply | yes | — |
| `umbrellaName` | string | The umbrella (SICAV, FCP or Swiss umbrella fund), when the fund is a sub-fund | matcher, screens | yes | shown as blank |
| `shareClassName` | string, *required* | The class label, e.g. `BI-EUR`, `I Cap` | matcher, screens, reply | yes | — |
| `currency` | ISO 4217, *required* | The class currency | minimum initial subscription (FX from the amount's currency) | yes | — |
| `domicile` | ISO 3166-1 alpha-2 | Where the fund is domiciled | screens | yes | shown as blank |
| `fundType` | `ucits` \| `sif` \| `raif` \| `part_ii` \| `ch_securities_fund` \| `ch_other_fund` \| `ch_qualified_investor_fund` \| `other` | The fund's legal form, a canonical enum the adapter maps (a legal category, ADR 0008) | screens ("fund regime"), Justification | yes | shown as blank |
| `productType` | bank code | The bank's product type for the class, the same scale as the Client Profile's `knowledgeAndExperience[].productType`, e.g. `FUNDS`, `PRIVATE_EQUITY` | Target Market (knowledge and experience): picks the client's entry for this product type | yes | Target Market Review |
| `valor` | string | The Swiss Valor number | matcher, screens | yes | not searchable by Valor |
| `internalCode` | string | The bank's own fund code | matcher, screens | yes | not searchable by code |
| `retrievedAt` | date-time, *required* | When the adapter read FMD | Check Record | no | — |

### Joining a Share Class to its Rule Card

A Rule Card covers one fund and lists its Share Classes. A check finds the card through the class's ISIN:

- **If FMD has a fund id**, the card is stored under it, and every ISIN with that `fundId` finds it.
- **If not**, the card's own ISIN list is the join, as the [extraction spike](https://github.com/smnajid/fund-gate/tree/spike/restriction-routing/spikes/java-rule-card-extraction)'s `RuleCard.shareClasses` already does. A fund mention fans out to the classes of the card that names it.

Either way, a class whose ISIN no card lists has no card.

## Fixtures

[`fixtures/share-classes.json`](fixtures/share-classes.json): the demo dataset's 9 classes and 2 card-less ones. The Swiss Life ISINs are unconfirmed (see the demo dataset), so the fixture uses placeholders starting `XX`, which no real ISIN uses; the 2 card-less classes do too. Replace them with real ISINs from DEV FMD at the event.

## Inside the bank: the FMD adapter

1. Find the FMD call that returns instruments by ISIN, and the one that lists what the bank distributes (or get the desk's ISIN list).
2. Fill the mapping table below. Map FMD's legal-form codes onto `fundType`; a code that matches none gives `other`, logged.
3. `productType` must use the same codes as the CRM's knowledge-and-experience product types, or the Scale Mapping gets a row that maps one to the other.
4. Check that the demo dataset's ISINs come back with the expected names and currencies.

| Contract field | FMD field (fill at the event) | Notes |
|---|---|---|
| `isin` | `?` | |
| `fundId` | `?` | ask Samuel: does it exist? |
| `fundName`, `umbrellaName`, `shareClassName` | `?` | |
| `currency` | `?` | |
| `domicile`, `fundType` | `?` | map to the enum |
| `productType` | `?` | same scale as the CRM's |
| `valor`, `internalCode` | `?` | |
| distribution filter | `?` | a flag, or the desk's list |
