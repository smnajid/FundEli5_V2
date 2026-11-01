# Timing log

Unattended run on the VM (Standard_E4s_v3), Sonnet 5.5, at most 3 tickets at once. Started 2026-11-01T20:17:16+00:00, map charted 2026-11-01T20:21:20+00:00, ended 2026-11-01T20:58:38+00:00.
Wall time runs from the agent claiming the ticket to the merge. Attempts counts headless sessions, including resumes after a usage limit.

| Ticket | Phase | Mode | Status | Started | Ended | Wall time | Attempts | PR | Note |
|---|---|---|---|---|---|---|---|---|---|
| #2 S1a Skeleton: Spring Boot 4 backend, Cucumber runner, ArchUnit rules, RuntimeProfilesTest | Spine | HITL | built | 2026-11-01T20:22 | 2026-11-01T20:37 | 14 min | 1 | #28 |  |
| #3 S1b Skeleton: Angular shell and generated API client (one npm script) | Spine | HITL | built | 2026-11-01T20:30 | 2026-11-01T20:35 | 5 min | 1 | #29 |  |
| #4 S2a Schemas and ports with fixture adapters | Spine | HITL | built | 2026-11-01T20:36 | 2026-11-01T20:45 | 10 min | 1 | #30 |  |
| #5 S2b LLM egress interface with replay mode | Spine | HITL | built | 2026-11-01T20:45 | 2026-11-01T20:50 | 4 min | 1 | #31 |  |
| #6 S3a Walking skeleton: Anna's flow over HTTP with stubs | Spine | HITL | needs_you | 2026-11-01T20:50 |  | 8 min | 3 |  | agent stuck after 3 attempts |
