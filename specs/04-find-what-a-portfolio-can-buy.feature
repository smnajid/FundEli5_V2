@should @reverse-lookup
Feature: Find what a portfolio can buy
  As a Fund Dealer
  I want to check one portfolio against every Share Class in the catalog at once
  So that I can tell the Requester what the client can buy instead of only what they cannot

  The reverse lookup runs the same Eligibility Check as a single request, once
  per Share Class of the catalog, with no LLM. Results are grouped by Verdict;
  Not Eligible is grouped again by the deciding rule.

  Background:
    Given the demo dataset
    And the Validated Rule Cards of the sample funds

  Rule: Each Share Class gets the Verdict a single Eligibility Check would give
    Example: Tom Whitfield - one US citizen, three US Person definitions
      When the Fund Dealer asks what LOC90000201 can buy with CHF 2,000,000
      Then every Share Class of the catalog has a row
      And the rows include:
        | Share Class | Verdict      | deciding rule      |
        | GCE-BP      | Eligible     |                    |
        | GCE-BI      | Not Eligible | investor category  |
        | BNP-A       | Not Eligible | US Person exclusion |
        | BNP-I       | Not Eligible | US Person exclusion |
        | SL-ICAP     | Not Eligible | US Person exclusion |
        | SIF-W       | Not Eligible | Fund Registration  |
      And no LLM call is made

    Example: Classes the engine cannot decide
      When the Fund Dealer asks what LOC90000201 can buy with CHF 2,000,000
      Then BNP-AAC gives Needs Review "Rule Card not validated"
      And the placeholder classes XXNOTCOV0001 and XXNOTCOV0002 give Needs Review "Fund not covered"

  Rule: Not Eligible rows are grouped by their deciding rule
    Example: Tom's Not Eligible rows
      When the Fund Dealer asks what LOC90000201 can buy with CHF 2,000,000
      Then the Not Eligible group has sub-groups for US Person exclusion, Fund Registration and investor category
      And US Person exclusion is the largest sub-group
      And the count line shows how many Share Classes are Eligible, Needs Review and Not Eligible

  @share-class-catalog
  Rule: Search narrows the rows already computed, deterministically, with the catalog's matcher
    Case, accents and punctuation are ignored; every typed word must appear, in any order;
    an ISIN matches exactly or by prefix. Searching never re-runs the engine.

    Scenario Outline: Searching Tom's results for "<search>"
      Given the Fund Dealer has asked what LOC90000201 can buy with CHF 2,000,000
      When the Fund Dealer searches for "<search>"
      Then only the rows of <Share Classes> remain
      And the engine did not run again

      Examples:
        | search               | Share Classes          |
        | climate nordea BI    | GCE-BI                 |
        | LU03489              | GCE-BP, GCE-BI         |
        | bnp chf              | BNP-A, BNP-AAC, BNP-I  |
        | Bnp-Paribas  Bonds   | BNP-A, BNP-AAC, BNP-I  |
