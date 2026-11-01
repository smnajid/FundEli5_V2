@must @check-record
Feature: Audit a check
  As an auditor, or a Fund Dealer asked months later why a portfolio was refused
  I want every Eligibility Check recorded with everything it read, decided and sent to an LLM
  So that any Verdict can be explained and reproduced

  One append-only Check Record per Eligibility Check. The dealer screen opens it
  as "What the AI saw".

  Background:
    Given the demo dataset
    And the Validated Rule Cards of the sample funds

  Rule: Every Eligibility Check writes one Check Record with everything needed to reproduce its Verdict
    Example: Anna's email gives two Check Records
      When the Fund Dealer answers Anna's email
      Then two Check Records are written, one for LOC90000101 and one for LOC90000102
      And each Check Record holds:
        | the Subscription Request                  |
        | the Rule Card version                     |
        | the Scale Mapping version                 |
        | the Client Profile fields the check read  |
        | the engine version                        |
        | the Verdict and the Justification         |

  @privacy-gate
  Rule: Every LLM call is recorded with what the LLM saw
    ADR 0004: the record shows that the LLM saw Portfolio Ids only.

    Example: The calls made for Anna's email
      When the Fund Dealer answers Anna's email
      Then the Check Records hold, for each LLM call, the gated prompt, the client fields sent, the response and the model id
      And no recorded prompt contains a Portfolio Reference or a client name

    Example: A refused email records that no LLM was called
      When the Privacy Gate refuses the email naming Anna Steinmann
      Then the Check Record of the request entered by hand holds no LLM call

  Rule: A Check Record is never changed
    Example: A check is run again
      Given a Check Record for LOC90000101 on GCE-BI
      When the same check is run again
      Then a second Check Record is written
      And the first one is unchanged

  @assumed
  Rule: A correction by the Fund Dealer is recorded beside what the AI read
    Assumed: whether editing the parsed request re-runs the check, and how both values are kept,
    are open (screens.md, "The request form").

    Example: The dealer corrects the currency
      Given Anna's email has been answered
      When the Fund Dealer changes the currency of LOC90000101's request from EUR to CHF
      Then the Check Record keeps both the parsed currency EUR and the dealer's CHF, with who and when
