# V2 rebuild map: charting prompt

Paste after `/wayfinder` in a fresh Claude Code session at the root of FundEli5_V2, once [the kit](../hackathon-kit.md) checks pass. The goal of this rehearsal is to measure how long it takes to rebuild every technical component from an empty repo. The business rules are the reference ones, already written down (`specs/`, `docs/playbook/hypotheses.md`), so the map carries execution and grills only where a technical decision is open.

For the event version, where the team decides the rules, use [team-map.md](team-map.md) instead.

```
Chart the map for rebuilding FundEli5 from an empty repo, as the hackathon's solo builder.
I am Mohamed, the only person in this session: ask me, never answer for me.

Destination: the @must tier of specs/00-fundeli5.feature passes end to end on fixtures,
@answer-key is green, and Anna's flow runs from the dealer screen (paste email → cited
Verdicts → reply → Check Record), plus a Draft card validated on the validation screen.
Every component is built in this repo: no code is copied from smnajid/fund-gate.

Notes for every session:
- This map carries execution (overrides "plan, don't do"). Decision tickets come first only
  where a technical choice is still open; the rest are build tickets.
- The business is decided: specs/ and docs/playbook/hypotheses.md are the team's rules.
  Agents never edit them (build-workflow.md rule 2). A missing business answer stops the
  ticket; it is not guessed.
- Fixed frame: GLOSSARY.md, docs/adr/ (the four promises), docs/playbook/architecture.md,
  docs/playbook/contracts/ with fixtures, docs/playbook/screens.md.
- Technical choices are already made: docs/playbook/tech-choices.md (stack and versions,
  package per module, ArchUnit rules, the four promises in JUnit, rule plug-ins, LLM egress
  with replay and Claude-plan adapters, Jev page selection, quote check traps, Cucumber
  wiring, generated Angular client). Reopen one only if it blocks; say why.
- LLM access: Claude Code and the app's egress on my Claude plan (claude -p); Jev through
  TYPESAFE_API_KEY. Tests run in replay mode; live calls only under @live-llm.
- The ticket order and template come from docs/playbook/build-workflow.md: spine S1–S4 one
  at a time (skeleton, schemas and ports, walking skeleton, Rule Engine with /tdd), then
  waves of at most 3 worktrees, then integration. Use its build-ticket template verbatim,
  with HITL/AFK modes and gate agents as listed there.
- Measure time: every build ticket ends by appending its row to docs/exercise/timing-log.md
  (start, end, my review minutes, agent rework loops, what slowed it down).
- Skills: /tdd for engine tickets, /implement for AFK tickets, /code-review before merge,
  the gate agents named in CLAUDE.md.

Breadth-first, surface at least: the spine tickets; each wave's module tickets with their
Cucumber tags; the Rule Card pipeline (Jev ranking, Claude-plan drafting, quote check,
recording the four demo funds' drafting answers for replay); the validation screen; the
Angular screens; the integration and demo-flow check; a one-command start.

Out of scope: the advisory lane, reverse lookup, what-if, form checklist (Should tier,
only if time is left); bank adapters; the pitch.

Create the build tickets with /to-tickets once their blocking decisions are closed,
labelled ready-for-agent (AFK) or ready-for-human (HITL), with native blocking edges
following the spine and the waves. Use GLOSSARY.md vocabulary.
```
