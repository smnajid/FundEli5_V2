## Agent skills

### Issue tracker

Issues are tracked in GitHub Issues through the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Uses the five default labels: needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` plus `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## V2 rehearsal

This repo rebuilds FundEli5 from no code to measure the build time (README.md). Never copy code from smnajid/fund-gate; its docs and issues are fine to read. Technical decisions already made: `docs/playbook/tech-choices.md`. Each merged build ticket appends its row to `docs/exercise/timing-log.md`.

## Hackathon kit

Before merging, run the `privacy-gate-auditor` agent on changes touching an LLM call or client field, and `engine-purity-reviewer` on Rule Engine changes. Setup, plugins and daily moves: `docs/agents/hackathon-kit.md`.
