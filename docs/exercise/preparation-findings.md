# Preparing the starting repo: what the agents used, what they never opened

Source: 98 agent transcripts of the 26-ticket run (`../transcripts`), script `usage.py`, full list `unused.txt`.
Method: file paths in tool calls (exact match) plus a check of globs, Skill and subagent calls. Counts are for files outside backend/ and frontend/.

## Never used (remove from the starting repo)
- Skills: 0 Skill-tool calls in the whole run. `.agents/skills/` (about 40 skills, ~330 KB) and the 40 stubs in `.claude/skills/` were never loaded. `.claude/skills/angular-developer` (41 files, 160 KB) was never read either. Keep at most `wayfinder` (the only one referenced) and the skills the map prompt names.
- `skills-lock.json`, `.claude/THIRD_PARTY_NOTICES.md`, `pipeline.sh`, `start.sh`.
- Docs for the humans, not the build: `docs/playbook/pitch.md`, `team-plan.md`, `kickoff-slide.md`, `handover-to-the-team.md`, `ai-tooling.md`, `client-profile-api.md`; `docs/research/ai-tools-*.md`, `foundry-models.md` (~100 KB).
- Rarely used: `order-flow-integration.md` (4 mentions), `llm-rule-extraction.md` (2).
- About 21 of 33 files in `docs/playbook/contracts` were not opened by exact path (84 KB). Check whether they are the unmodelled ones before removing.
- 13 of 16 files in `docs/reference/rule-card-extraction` (42 KB) and 8 of the 10 `fund-documents` PDFs by exact path (3.4 MB), but PDFs are read through code, so this undercounts. Verify before removing.

## Used (keep)
- CLAUDE.md, GLOSSARY.md, README, ADRs (read by glob `docs/adr/*`, `0004*` 30 times), hackathon-kit, issue-tracker, architecture, build-workflow, tech-choices, screens, demo-dataset, hypotheses, specs, `docs/research/sample-funds.md` (12 mentions), the 7 `.claude/agents` (reviewers were launched 136 times: privacy-gate-auditor 56, java-reviewer 49, engine-purity-reviewer 15, typescript-reviewer 14, demo-flow-verifier 2).

## Missing in the starting repo (add)
- Visual prototype screenshots plus a styling requirement (UI came out unstyled).
- Fund Registration rule ticket in the map; a citations/evidence API ticket; API gaps (suggestions, Share Class dropdown).
- `.gitignore` entry fix: `jev-rankings` must be committed with the recordings for replay tests.
- Remove "Issue #41" and other fund-gate references from specs.
- A decision on personal-data detection (Presidio, languages, Requester names) before the Privacy Gate ticket, which stopped the run.
- Plan-limit handling: run on a plan with enough quota, or expect ~12 h of waiting for ~3 h of agent work.
