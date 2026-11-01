@must @answer-key @rule-engine
Feature: Answer key
  The coverage set: every check the demo data must produce, with the Verdict and
  the outcome of every rule. Each row also appears, as an example, under the
  Rule it proves in 01-check-eligibility.feature and 02-answer-a-subscription-request.feature.
  Rerun before every rehearsal; every row must pass before the demo is rehearsed.

  Rule columns: investor category, US Person exclusion, minimum initial
  subscription, mandate-only class, KID required for retail, Unmodelled
  Restrictions, Target Market, Fund Registration. "—" means the card has no
  such rule, or, on rows 24 and 25, that no rule is evaluated. Rows 4 to 9 are
  Tom's reverse lookup.

  # Legend, a reading aid only: the source of truth is docs/playbook/demo-dataset.md
  # (portfolios) and docs/playbook/contracts/fixtures/rule-cards/ (classes).
  #
  # Portfolios: residence, Investor Classification, mandate, and what the rows test
  #   LOC90000101  Anna    CH  retail                         advisory
  #   LOC90000102  Anna    CH  retail                         discretionary mandate
  #   LOC90000201  Tom     CH  professional, HNWI opt-out     advisory; CH+US citizen, FATCA US person
  #   LUX90000301  Claire  LU  retail                         advisory; Income, limited loss, 2y, funds basic
  #   LUX90000401          LU  professional (per se)          advisory
  #   LUX90000501          LU  retail + well-informed conf.   advisory; Income, 5y, private equity informed
  #   LUX90000601          LU  retail + well-informed conf.   advisory; private equity knowledge none
  #   LOC90000701          CH  professional, HNWI opt-out     advisory
  #   LUX90000801          LU  retail                         advisory
  #   LOC90000901          CH  professional, HNWI opt-out     advisory
  #   LOC90001001          CH  retail                         discretionary mandate, not with Swiss Life
  #   LOC90001101          CH  retail                         discretionary mandate
  #   LOC90001201          CH  retail                         advisory; US Person status missing
  #   SIN90001301          SG  Singapore entity               regime not covered
  #   LOC90001401          CH  retail                         advisory
  #
  # Classes: who may buy, minimum, US Person definition, where registered
  #   GCE-BP    Nordea 1 Global Climate BP-EUR  all investors                   none           Reg S             CH, LU
  #   GCE-BI    Nordea 1 Global Climate BI-EUR  institutional (art. 174)        EUR 75,000     Reg S             CH, LU
  #   SIF-W     Nordea SIF Private Equity BW    well-informed                   EUR 100,000    Reg S             LU only
  #   BNP-A     BNP (CH) Bonds CHF A            all investors                   none           Reg S + citizens  CH only
  #   BNP-AAC   BNP (CH) Bonds CHF A AC         Draft Rule Card only
  #   BNP-I     BNP (CH) Bonds CHF I            qualified                       CHF 1,000,000  Reg S + citizens  CH only
  #   SL-ICAP   Swiss Life iFunds I Cap         qualified                       none           any US citizen    CH only
  #   SL-AMCAP  Swiss Life iFunds AM Cap        qualified, no HNWI opt-outs,    none           any US citizen    CH only
  #                                             no 10(3ter) mandate clients; Swiss Life mandate only
  #   SL-IA1    Swiss Life iFunds I-A 1         qualified, no 10(3ter) mandate  none           any US citizen    CH only
  #                                             clients; Swiss Life mandate only

  Background:
    Given the demo dataset
    And the Validated Rule Cards of the sample funds
    And Nordea's "reject or close" clause and the SIF's "at AIFM discretion" clause are marked Noted

  Scenario Outline: Row <row> - <shows>
    When <portfolio> asks to subscribe <amount> to <class>, client initiated: <initiated>
    Then the Verdict is <verdict>
    And the rule outcomes are:
      | investor category   | <category>     |
      | US Person exclusion | <us person>    |
      | minimum             | <minimum>      |
      | mandate-only class  | <mandate>      |
      | KID for retail      | <kid>          |
      | Unmodelled          | <unmodelled>   |
      | Target Market       | <target market> |
      | Fund Registration   | <registration> |

    Examples: Confirmed readings
      | row | portfolio   | class    | amount        | initiated | category | us person | minimum | mandate | kid  | unmodelled | target market       | registration | verdict      | shows |
      | 1   | LOC90000101 | GCE-BI   | EUR 80,000    | no        | Fail     | Pass      | Pass    | —       | Pass | Pass       | Pass                | Pass         | Not Eligible | Anna: an advisory retail portfolio is not institutional |
      | 2   | LOC90000102 | GCE-BI   | EUR 80,000    | no        | Pass     | Pass      | Pass    | —       | Pass | Pass       | Pass                | Pass         | Eligible     | Anna: a discretionary mandate counts as institutional (2010 Law art. 174, prospectus p.5) |
      | 3   | LOC90000101 | GCE-BP   | EUR 80,000    | no        | Pass     | Pass      | Pass    | —       | Pass | Pass       | Pass                | Pass         | Eligible     | Anna: the class the reply suggests for portfolio 01 |
      | 4   | LOC90000201 | GCE-BP   | CHF 2,000,000 | no        | Pass     | Pass      | Pass    | —       | Pass | Pass       | Pass                | Pass         | Eligible     | Tom: under Reg S, US citizenship alone is not a US Person |
      | 6   | LOC90000201 | BNP-A    | CHF 2,000,000 | no        | Pass     | Fail      | Pass    | —       | Pass | —          | Warning             | Pass         | Not Eligible | Tom: "in particular investors with U.S. citizenship" |
      | 7   | LOC90000201 | BNP-I    | CHF 2,000,000 | no        | Pass     | Fail      | Pass    | —       | Pass | —          | Warning             | Pass         | Not Eligible | Tom: qualified and above CHF 1m, but a US citizen |
      | 8   | LOC90000201 | SL-ICAP  | CHF 2,000,000 | no        | Pass     | Fail      | —       | —       | —    | Pass       | Warning (no source) | Pass         | Not Eligible | Tom: the IRC definition covers any US citizen, dual nationals included |
      | 9   | LOC90000201 | SIF-W    | CHF 2,000,000 | no        | Pass     | Pass      | Pass    | —       | Pass | Pass       | Warning             | Fail         | Not Eligible | Tom: not registered in Switzerland |
      | 12  | LUX90000401 | SIF-W    | EUR 50,000    | no        | Pass     | Pass      | Fail    | —       | Pass | Pass       | Pass                | Pass         | Not Eligible | below EUR 100,000 for the W class |
      | 15  | LOC90000701 | SIF-W    | EUR 150,000   | no        | Pass     | Pass      | Pass    | —       | Pass | Pass       | Warning             | Fail         | Not Eligible | not registered in Switzerland |
      | 16  | LOC90000701 | SIF-W    | EUR 150,000   | yes       | Pass     | Pass      | Pass    | —       | Pass | Pass       | Warning             | Review       | Needs Review | reverse solicitation: the dealer records evidence |
      | 17  | LUX90000801 | BNP-A    | CHF 50,000    | no        | Pass     | Pass      | Pass    | —       | Pass | —          | Pass                | Fail         | Not Eligible | "no issue or redemption abroad" |
      | 18  | LOC90000901 | SL-AMCAP | CHF 500,000   | no        | Fail     | Pass      | —       | Fail    | —    | Pass       | Warning (no source) | Pass         | Not Eligible | HNWI opt-outs excluded, and no Swiss Life mandate |
      | 19  | LOC90000901 | SL-ICAP  | CHF 500,000   | no        | Pass     | Pass      | —       | —       | —    | Pass       | Warning (no source) | Pass         | Eligible     | same client, open class; no Target Market source only warns in Switzerland |
      | 20  | LOC90001001 | SL-IA1   | CHF 300,000   | no        | Fail     | Pass      | —       | Fail    | —    | Pass       | Warning (no source) | Pass         | Not Eligible | CISA art. 10(3ter) mandate clients excluded |
      | 21  | LOC90001001 | SL-ICAP  | CHF 300,000   | no        | Pass     | Pass      | —       | —       | —    | Pass       | Warning (no source) | Pass         | Eligible     | CISA art. 10(3ter) mandate clients are qualified for I Cap |
      | 23  | LOC90001201 | BNP-A    | CHF 50,000    | no        | Pass     | Review    | Pass    | —       | Pass | —          | Warning             | Pass         | Needs Review | US Person status missing: Review, never Fail |
      | 24  | SIN90001301 | GCE-BP   | EUR 50,000    | no        | —        | —         | —       | —       | —    | —          | —                   | —            | Needs Review | regime not covered (SIN) |
      | 25  | LOC90001401 | BNP-AAC  | CHF 50,000    | no        | —        | —         | —       | —       | —    | —          | —                   | —            | Needs Review | only a Draft Rule Card exists for the class |
      | 26  | LOC90000102 | GCE-BI   | no amount     | no        | Pass     | Pass      | Review  | —       | Pass | Pass       | Pass                | Pass         | Needs Review | the request names no amount |

    # Each row below rests on a reading Samuel (desk) confirms; he removes the tag on acceptance.
    #  5: an HNWI opt-out is not a 2010 Law art. 174 institutional investor.
    # 10, 11: outside the positive Target Market gives Review in Luxembourg.
    # 13: a negative Target Market on risk and horizon gives Review in Luxembourg.
    # 14: a negative Target Market on knowledge and experience gives Fail in Luxembourg.
    # 22: below the minimum by at most 5% gives Review; EUR/CHF 0.9350 stands in for the bank's FX table.
    @assumed
    Examples: Assumed readings
      | row | portfolio   | class    | amount        | initiated | category | us person | minimum | mandate | kid  | unmodelled | target market       | registration | verdict      | shows |
      | 5   | LOC90000201 | GCE-BI   | CHF 2,000,000 | no        | Fail     | Pass      | Pass    | —       | Pass | Pass       | Pass                | Pass         | Not Eligible | Tom: an HNWI opt-out is not an art. 174 institutional investor |
      | 10  | LUX90000301 | GCE-BP   | EUR 20,000    | no        | Pass     | Pass      | Pass    | —       | Pass | Pass       | Review              | Pass         | Needs Review | Claire: outside the positive Target Market binds in Luxembourg |
      | 11  | LUX90000301 | GCE-BI   | EUR 20,000    | no        | Fail     | Pass      | Fail    | —       | Pass | Pass       | Review              | Pass         | Not Eligible | Claire: retail, and below EUR 75,000 |
      | 13  | LUX90000501 | SIF-W    | EUR 120,000   | no        | Pass     | Pass      | Pass    | —       | Pass | Pass       | Review              | Pass         | Needs Review | well-informed from a written confirmation; negative Target Market on risk and horizon |
      | 14  | LUX90000601 | SIF-W    | EUR 120,000   | no        | Pass     | Pass      | Pass    | —       | Pass | Pass       | Fail                | Pass         | Not Eligible | negative Target Market on knowledge and experience |
      | 22  | LOC90001101 | GCE-BI   | CHF 70,000    | no        | Pass     | Pass      | Review  | —       | Pass | Pass       | Pass                | Pass         | Needs Review | CHF 70,000 is EUR 74,866, below EUR 75,000 by 0.2% |
