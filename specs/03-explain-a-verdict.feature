Feature: Explain a Verdict
  As a Requester (relationship manager)
  I want each Verdict explained in plain words, "like I'm five"
  So that I can forward the explanation to the client and say what they could do instead

  The explanation is presentation, not decision: it is rendered from the
  Justification by a fixed template per rule kind, worded by the desk. In this
  demo no LLM rephrases or translates it (cut). "What would make it eligible?"
  re-runs the Rule Engine on a hypothetical; it never asks the LLM.

  Background:
    Given the demo dataset
    And the Validated Rule Cards of the sample funds

  @must @explanation @assumed
  Rule: The explanation is rendered from the Justification by a fixed template per rule kind
    ADR 0002 (note). Assumed: where it sits in the reply and on the screen, and whether a Review
    gets its own template, are open in issue #22. Samuel (desk) writes and accepts the wording.

    Example: Anna's advisory portfolio
      When LOC90000101 asks to subscribe EUR 80,000 to GCE-BI
      Then the explanation says in plain words that the deciding rule is the investor category
      And the explanation names GCE-BP as a class she can buy
      And no LLM call is made

    Example: The same check always gives the same explanation
      When LOC90000101 asks to subscribe EUR 80,000 to GCE-BI twice
      Then both explanations are identical

  @must @explanation
  Rule: The explanation never changes the Verdict
    Example: The explanation follows the Verdict, never the other way round
      When LOC90000101 asks to subscribe EUR 80,000 to GCE-BI
      Then the Verdict is Not Eligible
      And the explanation states the Verdict Not Eligible

  @should @what-if @assumed
  Rule: "What would make it eligible?" re-runs the engine on a hypothetical and shows it as one
    ADR 0007: an answer never changes a Verdict. Assumed: which facts a what-if may change
    (here the mandate and the amount) is open in issue #23.

    Example: Anna's advisory portfolio under a discretionary mandate
      Given LOC90000101 was checked for EUR 80,000 of GCE-BI and is Not Eligible
      When the Fund Dealer asks what would happen with a discretionary mandate
      Then the hypothetical Verdict is Eligible, shown as hypothetical
      And the Verdict of the check stays Not Eligible
      And no LLM call is made

    Example: Row 22 with a slightly larger amount
      Given LOC90001101 was checked for CHF 70,000 of GCE-BI and is Needs Review
      When the Fund Dealer asks what would happen with CHF 71,000
      Then the hypothetical Verdict is Eligible, shown as hypothetical
      And the Verdict of the check stays Needs Review
