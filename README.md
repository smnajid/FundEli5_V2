# FundEli5 V2: rebuild rehearsal

FundEli5 helps a private bank's fund dealing desk answer "may this client subscribe to this fund?" quickly, with a cited Verdict and a reply. An LLM drafts Rule Cards from public fund documents; a human validates them; a deterministic Rule Engine decides. No client data ever reaches an LLM.

This repo is a rehearsal for the hackathon (Wed 4 – Thu 5 Nov 2026). It starts with **no code**. Everything is rebuilt from the material below, so that we learn how long it takes to build every component, technical ones included, with Claude Code. The original build is [smnajid/fund-gate](https://github.com/smnajid/fund-gate); none of its code is copied here.

## Assumptions

- The team chose the same rules as fund-gate, so `specs/` and `docs/playbook/hypotheses.md` arrive already written and serve as the definition of done. (At the event the team charts its own rules with [team-map.md](docs/agents/prompts/team-map.md).)
- Technical choices are carried over as decisions, not code: [tech-choices.md](docs/playbook/tech-choices.md).
- Claude Code and the app's LLM calls run on the Claude plan; Rule Card page selection uses TypeSafe Jev with its own credits.

## What's here

| Path | What |
|---|---|
| `CLAUDE.md`, `.claude/`, `.agents/skills/` | Agent setup: settings and plugins, the gate agents (`privacy-gate-auditor`, `engine-purity-reviewer`, `demo-flow-verifier`, stack reviewers and build resolvers), the skills (`wayfinder`, `to-tickets`, `tdd`, `implement`, `code-review`, …) |
| `GLOSSARY.md`, `docs/adr/` | Domain vocabulary and the eight decisions, including the four promises |
| `docs/playbook/` | Architecture, screens, contracts with fixtures, demo dataset, hypotheses, build workflow, team plan, pitch, tech choices |
| `specs/` | The executable specification in Gherkin |
| `docs/reference/rule-card-extraction/` | The extraction spike's findings, prompts, Rule Card schema, gold cards, page-recall keys |
| `docs/research/` | Research notes: CH/LU rules, sample funds, LLM extraction, Foundry models, AI tools |
| `fund-documents/` | Public prospectuses and KIDs of the four demo funds |
| `docs/agents/prompts/` | The charting prompts: [v2-rebuild-map.md](docs/agents/prompts/v2-rebuild-map.md) for this rehearsal |
| `docs/exercise/timing-log.md` | Where each ticket's time goes |

## Start

1. Set up the tools: [hackathon-kit.md](docs/agents/hackathon-kit.md) §1–4.
2. In `claude` at the repo root, run `/wayfinder` and paste the prompt from [v2-rebuild-map.md](docs/agents/prompts/v2-rebuild-map.md).
3. Work the map ticket by ticket; log each in the timing log.
