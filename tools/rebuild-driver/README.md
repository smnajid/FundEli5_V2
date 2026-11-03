# Rebuild driver

The unattended driver used for the rebuild rehearsal (2026-11-01 to 2026-11-03). It builds the tickets of this repo with headless Claude Code sessions. State lives on GitHub, so a restart resumes.

- `driver.sh` charts the map once (`map-prompt.md`), then claims `ready-for-agent` tickets, at most `MAX_PAR` at once, each in its own git worktree with `claude -p --model sonnet --dangerously-skip-permissions` (`ticket-prompt.md`). It waits 15 min when the plan limit is hit, labels `agent-stuck` after 3 attempts, and leaves a ticket labelled `needs-info` when the agent posts a `STOP:` comment. At the end it writes `docs/exercise/timing-log.md` (`timing.py`).
- `snapshot.py` builds `status.json` from the GitHub issues, their blocking links and `runs/<n>.json`, and prints the tickets ready to build. `ONLY="53 54"` limits which tickets this driver builds; tickets labelled `local` are skipped when `ONLY` is not set on a machine that must leave them alone.
- `activity.py` and `live.sh` + `live/index.html` show what each agent is doing now (port 8090).

## Run it

```
mkdir -p ~/rebuild-driver && cp -r tools/rebuild-driver/* ~/rebuild-driver && cd ~/rebuild-driver
MAIN=<clone of the repo> WT=<folder for worktrees> MAX_PAR=2 ONLY="<ticket numbers>" NO_DEMO=1 ./driver.sh
```

Needs `gh` (logged in), `claude` on the plan you want to use, and `TYPESAFE_API_KEY` exported in `~/.bashrc` for the Jev page ranking.

## What the rehearsal taught

- Plan-limit waits were most of the elapsed time (about 3 h of agent work in about 15 h). The timing log's wall time includes them.
- A ticket's `STOP:` comment is the useful signal: every stop was a missing decision, not a bug.
- Hard-coded to Sonnet and `--dangerously-skip-permissions`: run it only on a machine where that is acceptable.
