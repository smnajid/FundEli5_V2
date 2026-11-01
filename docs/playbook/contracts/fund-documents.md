# Fund Documents contract

Part of [What contracts let the app move from the external to the internal environment?](https://github.com/smnajid/fund-gate/issues/41). Why the app reads bank data only through contracts like this one: [ADR 0008](../../adr/0008-bank-data-enters-through-canonical-contracts.md).

The offline Rule Card pipeline reads **Fund Documentation** through one out port, `FundDocuments`: which documents a fund has, and their PDFs. Two adapters implement it:

```
Rule Card pipeline ──► FundDocuments (out port)
(offline CLI)            ├── FolderFundDocuments   reads a folder of PDFs and fixtures/fund-documents.json
                         └── FmdFundDocuments      FMD document metadata + PDFs
```

**At the event `FolderFundDocuments` runs inside the bank too.** DEV FMD, the only environment we can reach, serves mock PDFs, and a Rule Card drafted from a mock is meaningless. So the PDFs come from the funds' public websites or from the bank by hand, and `FmdFundDocuments` is specified here but not built. In production it replaces the folder adapter, and nothing else changes. This page is a specification; the Java interface and records are written from it before the event.

## The port

```
interface FundDocuments {

  /** Every document of the fund the Share Class belongs to: its prospectus and every KID. */
  List<FundDocument> documents(Isin anyShareClassOfTheFund);

  /** The PDF. Throws DocumentUnavailable. */
  byte[] content(DocumentId id);
}
```

### The manifest

Inside the bank, the folder adapter's manifest is **generated from DEV FMD's document metadata**, which is real (type, language, edition date, Share Classes covered). Only the PDF comes from elsewhere. That keeps each entry true to the bank's data, and narrows the production gap to fetching the PDF too. Outside the bank, the manifest is the fixture below.

### Outcomes

| Case | What the pipeline does |
|---|---|
| A document with no `contentSha256`, or bytes that don't match it | Refuses it. This guards against drafting from a mock PDF or the wrong file |
| `confidentiality` is `confidential`, or absent | Refuses it: no LLM call. The pipeline drafts only public Fund Documentation, through the one LLM egress client ([ADR 0004](../../adr/0004-two-runtimes-one-llm-egress.md)). Admitting confidential documents would need its own decision first. The spike's Claude-plan drafter is never part of the product |
| `fmdEditionDate` and `pdfEditionDate` differ | Drafts anyway. The validation screen shows "the PDF is the 2026-03 edition, FMD lists 2026-09" |
| FMD later lists a newer edition than a Validated card was drafted from | The card becomes a **Stale Rule Card**: the dealer screen and the validation screen warn "newer edition available". The Verdict does not change |
| The fund has no documents | No card. Its Share Classes give Needs Review "fund not covered" |
| `content` throws `DocumentUnavailable` | No draft for that fund this run; retry |

### What the Rule Card keeps

For each document a card was drafted from: `documentId`, `contentSha256`, both edition dates and `confidentiality`. For the drafting itself: the model id and the endpoint the egress client called. That is what makes "newer edition available" possible, and it lets an auditor fetch the exact PDF a Citation points into and see which model read it.

## The Fund Document

```json
{
  "documentId": "nordea1-prospectus-2026-06",
  "isins": ["LU0348926287", "LU0348927095"],
  "type": "prospectus",
  "language": "en",
  "fmdEditionDate": null,
  "pdfEditionDate": "2026-06",
  "confidentiality": "public",
  "contentSha256": "77cf3ae3082a99539306781c237c2bb6f138ed029e57c7c91874a781d717c8b4",
  "sourceUrl": null,
  "file": "prospectus.pdf"
}
```

### Fields

Columns as in the [Client Profile contract](client-profile.md#fields). Fund Documentation describes a fund, not a client, so no per-investor document (a side letter, a subscription form) belongs in this port. A document's content reaches an LLM only when it is `public`. Every field is optional unless marked *required*.

| Field | Type | Meaning | Read by | LLM | Unknown |
|---|---|---|---|---|---|
| `documentId` | string, *required* | FMD's document id; in the folder adapter, the manifest's own id. **Ask Samuel whether FMD has one** | the Rule Card's sources, Citations | yes | — |
| `isins` | array of ISIN, *required* | The Share Classes the document covers: every class of the fund for a prospectus, one for a KID | `documents(isin)`; the Share Class map of the draft | yes | — |
| `type` | `prospectus` \| `kid` \| `supplement` \| `fund_contract` \| `ppm` \| `other` | What the document is. `fund_contract`: a Swiss fund contract. `ppm`: a private placement memorandum, always `confidential` whatever the manifest says | page selection (an umbrella prospectus needs it); which rules a KID can source (Target Market) | yes | `other` |
| `language` | ISO 639-1 | The document's language | page selection (English, German, French) | yes | detected from the text |
| `fmdEditionDate` | date or `YYYY-MM` | The edition FMD lists. Absent outside the bank | Stale Rule Card; the edition warning | yes | no edition check |
| `pdfEditionDate` | date or `YYYY-MM` | The edition printed in the PDF, read by whoever adds it to the folder | the edition warning; the Check Record | yes | no edition check |
| `confidentiality` | `public` \| `confidential` | `public`: published by the fund on its website. `confidential`: a PPM, or anything the bank hands over under its terms | whether the pipeline may draft it | yes | treated as `confidential` |
| `contentSha256` | hex SHA-256, *required* | Hash of the PDF bytes | the guard above; the Rule Card's sources | no | — (the pipeline refuses the document) |
| `sourceUrl` | URL | Where the PDF was downloaded from, for a public document. **Ask Samuel whether FMD keeps one** | validation screen, audit | no (an internal URL could name a bank host) | blank |
| `file` | path relative to the folder | The PDF's file name. Folder adapter only | `content` | no | — |

## Fixtures

[`fixtures/fund-documents.json`](fixtures/fund-documents.json): the 9 sample documents behind the demo dataset's 4 funds, as the extraction spike downloaded them. Hashes are of those PDFs; `pdfEditionDate` is left empty where the PDF prints no edition date. The PDFs themselves stay out of git (they are in the spike's ignored `docs/` folder); snapshot them again close to the event, as the map's open item says, and update the hashes.

## Inside the bank

1. Generate the manifest from DEV FMD's document metadata for the demo funds' ISINs: fill `documentId`, `isins`, `type`, `language`, `fmdEditionDate` and, if FMD keeps it, `sourceUrl`.
2. Put the real PDF next to each entry, fill `file`, `pdfEditionDate`, `confidentiality` and `contentSha256`. Mark `public` only a PDF downloaded from the fund's public website; a file the bank hands over is `confidential` unless the bank confirms it is published.
3. Never point `file` at a PDF fetched from DEV FMD: those are mocks.
4. **Production (slide only):** `FmdFundDocuments` fetches the PDF from FMD PROD as well, and the manifest disappears.

| Contract field | FMD field (fill at the event) | Notes |
|---|---|---|
| `documentId` | `?` | ask Samuel |
| `isins` | `?` | a KID per class, the prospectus per fund |
| `type` | `?` | map to the enum; unknown codes give `other` |
| `language` | `?` | |
| `fmdEditionDate` | `?` | |
| `sourceUrl` | `?` | ask Samuel |
