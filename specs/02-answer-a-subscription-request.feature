@must
Feature: Answer a subscription request
  As a Fund Dealer
  I want to paste a Requester's email and get one Verdict per portfolio and a cited reply
  So that I answer in seconds without retyping the request or reading the prospectus

  The Privacy Gate swaps every Portfolio Reference for its Portfolio Id before
  any LLM call. The Request Parser (LLM) turns the pseudonymised email into
  Subscription Requests, the Rule Engine checks each one with no LLM, and the
  Reply Drafter (LLM) writes the reply in the request's language. The LLM parts
  are asserted by properties, never by wording. Scenarios run in replay mode,
  on the answers recorded at the last rehearsal, unless tagged @live-llm.

  Background:
    Given the demo dataset
    And the Validated Rule Cards of the sample funds

  @privacy-gate
  Rule: Client identity never reaches the LLM
    ADR 0004. The Gate fails closed: a known client name or an unknown Portfolio Reference
    left in the text means no LLM call, and the Fund Dealer enters the request by hand.

    Example: Anna's email reaches the LLM with Portfolio Ids only
      Given the Requester's email:
        """
        From: Marc Aebi (relationship manager)
        To: Fund dealing desk
        Subject: Subscription Nordea Global Climate - LOC90000101, LOC90000102

        Hi desk,
        The client would like to put EUR 80,000 into the Nordea Global Climate fund, institutional class,
        from portfolios LOC90000101 and LOC90000102. Can you confirm they can subscribe?
        Thanks, Marc
        """
      When the Fund Dealer asks FundEli5 to answer it
      Then the Privacy Gate replaces LOC90000101 and LOC90000102 by their Portfolio Ids
      And no text sent to the LLM contains a Portfolio Reference or a client name
      And the screen shows "the LLM saw Portfolio Ids only"

    Example: An email naming the client makes no LLM call
      Given the Requester's email:
        """
        From: Marc Aebi
        Subject: Nordea Global Climate for Anna Steinmann

        Hi desk, can Anna Steinmann (LOC90000101) subscribe EUR 80,000 to Nordea Global Climate BP-EUR?
        """
      When the Fund Dealer asks FundEli5 to answer it
      Then no LLM call is made
      And the screen says no AI was used for this email, and shows the client name it found
      And the Fund Dealer gets the request form to fill by hand

    Example: An email with a Portfolio Reference the bank does not know makes no LLM call
      Given the Requester's email:
        """
        Hi desk, can LOC90009999 subscribe EUR 80,000 to Nordea Global Climate BP-EUR?
        """
      When the Fund Dealer asks FundEli5 to answer it
      Then no LLM call is made
      And the Fund Dealer gets the request form to fill by hand

    Example: A request entered by hand is still checked and answered from a template
      Given the Privacy Gate refused the email naming Anna Steinmann
      When the Fund Dealer enters LOC90000101, GCE-BP, EUR 80,000, not client initiated
      Then the Verdict is Eligible
      And the reply is the template for Eligible, with no LLM call

    Example: The LLM receives only the client facts its call needs
      When the Reply Drafter drafts the reply to Anna's email
      Then every client fact sent is on the allow-list
      And the Check Record lists exactly the fields sent

  @request-parser @share-class-catalog
  Rule: Each Portfolio Reference and each Share Class named gives its own Eligibility Check
    A fund named without a Share Class means every class of that fund in the catalog.

    Example: Anna - two portfolios, one class
      Given Anna's email
      When the Fund Dealer asks FundEli5 to answer it
      Then these Eligibility Checks run:
        | portfolio   | Share Class | amount     | Verdict      |
        | LOC90000101 | GCE-BI      | EUR 80,000 | Not Eligible |
        | LOC90000102 | GCE-BI      | EUR 80,000 | Eligible     |

    Example: Claire - a fund named without a class, in French
      Given the Requester's email:
        """
        De : Sophie Lentz (conseillère)
        À : Fund dealing desk
        Objet : Souscription Nordea Global Climate - LUX90000301

        Bonjour,
        Ma cliente (LUX90000301) souhaite investir 20 000 EUR dans Nordea 1 Global Climate and Environment.
        Pouvez-vous valider ?
        Merci, Sophie
        """
      When the Fund Dealer asks FundEli5 to answer it
      Then these Eligibility Checks run:
        | portfolio   | Share Class | amount     | Verdict      |
        | LUX90000301 | GCE-BP      | EUR 20,000 | Needs Review |
        | LUX90000301 | GCE-BI      | EUR 20,000 | Not Eligible |

  @request-parser
  Rule: What the request leaves unclear gives Needs Review, never a guess
    Example: Row 26 - amount to follow
      Given the Requester's email:
        """
        From: Marc Aebi
        Subject: LOC90000102 - Nordea Global Climate BI-EUR

        Please check whether LOC90000102 can subscribe to Nordea Global Climate BI-EUR. Amount to follow.
        """
      When the Fund Dealer asks FundEli5 to answer it
      Then the Eligibility Check of LOC90000102 on GCE-BI has no amount
      And the Verdict is Needs Review
      And the minimum initial subscription rule gives Review

  @request-parser
  Rule: The Fund Dealer may correct what the AI read, never what code found
    The Portfolio Reference is found by a pattern and the CRM, so a wrong one means fixing the email.
    The Share Class, amount, currency and client-initiated flag come from the LLM, so they are editable.

    Example: The parsed request form
      Given Anna's email has been answered
      Then in the parsed request form these fields are editable:
        | field               | editable |
        | Portfolio Reference | no       |
        | Share Class         | yes      |
        | amount              | yes      |
        | currency            | yes      |
        | client initiated    | yes      |

  @reply-drafter
  Rule: The reply cites the deciding rules, names portfolios by Portfolio Reference and uses the request's language
    The Portfolio Ids are swapped back to Portfolio References locally, after the LLM call.

    Example: Anna's reply
      Given Anna's email
      When the Fund Dealer asks FundEli5 to answer it
      Then the drafted reply names LOC90000101 and LOC90000102 and contains no Portfolio Id
      And the drafted reply gives each portfolio its Verdict
      And the drafted reply cites the investor category rule for LOC90000101, with its page
      And the drafted reply suggests GCE-BP for LOC90000101

    Example: Claire's reply is in French and recommends BP-EUR
      Given Claire's email
      When the Fund Dealer asks FundEli5 to answer it
      Then the drafted reply is in French
      And the drafted reply recommends GCE-BP

    Example: The LLM is unavailable
      Given the LLM egress client cannot reach the model
      When the Fund Dealer answers Anna's email entered by hand
      Then the Verdicts are the same as with the LLM
      And the reply is the template for each Verdict

    @live-llm
    Example: Anna's email against the live model
      Given Anna's email
      When the Fund Dealer asks FundEli5 to answer it
      Then no text sent to the LLM contains a Portfolio Reference or a client name
      And the drafted reply names LOC90000101 and LOC90000102 and contains no Portfolio Id
      And the drafted reply cites the investor category rule for LOC90000101, with its page

  @should @rule-engine
  Rule: A structured Subscription Request gets its Verdict with no LLM at all
    The same check the dealer's form runs, for a caller that already knows the portfolio, ISIN and amount.
    It is the first step of the order flow roadmap (order-flow-integration.md).

    Example: Anna's advisory portfolio, by ISIN
      When a structured Subscription Request arrives for LOC90000101, ISIN LU0348927095, EUR 80,000
      Then the Verdict is Not Eligible
      And the investor category rule gives Fail
      And no LLM call is made
