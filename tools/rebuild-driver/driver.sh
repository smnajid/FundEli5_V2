#!/bin/bash
# Runs the FundEli5_V2 rebuild unattended: charts the map once, then builds ready tickets, at most MAX_PAR at once,
# each in its own worktree with a headless Sonnet session. State lives on GitHub, so a restart resumes.
set -uo pipefail

REPO=smnajid/FundEli5_V2
HERE="$(cd "$(dirname "$0")" && pwd)"
MAIN=${MAIN:-~/projects/FundEli5_V2}
WT=${WT:-~/projects/v2-wt}
MAX_PAR=${MAX_PAR:-3}
MAX_ATTEMPTS=3
export PATH="$HOME/.local/bin:$PATH"
# Sonnet everywhere: the session, its subagents, and any agent that asks for opus
export ANTHROPIC_DEFAULT_OPUS_MODEL=claude-sonnet-5-5
export CLAUDE_CODE_SUBAGENT_MODEL=claude-sonnet-5-5
export REPO
CLAUDE=(claude -p --model sonnet --dangerously-skip-permissions --output-format json)

mkdir -p "$HERE/runs" "$HERE/logs" "$HERE/pids" "$WT"
log() { echo "$(date -u +%FT%TZ) $*" | tee -a "$HERE/logs/driver.log"; }
now() { date -u +%FT%T+00:00; }

load_secrets() {
  eval "$(grep -h '^export TYPESAFE_API_KEY=' ~/.bashrc ~/.fundeli5.env 2>/dev/null | tail -1)" || true
}

set_run() { # key value: one field of runs/<n>.json
  python3 - "$HERE/runs/$1.json" "$2" "$3" <<'EOF'
import json, os, sys
p, k, v = sys.argv[1:]
d = json.load(open(p)) if os.path.exists(p) else {}
d[k] = int(v) if v.isdigit() else v
json.dump(d, open(p, "w"))
EOF
}

get_run() { python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get(sys.argv[2], ""))' "$HERE/runs/$1.json" "$2" 2>/dev/null; }

issue_state() { gh issue view "$1" -R $REPO --json state,labels -q '.state + " " + ([.labels[].name] | join(","))'; }

chart_map() {
  if [ -n "$(gh issue list -R $REPO --label wayfinder:map --state all --json number -q '.[0].number')" ]; then
    return
  fi
  log "charting the map"
  python3 -c 'import json,sys; json.dump({"startedAt": sys.argv[1], "model": "Sonnet 5.5", "vm": "Standard_E4s_v3", "maxParallel": int(sys.argv[2])}, open(sys.argv[3], "w"))' "$(now)" "$MAX_PAR" "$HERE/run.json"
  (cd "$MAIN" && git pull -q && "${CLAUDE[@]}" "$(cat "$HERE/map-prompt.md")" > "$HERE/logs/map.json" 2> "$HERE/logs/map.err")
  python3 - "$HERE/run.json" "$(now)" <<'EOF'
import json, sys
d = json.load(open(sys.argv[1])); d["mapChartedAt"] = sys.argv[2]; json.dump(d, open(sys.argv[1], "w"))
EOF
  log "map charted"
}

restart_demo() { [ -n "${NO_DEMO:-}" ] && return 0 # keeps the latest main running on 8080 (backend) and 4200 (Angular) for whoever tunnels in
  (cd "$MAIN" && git pull -q) || return
  if [ -x "$MAIN/backend/mvnw" ]; then
    tmux kill-window -t rebuild:demo-backend 2>/dev/null
    tmux new-window -d -t rebuild -n demo-backend "cd $MAIN/backend && ./mvnw -q spring-boot:run 2>&1 | tee $HERE/logs/demo-backend.log"
  fi
  if [ -f "$MAIN/frontend/package.json" ]; then
    tmux kill-window -t rebuild:demo-frontend 2>/dev/null
    tmux new-window -d -t rebuild -n demo-frontend "cd $MAIN/frontend && npm ci --silent && npx ng serve --port 4200 --host 127.0.0.1 2>&1 | tee $HERE/logs/demo-frontend.log"
  fi
}

run_ticket() { # number: one ticket from claim to merge or stop; runs in the background
  local n=$1 slug branch dir out attempt session prompt
  slug=$(gh issue view "$n" -R $REPO --json title -q .title | tr 'A-Z' 'a-z' | tr -cs 'a-z0-9' '-' | cut -c1-40 | sed 's/-$//')
  branch="build/$n-$slug"
  dir="$WT/$n"
  gh issue edit "$n" -R $REPO --add-label in-progress --add-assignee @me >/dev/null
  [ -n "$(get_run "$n" startedAt)" ] || set_run "$n" startedAt "$(now)"
  (cd "$MAIN" && git fetch -q origin)
  if [ ! -d "$dir" ]; then
    if git -C "$MAIN" ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
      git -C "$MAIN" worktree add -q "$dir" -B "$branch" "origin/$branch"
    else
      git -C "$MAIN" worktree add -q "$dir" -B "$branch" origin/main
    fi
  fi
  prompt=$(sed -e "s|{{N}}|$n|g" -e "s|{{BRANCH}}|$branch|g" "$HERE/ticket-prompt.md")
  session=""
  while :; do
    attempt=$(get_run "$n" attempts)
    attempt=$(( ${attempt:-0} + 1 ))
    set_run "$n" attempts "$attempt"
    load_secrets
    out="$HERE/logs/$n-$attempt.json"
    log "#$n attempt $attempt"
    if [ -n "$session" ]; then
      (cd "$dir" && "${CLAUDE[@]}" --resume "$session" "Continue ticket #$n where you left off, then finish it as the first prompt said." > "$out" 2> "${out%.json}.err")
    else
      (cd "$dir" && "${CLAUDE[@]}" "$prompt" > "$out" 2> "${out%.json}.err")
    fi
    session=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get("session_id",""))' "$out" 2>/dev/null)
    read -r state labels <<<"$(issue_state "$n")"
    if [ "$state" = CLOSED ]; then
      set_run "$n" endedAt "$(now)"; log "#$n built"
      gh issue edit "$n" -R $REPO --remove-label in-progress >/dev/null
      git -C "$MAIN" worktree remove --force "$dir"
      restart_demo
      break
    fi
    if [[ ",$labels," == *",needs-info,"* ]]; then
      set_run "$n" note "stopped: needs Mohamed"; log "#$n stopped, needs-info"
      gh issue edit "$n" -R $REPO --remove-label in-progress >/dev/null
      break
    fi
    if grep -qiE "hit your [^\"]*limit|usage limit|limit reached|rate.?limit|overloaded" "$out" "${out%.json}.err" 2>/dev/null; then
      set_run "$n" note "waiting for plan usage limit"; log "#$n hit a usage limit, waiting 15 min"
      sleep 900
      set_run "$n" attempts "$((attempt - 1))"
      continue
    fi
    if [ "$attempt" -ge "$MAX_ATTEMPTS" ]; then
      set_run "$n" note "agent stuck after $attempt attempts"; log "#$n stuck"
      gh issue edit "$n" -R $REPO --remove-label in-progress --add-label agent-stuck >/dev/null
      gh issue comment "$n" -R $REPO --body "STOP: the build agent ended $attempt times without merging. Logs on the VM: ~/projects/fundeli5-driver/logs/$n-*.json" >/dev/null
      break
    fi
  done
  rm -f "$HERE/pids/$n"
}

running() { local c=0 f; for f in "$HERE"/pids/*; do [ -e "$f" ] || continue; if kill -0 "$(cat "$f")" 2>/dev/null; then c=$((c+1)); else rm -f "$f"; fi; done; echo $c; }

write_timing_log() {
  (cd "$MAIN" && git pull -q --rebase) || return
  python3 "$HERE/timing.py" "$HERE/status.json" "$MAIN/docs/exercise/timing-log.md" || return
  (cd "$MAIN" && git add docs/exercise/timing-log.md && git commit -qm "Timing log: rebuild run results

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" && git push -q) || true
}

main() {
  gh label create agent-stuck -R $REPO --color b60205 --description "The build agent could not finish" --force >/dev/null
  chart_map
  local idle=0
  while :; do
    ready=$(python3 "$HERE/snapshot.py" 2>>"$HERE/logs/snapshot.err")
    for n in $ready; do
      [ "$(running)" -lt "$MAX_PAR" ] || break
      [ -e "$HERE/pids/$n" ] && continue
      run_ticket "$n" & echo $! > "$HERE/pids/$n"
      log "#$n started"
      sleep 5
    done
    # Done only when every ticket is closed; otherwise wait for Mohamed to unblock a needs-you ticket
    left=$(python3 -c 'import json,sys; print(sum(t["status"] not in ("built","skipped") for t in json.load(open(sys.argv[1]))["tickets"]))' "$HERE/status.json")
    if [ -z "$ready" ] && [ "$(running)" -eq 0 ] && [ "$left" -eq 0 ]; then
      idle=$((idle+1))
      if [ $idle -ge 2 ]; then
        python3 - "$HERE/run.json" "$(now)" <<'EOF'
import json, sys
d = json.load(open(sys.argv[1])); d["endedAt"] = sys.argv[2]; json.dump(d, open(sys.argv[1], "w"))
EOF
        python3 "$HERE/snapshot.py" >/dev/null 2>&1
        write_timing_log
        log "nothing left to build: done"
        break
      fi
    else
      idle=0
    fi
    sleep 60
  done
}

main "$@"
