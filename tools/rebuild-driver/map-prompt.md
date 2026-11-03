You are charting the rebuild map of FundEli5_V2 in an unattended run. Mohamed delegated every decision of this session to you and pre-approved the ticket breakdown: never wait for an answer, there is no one to reply. Where the skills below say "ask the user" or "quiz the user", decide from the docs instead and record why in the map's Decisions so far.

Read first: CLAUDE.md, README.md, docs/agents/prompts/v2-rebuild-map.md (the charting brief: destination, notes, scope), docs/playbook/build-workflow.md (spine, waves, the build-ticket template, HITL/AFK modes), docs/playbook/tech-choices.md, docs/playbook/architecture.md, specs/00-fundeli5.feature (module tags and tiers), docs/agents/issue-tracker.md (how the map, sub-issues and native blocking edges are written with gh).

Do, in this order:

1. Create the map issue, labelled `wayfinder:map`, in the wayfinder map format (.agents/skills/wayfinder/SKILL.md). Destination and Notes from the brief; Out of scope from the brief.
2. Every technical choice is already made in tech-choices.md, so create no grilling or research tickets unless a choice blocks a build ticket outright; if you create one, resolve it yourself in this session from the docs and close it with the answer.
3. Create the build tickets as sub-issues of the map, following build-workflow.md's tables. Use its build-ticket template exactly (Goal, Done when, Read first, Seam, Must not, Stop and ask when), with the module's real Cucumber tags from specs/. At the top of every body put two lines:
   `Phase: <Spine|Wave A|Wave B|Integrate|Should>`
   `Mode: <HITL|AFK>`
   Label HITL tickets `ready-for-human` and AFK tickets `ready-for-agent`. Each ticket fits one agent session (about 100K tokens of work); split a row of build-workflow.md if it doesn't.
   Make sure the tickets cover at least: S1 skeleton (with ArchUnit rules and RuntimeProfilesTest as in tech-choices), S2 schemas, ports, fixture adapters and the replay egress, S3 walking skeleton, S4 Rule Engine with the plug-in rule catalog, Scale Mapping and the answer key, the four promises in JUnit, Privacy Gate and Request Parser (with the Claude-plan egress adapter), Share Class catalog, Check Record, explanation templates and Reply Drafter, the Rule Card pipeline (PDF text, Jev page ranking tested against a stubbed HTTP server, Claude-plan drafting, quote check), validation screen, dealer screen, integration with demo-flow-verifier on @must, and a one-command start.
   Add one HITL ticket "Record the four demo funds' drafting answers with a live egress" (Phase: Integrate) that needs TYPESAFE_API_KEY; put `Needs: TYPESAFE_API_KEY on the VM` in its body.
   Put Should-tier tickets (reverse lookup, what-if, form checklist) in the map's Out of scope, not as tickets.
4. Second pass: wire native blocking edges (`gh api --method POST repos/smnajid/FundEli5_V2/issues/<child>/dependencies/blocked_by -F issue_id=<blocker database id>`) so the spine runs S1 → S2 → S3 → S4 one at a time, and each wave's tickets depend only on what they truly need, so independent tickets can run in parallel (at most 3 at once).
5. Fill the map's Decisions so far with one line per decision you made in step 2 or in splitting rows, and list the build tickets in order in the map body.
6. Do not write any code and do not commit anything. Finish by printing the list of tickets: number, title, phase, mode, blocked by.
