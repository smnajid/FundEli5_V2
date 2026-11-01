# AI tooling for the 20 build hours

Resolves [Which AI tools make the team faster during the 36 hours?](https://github.com/smnajid/fund-gate/issues/18).

The team uses **Claude Code**, served from **Microsoft Foundry**, on the bank's stack: **Spring Boot 4 and Angular**, with Python only where it adds real value ([ADR 0006](../adr/0006-spring-boot-and-angular-python-only-for-real-added-value.md)). This page says what we bring, what each tool is for, and what we left out. The kit itself is committed in `.claude/` and `.agents/skills/`. Installing it takes about 10 minutes before the first build: see [hackathon-kit.md](../agents/hackathon-kit.md). The evidence behind every claim is in two research notes: [Claude Code, plugins, Foundry](../research/ai-tools-claude-code.md) and [ECC, pstack, Cursor plugins](../research/ai-tools-ecc-pstack.md).

## What decided it

- **Foundry, "Hosted on Azure" deployments only.** With this option, prompts and completions stay in Azure. "Hosted on Anthropic" deployments, and every Fable model, run on Anthropic's infrastructure. Foundry turns off telemetry by default.
- **Foundry costs some features.** Claude in Chrome, Artifacts (so no Slides artifact), cloud sessions, `/schedule` and Routines are unavailable. Subagents, skills, plugins, hooks, MCP, worktrees, `/loop` and the sandbox all work.
- **Context is the scarce resource, not money.** Budget is unlimited, but every always-loaded rule and MCP tool schema costs every turn of every agent session, before and during the event. MCP tool search is unsupported on Azure-hosted deployments, so each enabled MCP server's full tool list sits in context.
- **We already have a good base.** About 37 Matt Pocock skills: `tdd`, `prototype`, `diagnosing-bugs`, `codebase-design`, `code-review`, `to-tickets`, `handoff` and others. A new tool has to add something these don't.

## The kit

| What | Where | Who uses it, when |
|---|---|---|
| **privacy-gate-auditor** subagent: one egress, gate-only input, fail closed, allow-list, logged (ADR 0004/0005) | `.claude/agents/` | Mohamed before each merge touching an LLM call, and before every rehearsal |
| **engine-purity-reviewer** subagent: no LLM, deterministic, Draft never decides, Advisory Suggestions never change the Verdict (ADR 0002/0003/0005/0007) | `.claude/agents/` | Mohamed on every Rule Engine, Scale Mapping or reverse-lookup change |
| **demo-flow-verifier** subagent: runs the `specs/` scenarios and the hero flow, times each step | `.claude/agents/` | Mohamed at each checkpoint, after feature freeze, before each rehearsal |
| **java-reviewer / java-build-resolver** (from ECC, trimmed) for Spring Boot | `.claude/agents/` | Mohamed: the resolver when the build breaks, the reviewer before merging |
| **typescript-reviewer / ts-build-resolver** (from ECC, rewritten for Angular: signals, `@if`/`@for`, `OnPush`, NG error codes) | `.claude/agents/` | Mohamed |
| **angular-developer** skill (official, from `angular/skills`): current Angular practice for signals, Signal Forms, DI, routing, Aria, testing | `.claude/skills/` | Mohamed; it loads only when Angular work comes up |
| **jdtls-lsp** and **typescript-lsp** plugins: go-to-definition, references and compiler errors for the agent | `enabledPlugins` | Everyone (needs `jdtls` and `typescript-language-server` installed) |
| **Angular CLI MCP** (`ng mcp`): build, test and serve targets, docs search, best-practices guide | opt-in per person | Mohamed |
| **create-verification-skill** (ported from pstack): generates a `verify-fund-gate` skill that launches, checks and drives the real app, with a feature map | `.agents/skills/` | Mohamed once the app boots (Mon 2 Nov). Every agent uses the generated skill afterwards |
| **blast-radius** (ported from pstack): finds the one fact a change is safe because of, and proves it by running code | `.agents/skills/` | Before merging Rule Engine or Privacy Gate changes during integration |
| **unslop** (pstack): strips AI tells from prose | `.agents/skills/` | Pitch text, README, reply templates |
| **frontend-design** plugin: design direction so the screens don't look like a stock AI dashboard | `enabledPlugins` | Mohamed |
| **pr-review-toolkit** plugin: six focused review agents (silent failures, test gaps, type design, comments) | `enabledPlugins` | Everyone on PRs |
| **playwright** plugin (MCP): Claude opens and checks its own screens through accessibility snapshots | opt-in per person | Mohamed, for the screens and the demo verifier |
| **document-skills** plugin (`pptx`): the pitch deck on the bank's template | opt-in per person | Whoever builds the deck |
| **context7** plugin (MCP): current library docs on demand, so agents stop guessing framework APIs | opt-in per person | Anyone fighting a framework version |

Playwright, Context7 and `document-skills` are opt-in because MCP tool schemas and large skills cost every person context on every turn. The other three people don't need them.

## Who uses what

| Person | Owns (architecture.md) | Main tools |
|---|---|---|
| 1 | Rule Card pipeline (command-line mode of the Spring Boot app: Anthropic Java SDK + PDFBox), Share Class catalog | `tdd`, `java-build-resolver`, `claude-api` skill for PDF + Citations calls, `blast-radius` on the quote-on-page check |
| 2 | Rule Engine, Scale Mapping, reverse lookup | `tdd` against the `@rule-engine` scenarios in `specs/`, `engine-purity-reviewer`, `blast-radius` |
| 3 | Dealer UI, Validation UI (Angular) | `prototype` (before the event, issue #17), `angular-developer`, Angular CLI MCP, `frontend-design`, Playwright, `typescript-reviewer` |
| 4 | Intake, Privacy Gate, egress, Parser, Reply Drafter, advisory lane | `privacy-gate-auditor` on every merge, `tdd` for the gate's fail-closed cases, `claude-api` skill for Foundry calls |
| 5 | Adapters, Check Record, compose, pitch | `demo-flow-verifier`, `create-verification-skill`, `pptx`, `unslop` |

## How we work with agents in the 20 build hours

- **One worktree per module owner**: `claude --worktree <module>`. `.claude/worktrees/` is already git-ignored.
- **Background agents for slow loops**: `/bg` a session that runs the coverage set or a long build. `/loop 10m` to rerun the demo-flow verifier during the last hours.
- **Subagents for review, not for writing the core**: the Rule Engine and Privacy Gate are written test-first by their owner. Reviewers and verifiers report and never edit.
- **Pin the models** in each person's environment (`ANTHROPIC_DEFAULT_OPUS_MODEL` and the Sonnet and Haiku equivalents). Otherwise the `opus` alias on Foundry resolves to an older model, or to one that isn't deployed.

## Where the time goes, and which tool wins it back

| Time sink in a 20-hour build | Tool |
|---|---|
| Broken build after a merge, dependency clash | `java-build-resolver` (Maven/Gradle, Spring) / `ts-build-resolver` (`ng build`): minimal-diff fixes, no refactoring |
| Agent writing outdated Angular (decorators, `*ngIf`, NgModules) | `angular-developer` skill and the Angular CLI MCP's best-practices guide |
| "Does the demo still work?" after each integration | `demo-flow-verifier` and the generated `verify-fund-gate` skill, `/loop` in the last hours |
| A Verdict that changed and nobody knows why | `engine-purity-reviewer`, then `blast-radius` on the suspect change |
| Screens that look generic or break on stage | `frontend-design` for direction, Playwright so Claude checks its own screens |
| Wrong framework API from stale model memory | the LSP plugins catch it at once; Context7 for Spring Boot 4 docs; the Microsoft Learn MCP for Foundry and Azure SDKs |
| Review bottleneck with one builder merging everything | `pr-review-toolkit` agents plus the stack reviewer, run by the author before asking a human |
| Pitch deck and slide text at hour 34 | `pptx` skill on the bank template, `unslop` on the text |
| Five people stepping on each other | one worktree per module owner, background sessions for slow loops |

## Python: where it would add value

None of the modules needs it (ADR 0006). The one candidate was the Rule Card pipeline, because the spike is in Python. Everything the spike used exists in Java:

| Spike (Python) | Java equivalent |
|---|---|
| `anthropic` SDK, PDF document blocks with Citations | `com.anthropic:anthropic-java`: `DocumentBlockParam`, `Base64PdfSource`, `CitationsConfigParam`, `CitationPageLocation` |
| `AnthropicFoundry` client | `com.anthropic:anthropic-java-foundry`: `FoundryBackend.fromEnv()` |
| JSON-schema output for the Rule Card | structured outputs (`StructuredMessageCreateParams`, `OutputConfig`) |
| `pypdf`: page selection, page text for the quote check | Apache PDFBox |

Revisit only if the bank's data brings something the JVM handles badly, such as scanned PDFs that need OCR. Even then, Azure Document Intelligence has a Java SDK.

## Considered and left out

| Candidate | Verdict | Why |
|---|---|---|
| **ECC full install** (`--profile minimal` or `core`) | Skip | Even "minimal" writes about 400 files into `~/.claude`: 68 agents, 94 commands that clash with our `pr` and `code-review`, and 122 rules adding roughly 18k tokens to every turn (our estimate). Its rules impose 80% coverage and "always immutable" code. The `core` hooks block the first edit of each file and re-send the transcript to a model every 50 messages. We copied 4 agents instead. |
| ECC `nutrient-document-processing` | Never | Sends documents to an outside API. |
| **pstack as a whole** (`poteto-mode`, `arena`, `architect`, principles) | Skip | Built for Cursor: custom modes, Grok routing, Cursor's question tool. `architect` overlaps `codebase-design` and `prototype`. We ported the three skills that work anywhere. |
| pstack `interrogate` (multi-model adversarial review) | Later, if someone owns it | Needs its reviewer step rewritten for the Agent tool. `pr-review-toolkit` covers most of it. |
| Cursor Orchestrate, Advisor, Ralph Loop, Figma-to-UI | Skip | Cursor-only, or an autonomous loop we can't supervise in 20 hours, or a Figma step we won't have. |
| `security-guidance` plugin | Optional | Runs an LLM review of the diff at the end of every turn. That is useful, but slow when iterating fast. Mohamed may turn it on for the Privacy Gate. |
| `feature-dev`, `hookify`, `code-review` plugins | Skip | Overlap our `implement`, `to-tickets` and `code-review` skills. |
| Claude in Chrome, Slides artifact | Not available | Not available on Foundry. Use Playwright and `pptx`. |
| Figma MCP | Skip | Nobody designs in Figma first in 20 hours. Use `prototype` before the event instead. |

## Validation (2026-10-08)

- **Subagents:** a headless session in a throwaway copy of the kit found both auditors by name. On a planted fixture they flagged every violation: the engine imports the SDK and asks the model for the Target Market, the engine never checks Draft cards or the regime, the parser sends the client name ungated, and a second SDK client sits outside the egress.
- **Not done:** a timed with-and-without comparison on a real Rule Engine slice. The first real measurement is the Rule Engine build on Thu 29 Oct.

## Open questions

- **Organisers**: are agent configs, plugins and skills allowed? They are tooling, not product code. Asked on issue #18.
- **Event subscription**: can it deploy Claude "Hosted on Azure" with enough tokens per minute for parallel coding agents plus the product? See the Foundry note on the map.
- **Pinning**: `claude-plugins-official` has no release tags. We set `autoUpdate: false`; to fully freeze, fork it and pin `ref` a week before the event.
