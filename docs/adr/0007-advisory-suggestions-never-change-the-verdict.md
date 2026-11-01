---
status: accepted, amends ADR-0005 (advisory LLM lane after the engine)
---

# Advisory Suggestions never change the Verdict; accepted Requester Questions go into the reply

ADR 0005 let the advisory lane downgrade a Verdict, but a downgrade means the AI arguing for a refusal from facts the engine already read: if the rules support Not Eligible the engine says so, and if they don't the AI shouldn't. So the lane only says what to check or which evidence to file, and never changes the Verdict in either direction. Each Advisory Suggestion is a **Requester Question** or a **Dealer To-do**. The Fund Dealer accepts or dismisses each one (dismissal with an optional one-line note), and every decision is appended to the Check Record with who and when. An accepted Requester Question is asked in the reply; an accepted Dealer To-do is only recorded. On a check with suggestions the dealer drafts the reply after deciding, in one Reply Drafter call in the request's language; undecided suggestions are left out with a warning, and changing a decision afterwards offers a redraft that never overwrites the dealer's edits. A check still ends when the reply is sent: the app does not track the follow-up of a Needs Review.

## Considered Options

- **Keep downgrade proposals**: rejected; no demo case uses them and they blur "the AI never decides".
- **Draft the reply at once and redraft on every decision**: one LLM call per click, and the dealer's edits are lost.
- **Append accepted questions to the draft without a new call**: cheap, but the questions stay in the dealer's language inside a reply in the Requester's language.
- **Track a Needs Review to its final answer (a dealer decision beside the Verdict), or require a reason list on dismissal**: worth having in production, where the reason list measures how often the lane is wrong; left for the production slide.
