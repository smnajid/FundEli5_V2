# Team plan

Resolves [How does a team of 5 run the 20 build hours?](https://github.com/smnajid/fund-gate/issues/11). Replanned on 2026-10-31 with [handover-to-the-team.md](handover-to-the-team.md): the platform is pre-built, and the team rebuilds the demo story, funds, rules and answer key from its own map. Who does what is decided by the team once its map is ready; this plan names no one. The one-slide version for the kickoff is [kickoff-slide.md](kickoff-slide.md).

## The window

| When | What |
|---|---|
| Until Tue 3 Nov, 19:00 | Pre-build on fixtures: the platform tickets and the plug-in rule catalog |
| Mon 2 Nov, 13:30 | The organisers present the infrastructure; the bank repo exists |
| Tue 3 Nov, 17:00 | Remote, 45 min: the team charts its map ([charting prompt](../agents/prompts/team-map.md)) |
| Tue 3 Nov, 19:00 | Pre-event freeze; tag `fallback/pre-event` |
| Wed 4 Nov, day 1 | 9:00–22:00 (13 h) build, then everyone goes home and sleeps |
| Thu 5 Nov, day 2 | 8:00–15:00 (7 h) build; 15:00–16:00 finalise the presentation, no code changes |

No overnight work. Nothing merges after 23:00.

## Scope

The test for every request: does it show in the 5-minute demo? If not, it waits. The Must and Should tiers come from the team's map. What stands until then:

| Tier | What | Rule |
|---|---|---|
| **Platform** (pre-built) | Privacy Gate, LLM egress, Rule Card pipeline and quote check, rule engine frame, Check Record, validation screen, replay mode | Runs on fixtures before the freeze |
| **Must**, **Should** | Decided on the team's map | Must runs in the bank's environment by 13:00 day 1 |
| **Proposed cut** | Advisory lane, free-text Q&A, LLM polish of the explanation, reverse lookup (not the desk's job) | Production slide only, unless the map brings one back |

Three of the four organiser capabilities (explanation, form, questions) are in view. Deadline alerts were agreed out of scope with the sponsor on 2026-10-15 ([#24](https://github.com/smnajid/fund-gate/issues/24)).

## Roles

The demo cannot run without this content and these skills. The team assigns them after the map; one person may hold several.

| Role | Covers | On stage |
|---|---|---|
| Build | Modules, the executable specification in `specs/`, merges | Drives the demo |
| Requester's voice | Request emails in real RM wording; whether the plain-language explanation can be forwarded to a client as written; the reply's tone | The Requester |
| Rule Card Validator | Rule Card validation (before the desk confirms), Fund Registration facts, form checklist content | Answers the jury on fund rules |
| Data | Client Profile fixtures, mapping the CRM and FMD fields onto the [contracts](contracts/), running the answer key in the app as acceptance tester | Answers the jury on data and audit |
| Pitch | The pitch, the business case (the desk's minutes per request, [#14](https://github.com/smnajid/fund-gate/issues/14)), roadmap slides, jury Q&A prep, timekeeping at rehearsals | Opens and closes the presentation |

Ideas that fail the 5-minute test go on the production slide, credited to whoever raised them.

## The desk's 30 minutes

The fund dealing desk gives 30 minutes a day, before and during the event.

- **One queue.** Questions go into [Questions for the desk](https://github.com/smnajid/fund-gate/issues?q=label%3Aask-desk) (label `ask-desk`), or the bank repo's equivalent once it exists.
- **Three questions per slot**, the ones that block the build first, picked the evening before.
- **Answers go into the repo the same day**: a closed question, a glossary or contract edit, an `@assumed` tag removed from `specs/`.

## Day 1 (Wed 4 Nov)

| Time | What | Notes |
|---|---|---|
| Before 9:00, or 9:00–10:00 | Rule strawman and first answer-key rows, if Tuesday's session didn't cover them; split the tickets | |
| Morning | Grill the map's tickets, each closing with its build tickets. Clone into the bank's internal environment; bank adapters one port at a time, each falling back to its fixture adapter | |
| **13:00** | **Checkpoint 1**, lunch together. Cut-off for the team's content | |
| 13:00–18:00 | Must, then Should tier. Run the answer key in the bank's environment; review every Verdict sentence and reply | |
| **18:00** | **Checkpoint 2**, backup video, dinner | |
| 18:00–21:30 | Should tier. **Rehearsal 1 at 20:00** | |
| **21:30** | **Checkpoint 3**: pick the Should items for day 2 | |
| 22:00 | Leave | |

## Day 2 (Thu 5 Nov)

| Time | What | Notes |
|---|---|---|
| 8:00 | 15-min sync | |
| 8:00–11:00 | Finish the picked Should items; rehearse the handovers and the jury Q&A | |
| **11:00** | **Feature freeze** | Unfinished work is hidden, not rushed. Bug fixes only |
| 11:00–15:00 | Harden. `demo-flow-verifier` on `/loop 10m`. Rehearsals at **12:00, 13:30, 14:30**, each followed by a 10-min fix list | Final backup video after the 13:30 rehearsal |
| 15:00 | Code stops | |
| 15:00–16:00 | Finalise the presentation | |

## Checkpoints and cut triggers

Each checkpoint is a 5-minute standing sync: done, next, blocked.

| Checkpoint | Done means | If not |
|---|---|---|
| Tue 3 Nov, 19:00 | Fallback tag, one-command start practised, backup video v0 | Record the video before anything else on day 1 |
| 10:30 day 1 | The app boots in the bank's environment; Foundry answers | Switch to our own Anthropic key; keep every port on fixtures |
| 13:00 day 1 | The team's main flow runs, fixture adapters allowed | The demo runs on the fallback tag; the team's content becomes "what we validated today" |
| 18:00 day 1 | Must tier on validated Rule Cards; backup video recorded | Drop the Should items furthest from done |
| 21:30 day 1 | Should items picked for day 2 | Drop the one furthest from done |
| 11:00 day 2 | Feature freeze | Hide what isn't done |

## Merging

- **Short-lived branches**, in parallel worktrees when agents build two modules at once (`claude --worktree <module>`), merged to `main` after the reviewer agents report nothing. Who merges is decided by the team.
- **Content changes** (template wording, checklist content, fixtures, the examples in `specs/`) go on a branch, by pull request.
- **From the pre-event freeze on, `main` is the demo.** `demo-flow-verifier` (which runs the `@must` scenarios) passes before every merge. A merge that breaks the demo is reverted on the spot, never debugged on `main`.

## Reviewer agents

All live in `.claude/agents/` and load when the folder is trusted. They report; they never edit. Ask by name, for example: *"use the privacy-gate-auditor on my branch's diff against main"*.

| Agent | Checks | When |
|---|---|---|
| `java-reviewer` | Spring Boot layering, security, tests | Any Java change |
| `typescript-reviewer` | Angular types, async, templates | Any Angular change |
| `privacy-gate-auditor` | Client data that could reach the LLM (ADR 0004) | Intake, Privacy Gate, LLM client, Request Parser, Reply Drafter, Check Record; before every rehearsal |
| `engine-purity-reviewer` | A Verdict that isn't deterministic or moves toward Eligible without a validated rule (ADR 0002, 0007) | Rule Engine, rule catalog, Scale Mapping |
| `demo-flow-verifier` | The demo story against the running app | Checkpoints, freeze, rehearsals |

## Keeping the demo alive

- **Replay mode**: one flag serves the Request Parser and Reply Drafter answers recorded at the last rehearsal. The engine still runs live, so the Verdicts stay real.
- **Fallback tag**: `fallback/pre-event`, the frozen build of Tue 3 Nov, 19:00, started with one command.
- **Backup video**: first version on Tue 3 Nov, refreshed at 18:00 day 1 and after the 13:30 rehearsal.
- **Anyone can start the demo**: one documented command on the presenting laptop, practised by at least two presenters.

## Definition of done

The demo is done when all of these hold:

- The Must tier runs cleanly under `demo-flow-verifier`.
- Every row of the team's answer key and every `@must` scenario in `specs/` passes.
- `privacy-gate-auditor` finds nothing.
- A backup video is recorded.
- Two rehearsals finish under 5 minutes.

A Should item counts only when it passes the same checks.

## Rehearsals

A rehearsal is the full presentation as it will happen on stage: same presenters and order, the live app on the presenting laptop with the demo data, the slides, a stopwatch against 5 minutes, and someone playing the jury who asks 2–3 hard questions afterwards. It catches what tests don't: overrunning, confusing click paths, a silent wait on an LLM call, the projector, the handovers.
