# Timing log

Unattended run, Sonnet 5.5, at most 3 tickets at once. Tickets #2 to #27 ran on a VM (Standard_E4s_v3 for most of the run), the UI tickets #53 to #55 on the laptop (2 at once). Started 2026-11-01T20:17:16+00:00, map charted 2026-11-01T20:21:20+00:00, last ticket built 2026-11-03T00:52 (about 28 h elapsed, of which roughly 3 h was agent work; the rest was plan-limit waits and the VM being stopped). Tickets #56 and #61 were created after the run.
Wall time runs from the agent claiming the ticket to the merge. Attempts counts headless sessions, including resumes after a usage limit.

| Ticket | Phase | Mode | Status | Started | Ended | Wall time | Attempts | PR | Note |
|---|---|---|---|---|---|---|---|---|---|
| #2 S1a Skeleton: Spring Boot 4 backend, Cucumber runner, ArchUnit rules, RuntimeProfilesTest | Spine | HITL | built | 2026-11-01T20:22 | 2026-11-01T20:37 | 14 min | 1 | #28 |  |
| #3 S1b Skeleton: Angular shell and generated API client (one npm script) | Spine | HITL | built | 2026-11-01T20:30 | 2026-11-01T20:35 | 5 min | 1 | #29 |  |
| #4 S2a Schemas and ports with fixture adapters | Spine | HITL | built | 2026-11-01T20:36 | 2026-11-01T20:45 | 10 min | 1 | #30 |  |
| #5 S2b LLM egress interface with replay mode | Spine | HITL | built | 2026-11-01T20:45 | 2026-11-01T20:50 | 4 min | 1 | #31 |  |
| #6 S3a Walking skeleton: Anna's flow over HTTP with stubs | Spine | HITL | built | 2026-11-01T20:50 | 2026-11-01T21:43 | 52 min | 1 | #32 | resumed after the 5-hour plan limit (20:55-21:30 UTC) |
| #7 S3b Walking skeleton: bare dealer screen | Spine | HITL | built | 2026-11-01T21:40 | 2026-11-01T21:44 | 3 min | 1 | #33 |  |
| #8 S4a Rule Engine frame and rule plug-in catalog | Spine | HITL | built | 2026-11-01T21:44 | 2026-11-01T21:58 | 14 min | 1 | #34 |  |
| #12 Privacy Gate: Portfolio Reference to Portfolio Id, fail-closed guard, fact allow-list | Wave A | HITL | built | 2026-11-01T21:44 | 2026-11-01T22:19 | 35 min | 2 | #36 | stopped: needs Mohamed |
| #13 Share Class catalog: mention or ISIN to Share Class(es) | Wave A | AFK | built | 2026-11-01T21:44 | 2026-11-01T22:05 | 21 min | 2 | #35 | stopped: needs Mohamed |
| #9 S4b Rule kinds: investor category, US Person, minimum subscription, mandate-only class, KID required | Spine | HITL | built | 2026-11-01T21:57 | 2026-11-01T22:17 | 20 min | 1 | #37 |  |
| #14 Check Record: append-only record of everything a check read and produced | Wave A | AFK | built | 2026-11-01T22:12 | 2026-11-01T22:25 | 13 min | 1 | #38 |  |
| #10 S4c Rule kinds: Unmodelled Restriction, Target Market, and Scale Mapping | Spine | HITL | built | 2026-11-01T22:18 | 2026-11-01T22:31 | 13 min | 1 | #40 |  |
| #15 Claude-plan egress adapter (`claude -p`) | Wave A | HITL | built | 2026-11-01T22:19 | 2026-11-01T22:34 | 15 min | 1 | #39 |  |
| #21 Dealer screen (Angular) | Wave B | AFK | built | 2026-11-01T22:25 | 2026-11-01T22:35 | 10 min | 1 | #41 |  |
| #11 S4d Answer key green and the four promises in JUnit | Spine | HITL | built | 2026-11-01T22:31 | 2026-11-02T02:58 | 4 h 27 | 2 | #43 | waiting for plan usage limit |
| #16 Request Parser on the Privacy Gate output | Wave A | HITL | built | 2026-11-01T22:33 | 2026-11-02T02:56 | 4 h 23 | 3 | #42 | stopped: needs Mohamed |
| #17 Explanation templates keyed by rule kind and outcome | Wave B | AFK | built | 2026-11-02T02:58 | 2026-11-02T03:09 | 10 min | 1 | #44 |  |
| #19 Validation module: Draft to Validated Rule Card with versions | Wave B | AFK | built | 2026-11-02T02:58 | 2026-11-02T03:27 | 28 min | 1 | #46 |  |
| #22 Rule Card pipeline 1: pipeline profile, PDF page text and quote check | Wave B | AFK | built | 2026-11-02T02:58 | 2026-11-02T03:15 | 17 min | 1 | #45 |  |
| #18 Reply Drafter with template fallback | Wave B | HITL | built | 2026-11-02T03:10 | 2026-11-02T03:40 | 30 min | 2 | #48 | stopped: needs Mohamed |
| #23 Rule Card pipeline 2: Jev page ranking against a stubbed HTTP server | Wave B | AFK | built | 2026-11-02T03:16 | 2026-11-02T03:31 | 15 min | 1 | #47 |  |
| #20 Validation screen (Angular) | Wave B | AFK | built | 2026-11-02T03:27 | 2026-11-02T03:50 | 23 min | 2 | #49 | stopped: needs Mohamed |
| #24 Rule Card pipeline 3: Claude-plan drafting and Draft Rule Card output | Wave B | AFK | built | 2026-11-02T03:32 | 2026-11-02T03:54 | 23 min | 1 | #50 |  |
| #26 One-command start of the whole app | Integrate | AFK | built | 2026-11-02T03:50 | 2026-11-02T07:52 | 4 h 02 | 1 | #51 | VM resized to E2s_v3 mid-session; resumed |
| #25 Record the four demo funds' drafting answers with a live egress | Integrate | HITL | built | 2026-11-02T03:51 | 2026-11-02T11:34 | 7 h 43 | 2 | #52 | VM resized to E2s_v3 mid-session; resumed |
| #27 Integrate: merge and demo-flow-verifier on @must | Integrate | HITL | built | 2026-11-02T11:35 | 2026-11-02T12:25 | 50 min | 2 | #57 | stopped: needs Mohamed |
| #53 Style the dealer screen like the chosen design | Polish | AFK | built | 2026-11-02T23:34 | 2026-11-02T23:40 | 7 min | 1 | #58 |  |
| #54 Style the validation screen like the chosen design | Polish | AFK | built | 2026-11-02T23:34 | 2026-11-03T00:51 | 1 h 17 | 2 | #59 | stopped: needs Mohamed |
| #55 Citations on outcomes and the evidence rail | Polish | AFK | built | 2026-11-02T23:41 | 2026-11-03T00:52 | 1 h 10 | 1 | #60 |  |
