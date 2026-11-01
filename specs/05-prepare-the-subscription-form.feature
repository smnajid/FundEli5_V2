@should @form-checklist @assumed
Feature: Prepare the subscription form
  As a Fund Dealer
  I want a checklist of the declarations and documents the subscription needs, pre-filled from the client's facts
  So that the subscription form is complete the first time

  No subscription form has been provided, so the guide is a checklist generated
  from the Rule Card: each item comes from a rule and carries its Citation, and
  pre-filled values come from the Client Profile, locally. If the bank provides
  its form at the event, the checklist maps onto it.

  Assumed: which items each rule kind produces, who receives the checklist, and
  how it behaves on Not Eligible and Needs Review are open in issue #25. Samuel
  (desk) writes the item wording and accepts the items.

  Background:
    Given the demo dataset
    And the Validated Rule Cards of the sample funds

  Rule: Every checklist item comes from a rule of the Rule Card and cites it
    Example: A well-informed investor subscribing to the SIF
      When LUX90000501 asks to subscribe EUR 120,000 to SIF-W
      Then the checklist includes:
        | item                                        | pre-filled                    |
        | well-informed investor written confirmation | signed 2026-09-15             |
        | minimum initial subscription EUR 100,000    | EUR 120,000 requested         |
        | US Person self-certification                | not a US Person, per the bank |
      And every checklist item cites the rule and the page it comes from

  @privacy-gate
  Rule: Pre-filled values are client data and never reach the LLM
    ADR 0004.

    Example: Building the checklist makes no LLM call
      When LUX90000501 asks to subscribe EUR 120,000 to SIF-W
      Then the checklist is built with no LLM call
