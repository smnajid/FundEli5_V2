@must @rule-engine
Feature: Check eligibility
  As a Fund Dealer
  I want each Subscription Request checked against the fund's validated rules
  So that I can say, and defend, whether the portfolio may subscribe

  An Eligibility Check takes one Subscription Request (a portfolio, a Share
  Class, an amount and whether the client initiated the order) and gives a
  Verdict with a Justification: one outcome per rule of the Rule Card, each
  with its Citation. The rules are Hard Restrictions, Unmodelled Restrictions,
  the Target Market and the bank's Fund Registration. Suitability is not part of it.

  Each example below is one row of the coverage set; 99-answer-key.feature
  holds every row with all its rule outcomes.

  Background:
    Given the demo dataset
    And the Validated Rule Cards of the sample funds
    And Nordea's "reject or close" clause and the SIF's "at AIFM discretion" clause are marked Noted

  Rule: Any Fail gives Not Eligible; otherwise any Review gives Needs Review; otherwise Eligible
    Rule Card model, issue #6. A warning never changes the Verdict.

    Example: Row 3 - every rule passes
      When LOC90000101 asks to subscribe EUR 80,000 to GCE-BP
      Then the Verdict is Eligible

    @assumed
    Example: Row 11 - two Fails, both shown
      Assumed: outside the positive Target Market gives Review in Luxembourg (see row 10). Samuel (desk) confirms.
      When LUX90000301 asks to subscribe EUR 20,000 to GCE-BI
      Then the Verdict is Not Eligible
      And the investor category rule gives Fail
      And the minimum initial subscription rule gives Fail
      And the Target Market rule gives Review

  Rule: The Verdict is computed by fixed rules, with no LLM, and the same inputs always give the same Verdict
    ADR 0001, ADR 0002.

    Example: The same check twice gives the same Verdict and Justification, and no LLM call
      When LOC90000101 asks to subscribe EUR 80,000 to GCE-BI twice
      Then both checks give the same Verdict and the same Justification
      And no LLM call is made

  Rule: Every rule outcome carries its Citation
    Glossary: Justification, Citation.

    Example: The deciding rule cites the passage it relies on
      When LOC90000101 asks to subscribe EUR 80,000 to GCE-BI
      Then every rule outcome cites a passage of the Fund Documentation or a field of the Client Profile
      And the investor category rule cites the prospectus page it relies on

  Rule: Only a Validated Rule Card decides; a Draft card or no card gives Needs Review
    ADR 0001.

    Example: Row 25 - only a Draft Rule Card exists for the class
      Given BNP-AAC has only a Draft Rule Card
      When LOC90001401 asks to subscribe CHF 50,000 to BNP-AAC
      Then the Verdict is Needs Review
      And the Justification says "Rule Card not validated"

    Example: A Share Class with no Rule Card
      When LOC90000101 asks to subscribe EUR 80,000 to the placeholder class XXNOTCOV0001
      Then the Verdict is Needs Review
      And the Justification says "Fund not covered"

    Example: A Stale Rule Card still decides, with a warning
      Given the fund master data lists a newer prospectus edition than the one GCE-BP's Rule Card was drafted from
      When LOC90000101 asks to subscribe EUR 80,000 to GCE-BP
      Then the Verdict is Eligible
      And the Justification warns "Stale Rule Card"

  Rule: Only the Swiss and Luxembourg regimes are covered, chosen by the portfolio's Legal Entity
    LOC is Switzerland (FinSA, CISA), LUX is Luxembourg (MiFID II, UCITS, AIF). Issue #6.

    Example: Row 24 - a Singapore portfolio
      When SIN90001301 asks to subscribe EUR 50,000 to GCE-BP
      Then the Verdict is Needs Review
      And the Justification says "regime not covered"

  Rule: An unknown fact gives Review, never Fail and never a default
    ADR 0008: every fact is optional; absent means unknown.

    Example: Row 23 - US Person status missing
      When LOC90001201 asks to subscribe CHF 50,000 to BNP-A
      Then the Verdict is Needs Review
      And the US Person exclusion rule gives Review

    Example: Row 26 - the request names no amount
      When LOC90000102 asks to subscribe an unstated amount to GCE-BI
      Then the Verdict is Needs Review
      And the minimum initial subscription rule gives Review

  Rule: Investor category follows the regime's Investor Classification, mandate and declarations
    Qualified investor (CH) and well-informed investor (LU) are derived, never stored. Issue #2.

    Example: Row 1 - an advisory retail portfolio is not an institutional investor
      When LOC90000101 asks to subscribe EUR 80,000 to GCE-BI
      Then the Verdict is Not Eligible
      And the investor category rule gives Fail

    Example: Row 2 - a discretionary mandate counts as institutional
      Same client and fund as row 1; 2010 Law art. 174, prospectus p.5.
      When LOC90000102 asks to subscribe EUR 80,000 to GCE-BI
      Then the Verdict is Eligible
      And the investor category rule gives Pass

    @assumed
    Example: Row 5 - an HNWI opt-out is not an art. 174 institutional investor
      Assumed reading of the 2010 Law art. 174 for a FinSA professional by opt-out. Samuel (desk) confirms.
      When LOC90000201 asks to subscribe CHF 2,000,000 to GCE-BI
      Then the Verdict is Not Eligible
      And the investor category rule gives Fail

    @assumed
    Example: Row 13 - well-informed from a written confirmation and at least EUR 100,000
      Assumed: a negative Target Market on risk (SRI 6 above max 3) and horizon (10-year holding period
      above 5 years) gives Review in Luxembourg, while row 14's on knowledge and experience gives Fail.
      Samuel (desk) confirms.
      When LUX90000501 asks to subscribe EUR 120,000 to SIF-W
      Then the investor category rule gives Pass
      And the Verdict is Needs Review
      And the Target Market rule gives Review

    Example: Row 21 - clients of a CISA art. 10(3ter) mandate are qualified for I Cap
      When LOC90001001 asks to subscribe CHF 300,000 to SL-ICAP
      Then the Verdict is Eligible
      And the investor category rule gives Pass

  Rule: US Person is judged by the fund's own definition
    Reg S, FATCA and the IRC definitions differ: one US citizen can pass one fund and fail another. Issue #2.

    Example: Row 4 - Reg S: US citizenship alone is not a US Person
      When LOC90000201 asks to subscribe CHF 2,000,000 to GCE-BP
      Then the Verdict is Eligible
      And the US Person exclusion rule gives Pass

    Example: Row 6 - "in particular investors with U.S. citizenship"
      When LOC90000201 asks to subscribe CHF 2,000,000 to BNP-A
      Then the Verdict is Not Eligible
      And the US Person exclusion rule gives Fail

    Example: Row 7 - qualified and above CHF 1m, but a US citizen
      When LOC90000201 asks to subscribe CHF 2,000,000 to BNP-I
      Then the Verdict is Not Eligible
      And the US Person exclusion rule gives Fail

    Example: Row 8 - the IRC definition covers any US citizen, dual nationals included
      When LOC90000201 asks to subscribe CHF 2,000,000 to SL-ICAP
      Then the Verdict is Not Eligible
      And the US Person exclusion rule gives Fail

  Rule: The minimum initial subscription is compared in the class's minimum currency
    Converted at the bank's FX table. Below the minimum by at most 5% gives Review, further below gives Fail.

    Example: Row 12 - below EUR 100,000 for the W class
      When LUX90000401 asks to subscribe EUR 50,000 to SIF-W
      Then the Verdict is Not Eligible
      And the minimum initial subscription rule gives Fail

    @assumed
    Example: Row 22 - CHF 70,000 is EUR 74,866, below EUR 75,000 by 0.2%
      Assumed: the 5% tolerance, and the EUR/CHF rate 0.9350 standing in for the bank's FX table. Samuel (desk) confirms the tolerance; the bank's FX table replaces the rate at the event.
      When LOC90001101 asks to subscribe CHF 70,000 to GCE-BI
      Then the Verdict is Needs Review
      And the minimum initial subscription rule gives Review

  Rule: A mandate-only class needs a mandate with the fund's manager
    Example: Row 18 - no Swiss Life mandate, and HNWI opt-outs excluded
      When LOC90000901 asks to subscribe CHF 500,000 to SL-AMCAP
      Then the Verdict is Not Eligible
      And the mandate-only class rule gives Fail
      And the investor category rule gives Fail

    Example: Row 20 - CISA art. 10(3ter) mandate clients excluded
      When LOC90001001 asks to subscribe CHF 300,000 to SL-IA1
      Then the Verdict is Not Eligible
      And the mandate-only class rule gives Fail

  Rule: The Share Class must be registered for the client's country, as the bank's Fund Registration says
    Fund Registration, not the Fund Documentation's Selling Restriction, decides. NotRegistered gives Fail,
    or Review when the client initiated the order (reverse solicitation); Unknown gives Review. Issue #41.

    Example: Row 9 - not registered in Switzerland
      When LOC90000201 asks to subscribe CHF 2,000,000 to SIF-W
      Then the Verdict is Not Eligible
      And the Fund Registration rule gives Fail

    Example: Row 15 - not registered in Switzerland, bank-initiated
      When LOC90000701 asks to subscribe EUR 150,000 to SIF-W
      Then the Verdict is Not Eligible
      And the Fund Registration rule gives Fail

    Example: Row 16 - same order, initiated by the client
      The dealer records the evidence of reverse solicitation.
      When LOC90000701 asks to subscribe EUR 150,000 to SIF-W at the client's initiative
      Then the Verdict is Needs Review
      And the Fund Registration rule gives Review

    Example: Row 17 - "no issue or redemption abroad"
      When LUX90000801 asks to subscribe CHF 50,000 to BNP-A
      Then the Verdict is Not Eligible
      And the Fund Registration rule gives Fail

  @scale-mapping
  Rule: The Target Market is matched through the Scale Mapping; it binds in Luxembourg and only warns in Switzerland
    ADR 0002: fixed rules on the bank's scales translated by the validated Scale Mapping, no LLM.

    @assumed
    Example: Row 10 - Luxembourg, outside the positive Target Market
      SRI 4 above max 3, 5-year holding period above a 2-year horizon, total loss above limited loss.
      Assumed: outside the positive Target Market gives Review in Luxembourg. Samuel (desk) confirms.
      When LUX90000301 asks to subscribe EUR 20,000 to GCE-BP
      Then the Verdict is Needs Review
      And the Target Market rule gives Review

    @assumed
    Example: Row 14 - Luxembourg, inside the negative Target Market on knowledge and experience
      Assumed: a negative Target Market gives Fail in Luxembourg. Samuel (desk) confirms.
      When LUX90000601 asks to subscribe EUR 120,000 to SIF-W
      Then the Verdict is Not Eligible
      And the Target Market rule gives Fail

    Example: Row 19 - Switzerland, no Target Market source is a warning only
      When LOC90000901 asks to subscribe CHF 500,000 to SL-ICAP
      Then the Verdict is Eligible
      And the Target Market rule gives a warning "no Target Market source"

    @assumed
    Example: Row 2 - a discretionary mandate is matched on the mandate's profile and the client's knowledge
      Anna's discretionary mandate: 4 Growth (max SRI 5), full loss, 10-year horizon, against GCE-BI's SRI 4
      and 5-year holding period. Her knowledge and experience in funds is informed.
      Assumed: under a discretionary mandate, risk, ability to bear losses and horizon are read from the
      portfolio, which carries the mandate's profile; knowledge and experience is read from the client.
      Samuel (desk) confirms.
      When LOC90000102 asks to subscribe EUR 80,000 to GCE-BI
      Then the Target Market rule gives Pass
      And the Target Market rule cites the risk profile, ability to bear losses and horizon of portfolio LOC90000102
      And the Target Market rule cites client 900001's knowledge and experience in funds

  Rule: An Unmodelled Restriction gives Review while Blocking, and passes once a Rule Card Validator marks it Noted
    ADR 0003. A Noted Restriction stays in the Justification with its Citation.

    Example: The Noted clause is shown but does not change the Verdict
      When LOC90000101 asks to subscribe EUR 80,000 to GCE-BP
      Then the Verdict is Eligible
      And the Justification shows Nordea's "reject or close" clause as Noted, with its Citation

    Example: The same clause left Blocking
      Given Nordea's "reject or close" clause is Blocking on GCE-BP's Rule Card
      When LOC90000101 asks to subscribe EUR 80,000 to GCE-BP
      Then the Verdict is Needs Review
      And the Unmodelled Restrictions rule gives Review
