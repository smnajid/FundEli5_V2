# AI tooling research: ECC, pstack and other cursor/plugins for a 36h Claude Code hackathon

Researched 2026-10-08 against the repos' own files. Versions read:

- ECC at tag `v2.2.3` (released 2026-10-01), commit `c05b2d6` ([releases][ecc-rel], [VERSION][ecc-version]).
- cursor/plugins at `main` commit `ccb5507` ([repo][cp-repo]). pstack manifest version `0.15.15` ([plugin.json][ps-manifest]).

Context: 5 people, Claude Code only, Claude on Microsoft Foundry, fund-eligibility app with a privacy gate. The repo already ships ~37 Matt Pocock skills plus three project agents (`privacy-gate-auditor`, `engine-purity-reviewer`, `demo-flow-verifier`) in `.claude/`.

Link shorthand: `ECC:` links point to `github.com/affaan-m/ECC/blob/v2.2.3/...`, `CP:` links point to `github.com/cursor/plugins/blob/ccb5507.../...`. All are listed at the bottom.

---

## TL;DR recommendation

| Candidate | Verdict |
|---|---|
| ECC whole install (any profile, `install.sh` or `npx ecc-universal setup`) | **Skip** |
| ECC agents/skills | **Cherry-pick 3 to 5 files by hand** (stack-dependent list below) |
| pstack `create-verification-skill` | **Port** (light) |
| pstack `blast-radius` | **Port** (light) |
| pstack `interrogate` | **Port** (medium), only if someone owns it before the event |
| pstack `unslop` | **Port** (copy as is) for pitch and README text, optional |
| pstack `arena`, `architect`, `no-comments`, `poteto-mode`, `maintain-verification-skill`, principles | **Skip** (borrow ideas only) |
| cursor-team-kit `deslop` + `verify-this` | **Port** (copy as is), optional |
| Thermos | Optional port of the one review subagent. Otherwise skip |
| Orchestrate, Advisor, Ralph Loop, dyl-stack build-figma | **Skip** |

---

## 1. ECC (affaan-m/ECC)

### Facts

- **Version and license.** Latest tag `v2.2.3`, published 2026-10-01 ([releases][ecc-rel]). The license is MIT, "Copyright (c) 2026 Affaan Mustafa" ([LICENSE][ecc-license]). The npm package name is `ecc-universal` 2.2.3. Its bins are `ecc`, `ecc-install`, `ecc-universal` and others ([package.json][ecc-pkg]).
- **Size.** The plugin manifest advertises "68 agents, 293 skills, 94 legacy command shims" ([plugin.json][ecc-plugin]).
- **`install.sh`** is a wrapper. On a git clone it runs `npm install --ignore-scripts` when `node_modules` is missing, then `exec node scripts/install-apply.js "$@"` ([install.sh][ecc-installsh]). `--target claude` installs into `~/.claude/` (user-global) and `--target claude-project` installs into `./.claude/`. The installer supports `--dry-run`, `--profile`, `--modules`, `--with/--without <component>` and `--skills <ids>` ([install-apply.js][ecc-installapply]).
- **Profiles** ([install-profiles.json][ecc-profiles]):
  - `minimal`: `rules-core, agents-core, commands-core, platform-configs, workflow-quality`. The manifest describes it as "...but no hook runtime".
  - `core`: the same modules plus `hooks-runtime`.
- **What `--profile minimal --target claude` actually writes.** I ran a dry run with `HOME` set to a scratch directory. The plan has **400 file operations** across these selected modules: `rules-core, agents-core, commands-core, platform-configs, skill-unified-memory, workflow-quality`. Note that `skill-unified-memory` is pulled in implicitly. The files are:
  - 122 rule files into `~/.claude/rules/ecc/` (all 22 language folders plus `common/`).
  - All 68 agents into `~/.claude/agents/`.
  - 94 commands into `~/.claude/commands/`. They include `/plan`, `/pr`, `/code-review` and `/tdd`-style names that **collide with this repo's existing skills** (`pr`, `code-review`).
  - 47 workflow skills, `mcp-configs/mcp-servers.json`, `plugin.json` and `marketplace.json`.

  Module paths: [install-modules.json][ecc-modules].
- **No hooks in minimal.** Neither the dry run nor `settings.json` included any hook in the minimal plan. With `--profile core`, the plan maps `hooks/hooks.json -> ~/.claude/settings.json` and refuses to apply without an explicit `--enable-hooks` or `--no-hooks` ([install-apply.js][ecc-installapply], `core` dry-run output).
- **Always-on context cost of minimal (my estimate; Claude Code itself was not measured).**
  - Claude Code loads every `.claude/rules/**/*.md` without `paths:` frontmatter at launch. Rules with `paths:` load only when a matching file is read or edited ([Claude Code memory docs][cc-memory]). `~/.claude/rules/` applies to every project on the machine ([cc-memory][cc-memory]).
  - In ECC, 111 of the 122 rule files carry `paths:`. The 11 unconditional ones are `rules/common/*.md` (18,377 bytes) and `rules/README.md` (5,694 bytes). That is about **24 KB, roughly 6k tokens, on every turn in every project** ([rules/common][ecc-rules-common]).
  - Skill, agent and command descriptions stay in context unless the skill sets `disable-model-invocation: true` ([Claude Code skills docs][cc-skills]). The installed agent, skill and command descriptions total about 47.6 KB, roughly **12k tokens** (computed from frontmatter).
  - **Total ≈ 18k tokens always on**, plus path-scoped rules when they trigger: java ≈ 15 KB, typescript ≈ 7 KB, web ≈ 16 KB ([rules/java][ecc-rules-java], [rules/typescript][ecc-rules-ts], [rules/web][ecc-rules-web]).
- **The common rules are opinionated in ways that will fight a 36h build.**
  - "Minimum Test Coverage: 80%" ([testing.md][ecc-rule-testing]).
  - "Immutability (CRITICAL) ALWAYS create new objects, NEVER mutate" ([coding-style.md][ecc-rule-style]).
  - A model-selection policy in [performance.md][ecc-rule-perf].
- **Cherry-picking through the installer is not granular.**
  - `--skills tdd-workflow` resolved to the whole `workflow-quality` module plus `platform-configs` (113 operations).
  - `--with agent:security-reviewer` resolved to all of `agents-core` (158 operations).
  - Components map to whole modules ([install-components.json][ecc-components]). **Copying individual raw files by hand is the only real cherry-pick.**
- **`npx ecc-universal setup`** runs `scripts/setup.js`. The only mode is `claude-plugin`, which installs the `ecc@ecc` Claude Code plugin from the `affaan-m/ECC` marketplace at user, project or local scope, with `--hooks off|minimal|standard|strict` ([setup.js][ecc-setup], [claude-plugin-setup.js][ecc-plugin-setup]). The plugin manifest registers all of `./skills/` and `./commands/`. Its default `hook_profile` is `standard` and its default `hooks_enabled` is `true` ([plugin.json][ecc-plugin]).
- **Hooks (core profile or plugin).** The hooks are defined in [hooks.json][ecc-hooks] and described in [hooks.metadata.json][ecc-hooks-meta]. The default profile is `standard` (`ECC_HOOK_PROFILE`, [hook-flags.js][ecc-hookflags]). Hooks that run under `standard`:
  - **PreToolUse**
    - `pre:bash:dispatcher` (quality, tmux, push and GateGuard checks).
    - **`gateguard-fact-force`**: "block first Edit/Write/MultiEdit per file and demand investigation" ([gateguard-fact-force.js][ecc-gateguard]). This is real friction in a hackathon.
    - `doc-file-warning`.
    - `suggest-compact`.
    - `observe` (continuous-learning observation capture).
    - `governance-capture`.
    - `config-protection`: blocks edits to linter and formatter configs.
    - `mcp-health-check`: makes an HTTP/HTTPS request to MCP servers ([mcp-health-check.js][ecc-mcphealth]).
  - **PreCompact**: `pre-compact`.
  - **SessionStart**: loads the previous session context and plan-canvas sessions.
  - **PostToolUse**: dispatchers covering a quality gate, design-quality check, metrics bridge, context monitor and continuous-learning observe ([posttooluse-dispatcher.js][ecc-postdispatch]).
  - **Stop**: format/typecheck, console.log check, `session-end`, `evaluate-session`, `cost-tracker` (writes `~/.claude/metrics`, [cost-tracker.js][ecc-cost]) and `desktop-notify`.
  - **SessionEnd**: marker.
- **Privacy-relevant hook behaviour.**
  - **`session-end.js` launches an extra LLM call over the session transcript.** It fires when context drops below 20% or every 50 messages ([session-end.js][ecc-sessionend], L249-262). It runs `spawnSync('claude', ['--model', 'haiku', '-p'])` ([llm-summary.js][ecc-llmsummary]). That call goes through whatever provider `claude` is configured for, so here it would go to Foundry, not a new third party. It still means transcript content, which could include pasted client data, is re-sent outside the main conversation. On Foundry the `haiku` alias also needs `ANTHROPIC_DEFAULT_HAIKU_MODEL` or the call fails ([Foundry docs][cc-foundry]).
  - The continuous-learning observer agent is **disabled by default** (`"observer": {"enabled": false}`, [config.json][ecc-cl-config]). Observation capture to local files is on under `standard`.
  - I found no telemetry endpoint in `scripts/hooks` or `scripts/lib`. The only outbound HTTP in hooks is the MCP health probe and a localhost Plan Canvas call. `skill-run-tracker` "telemetry" is local ([grep of scripts/hooks][ecc-hooks-dir]).
  - The installer prints a sponsor link (`compute.itomarkets.com`) but does not call it ([compute-sponsor.js][ecc-sponsor]).
- **AgentShield.**
  - It is a separate MIT repo ([affaan-m/agentshield][as-repo]) published as npm `ecc-agentshield` 1.6.0, which depends on `@anthropic-ai/sdk` ([package.json][as-pkg]).
  - The `security-scan` skill runs `npx ecc-agentshield scan` to audit `.claude/` (CLAUDE.md, settings, MCP, hooks, agents) for injection and misconfiguration ([security-scan SKILL.md][ecc-secscan]).
  - The `--opus` mode needs `ANTHROPIC_API_KEY`, so it calls Anthropic directly, not Foundry ([security-scan SKILL.md][ecc-secscan]).
  - Running the static mode once with `npx` on our `.claude/` before the event is harmless, but low value.
- **Model fields.** ECC agents hard-code `model: sonnet` or `model: opus` (for example [planner.md][ecc-planner] and [java-reviewer.md][ecc-java-rev]). On Foundry, aliases resolve to built-in defaults unless you set `ANTHROPIC_DEFAULT_*_MODEL`, and unavailable defaults fail at request time ([Foundry docs][cc-foundry]). **When copying, change these to `model: inherit`** ([subagent docs][cc-subagents]).

### Item-by-item for our 36h

| ECC item | Size | What it adds vs our skills | Verdict |
|---|---|---|---|
| [`agents/java-build-resolver.md`][ecc-java-build] | 12 KB | Maven/Gradle/Spring/Lombok compile-fix playbook with "minimal changes" discipline. `diagnosing-bugs` is a general loop with no build-tool knowledge. | **Cherry-pick if Java** |
| [`agents/build-error-resolver.md`][ecc-build] | 4.7 KB | Same for `tsc`/bundler errors. | **Cherry-pick if TypeScript** |
| [`agents/java-reviewer.md`][ecc-java-rev] | 13 KB | Spring layering, JPA, security, concurrency checklist. Our `code-review` checks repo standards and spec, not Java idioms. Drop the Quarkus/Mongo sections. | **Cherry-pick if Java** |
| [`agents/typescript-reviewer.md`][ecc-ts-rev] / [`react-reviewer.md`][ecc-react-rev] | 9 / 11 KB | Type-safety, async, hooks and render checklist. | **Cherry-pick one if TS/React UI** |
| [`skills/springboot-verification/SKILL.md`][ecc-sb-verif] | 6 KB | One-shot build, static analysis, tests, dependency/secret scan, PASS/FAIL. Useful before each integration checkpoint. | **Cherry-pick if Java** |
| [`skills/e2e-testing/SKILL.md`][ecc-e2e-skill] | 8 KB | Playwright POM, config and flaky-test patterns for the dealer/validation UI. | Optional |
| [`agents/e2e-runner.md`][ecc-e2e] | 5 KB | Prefers `npm install -g agent-browser` (a global install), with Playwright as the fallback. Overlaps our `demo-flow-verifier`. | Skip (take the skill instead) |
| [`agents/security-reviewer.md`][ecc-sec] | 5.4 KB | Generic OWASP list that is npm-centric (`npm audit`, `express-rate-limit`). Our `privacy-gate-auditor` plus the built-in `/security-review` cover the real risk. | Skip |
| [`agents/silent-failure-hunter.md`][ecc-silent] | 2 KB | Swallowed errors and bad fallbacks. Relevant to "Verdict drifts to Eligible on error", but `engine-purity-reviewer` already owns that. | Optional |
| [`agents/tdd-guide.md`][ecc-tdd] | 3.9 KB | Enforces 80%+ coverage. Duplicates our `tdd`. | Skip |
| [`agents/planner.md`][ecc-planner] | 8 KB | Generic plan. We have `to-spec`, `to-tickets`, `wayfinder` and `grilling`. | Skip |
| [`skills/regex-vs-llm-structured-text/SKILL.md`][ecc-regex] | 6.7 KB | Regex-first plus confidence scorer plus LLM-only-for-edge-cases. This is directly relevant to the offline PDF extractor design. Read it, don't install it. | Reference only |
| [`skills/nutrient-document-processing`][ecc-nutrient] | n/a | Sends documents to the Nutrient DWS API. **Privacy violation for client PDFs.** | **Never** |
| UI/frontend: [`frontend-patterns`][ecc-fe], [`a11y-architect`][ecc-a11y] | 15 / 7 KB | Generic React/Next guidance. | Skip unless the UI team asks |

---

## 2. pstack (cursor/plugins/pstack)

**License:** MIT, "Copyright (c) 2026 Lauren Tan" ([LICENSE][ps-license]). Attribution means keeping that copyright line and the MIT permission notice with any copied or substantially derived file. A header block in each ported SKILL.md plus a `THIRD_PARTY_NOTICES` entry satisfies this ([LICENSE][ps-license]).

**General Cursor-isms**:
- `Task` tool with `subagent_type`, `model` slugs (`claude-opus-5-5-xhigh`, `grok-4.7-xhigh-fast`), `readonly: true` and `run_in_background`.
- A model config file at `~/.cursor/rules/pstack-models.mdc`.
- `.cursor/skills/` paths.
- `AskQuestion`.
- Cursor "mode" frontmatter (`mode`, `icon`, `color`, `reminder`).
- Dependencies on `cursor-team-kit` skills.

Claude Code equivalents:
- Agent tool `model` takes `sonnet|opus|haiku|fable|inherit` or a full ID.
- `isolation: worktree` and background subagents exist ([subagent docs][cc-subagents]).
- `disable-model-invocation: true` is supported and keeps the description out of context.
- Unknown frontmatter fields are silently ignored ([skills docs][cc-skills]).

**On Foundry the team only has Claude models**, so pstack's "diversity comes from model families (Claude vs Grok)" premise weakens to "Opus vs Sonnet" ([interrogate][ps-interrogate]).

| Skill | What it does | Cursor-specificity | Port effort for Claude Code | Verdict |
|---|---|---|---|---|
| [arena][ps-arena] | N parallel candidates write to worktrees or `/tmp`, a cross-family judge scores against a rubric, then pick a base and graft. | High. Runner models come from `pstack-models.mdc` with Grok defaults. It also references the principle skills. | Medium. Replace the model lines with `opus`/`sonnet`, use `isolation: worktree`, inline the 3 principle references. | Skip. It costs N× tokens and our `prototype` covers design exploration. Use the idea ad hoc |
| [interrogate][ps-interrogate] | One readonly reviewer per model, the same rubric, then a lead judgment into Act on / Consider / Noted / Dismissed with an agreement map. | High in the Task/model fallback section. The core flow is portable. It needs [4 reference files][ps-interrogate-refs] (16 KB). | Medium (~30-45 min). Rewrite step 3 for the Agent tool (2-3 reviewers: opus + sonnet + inherit) and copy the references verbatim. | **Port, optional.** Its synthesis and "Dismissed" bucket complement `code-review` |
| [unslop][ps-unslop] | 33 numbered anti-AI-prose rules. | None. | Copy verbatim. `disable-model-invocation: true` already works. | **Port (optional)** for pitch deck and README |
| [no-comments][ps-nocomments] | Spawns the "Comment Sicko" agent, then fixes accepted findings. | High. It depends on the [Comment Sicko agent][ps-sicko], `/how`, `/why`, `/architect` and principle skills. | Medium-heavy because of the dependency chain. | Skip. `cursor-team-kit/deslop` or built-in `simplify` cover it |
| [create-verification-skill][ps-cvs] | Generates a repo-local `verify-<app>` skill with launch, doctor, drive, evidence, cleanup and a feature map, then proves it by running it once. | Low. Only the `.cursor/skills/` output path. | **Light.** Change the path to `.claude/skills/verify-<app>/` and copy the [feature-map example][ps-cvs-ex]. | **Port.** Run it once the app boots (around hour 6-10) for the CLI and the dealer UI. It complements `demo-flow-verifier` (which runs the demo story) by giving agents a reusable driver |
| [maintain-verification-skill][ps-mvs] | Periodic audit: one read-only subagent per feature, one live pass, one PR. | Low (`.cursor/skills/verify-*`). | Light. | Skip. A 36h map won't rot enough. Maybe one run before feature freeze |
| [poteto-mode][ps-poteto] | 20 KB router: non-negotiables, principles, autonomy, subagent defaults, reply style, 22 playbooks plus bun/TS scripts (`orch`, `watch-pr`). | Very high. Cursor mode frontmatter, `AskQuestion`, Grok defaults, Bugbot, `cursor-team-kit`, Origin. | Heavy (days). | Skip. Its "Writing the reply" and "Autonomy: always pause for irreversible writes" ideas could become 3 CLAUDE.md lines |
| [architect][ps-architect] | Ground with `how`/`why`, sketch via arena ("design it twice"), screen against [design red flags][ps-redflags], then implement against the sketch and scrap on pattern friction. | High. It chains how, why, arena and pstack-models. | Heavy. | Skip. It overlaps `codebase-design`, `improve-codebase-architecture` and `prototype`. Optionally read `design-red-flags.md` |
| [blast-radius][ps-blast] | Find the one fact a change is safe because of, prove it with a script, and list risks and cleared items. | Low. Mentions `why`, `arena` and `unslop` only as optional companions. | **Light** (drop or rewrite 2 lines). | **Port.** Useful before merging changes to the Rule Engine or Privacy Gate during integration |
| Principles ([25 `principle-*` skills][ps-skills], 41 KB total, e.g. [prove-it-works][ps-prove]) | One-page doctrines, all `disable-model-invocation: true`. | Low. | Light but large. | Skip as skills. If wanted, distill "prove it works", "fix root causes" and "test behavior, not implementation" into CLAUDE.md one-liners |

---

## 3. Other cursor/plugins (one line each)

All MIT. Copyright is "Cursor" for orchestrate, thermos, advisor, ralph-loop and cursor-team-kit, and "Dylan Gattey" for dyl-stack ([orchestrate][cp-orch-lic], [thermos][cp-thermos-lic], [advisor][cp-adv-lic], [ralph-loop][cp-ralph-lic], [cursor-team-kit][cp-ctk-lic], [dyl-stack][cp-dyl-lic]).

- **Orchestrate.** Planner/worker/subplanner tree on Cursor cloud agents via the Cursor SDK, `CURSOR_API_KEY` and optional Slack ([SKILL.md][cp-orch]). The portable idea is file-based structured handoffs between isolated workers. Not worth it for 36h, because it needs Cursor cloud and 5 humans are the orchestration.
- **Thermos.** Two parallel review subagents (security/correctness/devex/feature-leak, plus harsh maintainability) followed by a synthesis ([thermos SKILL.md][cp-thermos], [review subagent][cp-thermos-agent], [review rubric][cp-thermos-review]). Easy to port: agents to `.claude/agents/`, skills to `.claude/skills/`. It overlaps `code-review` plus our project reviewers. Optional. At most port `thermo-nuclear-review` for a last pre-freeze audit.
- **Advisor.** The main model consults a stronger read-only model at decision, stuck and done checkpoints. It is driven by Cursor hooks (`afterFileEdit`, `afterAgentResponse`, `subagentStop`, `stop`) and a Grok subagent ([SKILL.md][cp-adv], [hooks.json][cp-adv-hooks], [advisor-subagent.md][cp-adv-agent]). The idea of an `opus` read-only "advisor" agent is a 10-line `.claude/agents` file if wanted. Skip the hook machinery.
- **Ralph Loop.** The same prompt re-fed on every stop through a Cursor `stop` hook and a `.cursor/ralph/scratchpad.md` state file ([SKILL.md][cp-ralph], [hooks.json][cp-ralph-hooks]). Claude Code already ships a `/loop` skill (available in this session). Skip.
- **dyl-stack build-figma.** Figma URL to production UI via the Figma MCP, design-system mapping and a `verify-this` visual judge. It requires the Figma plugin and `cursor-team-kit` ([SKILL.md][cp-figma]). Only worth it if the UI designer works in Figma and the team accepts a Figma MCP connection (no client data involved). Default skip.
- **cursor-team-kit deslop.** A 20-line "remove AI slop from the diff vs main" checklist with no Cursor references ([SKILL.md][cp-deslop]). Copy verbatim if wanted. It overlaps built-in `simplify`. Its sibling [verify-this][cp-verify] (claim, baseline vs treatment, VERIFIED/NOT VERIFIED/INCONCLUSIVE) is also Cursor-free and pairs well with `create-verification-skill`.

---

## 4. Scores

Scale 1-5, higher is better on every axis. Setup, context, overlap and privacy are inverted, so 5 means cheap setup, little context, little overlap, low risk.

| Candidate | Time saved | Setup cost (inv) | Context cost (inv) | Overlap (inv) | Privacy risk (inv) | Total | Recommendation |
|---|---|---|---|---|---|---|---|
| ECC `--profile minimal` (global) | 2 | 3 | 1 | 1 | 4 | 11 | Skip |
| ECC `core` / `ecc-universal setup` (hooks) | 2 | 2 | 1 | 1 | 2 | 8 | Skip |
| ECC java-build-resolver (Java) | 4 | 5 | 5 | 4 | 5 | 23 | Cherry-pick |
| ECC build-error-resolver (TS) | 4 | 5 | 5 | 4 | 5 | 23 | Cherry-pick |
| ECC java-reviewer | 3 | 5 | 5 | 3 | 5 | 21 | Cherry-pick (Java) |
| ECC typescript-reviewer / react-reviewer | 3 | 5 | 5 | 3 | 5 | 21 | Cherry-pick one (TS) |
| ECC springboot-verification | 3 | 5 | 5 | 4 | 5 | 22 | Cherry-pick (Java) |
| ECC e2e-testing skill | 3 | 4 | 5 | 3 | 5 | 20 | Optional |
| ECC e2e-runner agent | 3 | 2 | 5 | 2 | 4 | 16 | Skip |
| ECC security-reviewer | 2 | 5 | 5 | 2 | 5 | 19 | Skip |
| ECC tdd-guide / planner | 1 | 5 | 5 | 1 | 5 | 17 | Skip |
| AgentShield (`npx`, static) | 1 | 4 | 5 | 4 | 4 | 18 | Skip (optional pre-event scan) |
| pstack create-verification-skill | 4 | 4 | 5 | 3 | 5 | 21 | Port |
| pstack blast-radius | 3 | 5 | 5 | 4 | 5 | 22 | Port |
| pstack interrogate | 3 | 3 | 5 | 3 | 5 | 19 | Port (optional) |
| pstack unslop | 2 | 5 | 5 | 3 | 5 | 20 | Port (optional) |
| pstack arena | 2 | 3 | 4 | 3 | 5 | 17 | Skip |
| pstack architect | 2 | 1 | 4 | 2 | 5 | 14 | Skip |
| pstack no-comments | 1 | 2 | 5 | 2 | 5 | 15 | Skip |
| pstack poteto-mode | 2 | 1 | 3 | 1 | 5 | 12 | Skip |
| pstack maintain-verification-skill | 1 | 4 | 5 | 3 | 5 | 18 | Skip |
| pstack principles (25) | 1 | 3 | 5 | 2 | 5 | 16 | Skip (distill) |
| Thermos | 2 | 4 | 5 | 2 | 5 | 18 | Optional |
| Advisor | 2 | 2 | 4 | 3 | 5 | 16 | Skip |
| Ralph Loop | 1 | 2 | 5 | 1 | 4 | 13 | Skip |
| Orchestrate | 1 | 1 | 5 | 3 | 2 | 12 | Skip |
| dyl-stack build-figma | 2 | 1 | 5 | 4 | 4 | 16 | Skip |
| cursor-team-kit deslop + verify-this | 2 | 5 | 5 | 2 | 5 | 19 | Optional |

Context scores assume ported pstack skills keep `disable-model-invocation: true`, so they cost zero tokens until invoked ([skills docs][cc-skills]). For copied ECC agents, only the `description` lines stay resident.

---

## 5. Exact files to cherry-pick or port

Put copies under the repo's `.claude/`, not `~/.claude/`. Add an MIT header (copyright line plus permission notice) to each, change `model:` to `inherit`, and drop sections for frameworks we don't use (Quarkus, Mongo, Next.js).

**ECC (pin to tag v2.2.3), Java stack:**
- https://raw.githubusercontent.com/affaan-m/ECC/v2.2.3/agents/java-build-resolver.md
- https://raw.githubusercontent.com/affaan-m/ECC/v2.2.3/agents/java-reviewer.md
- https://raw.githubusercontent.com/affaan-m/ECC/v2.2.3/skills/springboot-verification/SKILL.md

**ECC, TypeScript stack (instead of or in addition to the Java files):**
- https://raw.githubusercontent.com/affaan-m/ECC/v2.2.3/agents/build-error-resolver.md
- https://raw.githubusercontent.com/affaan-m/ECC/v2.2.3/agents/typescript-reviewer.md (or `react-reviewer.md`)

**ECC, optional:**
- https://raw.githubusercontent.com/affaan-m/ECC/v2.2.3/skills/e2e-testing/SKILL.md
- https://raw.githubusercontent.com/affaan-m/ECC/v2.2.3/skills/regex-vs-llm-structured-text/SKILL.md (reference reading for the extractor)

**pstack (pin to commit ccb5507), port:**
- https://raw.githubusercontent.com/cursor/plugins/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/create-verification-skill/SKILL.md, plus `references/feature-map-example/{README.md,create-note.md,search.md}`. Replace `.cursor/skills/` with `.claude/skills/`.
- https://raw.githubusercontent.com/cursor/plugins/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/blast-radius/SKILL.md. Drop the `why`/`arena`/`unslop` cross-references or point them at our skills.
- Optional: https://raw.githubusercontent.com/cursor/plugins/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/interrogate/SKILL.md, plus `references/{reviewer-prompt.md,rubric.md,code-quality-review.md,lead-judgment.md}`. Rewrite step 3 for the Agent tool with `opus`/`sonnet` reviewers.
- Optional: https://raw.githubusercontent.com/cursor/plugins/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/unslop/SKILL.md (verbatim).

**cursor-team-kit, optional, verbatim:**
- https://raw.githubusercontent.com/cursor/plugins/ccb5507cec1546dc88135c1139c811e6c59115ba/cursor-team-kit/skills/deslop/SKILL.md
- https://raw.githubusercontent.com/cursor/plugins/ccb5507cec1546dc88135c1139c811e6c59115ba/cursor-team-kit/skills/verify-this/SKILL.md

All of the above is tooling (prompts), which the hackathon rule allows. None of it is product code.

---

## Could not verify

- **Real token counts.** The ~6k (rules) and ~12k (descriptions) figures are byte/4 estimates computed from the files. I did not install ECC and measure with `/context`. Claude Code may also truncate long descriptions (combined description cap 1,536 chars per skill, [skills docs][cc-skills]) or treat agent and command descriptions differently from skills.
- **Hook profile for the Stop/SessionEnd lifecycle hooks.** These go through `lifecycle-hook-bootstrap.js` rather than `run-with-flags.js`, so I could not confirm from `hooks.json` alone whether `session-end` (and its LLM summary) also runs under `ECC_HOOK_PROFILE=minimal`. The PreToolUse and PostToolUse gating was confirmed ([hooks.json][ecc-hooks], [posttooluse-dispatcher.js][ecc-postdispatch]).
- **`npx ecc-universal setup` was not executed.** It shells out to `claude plugin ...` and would modify real Claude config. The behaviour above is from reading `setup.js` and `claude-plugin-setup.js` only.
- **AgentShield's static scan making no network calls.** I read only its `package.json` (which depends on `@anthropic-ai/sdk`) and the ECC skill text, not its source.
- **Whether the `agent-browser` tool used by `e2e-runner` sends page content off-machine.** Not investigated. It is a global npm install, which the brief disallows anyway.
- **pstack sub-dependencies of `how`, `why` and `swarm`.** I read them only to the extent they are referenced (they contain Cursor references per grep). Their full text was not reviewed.
- **Whether Claude Code ships or recommends an official Ralph-style plugin.** Not checked. The recommendation relies on the built-in `/loop` skill seen in this session.
- **The `v2.2.3` tag object.** It is an annotated tag (git reported `93f9f61` as "not a commit"). The clone resolved it to commit `c05b2d6`, so raw URLs using `v2.2.3` should resolve, but I did not fetch each raw URL individually.

---

[ecc-rel]: https://github.com/affaan-m/ECC/releases/tag/v2.2.3
[ecc-version]: https://github.com/affaan-m/ECC/blob/v2.2.3/VERSION
[ecc-license]: https://github.com/affaan-m/ECC/blob/v2.2.3/LICENSE
[ecc-pkg]: https://github.com/affaan-m/ECC/blob/v2.2.3/package.json
[ecc-plugin]: https://github.com/affaan-m/ECC/blob/v2.2.3/.claude-plugin/plugin.json
[ecc-installsh]: https://github.com/affaan-m/ECC/blob/v2.2.3/install.sh
[ecc-installapply]: https://github.com/affaan-m/ECC/blob/v2.2.3/scripts/install-apply.js
[ecc-profiles]: https://github.com/affaan-m/ECC/blob/v2.2.3/manifests/install-profiles.json
[ecc-modules]: https://github.com/affaan-m/ECC/blob/v2.2.3/manifests/install-modules.json
[ecc-components]: https://github.com/affaan-m/ECC/blob/v2.2.3/manifests/install-components.json
[ecc-rules-common]: https://github.com/affaan-m/ECC/tree/v2.2.3/rules/common
[ecc-rules-java]: https://github.com/affaan-m/ECC/tree/v2.2.3/rules/java
[ecc-rules-ts]: https://github.com/affaan-m/ECC/tree/v2.2.3/rules/typescript
[ecc-rules-web]: https://github.com/affaan-m/ECC/tree/v2.2.3/rules/web
[ecc-rule-testing]: https://github.com/affaan-m/ECC/blob/v2.2.3/rules/common/testing.md
[ecc-rule-style]: https://github.com/affaan-m/ECC/blob/v2.2.3/rules/common/coding-style.md
[ecc-rule-perf]: https://github.com/affaan-m/ECC/blob/v2.2.3/rules/common/performance.md
[ecc-setup]: https://github.com/affaan-m/ECC/blob/v2.2.3/scripts/setup.js
[ecc-plugin-setup]: https://github.com/affaan-m/ECC/blob/v2.2.3/scripts/lib/claude-plugin-setup.js
[ecc-hooks]: https://github.com/affaan-m/ECC/blob/v2.2.3/hooks/hooks.json
[ecc-hooks-meta]: https://github.com/affaan-m/ECC/blob/v2.2.3/hooks/hooks.metadata.json
[ecc-hooks-dir]: https://github.com/affaan-m/ECC/tree/v2.2.3/scripts/hooks
[ecc-hookflags]: https://github.com/affaan-m/ECC/blob/v2.2.3/scripts/lib/hook-flags.js
[ecc-gateguard]: https://github.com/affaan-m/ECC/blob/v2.2.3/scripts/hooks/gateguard-fact-force.js
[ecc-mcphealth]: https://github.com/affaan-m/ECC/blob/v2.2.3/scripts/hooks/mcp-health-check.js
[ecc-postdispatch]: https://github.com/affaan-m/ECC/blob/v2.2.3/scripts/hooks/posttooluse-dispatcher.js
[ecc-cost]: https://github.com/affaan-m/ECC/blob/v2.2.3/scripts/hooks/cost-tracker.js
[ecc-sessionend]: https://github.com/affaan-m/ECC/blob/v2.2.3/scripts/hooks/session-end.js
[ecc-llmsummary]: https://github.com/affaan-m/ECC/blob/v2.2.3/scripts/lib/llm-summary.js
[ecc-cl-config]: https://github.com/affaan-m/ECC/blob/v2.2.3/skills/continuous-learning-v2/config.json
[ecc-sponsor]: https://github.com/affaan-m/ECC/blob/v2.2.3/scripts/lib/compute-sponsor.js
[ecc-secscan]: https://github.com/affaan-m/ECC/blob/v2.2.3/skills/security-scan/SKILL.md
[ecc-java-build]: https://github.com/affaan-m/ECC/blob/v2.2.3/agents/java-build-resolver.md
[ecc-build]: https://github.com/affaan-m/ECC/blob/v2.2.3/agents/build-error-resolver.md
[ecc-java-rev]: https://github.com/affaan-m/ECC/blob/v2.2.3/agents/java-reviewer.md
[ecc-ts-rev]: https://github.com/affaan-m/ECC/blob/v2.2.3/agents/typescript-reviewer.md
[ecc-react-rev]: https://github.com/affaan-m/ECC/blob/v2.2.3/agents/react-reviewer.md
[ecc-sb-verif]: https://github.com/affaan-m/ECC/blob/v2.2.3/skills/springboot-verification/SKILL.md
[ecc-e2e-skill]: https://github.com/affaan-m/ECC/blob/v2.2.3/skills/e2e-testing/SKILL.md
[ecc-e2e]: https://github.com/affaan-m/ECC/blob/v2.2.3/agents/e2e-runner.md
[ecc-sec]: https://github.com/affaan-m/ECC/blob/v2.2.3/agents/security-reviewer.md
[ecc-silent]: https://github.com/affaan-m/ECC/blob/v2.2.3/agents/silent-failure-hunter.md
[ecc-tdd]: https://github.com/affaan-m/ECC/blob/v2.2.3/agents/tdd-guide.md
[ecc-planner]: https://github.com/affaan-m/ECC/blob/v2.2.3/agents/planner.md
[ecc-regex]: https://github.com/affaan-m/ECC/blob/v2.2.3/skills/regex-vs-llm-structured-text/SKILL.md
[ecc-nutrient]: https://github.com/affaan-m/ECC/blob/v2.2.3/skills/nutrient-document-processing/SKILL.md
[ecc-fe]: https://github.com/affaan-m/ECC/blob/v2.2.3/skills/frontend-patterns/SKILL.md
[ecc-a11y]: https://github.com/affaan-m/ECC/blob/v2.2.3/agents/a11y-architect.md
[as-repo]: https://github.com/affaan-m/agentshield
[as-pkg]: https://github.com/affaan-m/agentshield/blob/main/package.json
[cp-repo]: https://github.com/cursor/plugins/tree/ccb5507cec1546dc88135c1139c811e6c59115ba
[ps-manifest]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/.cursor-plugin/plugin.json
[ps-license]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/LICENSE
[ps-skills]: https://github.com/cursor/plugins/tree/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills
[ps-arena]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/arena/SKILL.md
[ps-interrogate]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/interrogate/SKILL.md
[ps-interrogate-refs]: https://github.com/cursor/plugins/tree/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/interrogate/references
[ps-unslop]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/unslop/SKILL.md
[ps-nocomments]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/no-comments/SKILL.md
[ps-sicko]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/agents/comment-sicko.md
[ps-cvs]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/create-verification-skill/SKILL.md
[ps-cvs-ex]: https://github.com/cursor/plugins/tree/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/create-verification-skill/references/feature-map-example
[ps-mvs]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/maintain-verification-skill/SKILL.md
[ps-poteto]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/poteto-mode/SKILL.md
[ps-architect]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/architect/SKILL.md
[ps-redflags]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/architect/references/design-red-flags.md
[ps-blast]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/blast-radius/SKILL.md
[ps-prove]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/pstack/skills/principle-prove-it-works/SKILL.md
[cp-orch]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/orchestrate/skills/orchestrate/SKILL.md
[cp-orch-lic]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/orchestrate/LICENSE
[cp-thermos]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/thermos/skills/thermos/SKILL.md
[cp-thermos-agent]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/thermos/agents/thermo-nuclear-review-subagent.md
[cp-thermos-review]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/thermos/skills/thermo-nuclear-review/SKILL.md
[cp-thermos-lic]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/thermos/LICENSE
[cp-adv]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/advisor/skills/advisor/SKILL.md
[cp-adv-hooks]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/advisor/hooks/hooks.json
[cp-adv-agent]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/advisor/agents/advisor-subagent.md
[cp-adv-lic]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/advisor/LICENSE
[cp-ralph]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/ralph-loop/skills/ralph-loop/SKILL.md
[cp-ralph-hooks]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/ralph-loop/hooks/hooks.json
[cp-ralph-lic]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/ralph-loop/LICENSE
[cp-figma]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/dyl-stack/skills/build-figma/SKILL.md
[cp-dyl-lic]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/dyl-stack/LICENSE
[cp-deslop]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/cursor-team-kit/skills/deslop/SKILL.md
[cp-verify]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/cursor-team-kit/skills/verify-this/SKILL.md
[cp-ctk-lic]: https://github.com/cursor/plugins/blob/ccb5507cec1546dc88135c1139c811e6c59115ba/cursor-team-kit/LICENSE
[cc-memory]: https://code.claude.com/docs/en/memory
[cc-skills]: https://code.claude.com/docs/en/skills
[cc-subagents]: https://code.claude.com/docs/en/sub-agents
[cc-foundry]: https://code.claude.com/docs/en/microsoft-foundry
