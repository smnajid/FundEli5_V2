# AI tooling for the hackathon: Claude Code only

Researched 2026-10-08 against primary sources (code.claude.com, platform.claude.com, the anthropics GitHub repos, and vendor docs). Every claim links to the page it came from. Versions and catalogues change often, so re-check anything marked "v2.1.x" against `claude --version` on the day.

Context: 5 people, 36 hours, Claude Code only, Claude served from Microsoft Foundry. Hard rule: no client-identifying data may reach an external LLM, and that includes coding agents reading the bank's hour-0 export (`data/bank/`, already in `.gitignore`).

---

## TL;DR

1. **Data guard: use three layers, because none is enough by itself.** (a) A `permissions.deny` rule `Read(./data/bank/**)` covers Read, Edit, Grep/Glob, `@` mentions, and recognised Bash file commands (`cat`, `head`, `tail`, `sed`, `tee`, redirects). (b) `sandbox.enabled: true` with `allowUnsandboxedCommands: false` lets the OS block every Bash subprocess (python, node, `grep -r`), and the Read deny rules are merged into the sandbox automatically. (c) A PreToolUse hook on `Read|Grep|Glob|Bash|Edit|Write` acts as a belt-and-braces check that also logs attempts. The sandbox does not cover MCP servers or hooks. The only airtight option is to keep the raw export off any machine or folder a Claude session can reach. See section 5.
2. **Foundry keeps code-agent traffic in Azure only if the deployment uses the "Hosted on Azure" option.** "Hosted on Anthropic" runs inference on Anthropic infrastructure. Set `CLAUDE_CODE_USE_FOUNDRY=1` plus `ANTHROPIC_FOUNDRY_RESOURCE`, and pin `ANTHROPIC_DEFAULT_{OPUS,SONNET,HAIKU}_MODEL`. See section 4.
3. **On Foundry you lose Claude in Chrome, Artifacts (including any Slides artifact), Desktop, cloud sessions, Routines, and `/schedule`.** For visual checks use the Playwright MCP plugin. For the pitch deck use the `pptx` skill from `anthropics/skills`. See sections 2 and 6.
4. **Plugins to enable from `claude-plugins-official`:** `frontend-design`, `playwright`, `security-guidance`, `pr-review-toolkit`, `feature-dev`, `hookify`, plus `jdtls-lsp` or `typescript-lsp` once the stack is chosen. Add `context7` and `microsoft-docs` only if the team accepts that doc queries leave the tenant. See sections 1 and 3.

---

## 1. Anthropic plugin marketplaces

### Which marketplaces exist

| Marketplace name (after `@`) | Repo | How you get it |
|---|---|---|
| `claude-plugins-official` | `anthropics/claude-plugins-official` | Added automatically on the first interactive session, unless managed policy or `CLAUDE_CODE_DISABLE_OFFICIAL_MARKETPLACE_AUTOINSTALL` blocks it |
| `claude-community` | `anthropics/claude-plugins-community` | `/plugin marketplace add anthropics/claude-plugins-community` |
| `claude-code-plugins` (demo) | `anthropics/claude-code` (`plugins/`) | `/plugin marketplace add anthropics/claude-code` |
| `anthropic-agent-skills` | `anthropics/skills` | `/plugin marketplace add anthropics/skills` |

Sources: [Anthropic's marketplaces](https://code.claude.com/docs/en/plugins/anthropic-marketplaces). The `anthropic-agent-skills` name comes from [anthropics/skills marketplace.json](https://github.com/anthropics/skills/blob/main/.claude-plugin/marketplace.json).

- The demo marketplace is **not** the official one. Most of its plugins (`code-review`, `feature-dev`, `commit-commands`, `security-guidance`) are duplicated in the official marketplace, so install them from `claude-plugins-official` to avoid ending up with two copies. ([source](https://code.claude.com/docs/en/plugins/anthropic-marketplaces))
- `anthropics/skills` publishes the plugins `document-skills` (xlsx, docx, **pptx**, pdf), `example-skills` (includes `frontend-design`, `webapp-testing`, `theme-factory`, `web-artifacts-builder`, and others), `claude-api`, `academy-guide`, and `discernment-nudge`. ([marketplace.json](https://github.com/anthropics/skills/blob/main/.claude-plugin/marketplace.json))

### Plugins that matter for us (all from `claude-plugins-official`)

The catalogue was read from [`.claude-plugin/marketplace.json`](https://github.com/anthropics/claude-plugins-official/blob/main/.claude-plugin/marketplace.json), which listed 315 plugins on 2026-10-08.

| Plugin | Source in catalogue | What it gives us |
|---|---|---|
| `frontend-design` | `./plugins/frontend-design` | A skill that pushes toward "distinctive, intentional visual design… choices that don't read as templated defaults" (palette, typography, layout grounded in the subject). [SKILL.md](https://github.com/anthropics/claude-plugins-official/blob/main/plugins/frontend-design/skills/frontend-design/SKILL.md) |
| `playwright` | `./external_plugins/playwright` | Microsoft's Playwright MCP server, run as `npx @playwright/mcp@latest`. [.mcp.json](https://github.com/anthropics/claude-plugins-official/blob/main/external_plugins/playwright/.mcp.json) |
| `security-guidance` | `./plugins/security-guidance` | Regex warnings on Edit/Write, an LLM diff review at the end of each turn, and an agentic review on `git commit`. On Foundry it runs through your Claude Code `opus` alias. Needs Python 3.8+ and CLI v2.1.144 or later. [README](https://github.com/anthropics/claude-plugins-official/blob/main/plugins/security-guidance/README.md) |
| `pr-review-toolkit` | `./plugins/pr-review-toolkit` | Six review agents: comment-analyzer, pr-test-analyzer, silent-failure-hunter, type-design-analyzer, code-reviewer, and a simplifier. [README](https://github.com/anthropics/claude-plugins-official/blob/main/plugins/pr-review-toolkit/README.md) |
| `feature-dev` | `./plugins/feature-dev` | `/feature-dev`, a 7-phase workflow that runs parallel `code-explorer` agents and then an architecture step. [README](https://github.com/anthropics/claude-plugins-official/blob/main/plugins/feature-dev/README.md) |
| `code-review` | `./plugins/code-review` | `/code-review` on a PR: 4 parallel agents, a confidence threshold of 80, and it posts a PR comment. [README](https://github.com/anthropics/claude-plugins-official/blob/main/plugins/code-review/README.md). This overlaps with the repo's own `code-review` skill. Plugin skills are namespaced `/<plugin>:<skill>` ([install docs](https://code.claude.com/docs/en/plugins/install)), so both can coexist, but pick one. |
| `hookify` | `./plugins/hookify` | `/hookify <rule>` writes `.claude/hookify.*.local.md` rules (regex, `warn` or `block`) with no restart needed. [README](https://github.com/anthropics/claude-plugins-official/blob/main/plugins/hookify/README.md) |
| `context7` | `./external_plugins/context7` | Remote MCP at `https://mcp.context7.com/mcp?client=claude-code-plugin`. [.mcp.json](https://github.com/anthropics/claude-plugins-official/blob/main/external_plugins/context7/.mcp.json) |
| `microsoft-docs` | `url: https://github.com/MicrosoftDocs/mcp.git` (external) | Microsoft Learn docs MCP. [catalogue](https://github.com/anthropics/claude-plugins-official/blob/main/.claude-plugin/marketplace.json) |
| `figma` | `url: https://github.com/figma/mcp-server-guide.git` (external) | Figma MCP. Same catalogue. |
| `chrome-devtools-mcp` | `url: https://github.com/ChromeDevTools/chrome-devtools-mcp.git` (external) | Drives a live Chrome through DevTools. Same catalogue. |
| `jdtls-lsp` / `typescript-lsp` | `./plugins/…` | Language-server code intelligence for Java or TS. Same catalogue. |
| `commit-commands`, `claude-md-management`, `code-simplifier`, `ralph-loop`, `session-report` | `./plugins/…` | Optional. Same catalogue. |

No `shadcn` and no dedicated accessibility plugin appeared in the official catalogue. The nearest match is `liquid-skills` (Shopify Liquid, WCAG patterns), which does not fit our stack. ([catalogue](https://github.com/anthropics/claude-plugins-official/blob/main/.claude-plugin/marketplace.json))

### How teammates get plugins automatically

- `enabledPlugins` maps `plugin-name@marketplace-name` to a Boolean. ([settings reference](https://code.claude.com/docs/en/settings-reference#enabledplugins))
- `extraKnownMarketplaces` maps a marketplace name to `{ "source": {...}, "autoUpdate"?: bool }`. Source types are `github` (`repo`), `git` (`url`), `url`, `file`, `directory`, and `settings`. The entries are honoured **only after the user accepts the workspace trust dialog**. ([settings reference](https://code.claude.com/docs/en/settings-reference#extraknownmarketplaces))
- A plugin that the marketplace lists by a **relative path** (every `./plugins/…` and `./external_plugins/…` row above) loads once the marketplace applies. A plugin with an **external source** (`microsoft-docs`, `figma`, `chrome-devtools-mcp`) does not. Each teammate sees `Plugin "<name>" is enabled in project settings but isn't installed` until they run `claude plugin install <name>@<marketplace> --scope project`. ([plugins/org](https://code.claude.com/docs/en/plugins/org#require-plugins-per-repository))
- In `-p` or CI runs, plugins install in the background. Set `CLAUDE_CODE_SYNC_PLUGIN_INSTALL=1` to make the run wait for them. ([plugins/org](https://code.claude.com/docs/en/plugins/org#when-each-surface-applies-the-plugin-keys))
- To opt out personally, set the plugin to `false` in `.claude/settings.local.json`. A `false` in user settings does not override project settings. ([settings reference](https://code.claude.com/docs/en/settings-reference#enabledplugins))

### Version pinning

- **Marketplace level:** add `"ref"` (a branch or tag) to a `github` or `git` source, for example `{ "source": "github", "repo": "acme-corp/plugins", "ref": "main" }`. ([settings reference, allowed source types](https://code.claude.com/docs/en/settings-reference#strictknownmarketplaces)). You can also run `/plugin marketplace add owner/repo#v1.2.0`. ([install docs](https://code.claude.com/docs/en/plugins/install#add-a-marketplace))
- **Plugin level (inside a marketplace.json you own):** `github` plugin sources accept `ref` and `sha`. ([marketplace reference](https://code.claude.com/docs/en/plugins/marketplace-reference))
- **Auto-update:** it is on by default for `claude-plugins-official` and off for third-party marketplaces. Pass `"autoUpdate": false` on the marketplace entry to freeze it. ([settings reference](https://code.claude.com/docs/en/settings-reference#extraknownmarketplaces), [install docs](https://code.claude.com/docs/en/plugins/install#keep-plugins-updated))
- The `anthropics/claude-plugins-official` repo tracks `main` and has no release tags that we verified. To freeze the whole set for 36 hours, pin `ref` to a commit-ish branch or tag you control, for example a fork. See Could not verify.

---

## 2. UX / UI tooling for two internal screens in 36 hours

- **`frontend-design` skill:** a design-direction prompt with no code generator or component library. It tells Claude to ground palette, typography, and layout in the subject, and to avoid "templated defaults". It is useful for the dealer UI and Rule Card screen so they don't look like generic AI dashboards. ([SKILL.md](https://github.com/anthropics/claude-plugins-official/blob/main/plugins/frontend-design/skills/frontend-design/SKILL.md))
- **shadcn MCP (if we choose React):** it lets the agent "browse available components, search for specific ones, and install them directly into your project". Set it up with `pnpm dlx shadcn@latest mcp init --client claude`, which writes `.mcp.json` `{"mcpServers":{"shadcn":{"command":"npx","args":["shadcn@latest","mcp"]}}}`. It needs network access to the registries. ([ui.shadcn.com/docs/mcp](https://ui.shadcn.com/docs/mcp)). It only fetches public components and does not send our data.
- **Figma MCP:** this is an external-source plugin (`figma`). It is only useful if someone designs in Figma first, which is unlikely to pay off in 36 hours. ([catalogue](https://github.com/anthropics/claude-plugins-official/blob/main/.claude-plugin/marketplace.json))
- **Playwright MCP vs Claude in Chrome:**
  - Playwright MCP works from "accessibility snapshots" ("No vision models needed"). It has the flags `--headless`, `--isolated`, `--caps vision`, `--allowed-origins`, `--output-dir`, and `--save-session`. Install it with `claude mcp add playwright npx @playwright/mcp@latest`, or use the `playwright` plugin. The upstream README itself says coding agents "increasingly favor CLI–based workflows exposed as SKILLs over MCP because CLI invocations are more token-efficient". ([microsoft/playwright-mcp](https://github.com/microsoft/playwright-mcp))
  - **Claude in Chrome is unavailable on Foundry.** "Chrome integration is not available through third-party providers like … Microsoft Foundry", and it needs a claude.ai `/login`. ([chrome docs](https://code.claude.com/docs/en/chrome), [feature availability](https://code.claude.com/docs/en/feature-availability))
  - The `webapp-testing` skill in `anthropics/skills` ("example-skills") writes native Python Playwright scripts and ships `scripts/with_server.py` to manage the dev server. ([SKILL.md](https://github.com/anthropics/skills/blob/main/skills/webapp-testing/SKILL.md))
- **Accessibility:** we found no first-party accessibility skill. Playwright MCP's accessibility-tree snapshots are the practical way to check that labels and roles exist. (Sources as above.)
- **What actually speeds things up (our judgement, not a sourced claim):** use one component kit (shadcn plus Tailwind, or a Spring plus Thymeleaf template), the `frontend-design` skill for direction, and Playwright MCP so Claude can check its own screens. Skip Figma.

---

## 3. Docs MCPs

### Context7

- Remote MCP `https://mcp.context7.com/mcp`, with an optional API key in `Authorization: Bearer …`. The tools are `resolve-library-id` and `query-docs`. There is also a CLI-plus-skill mode via `npx ctx7 setup --claude` (`ctx7 library`, `ctx7 docs`). ([upstash/context7 README](https://github.com/upstash/context7))
- **What is sent:** "a search query formulated by the MCP client (not your original prompt)", library IDs, the API key, client name and version, transport, and client IP (for rate limiting). **Not sent:** "Your full prompts, code, and conversation context". The tool description tells the model to strip "API keys, passwords, credentials, personal data, and proprietary code" from queries. **Stored:** queries are "passed to LLMs to rerank" and "anonymously stored and used to benchmark retrieval accuracy". Only enterprise plans can turn that storage off. ([Context7 data privacy](https://context7.com/docs/security/data-privacy.md))
- **Risk for us:** the query is written by the model, so a careless query could carry client data. That stripping is model-side, not enforced. Enable Context7 only on machines where `data/bank/` is unreachable, or skip it.

### Microsoft Learn MCP

- Endpoint `https://learn.microsoft.com/api/mcp` (streamable HTTP), with "no authentication required" and no charge. It covers public Microsoft docs only, refreshed daily. ([Learn MCP overview](https://learn.microsoft.com/en-us/training/support/mcp))
- Add it as the `microsoft-docs` plugin (external source, so each user runs `claude plugin install microsoft-docs@claude-plugins-official --scope project`), or with `claude mcp add --transport http microsoft-learn https://learn.microsoft.com/api/mcp`. ([MCP docs](https://code.claude.com/docs/en/mcp)). It is useful for Foundry, Entra, and azure-identity questions.

---

## 4. Claude Code on Microsoft Foundry

### Environment

```bash
export CLAUDE_CODE_USE_FOUNDRY=1
export ANTHROPIC_FOUNDRY_RESOURCE=<resource-name>          # name only, not a URL
# or: export ANTHROPIC_FOUNDRY_BASE_URL=https://<resource>.services.ai.azure.com/anthropic
# auth, pick one:
export ANTHROPIC_FOUNDRY_API_KEY=...                       # key from portal
# export ANTHROPIC_FOUNDRY_AUTH_TOKEN=...                  # Entra bearer token (v2.1.203+), wins over API key
# (or leave both unset -> DefaultAzureCredential chain, e.g. `az login`)
export ANTHROPIC_DEFAULT_OPUS_MODEL='<opus deployment name>'
export ANTHROPIC_DEFAULT_SONNET_MODEL='<sonnet deployment name>'
export ANTHROPIC_DEFAULT_HAIKU_MODEL='<haiku deployment name>'
```

Source: [Claude Code on Microsoft Foundry](https://code.claude.com/docs/en/microsoft-foundry).

- **Pin the models.** Without the pin, the `opus` alias on Foundry resolves to Opus 4.6. Foundry has "no startup model check", so requests fail if the default model is not deployed. When you deploy, pick a specific model version, not "auto-update". ([Foundry page](https://code.claude.com/docs/en/microsoft-foundry))
- Background tasks fall back to the primary model unless `ANTHROPIC_DEFAULT_HAIKU_MODEL` points to a Haiku deployment. ([Foundry page](https://code.claude.com/docs/en/microsoft-foundry))
- **RBAC:** `Azure AI User` (now called "Foundry User") or `Cognitive Services User`, or a custom role with `Microsoft.CognitiveServices/accounts/providers/*`. ([Foundry page](https://code.claude.com/docs/en/microsoft-foundry), [platform docs](https://platform.claude.com/docs/en/build-with-claude/claude-in-microsoft-foundry))
- **Verify:** `/status` should show `Microsoft Foundry`. `/logout` is unavailable on Foundry. ([Foundry page](https://code.claude.com/docs/en/microsoft-foundry))
- Env vars can be committed in the settings `env` block, but `env` values apply only after the user trusts the folder. ([data usage](https://code.claude.com/docs/en/data-usage), [settings](https://code.claude.com/docs/en/settings))

### Data stays in the tenant only with "Hosted on Azure"

- There are two hosting options. **Hosted on Azure** is an "Anthropic-operated service running on Azure infrastructure". **Hosted on Anthropic** is an "Anthropic-operated service running on Anthropic infrastructure". "For deployments hosted on Azure, prompts and completions remain within Azure. Only usage metadata and content flagged by Anthropic's safety systems egress to Anthropic." ([platform docs](https://platform.claude.com/docs/en/build-with-claude/claude-in-microsoft-foundry#hosting-options))
- Models hosted on Azure include Opus 5.5, Opus 5, Opus 4.8, Sonnet 5.5 (Global Standard only), Sonnet 5, Haiku 5.5, and Haiku 4.5. Fable models are Anthropic-hosted only. A US Data Zone option exists, but there is no EU data zone listed. ([platform docs](https://platform.claude.com/docs/en/build-with-claude/claude-in-microsoft-foundry#api-model-ids-and-deployments))
- With Foundry, telemetry, error reports, and `/feedback` are **off by default**. Two calls still reach Anthropic: session quality surveys (turn off with `CLAUDE_CODE_DISABLE_FEEDBACK_SURVEY=1`) and the WebFetch hostname check to `api.anthropic.com` (turn off with `skipWebFetchPreflight: true`). Plugin marketplace auto-install contacts GitHub. ([data usage](https://code.claude.com/docs/en/data-usage#default-behaviors-by-api-provider))

### What you lose on Foundry

- **Not available:** everything that needs a Claude subscription (cloud sessions, Desktop except "Claude Desktop on 3P", Routines and `/schedule`, Ultrareview, Code Review, Remote Control, the **Chrome extension**, computer use, **Artifacts**, voice), plus fast mode, Advisor, Channels, GitLab CI/CD, the analytics dashboard, server-managed settings, `/design-sync`, and `/import`. ([feature availability](https://code.claude.com/docs/en/feature-availability))
- **Partial:** auto mode works only with Sonnet 5+, Opus 4.7+, Haiku 5.5, or Fable. Cross-session messaging works on the same machine only. MCP tool search is unsupported on Azure-hosted deployments. Use `/loop` instead of `/schedule`. ([feature availability](https://code.claude.com/docs/en/feature-availability))
- **Azure-hosted deployments return 400 for:** code execution, newer web search and web fetch tool versions, Agent Skills (API), programmatic tool calling, and the Files API. "Claude Code detects deployments hosted on Azure and automatically adapts its feature set." ([platform docs](https://platform.claude.com/docs/en/build-with-claude/claude-in-microsoft-foundry#additional-features-not-supported-when-hosted-on-azure))
- **Still available on every provider:** CLI, VS Code and JetBrains, subagents, hooks, skills, plugins, MCP, checkpoints, sandboxing, and Workflows. ([feature availability](https://code.claude.com/docs/en/feature-availability))

---

## 5. Claude Code features for a 5-person parallel team

### Project subagents (`.claude/agents/*.md`)

Frontmatter fields: `name` and `description` are required. The optional fields are `tools`, `disallowedTools`, `model` (`sonnet|opus|haiku|fable|<full id>|inherit`), `permissionMode`, `maxTurns`, `skills`, `mcpServers`, `hooks`, `memory` (`user|project|local`), `background`, `omitClaudeMd`, `effort`, `isolation` (`worktree`), `color`, `initialPrompt`, and `experimental.cacheTtl`. ([sub-agents](https://code.claude.com/docs/en/sub-agents#supported-frontmatter-fields))

- Frontmatter `hooks` and inline `mcpServers` in a project subagent are **not used until the folder is trusted**. ([permissions, "What runs before you trust a folder"](https://code.claude.com/docs/en/permissions#what-runs-before-you-trust-a-folder))
- Suggested idea: give a `rule-card-author` subagent `disallowedTools: Bash` plus its own PreToolUse hook. This is a design suggestion, not a sourced claim.

### PreToolUse hook contract

**Input on stdin:** the common fields are `session_id`, `transcript_path`, `cwd`, `permission_mode`, and `hook_event_name`. PreToolUse adds `tool_name`, `tool_input`, and `tool_use_id`. ([hooks](https://code.claude.com/docs/en/hooks#pretooluse-input))

| tool_name | tool_input fields |
|---|---|
| `Read` | `file_path` (always absolute; `~` and relative paths are expanded before the hook runs), `offset`, `limit` |
| `Edit` | `file_path`, `old_string`, `new_string`, `replace_all` |
| `Write` | `file_path`, `content` |
| `Grep` | `pattern`, `path`, `glob`, `output_mode`, `-i`, `multiline` |
| `Glob` | `pattern`, `path` |
| `Bash` | `command`, `description`, `timeout`, `run_in_background` |

Source: [hooks, PreToolUse input](https://code.claude.com/docs/en/hooks#pretooluse-input).

**Output, option A:** exit `2`. This blocks the tool call, and stderr is shown to Claude as the reason. Exit `1` is **non-blocking**, so the action proceeds. ([hooks, exit codes](https://code.claude.com/docs/en/hooks#exit-code-2))

**Output, option B:** exit `0` and print JSON:

```json
{
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "deny",
    "permissionDecisionReason": "data/bank is off-limits to agents"
  }
}
```

The `permissionDecision` values are `allow`, `deny`, `ask`, and `defer`. When several hooks disagree, precedence is `deny > defer > ask > allow`. "Deny and ask rules are still evaluated regardless of what the hook returns", so a hook `allow` cannot override a `permissions.deny` rule. Optional fields are `updatedInput` and `additionalContext`. The old top-level `decision`/`reason` fields are deprecated for this event. ([hooks, decision control](https://code.claude.com/docs/en/hooks#pretooluse-decision-control))

**Matcher:** `"Read|Grep|Glob|Bash|Edit|Write"`. Letters, digits, `_`, `-`, spaces, `,`, and `|` are matched exactly. Any other character makes the matcher a regex. ([hooks](https://code.claude.com/docs/en/hooks))

**Gaps in hooks:**
- PreToolUse does **not** fire for `@file` references in the prompt. Use a Read deny rule for those. ([hooks, PreToolUse warning](https://code.claude.com/docs/en/hooks#pretooluse))
- `${CLAUDE_PROJECT_DIR}` stays at the original root inside worktrees, so read `cwd` from stdin instead. ([worktrees](https://code.claude.com/docs/en/worktrees))
- Settings-file hooks run even in folders that are not trusted. ([permissions](https://code.claude.com/docs/en/permissions#what-runs-before-you-trust-a-folder))
- Hooks run **outside** the sandbox. ([sandboxing](https://code.claude.com/docs/en/sandboxing))

### `permissions.deny` for a folder

- Path syntax uses gitignore rules: `//abs`, `~/home`, `/relative-to-settings-file` (for project settings that means the project root), and `./` or a bare path, which is relative to the cwd. A single-segment directory pattern in a **deny** rule matches at any depth. ([permissions, Read and Edit](https://code.claude.com/docs/en/permissions#read-and-edit))
- A rule written as `Read(./data/bank/**)` is relative to the session cwd. `Read(/data/bank/**)` anchors to the project root of the settings file, and in a worktree session it matches that worktree's copy. ([permissions](https://code.claude.com/docs/en/permissions#read-and-edit))
- **Coverage:** "Read and Edit deny rules apply to Claude's built-in file tools, to file commands Claude Code recognizes in Bash, such as `cat`, `head`, `tail`, `sed`, and `tee`, and to the targets of Bash redirections… They don't apply to a command that reads files without naming them, such as `grep -r pattern .` … or to arbitrary subprocesses… like a Python or Node script". Grep and Glob are covered: "Claude Code applies `Read` deny rules to that directory". `@` mentions and IDE context are covered on a "best-effort" basis. Deny matches if either the requested path or its symlink target matches. ([permissions](https://code.claude.com/docs/en/permissions#read-and-edit), [settings reference, permissions.deny](https://code.claude.com/docs/en/settings-reference#permissions-deny))
- `.claudeignore` "has no effect". ([permissions](https://code.claude.com/docs/en/permissions#read-and-edit))
- `deny` and `ask` rules apply **immediately**, even before trust, whereas `allow` waits for trust. ([settings](https://code.claude.com/docs/en/settings))

### Sandbox

- An OS-enforced boundary on Bash, PowerShell, and Monitor commands **and their child processes**. It runs on macOS, Linux, and WSL2. Native Windows has no sandbox. ([sandboxing](https://code.claude.com/docs/en/sandboxing))
- "Paths and domains from both sandbox settings and permission rules are merged into the final sandbox configuration", including `Read` deny rules. ([sandboxing, permission rules](https://code.claude.com/docs/en/sandboxing#permission-rules))
- Set `allowUnsandboxedCommands: false` so Claude cannot retry a command outside the sandbox. Set `failIfUnavailable: true` so the session exits instead of silently running unsandboxed. ([sandboxing](https://code.claude.com/docs/en/sandboxing))
- Sandbox paths use the normal convention (`./x` is relative to the project, `/x` is absolute). This is **different** from permission rules. ([settings reference, sandbox path prefixes](https://code.claude.com/docs/en/settings-reference#sandbox-path-prefixes))
- The sandbox does **not** cover the Read, Edit, or WebFetch tools (permission rules do), **MCP servers**, or **hooks**. ([sandboxing](https://code.claude.com/docs/en/sandboxing))

### The most robust way to stop an agent reading a folder

Ranked from weakest to strongest:

1. A hook alone: bypassed by `@` mentions, and does not see subprocesses.
2. `permissions.deny` alone: does not cover Python or Node scripts, `grep -r .`, or MCP servers.
3. Deny plus sandbox (strict) plus hook: covers the built-in tools, every Bash subprocess at OS level, and logs attempts. The remaining gaps are MCP servers (for example a filesystem MCP) and anything a human pastes in.
4. **Physical separation**: keep the raw export outside the repo, on a machine or account with no Claude session. Only the privacy gate's output (pseudonymised) enters the repo.

Use 3 everywhere and 4 for the hour-0 export. The sources are the sections above. The ranking is our synthesis.

### Parallel work

- `claude --worktree <name>` (or `-w`) creates `.claude/worktrees/<name>/` on branch `worktree-<name>`. The repo already gitignores `.claude/worktrees/`. `.worktreeinclude` copies gitignored files into each worktree, so **never list `data/bank/` there**. `worktree.baseRef: "head"` branches from current work instead of `origin/HEAD`. Project-scope plugins and permission approvals are shared across worktrees. ([worktrees](https://code.claude.com/docs/en/worktrees))
- A subagent with `isolation: worktree` gets its own temporary worktree, which is auto-removed if it made no changes. ([worktrees](https://code.claude.com/docs/en/worktrees#isolate-subagents-with-worktrees))
- **Agent view:** `claude agents` is one screen for background sessions. Use `/bg` or `←` to background the current session. It is a research preview. ([agent view](https://code.claude.com/docs/en/agent-view), [agents](https://code.claude.com/docs/en/agents))
- **Agent teams** are experimental and disabled by default. ([agents](https://code.claude.com/docs/en/agents))
- **`/loop`:** `/loop 5m <prompt>` runs on a fixed interval. `/loop <prompt>` lets Claude choose the interval. It is session-scoped with a 1-minute minimum and works on Foundry. ([scheduled tasks](https://code.claude.com/docs/en/scheduled-tasks), [feature availability](https://code.claude.com/docs/en/feature-availability))

---

## 6. Pitch deck

- **Slides artifact: not available on Foundry.** Artifacts "are available on Pro, Max, Team, and Enterprise plans and require a session signed in with `/login`". ([artifacts](https://code.claude.com/docs/en/artifacts), [feature availability](https://code.claude.com/docs/en/feature-availability)). If a teammate has a separate claude.ai seat and uses **only non-client content**, a Slides artifact is an option, but that is a different data path from Foundry.
- **`pptx` skill** (`document-skills@anthropic-agent-skills`) is the Foundry-safe route. It creates and edits `.pptx` and `.potx` files, including templates, layouts, and speaker notes. ([anthropics/skills pptx SKILL.md](https://github.com/anthropics/skills/blob/main/skills/pptx/SKILL.md)). Use the bank's `.potx` template if one is available. The deck should show synthetic demo data only.

---

## Proposed `.claude/settings.json` (not applied)

```json
{
  "extraKnownMarketplaces": {
    "claude-plugins-official": {
      "source": { "source": "github", "repo": "anthropics/claude-plugins-official" }
    },
    "anthropic-agent-skills": {
      "source": { "source": "github", "repo": "anthropics/skills" }
    }
  },
  "enabledPlugins": {
    "frontend-design@claude-plugins-official": true,
    "playwright@claude-plugins-official": true,
    "security-guidance@claude-plugins-official": true,
    "pr-review-toolkit@claude-plugins-official": true,
    "feature-dev@claude-plugins-official": true,
    "hookify@claude-plugins-official": true,
    "document-skills@anthropic-agent-skills": true
  },
  "permissions": {
    "deny": [
      "Read(/data/bank/**)",
      "Edit(/data/bank/**)",
      "Read(**/*.client-export.*)"
    ]
  },
  "sandbox": {
    "enabled": true,
    "allowUnsandboxedCommands": false,
    "failIfUnavailable": true,
    "filesystem": { "denyRead": ["./data/bank"] }
  },
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Read|Grep|Glob|Bash|Edit|Write",
        "hooks": [
          { "type": "command", "command": "${CLAUDE_PROJECT_DIR}/.claude/hooks/data-guard.py" }
        ]
      }
    ]
  }
}
```

Notes:

- `Read(/data/bank/**)` with a single leading slash anchors to the project root of this settings file. `//` would mean an absolute path. ([permissions](https://code.claude.com/docs/en/permissions#read-and-edit))
- `"Read(**/*.client-export.*)"` is a placeholder pattern. Rename it to match the real export filename once known.
- Add `jdtls-lsp@claude-plugins-official` or `typescript-lsp@claude-plugins-official` once the stack is chosen.
- `.claude/hooks/data-guard.py` exists in the working tree but is untracked, and `.gitignore` is modified (both changes were made outside this research). Check that the hook reads `cwd` and `tool_input` and exits `2` or prints `permissionDecision: "deny"` as described in section 5.

---

## Could not verify

- Whether `claude-plugins-official` has release tags or another stable ref to pin to. Only `main` was observed, and the docs show `ref` examples only for generic repos.
- Whether redeclaring `claude-plugins-official` in `extraKnownMarketplaces` conflicts with the automatic registration. The docs say same-name entries are resolved by precedence, but nothing covers the built-in entry specifically.
- Whether sandbox `denyRead` blocks reads by a stdio **MCP server** launched by Claude Code. The docs say MCP servers run outside the sandbox, which implies it does not.
- Whether Glob and Grep results skip gitignored paths by default. This was not checked, so do not rely on `.gitignore` for privacy.
- Whether a Foundry deployment exists in an EU data zone. Only Global Standard and US Data Zone were listed on 2026-10-08.
- The Figma MCP and chrome-devtools-mcp data flows. Their vendor docs were not read.
- The exact behaviour of the `frontend-design` copy in `anthropics/skills` compared with the official plugin. Only the official plugin's SKILL.md was read.
- Context7's statement that model-written queries exclude personal data is a vendor claim. It is not enforceable by us.
