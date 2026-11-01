# Sample Fund Documentation for the demo

Resolves [#3](https://github.com/smnajid/fund-gate/issues/3): *Which public funds should be our sample Fund Documentation?*

Researched 2026-10-03. Each fact below was read directly from the fund's own document (PDF text extraction). Page numbers are the PDF page numbers, which match the printed page numbers in every document used here. PDFs are **not** committed; links only.

## Recommendation (TL;DR)

Use these four funds. Between them they exercise every Hard Restriction type in [GLOSSARY.md](../../GLOSSARY.md) and give both positive and negative Target Market outcomes.

| # | Fund (Share Classes used) | Regime | Why it is in the set |
|---|---|---|---|
| 1 | **Nordea 1 – Global Climate and Environment Fund** (BP-EUR retail, BI-EUR institutional) | LU UCITS SICAV | Retail and institutional class of the same fund: EUR 0 vs EUR 75,000 minimum, "all investors" vs "institutional investors". Reg S-style US Person exclusion. Swiss representative named, so CH distribution is possible. PRIIPs KID with Target Market text. |
| 2 | **Nordea Specialised Investment Fund, SICAV-FIS – Global Private Equity Fund** (BW-SEK, plus the class table) | LU SIF (AIF) | Well-informed investors only (SIF Law art. 2). Minimums of EUR 10k, 100k, 1m and 5m depending on class/channel. SRI 6, 10-year RHP: gives a strong **negative** Target Market case. KID present, no factsheet. |
| 3 | **BNP PARIBAS (CH) Fund – Bonds CHF** (A retail, I qualified-only) | CH contractual fund (FinSA/CISA) | Same fund has a retail class (no minimum) and a **qualified-investor-only** class (CHF 1m minimum). US Person exclusion names US citizenship explicitly. "No issue or redemption abroad" (a distribution-jurisdiction rule). Swiss KID per class with different "Kleinanleger-Zielgruppe" text. |
| 4 | **Swiss Life iFunds (CH)** umbrella (I Cap / I Dis vs AM / M / I-A classes) | CH fund for qualified investors only | Whole fund is limited to qualified investors. Class rules depend on the *kind* of qualified investor: HNWI opt-outs and Art. 10(3ter) mandate clients are excluded from some classes. **No KID and no prospectus** (FINMA exemptions), so no Target Market source exists. Strictest US Person definition (IRC-based: any US citizen, dual nationals included). |

Not covered: a LU **RAIF**. A SIF covers the "well-informed investor" rule the same way, and RAIF offering documents are rarely published. No **EMT** is public for any of the four (see [EMT availability](#emt-availability)).

---

## Document URL patterns

**Nordea (funds 1 and 2).** These ISIN-keyed URLs always serve the current version:

```
https://docs.fundconnect.com/GetDocument.aspx?clientid=i1g0xyvk-ztaq-jdr2-cm3y-0sshe4ukztmc&Isin=<ISIN>&lang=en-GB&type=<TYPE>&save=False
```

`TYPE` values that worked: `Prospectus`, `PRIIPS+KID`, `Factsheet`, `Annual+Report`. `EMT`, `KID`, and `Fact+Sheet` return 404. SIF Share Classes have no `Factsheet`.

**Swiss Fund Data (funds 3 and 4).** Files are versioned by date, so each **new edition gets a new URL**. Re-check before the hackathon:

```
https://www.swissfunddata.ch/sfdpub/docs/<doctype>-<fundId>[_<sub>_<class>]-<yyyymmdd>-<lang>.pdf
```

Observed doctypes: `fpd`/`frd` prospectus or fund contract, `prp` KID (Basisinformationsblatt), `fsm` monthly factsheet, `anr` annual report, `sar` semi-annual report, `emr` ESG report (*not* an EMT). Listings come from `https://www.swissfunddata.ch/sfdpub/en/funds/documents?text=<ISIN>`.

---

## 1. Nordea 1 – Global Climate and Environment Fund (LU UCITS)

Manager: Nordea Investment Funds S.A. Umbrella: Nordea 1, SICAV (UCITS, Luxembourg). Base currency: EUR.

### Share Classes

| Class | ISIN | Available to | Min. initial |
|---|---|---|---|
| BP-EUR (accumulating) | `LU0348926287` | All investors | None (factsheet shows "0 EUR") |
| BI-EUR (accumulating) | `LU0348927095` | Institutional investors | EUR 75,000 or equivalent |

### Documents

| Doc | URL | Version |
|---|---|---|
| Prospectus | [Prospectus](https://docs.fundconnect.com/GetDocument.aspx?clientid=i1g0xyvk-ztaq-jdr2-cm3y-0sshe4ukztmc&Isin=LU0348926287&lang=en-GB&type=Prospectus&save=False) | June 2026, 944 pp. (umbrella-wide) |
| KID BP-EUR | [KID](https://docs.fundconnect.com/GetDocument.aspx?clientid=i1g0xyvk-ztaq-jdr2-cm3y-0sshe4ukztmc&Isin=LU0348926287&lang=en-GB&type=PRIIPS+KID&save=False) | published 20/05/2026 |
| KID BI-EUR | [KID](https://docs.fundconnect.com/GetDocument.aspx?clientid=i1g0xyvk-ztaq-jdr2-cm3y-0sshe4ukztmc&Isin=LU0348927095&lang=en-GB&type=PRIIPS+KID&save=False) | published 20/05/2026 |
| Factsheet BP-EUR | [Factsheet](https://docs.fundconnect.com/GetDocument.aspx?clientid=i1g0xyvk-ztaq-jdr2-cm3y-0sshe4ukztmc&Isin=LU0348926287&lang=en-GB&type=Factsheet&save=False) | as of 31/08/2026 |
| EMT | not public | n/a |

### Hard Restrictions (with Citations)

- **US Person exclusion.** "The shares in the SICAV may not be directly or indirectly offered or sold in the USA. Unless otherwise decided by the board or the management company, US persons may not subscribe for shares in the SICAV." US persons exclude "Non-United States person" under CFTC Rule 4.7, so US *taxpayers* may subscribe if they are not US persons. *(Prospectus p.3, "Who can invest in these funds")*. The definition is Reg S-style: residents, US entities, accounts, "any other U.S. person defined by US Rule 902". Citizenship alone is **not** listed. *(Prospectus p.5, "US person")*
- **Distribution jurisdiction.** Offering is legal "only where the shares are registered for public sale or where sale is not prohibited by local law". *(Prospectus p.3)*. Switzerland has a named Representative and Paying Agent (BNP PARIBAS, Paris, Zurich branch). *(Prospectus p.146)*
- **Investor classification / share-class restriction.** Core class table: **P** = "All investors", no minimum. **I** = "Institutional investors", min EUR 75,000. Other classes need a written agreement, e.g. D, N, X, X1. *(Prospectus p.123, "Core share class characteristics")*. "Institutional investors" means investors within the meaning of art. 174 of the 2010 Law, including "financial professionals investing on behalf of … retail [clients] in connection with a discretionary management agreement". *(Prospectus p.5)*
- **Minimum initial subscription.** BP: none. BI: EUR 75,000. *(Prospectus p.123; factsheet "Share class details": BP minimum 0 EUR)*

### Target Market

- Prospectus: "Suitability: The fund is suitable for all types of investors through all distribution channels. Investor profile: Investors who understand the risks of the fund and plan to invest for at least 5 years." *(Prospectus p.16, "Investor Considerations")*
- KID (both classes, "Intended retail investor", p.1): intended for investors who accept that "the invested amount can be lost entirely". "Suitable for all types of investors through all distribution channels". Recommended holding period **5 years**.
- Risk: **SRI 4/7** in the KID (p.1). The factsheet's older UCITS-style "Risk & Reward Profile" shows **category 5**, which is a different scale. Use the KID SRI.

### Demo edge cases enabled

- Retail client, EUR 10k into BP-EUR: **Eligible**. The same client into BI-EUR: **Not Eligible** for two reasons (institutional-only class, below EUR 75k). One fund, two Verdicts.
- The BI-EUR KID still says "suitable for all types of investors". The Hard Restriction from the prospectus must win over the generic KID text.
- US citizen resident in Switzerland: **not** a US Person under Nordea's Reg S definition. Contrast with funds 3 and 4.
- Client horizon of 2 years against a 5-year RHP: Target Market mismatch on horizon.

---

## 2. Nordea Specialised Investment Fund, SICAV-FIS – Global Private Equity Fund (LU SIF)

AIFM: Nordea Investment Funds S.A. Governed by the Luxembourg law of 13 February 2007 (SIF Law). Base currency: EUR.

### Share Classes

| Class | ISIN | Notes |
|---|---|---|
| BW-SEK (W core class, accumulating "B") | `LU2607415275` | KID available |
| (sibling fund) Global Private Credit Fund HBW-SEK | `LU2607416596` | Same class rules. Its link also serves the umbrella prospectus |

Core class rules from the prospectus table *(p.22, "Core share class characteristics")*. Minimums are stated in EUR; SEK/NOK convert at a fixed 1 EUR = 10 SEK/NOK:

| Core class | Available to | Min. initial | Min. subsequent |
|---|---|---|---|
| W, U | Well-informed investors at AIFM discretion | EUR 100,000 | EUR 10,000 |
| T | Well-informed investors subscribing **through Nordea Group** | EUR 10,000 | EUR 10,000 |
| K | Well-informed, through intermediaries, **written agreement** | EUR 10,000 | EUR 10,000 |
| O | Well-informed, through intermediaries | EUR 10,000 | EUR 10,000 |
| I | Institutional investors | EUR 1,000,000 | EUR 100,000 |
| R | Institutional, Global Private Equity Fund shareholders only | EUR 1,000,000 | EUR 100,000 |
| X | Institutional, written agreement, custody at AIFM | EUR 5,000,000 | EUR 500,000 |

### Documents

| Doc | URL | Version |
|---|---|---|
| Prospectus | [Prospectus](https://docs.fundconnect.com/GetDocument.aspx?clientid=i1g0xyvk-ztaq-jdr2-cm3y-0sshe4ukztmc&Isin=LU2607415275&lang=en-GB&type=Prospectus&save=False) | September 2026, 63 pp. (visa 2026-08-27) |
| KID BW-SEK | [KID](https://docs.fundconnect.com/GetDocument.aspx?clientid=i1g0xyvk-ztaq-jdr2-cm3y-0sshe4ukztmc&Isin=LU2607415275&lang=en-GB&type=PRIIPS+KID&save=False) | published 20/05/2026 |
| Factsheet | none published (404) | n/a |
| EMT | not public | n/a |

### Hard Restrictions (with Citations)

- **Investor classification.** "The shares in the SICAV-FIS are exclusively offered to well-informed [investors] as defined in article 2 of the SIF Law." *(Prospectus p.3, "Who can invest in these funds")*. "investor" is defined as "a well-informed investor". *(p.5)*
  - Statutory test, SIF Law art. 2(1): an institutional investor, a MiFID II Annex II professional, or any other investor who (a) confirms the status **in writing** and (b) either (i) invests at least **EUR 100,000**, or (ii) has an assessment by a credit institution, investment firm, UCITS ManCo or AIFM certifying their expertise. The threshold was lowered from EUR 125,000 by the Law of 21 July 2023. *([CSSF consolidated SIF Law](https://www.cssf.lu/wp-content/uploads/L_130207_SIF.pdf), p.4, art. 2 and footnote 5)*
- **US Person exclusion.** Same wording as Nordea 1 (Reg S-style, Rule 4.7 carve-out). *(Prospectus p.3; definition p.5)*
- **Distribution jurisdiction.** Legal "only where the shares are registered for public sale or where sale is not prohibited". *(p.3)*. Unlike Nordea 1, **no Swiss representative** is named. For Swiss clients, treat this as an offer to qualified investors only and route to **Needs Review** (CISA art. 120 FINMA approval requirement for offering foreign funds to non-qualified investors; *not verified against Fedlex in this pass*).
- **Share-class restriction / channel.** T requires Nordea Group as the channel. K requires a written agreement. All classes are "at the discretion of the AIFM". *(p.22)*. The board/AIFM may reject applications and compulsorily redeem ineligible holders. *(p.30, "Rights We Reserve")*
- **Minimum initial subscription.** See the table above *(p.22)*. The AIFM may waive minimums. *(p.30)*

### Target Market

- Prospectus: "Investments in these funds are only suitable for well-informed investors who can afford the risks involved." *(p.3)*
- KID BW-SEK, "Intended retail investor" (p.1): for well-informed investors "with knowledge and/or experience of these types of products" who accept that "the invested amount can be lost entirely. The fund is not suitable for well-informed investors that are unable to sustain long-term investment." **RHP 10 years.**
- **SRI 6/7** ("second-highest risk class") *(KID p.1)*. Listed illiquidity risks: Liquidity, Over-commitment, Funds of funds, Private equity investments.

### Demo edge cases enabled

- Retail client with no well-informed confirmation: **Not Eligible** (investor classification).
- Retail client who signs the written confirmation and invests EUR 100k in a W class: passes the SIF Law test. The class is still "at AIFM discretion", so the Verdict is **Needs Review**.
- Professional client with EUR 50k into W: **Not Eligible**, below the EUR 100k minimum. The same amount into O (EUR 10k minimum via an intermediary) passes.
- Conservative client (risk tolerance 3, horizon 5y): **negative Target Market** against SRI 6 and RHP 10y.
- Swiss-domiciled client: **Needs Review** (no Swiss distribution set-up named).

---

## 3. BNP PARIBAS (CH) Fund – Bonds CHF (CH fund, retail plus qualified-only class)

Fund management company: BNP PARIBAS ASSET MANAGEMENT Switzerland AG (formerly AXA IM; documents still reference axa-im.ch). Contractual umbrella fund, "Other Funds for Traditional Investments", under CISA. Not UCITS.

### Share Classes

| Class | ISIN | Available to | Min. initial / subsequent |
|---|---|---|---|
| A (distributing) | `CH0112537516` | All investors | None |
| A AC (accumulating) | `CH0114292532` | All investors | None |
| I (distributing) | `CH0112537557` | **Qualified investors only** (CISA art. 10 para 3 and 3ter) | **CHF 1,000,000** / CHF 10,000 |
| I AC (accumulating) | `CH0114292540` | Qualified investors only | CHF 1,000,000 / CHF 10,000 |
| F / F AC | `CH0295589375` / `CH0295589508` | Distributors with separate fee agreements, or at FMC discretion | CHF 10,000 / CHF 5,000 |
| S / S AC | `CH0593936047` / `CH0593936054` | Investors with a written advisory/asset-management contract with an AXA Group company or the FMC; AXA/BNP Group companies | None |

*(ISINs: prospectus p.15, §5.1 "Key data". Eligibility and minimums: p.5, §1.5 a)*

### Documents

| Doc | URL | Version |
|---|---|---|
| Prospectus with integrated fund contract (EN) | [frd-5415-20260629-en.pdf](https://www.swissfunddata.ch/sfdpub/docs/frd-5415-20260629-en.pdf) | 29 June 2026, 34 pp. Swiss Fund Data now also lists a 23 July 2026 edition, FR only: [fpd-5415-20260723-fr.pdf](https://www.swissfunddata.ch/sfdpub/docs/fpd-5415-20260723-fr.pdf). Not checked for changes. |
| KID class A (DE) | [prp-5415_01_01-20260507-de.pdf](https://www.swissfunddata.ch/sfdpub/docs/prp-5415_01_01-20260507-de.pdf) | created 01/05/2026 |
| KID class I (DE) | [prp-5415_01_02-20260507-de.pdf](https://www.swissfunddata.ch/sfdpub/docs/prp-5415_01_02-20260507-de.pdf) | created 01/05/2026 |
| Factsheet class A | [fsm-5415_01_01-20260831-en.pdf](https://www.swissfunddata.ch/sfdpub/docs/fsm-5415_01_01-20260831-en.pdf) | as of 31/08/2026 |
| Factsheet class I | [fsm-5415_01_02-20260831-en.pdf](https://www.swissfunddata.ch/sfdpub/docs/fsm-5415_01_02-20260831-en.pdf) | as of 31/08/2026 |
| EMT | not public | n/a |

### Hard Restrictions (with Citations)

- **Investor classification / share-class restriction.** "Class 'I' and class 'I AC' units may only be held by qualified investors within the meaning of Art. 10 para 3 and 3ter CISA." *(Prospectus p.5, §1.5 a; repeated in the fund contract, p.19, §6.4)*. Qualified investors "include, in particular, regulated financial intermediaries … regulated insurance companies, public entities and retirement provision institutions with professional treasury departments … companies with professional treasury departments, as well as private clients who have declared their wish to opt out" (CISA art. 10(3)/(3ter) with FinSA art. 4(3)–(5), art. 5(1) and (4)). *(p.5)*
- **Minimum initial subscription.** A: none. F: CHF 10,000. I: CHF 1,000,000 (subsequent CHF 10,000). *(p.5)*. Also stated in the I-class KID: "Mindestzeichnungsbetrag bei Erstzeichnung: CHF 1'000'000" *(KID I, p.1)*
- **US Person exclusion.** Units "may not be offered, sold or delivered to investors who are U.S. Persons within the meaning of Regulation S … **in particular investors with U.S. citizenship** or investors who are resident or domiciled in the United States". US employee benefit plans (ERISA) are also excluded. *(Prospectus p.16, §5.3 "Sales restrictions")*
- **Distribution jurisdiction.** "No issue or redemption of units of the subfunds shall take place abroad; in particular, units … may not be offered, sold or delivered within the United States." *(p.16, §5.3)*. The factsheet confirms "UCITS Compliant: No", so there is no EU passport.

### Target Market

- Prospectus, "Profile of the typical investor" (Bonds CHF): "appropriate for investors with a medium- to long-term investment horizon who primarily seek regular income from the point of view of capital security. Investors can accept temporary fluctuations …" *(p.16, §6 a)*
- KIDs ("Kleinanleger-Zielgruppe", p.1). The text **differs by class**:
  - **A**: retail investors "die **kein** finanzielles Know-how und keine Kenntnisse besitzen" (basic investors), can bear total loss, seek growth, income and ESG, horizon at least 3 years.
  - **I**: retail investors "die ein **gewisses** finanzielles Know-how oder Kenntnisse besitzen" (informed investors), otherwise the same.
- **SRI 2/7**, RHP **3 years** (both KIDs, p.1). Both factsheets show the same SRI 2.

Note: FinSA has no MiFID-style product-governance Target Market duty. The Swiss KID "Zielgruppe" plus the prospectus "typical investor profile" are the closest equivalents.

### Demo edge cases enabled

- Swiss retail client, CHF 50k: **Eligible** for A. **Not Eligible** for I (not qualified, and below CHF 1m).
- Conflicting documents: the I-class KID addresses "Privatanleger", but the prospectus restricts I to qualified investors. Show that the Justification cites the prospectus Hard Restriction over the KID text.
- Swiss-resident **US citizen** (dual national): **Not Eligible** here because citizenship is named. The same person would pass Nordea 1's US Person test.
- Luxembourg-resident client: "no issue … abroad" leads to **Not Eligible** or Needs Review on distribution jurisdiction.
- Swiss HNWI who opted out under FinSA art. 5(1), CHF 2m: **Eligible** for I.

---

## 4. Swiss Life iFunds (CH) (CH fund for qualified investors only)

Fund management company: Swiss Life Asset Management Ltd, Zurich. Custodian: UBS Switzerland AG. Contractual umbrella, "other funds for traditional investments", **for qualified investors**. Sub-funds include Equity Switzerland (CHF), Equity Switzerland Small & Mid Cap (CHF), Bond Swiss Francs Domestic/Foreign (CHF), Bond Global Corporates/Government+/Aggregate (CHF hedged).

### Share Classes / ISINs

The fund contract dated 1 October 2026 defines these classes: **I Cap, I Dis** (all qualified investors), **AM Cap/Dis, M Cap/Dis** (qualified investors with a Swiss Life mandate), **I-A 1 / I-A 1 Cap** (mandate clients, 3ter excluded), and **I-A 3** (life insurers' tied assets) *(fund contract pp.7–8, §6.4)*. The latest annual report (30 Sept 2025) still uses the older names:

| Sub-fund | Class (annual report naming) | ISIN |
|---|---|---|
| Equity Switzerland Small & Mid Cap (CHF) | I-A1 / I-A2 | `CH0023989467` / `CH1197235372` |
| Equity Switzerland (CHF) | I-A1 | `CH0108009199` |
| Bond Swiss Francs Domestic (CHF) | I-A1 / I-A2 | `CH0023989764` / `CH0324001053` |
| Bond Swiss Francs Foreign (CHF) | I-A1 / I-A2 | `CH0023989582` / `CH0219870471` |

*(Annual report 2024/25, p.3 contents table, and the per-sub-fund "Class … ISIN" lines, e.g. p.30.)* A class launched 20/03/2026 for Small & Mid Cap, `CH1533828468`, appears on third-party data sites. Its mapping to the new class names is **unverified**. Confirm with Swiss Life before relying on a specific class name and ISIN pair.

### Documents

| Doc | URL | Version |
|---|---|---|
| Fund contract with appendix (EN; German binding) | [fpd-1405-20261001-en.pdf](https://www.swissfunddata.ch/sfdpub/docs/fpd-1405-20261001-en.pdf) | effective 1 October 2026, 47 pp. |
| Annual report | [anr-1405-20250930-en.pdf](https://www.swissfunddata.ch/sfdpub/docs/anr-1405-20250930-en.pdf) | FY to 30 Sept 2025 |
| Prospectus | **none**: FINMA exemption | n/a |
| KID | **none**: FINMA exemption | n/a |
| Factsheet | none found on Swiss Fund Data or swisslife.ch | n/a |
| EMT | not public | n/a |

### Hard Restrictions (with Citations)

- **Investor classification (fund level).** "The group of investors of all sub-funds is limited to qualified investors as per Art. 10, cl. 3 and 3ter CISA in conjunction with Art. 4, cl. 3-5 and Art. 5, cl. 1 and 4 FinSA." The FMC and custodian ensure eligibility. *(Fund contract p.5, §5.1)*
- **Share-class restriction (by kind of qualified investor).** *(pp.7–8, §6.4)*
  - I Cap / I Dis: "exclusively open to all qualified investors pursuant to Art. 10, cl. 3 and 3ter CISA."
  - AM Cap / AM Dis: qualified investors under art. 10(3) **with an asset management or other paid financial services contract** with Swiss Life (or a cooperating FinSA art. 4(3)(a)/(b) intermediary). "Qualified investors pursuant to Art. 10, cl. 3ter CISA and **high net worth individuals pursuant to Art. 5, cl. 1 FinSA are excluded**."
  - M Cap / M Dis: as AM, plus withholding-tax reporting-procedure eligibility, and custody at the custodian bank.
  - I-A 1 / I-A 1 Cap: qualified investors with a Swiss Life mandate. "Qualified investors pursuant to Art. 10, cl. 3ter CISA are excluded."
  - I-A 3: Swiss and foreign life insurers' tied assets only.
  - Investors who stop qualifying must redeem or convert within 30 days, otherwise there is a forced conversion or redemption. *(p.9, §6.6)*
- **US Person exclusion (strictest of the set).** "Units … may not be offered, sold or delivered to US persons. US person means: (i) a US citizen (including dual or multiple citizenship); (ii) a US resident (… Green Card or … substantial presence test); … (vi) a person subject to US tax law for any other reason …" The definition is construed under the US Internal Revenue Code. *(Appendix p.45, §5.3 b)*
- **Distribution jurisdiction.** "Distribution activities have been approved in the following countries: Switzerland." *(p.45, §5.3 a)*
- **Minimum initial subscription.** None stated. The contract only says classes *may* differ in minimum investment. *(p.7, §6.3)*

### Target Market

- **None published.** "FINMA has exempted this investment fund … pursuant to Art. 10, cl. 5 CISA from … c) the obligation to produce a key information document", and "from the duty to publish a prospectus in accordance with Art. 50 [FinSA]". *(Fund contract p.2, §1.6)*. An appendix replaces the prospectus. *(p.3, §1.8)*

### Demo edge cases enabled

- Swiss retail client: **Not Eligible** at fund level.
- Swiss HNWI who opted out (FinSA art. 5(1)): **Eligible** for I Cap, **Not Eligible** for AM Cap (HNWI explicitly excluded). This is a nuanced share-class rule.
- Retail client under a discretionary mandate (CISA art. 10(3ter)): **Eligible** for I Cap, **Not Eligible** for I-A 1.
- **Missing Target Market source.** With no KID or EMT, the TM step cannot run. Return **Needs Review** with a Justification citing the FINMA exemption in §1.6, or skip TM for professional clients. Either way the "no data" path gets tested.
- Dual US/Swiss national: **Not Eligible** (citizenship named).
- Any non-Swiss client: **Not Eligible** or Needs Review (distribution approved only in CH).

---

## Verdict coverage matrix (suggested demo clients)

| Client Profile | F1 BP-EUR | F1 BI-EUR | F2 W class | F3 A | F3 I | F4 I Cap |
|---|---|---|---|---|---|---|
| LU retail, French national, EUR/CHF 20k, SRI tolerance 4, 5y | Eligible | Not Eligible (class + min) | Not Eligible (not well-informed) | Not Eligible / Review (no issue abroad) | Not Eligible | Not Eligible |
| LU retail, signed well-informed confirmation, EUR 150k, SRI 6, 10y | Eligible | Not Eligible | Needs Review (AIFM discretion) | Review (abroad) | Not Eligible | Not Eligible |
| DE professional, EUR 50k, SRI 3, 3y | Eligible (TM horizon warning) | Not Eligible (min) | Not Eligible (min, TM SRI/horizon) | Review (abroad) | Review (abroad) | Not Eligible (CH-only) |
| CH retail, CHF 50k, no knowledge, SRI 2, 3y | TM mismatch (SRI 4, 5y) | Not Eligible | Needs Review (CH) | **Eligible** | Not Eligible (QI + min) | Not Eligible |
| CH HNWI opted out, CHF 2m, informed | Eligible | Eligible (if treated as institutional; Needs Review) | Needs Review (CH) | Eligible | **Eligible** | **Eligible** (but AM Cap Not Eligible) |
| CH-resident US citizen, HNWI opted out | Eligible (not a Reg S US Person) | Needs Review | Needs Review | **Not Eligible (US)** | **Not Eligible (US)** | **Not Eligible (US)** |

---

## EMT availability

No public EMT was found for funds 1–4. Nordea's fundconnect returns 404 for `EMT`. Swiss Fund Data's `emr` files are ESG reports. Both Nordea and BNP AM supply EMTs to distributors through data vendors.

If the demo needs a real EMT row, **UBS publishes FinDatEx EMT v4.1/v4.2 files publicly** at <https://www.ubs.com/funds-regulatoryinformation>, one CSV/XLSX per ManCo, including UBS Asset Management (Europe) S.A. (LU) and UBS Fund Management (Switzerland) AG. Example: `https://www.ubs.com/2/e/priip/EMT/EMT_UBS_FMCH_v_4.2.csv`. Scripted `curl` gets HTTP 403 (Akamai); a browser download works. Otherwise, take Target Market from the KID "Intended retail investor" section, as above.

## Caveats

- Swiss Fund Data URLs embed the edition date and will change when documents are updated. Nordea URLs are stable.
- BNP CH KIDs were taken in German, the only language listed. English factsheets are available.
- The CISA art. 120 point (fund 2, Swiss clients) is stated from general knowledge of Swiss law and was not checked against Fedlex in this pass ([CISA on Fedlex](https://www.fedlex.admin.ch/eli/cc/2006/822/en)). The CISA art. 10(3)/(3ter) definition is quoted as restated in the fund documents.
- A RAIF was not sourced. Add one later only if the demo must name the RAIF regime explicitly; the SIF covers the same Hard Restriction.
