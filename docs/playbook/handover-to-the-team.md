# Handover to the team

Decided on 2026-10-31. The pre-build retired the technical risks. The team now rebuilds everything the business may change, so it owns the project and a business change costs a rewrite of content, not a refactoring. This supersedes the parts of [team-plan.md](team-plan.md) and [hypotheses.md](hypotheses.md) that assume the reference content is the demo.

## What is kept, what the team rebuilds

| Layer | What | Fate |
|---|---|---|
| Platform | Spikes, ADRs, architecture, skeleton, Privacy Gate, LLM egress, Rule Card pipeline and quote check, Check Record, Share Class catalog, contracts and adapters, the engine frame (Verdict combination, Citations, Draft vs Validated, class overrides fund, contradictions, Stale cards) | Kept |
| Rule catalog | The rule kinds, one plug-in class each: evaluator, condition, prompt fragment | Kept as a mechanism; the kinds and readings follow the team's rules |
| Content | Demo story and cast, funds, Rule Card contents, rule readings, answer key, emails, wording, checklist, pitch | Rebuilt by the team. Today's content becomes the reference, used only to compare against the team's |

The platform code is kept rather than rebuilt at the event: a rebuild would spend most of the about 10 build hours left after the map, the move into the bank's environment and the rehearsals, and bring untested code back on demo day. Every module sits behind a port, so the team may put "rebuild module X" on its map as a ticket like any other.

## Timeline

| When | What |
|---|---|
| Sat 31 Oct – Tue 3 Nov 19:00 | On GitHub: plug-in rule catalog; finish #60, #62, #65, #61, #66, #71; generic #59 (Scale Mapping) and #63 (templates); close #68; park #69, #70. Content tickets wait for the team's map |
| Mon 2 Nov 13:30 | Organisers present the infrastructure; the bank repo exists |
| Mon 2 Nov evening | Import the frame into the bank repo: `.claude/`, `docs/agents/` (tracker doc rewritten for the bank's tracker and tested), `GLOSSARY.md`, `docs/adr/` |
| Tue 3 Nov 17:00 | Remote, 45 min: the team charts the map with `/wayfinder` and [the charting prompt](../agents/prompts/team-map.md). 10 min to check anyone can start the app |
| Tue 3 Nov 19:00 | Freeze. Tag `fallback/pre-event`, with a one-command start that at least two presenters practise |
| Tue 3 Nov evening | Import the code as one commit, as a clean break (below). The GitHub repo stays public and read-only, with a README line pointing to the bank repo |
| Wed 4 Nov, the hour before (else the first hour) | Rule strawman and first answer-key rows, if Tuesday's session couldn't cover them |
| Day 1 | The team grills the map's tickets; each closes with its build tickets (`/to-tickets`). Who takes what is split once the map is ready |
| Day 1, 13:00 | Cut-off: if the team's content can't carry the demo, it runs on the fallback tag |

## Rules of the rebuild

- **The map comes first.** The rule list and the answer key are tickets on it: the answer key proves the team's story, so it follows it.
- **Clean break: the bank repo never runs the reference.** The old Gherkin specs and their step classes stay on GitHub; `specs/` starts empty and the team's specs get their own steps. The platform's JUnit tests read frozen test-only fixtures under `src/test/resources`, never the demo content. The app's content folder starts with the reference fixtures as disposable seed data that no test reads: what the team keeps, it adopts and covers with its specs; the rest is deleted. Old and new never have to agree.
- **The four promises are guarded in JUnit, not Gherkin.** Before the freeze, the machinery checks that live only in Gherkin move to JUnit on the test-only fixtures: Privacy Gate fails closed, a Check Record per check, Verdict combination, a Draft card gives Needs Review, every outcome cited.
- **The reference stays on GitHub, at the tag, until a ticket's comparison step**, then serves as a checklist of edge cases (US citizenship under Reg S, art. 174 mandates, reverse solicitation). Anna, already shown, is presented as the strawman to beat.
- **New funds** come from the bank's shelf, with public prospectus and KID downloaded (DEV FMD serves mock documents). Dry-run the pipeline on them before the event if the team names them in time.
- **Answer-key gate.** Until Tuesday's freeze, the reference key on GitHub guards the plug-in refactor. In the bank repo, the team's key is the only gate.
- **Sign-off.** A row stays `@assumed` until the desk confirms it; the demo avoids unconfirmed rows.
- **Pitch.** "The platform was pre-built; the business wrote and validated the rules and the cases at the event."
- **Out of scope.** The reverse lookup is not the desk's job: a production slide only ("the engine is a pure function, other teams can ask what a portfolio can buy").

## Invitation for Tuesday

A short message ("Tuesday we draw the map of what we build, together; bring your view of how the desk works") with a half-page pre-read: the four promises and the organisers' four capabilities. No Anna recording.
