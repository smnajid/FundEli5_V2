#!/usr/bin/env python3
"""Writes activity.json: what each running build agent is doing now, read from its live session transcript.

For every ticket with a worktree under ~/projects/v2-wt it gives the last actions (tool and target, or a line of
text), seconds since the last event, the branch's commits and uncommitted file count, and whether a build or test
process is running in that worktree. Plus the driver log's tail.
"""
import glob, json, os, subprocess, datetime, re

HERE = os.path.dirname(os.path.abspath(__file__))
WT = os.path.expanduser("~/projects/v2-wt")
PROJECTS = os.path.expanduser("~/.claude/projects")


def sh(cmd, cwd=None):
    return subprocess.run(cmd, shell=True, cwd=cwd, capture_output=True, text=True).stdout.strip()


def short(s, n=140):
    s = re.sub(r"\s+", " ", s or "").strip()
    return s if len(s) <= n else s[: n - 1] + "…"


def describe(block):
    """One human line for a tool_use or text block."""
    if block.get("type") == "text":
        return "says: " + short(block["text"])
    if block.get("type") != "tool_use":
        return None
    name, inp = block.get("name", ""), block.get("input", {}) or {}
    if name == "Bash":
        return "runs: " + short(inp.get("description") or inp.get("command"))
    if name in ("Read", "Edit", "Write"):
        verb = {"Read": "reads", "Edit": "edits", "Write": "writes"}[name]
        return f"{verb} {inp.get('file_path', '').replace('/home/azureuser/projects/v2-wt/', '')}"
    if name in ("Grep", "Glob"):
        return f"searches {short(inp.get('pattern'), 60)}"
    if name == "Agent":
        return f"asks {inp.get('subagent_type', 'an agent')}: {short(inp.get('description'), 80)}"
    if name == "Skill":
        return f"uses skill {inp.get('skill')}"
    return name


def last_events(ticket, limit=8):
    d = os.path.join(PROJECTS, f"-home-azureuser-projects-v2-wt-{ticket}")
    files = sorted(glob.glob(os.path.join(d, "*.jsonl")), key=os.path.getmtime)
    if not files:
        return [], None
    path = files[-1]
    lines = subprocess.run(["tail", "-n", "400", path], capture_output=True, text=True).stdout.splitlines()
    events, last_ts = [], None
    for line in lines:
        try:
            e = json.loads(line)
        except ValueError:
            continue
        ts = e.get("timestamp")
        if ts:
            last_ts = ts
        msg = e.get("message")
        if e.get("type") == "assistant" and isinstance(msg, dict):
            for b in msg.get("content") or []:
                text = describe(b)
                if text:
                    events.append({"at": ts, "text": text})
    return events[-limit:], last_ts


def main():
    now = datetime.datetime.now(datetime.timezone.utc)
    procs = sh("ps -eo args")
    tickets = []
    for dirpath in sorted(glob.glob(os.path.join(WT, "*")), key=lambda p: int(os.path.basename(p))):
        n = int(os.path.basename(dirpath))
        events, last_ts = last_events(n)
        idle = None
        if last_ts:
            idle = int((now - datetime.datetime.fromisoformat(last_ts.replace("Z", "+00:00"))).total_seconds())
        running = []
        for tool, label in (("mvnw", "Maven"), ("surefire", "Maven tests"), ("ng test", "Angular tests"), ("ng build", "Angular build"),
                            ("npm", "npm"), ("docker", "Docker")):
            if re.search(rf"{re.escape(dirpath)}.*{re.escape(tool)}|{re.escape(tool)}.*{re.escape(dirpath)}", procs):
                running.append(label)
        commits = sh("git log --oneline origin/main..HEAD", dirpath).splitlines()
        tickets.append({
            "number": n,
            "branch": sh("git rev-parse --abbrev-ref HEAD", dirpath),
            "idleSeconds": idle,
            "lastEventAt": last_ts or "",
            "events": events,
            "commits": commits[:5],
            "uncommittedFiles": len(sh("git status --porcelain", dirpath).splitlines()),
            "running": running,
        })
    log = sh(f"tail -n 12 {HERE}/logs/driver.log").splitlines()
    out = {"at": now.isoformat(timespec="seconds"), "tickets": tickets, "driverLog": log}
    tmp = os.path.join(HERE, "activity.json.tmp")
    json.dump(out, open(tmp, "w"), indent=1)
    os.replace(tmp, os.path.join(HERE, "activity.json"))


if __name__ == "__main__":
    main()
