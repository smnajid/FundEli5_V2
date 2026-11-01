Feature: FundEli5
  A relationship manager asks the fund dealing desk whether a portfolio may
  subscribe to a fund. FundEli5 answers in seconds with a Verdict (Eligible,
  Not Eligible or Needs Review), a Justification that cites the Fund
  Documentation page behind every rule, and a reply the Fund Dealer can send,
  explained so plainly that the relationship manager can forward it to the
  client. An LLM drafts the Rule Cards and the reply; a person validates every
  Rule Card; deterministic code decides every Verdict. Client identity never
  reaches the LLM, and every check leaves an audit record.

  Read the features in this order:
    01 Check eligibility                 what decides a Verdict, rule by rule
    02 Answer a subscription request     email in, cited reply out, privacy kept
    03 Explain a Verdict                 the plain-language explanation and "what would make it eligible?"
    04 Find what a portfolio can buy     the reverse lookup across every Share Class
    05 Prepare the subscription form     the checklist of declarations and documents
    06 Validate a Rule Card              from a drafted card to one that may decide
    07 Audit a check                     the Check Record
    99 Answer key                        the 26 coverage-set rows with every rule outcome

  Not in this demo (no scenarios):
    - Advisory Suggestions (Requester Questions, Dealer To-dos): cut, production slide only.
    - Free-text questions about a check or a fund: cut, production slide only.
    - LLM rephrasing or translation of the plain-language explanation: cut.
    - Deadline alerts: out of scope, agreed with the sponsor.
    - Suitability, and every regime other than Switzerland and Luxembourg.
    - Checking Orders from the order flow, and reading mailboxes: roadmap only.

  Tags:
    Scope tier (team-plan.md): @must or @should. A cut capability has no scenario.
    Unconfirmed reading: @assumed, with a sentence saying what is assumed and who confirms it.
    The coverage set, rerun before every rehearsal: @answer-key.
    Live model: @live-llm. Every other scenario runs in replay mode.
    Engine modules: @rule-engine @scale-mapping @reverse-lookup @what-if @explanation @form-checklist
    LLM-facing modules: @privacy-gate @request-parser @reply-drafter
    Other modules: @share-class-catalog @rule-card-pipeline @validation @check-record
    Run one module's scenarios by its tag; /pre-merge runs the tags of the modules a change touches.

  Vocabulary: GLOSSARY.md. Why each constraint holds: docs/adr/.
  Cast, conventions, bank scales and the short Share Class names (GCE-BI, SIF-W, ...):
  docs/playbook/demo-dataset.md.
