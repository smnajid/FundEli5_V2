@must
Feature: Validate a Rule Card
  As a Rule Card Validator
  I want to check each rule an LLM drafted against the page it cites, and correct it
  So that only rules a person has accepted ever decide a Verdict

  The Rule Card pipeline runs offline on public Fund Documentation only. It
  selects the pages that matter, has the LLM draft each rule with its Citation,
  and checks every quote on its page. The result is a Draft Rule Card. The
  validator accepts or corrects each rule, settles every contradiction between
  documents, marks each Unmodelled Restriction Blocking or Noted, and validates
  the card. Only then does it decide Verdicts.

  @rule-card-pipeline
  Rule: The pipeline drafts from public Fund Documentation only, and every quote is found on its cited page
    ADR 0001, ADR 0004. Extraction takes minutes, so cards are drafted before the demo.

    Example: Drafting Nordea 1 Global Climate and Environment
      When the pipeline drafts the Rule Card of Nordea 1 Global Climate and Environment
      Then the Draft Rule Card has a rule for each rule kind the prospectus and KID state
      And every rule carries a quote found on its cited page
      And the pipeline read no Client Profile

    Example: A document without a content hash is refused
      Given a fund document whose metadata has no content hash
      When the pipeline is asked to draft from it
      Then no Rule Card is drafted
      And the pipeline says why

  @rule-card-pipeline
  Rule: The pipeline proposes every Unmodelled Restriction as Blocking
    ADR 0003: an unreviewed clause never passes silently.

    Example: Nordea's "reject or close" clause after drafting
      When the pipeline drafts the Rule Card of Nordea 1 Global Climate and Environment
      Then the "reject or close" clause is an Unmodelled Restriction marked Blocking

  @validation
  Rule: A card can be validated only once every rule is reviewed and every contradiction is settled
    Example: The prospectus and the KID disagree on GCE-BI's investors
      The prospectus says institutional investors only; the KID says all investors.
      Given the Draft Rule Card of Nordea 1 Global Climate and Environment
      Then the screen shows a contradiction on the investor category of GCE-BI
      And the card cannot be validated
      When the validator chooses to follow the prospectus
      And accepts or corrects every other rule
      Then the card can be validated

  @validation
  Rule: Validating records who validated which version and when; versions are superseded, never edited
    Example: A new prospectus edition
      Given Validated version 1 of GCE-BP's Rule Card
      When the pipeline drafts version 2 from a new prospectus edition
      Then version 1 still decides Verdicts
      And version 2 decides Verdicts only once validated
      And version 1 is unchanged

    Example: Marking a clause Noted is recorded
      When the validator marks the "reject or close" clause Noted
      Then the card's history records the marking, who made it and when

  @should @validation
  Rule: A core rule kind the pipeline may have missed gives Needs Review until the validator settles it
    When a large prospectus is sent as selected pages, a Share Class with no rule for investor
    classification, US Person, distribution or minimum is flagged "possibly missed", with the pages not read.

    Example: No minimum found for a class
      Given the Draft Rule Card has no minimum initial subscription rule for GCE-BI
      Then the screen warns "No minimum found for BI-EUR" and lists the pages not read
      And until the validator adds the rule or confirms there is none, checks on GCE-BI give Needs Review
