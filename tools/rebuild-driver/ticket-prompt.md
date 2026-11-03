You are building ticket #{{N}} of FundEli5_V2 in an unattended, timed rebuild. Mohamed delegated the human side of HITL tickets to you: there is no one to answer questions in this session.

You are in a git worktree on branch `{{BRANCH}}`, created from the latest origin/main.

1. Read CLAUDE.md, the ticket (`gh issue view {{N}} --comments`) and everything it lists under "Read first". Read docs/playbook/tech-choices.md before writing code.
2. Build it. Mode: a `ready-for-human` ticket uses /tdd, and you pick each next example yourself; a `ready-for-agent` ticket uses /implement. Commit often and push the branch (`git push -u origin {{BRANCH}}`) after each green step, so the work survives an interruption.
3. When the ticket's "Done when" holds: run its Cucumber tags, `@answer-key` once the Rule Engine exists, the full `./mvnw test` in backend/, and `npm run build` and `npm test -- --watch=false` in frontend/ if you touched it. Run the gate agents the ticket names (Agent tool, subagent types such as java-reviewer, typescript-reviewer, privacy-gate-auditor, engine-purity-reviewer) and fix what they find.
4. Merge: `git fetch origin && git rebase origin/main`, re-run the tests, push with `--force-with-lease`, open a PR whose body follows the pr skill, starts with `Closes #{{N}}` and ends with a "How to try" section (commands that show the ticket working, and what to look at on screen if it has one). Then `gh pr merge --squash --delete-branch` and check that issue #{{N}} is closed.

Before the Rule Engine is complete (ticket "S4d Answer key green and the four promises in JUnit" still open): `@answer-key` is not a gate, and a scenario of your tags that fails only because it asserts a Verdict the unfinished engine cannot give yet is accepted. List those scenarios in the PR under "Waiting for the Rule Engine" and merge; the Integrate ticket re-runs them. Everything else in your tags must pass.

Rules:
- Never edit specs/ or docs/playbook/hypotheses.md, generated client code, or another module's package outside the ticket's Seam.
- Never edit docs/exercise/timing-log.md: the driver logs time.
- Never read or copy code from smnajid/fund-gate. Its docs and issues are fine.
- Never bind ports 8080 or 4200: they belong to the demo instance. Tests use random ports.
- Never call a model except through the app's egress client; tests run in replay mode.
- Stop conditions (the ticket's "Stop and ask when", or a missing credential such as TYPESAFE_API_KEY): post a comment on #{{N}} starting with `STOP:` that says exactly what you need, add the label `needs-info`, push the branch, and end the session. Do not work around a stop condition.
