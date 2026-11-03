#!/usr/bin/env python3
"""Writes status.json: every ticket of the repo with its board status, blockers, PR and timings.

Status: built (closed with a merged PR or completed), in_progress (label in-progress), needs_you (label
needs-info or agent-stuck), blocked (an open blocker), ready (open, unblocked, labelled ready-for-*),
decision (an open wayfinder ticket), skipped (closed as not planned).
"""
import json, os, re, subprocess, sys, datetime

REPO = os.environ.get("REPO", "smnajid/FundEli5_V2")
HERE = os.path.dirname(os.path.abspath(__file__))
RUNS = os.path.join(HERE, "runs")


def gh(*args):
    out = subprocess.run(["gh", *args], capture_output=True, text=True, check=True).stdout
    return json.loads(out) if out.strip() else None


def blockers(number):
    try:
        deps = gh("api", f"repos/{REPO}/issues/{number}/dependencies/blocked_by")
    except subprocess.CalledProcessError:
        return []
    return [{"number": d["number"], "state": d["state"]} for d in deps or []]


def run_info(number):
    path = os.path.join(RUNS, f"{number}.json")
    if not os.path.exists(path):
        return {}
    with open(path) as f:
        return json.load(f)


def minutes(start, end):
    if not start:
        return None
    s = datetime.datetime.fromisoformat(start)
    e = datetime.datetime.fromisoformat(end) if end else datetime.datetime.now(datetime.timezone.utc)
    return round((e - s).total_seconds() / 60)


def field(body, name):
    m = re.search(rf"^{name}:\s*(.+)$", body or "", re.M | re.I)
    return m.group(1).strip() if m else ""


def try_command(body):
    for cmd in re.findall(r"`([^`]+)`", body or ""):
        if "cucumber.filter.tags" in cmd or cmd.startswith("./mvnw") or cmd.startswith("ng ") or cmd.startswith("npm "):
            return cmd
    return ""


def main():
    issues = gh("issue", "list", "-R", REPO, "--state", "all", "--limit", "200",
                "--json", "number,title,url,state,stateReason,labels,body,createdAt,closedAt,assignees")
    prs = gh("pr", "list", "-R", REPO, "--state", "all", "--limit", "200",
             "--json", "number,url,headRefName,mergedAt,state,body,createdAt")
    rows = []
    local = set()
    for i in issues:
        labels = [l["name"] for l in i["labels"]]
        if "wayfinder:map" in labels:
            continue
        n = i["number"]
        pr = next((p for p in prs if re.search(rf"(Closes|Fixes|Resolves) #{n}\b", p["body"] or "", re.I)
                   or p["headRefName"].startswith(f"build/{n}-")), None)
        run = run_info(n)
        open_blockers = []
        if i["state"] == "OPEN":
            open_blockers = [b["number"] for b in blockers(n) if b["state"] == "open"]
        if i["state"] == "CLOSED":
            status = "skipped" if i.get("stateReason") == "NOT_PLANNED" else "built"
        elif "in-progress" in labels:
            status = "in_progress"
        elif "needs-info" in labels or "agent-stuck" in labels:
            status = "needs_you"
        elif open_blockers:
            status = "blocked"
        elif any(l.startswith("ready-for-") for l in labels):
            status = "ready"
        elif any(l.startswith("wayfinder:") for l in labels):
            status = "decision"
        else:
            status = "untriaged"
        if "local" in labels:
            local.add(n)
        mode = "HITL" if "ready-for-human" in labels else "AFK" if "ready-for-agent" in labels else ""
        started = run.get("startedAt")
        ended = run.get("endedAt") or (pr or {}).get("mergedAt") if status == "built" else run.get("endedAt")
        rows.append({
            "number": n,
            "title": i["title"],
            "url": i["url"],
            "status": status,
            "mode": mode,
            "phase": field(i["body"], "Phase") or "Other",
            "blockedBy": " ".join(f"#{b}" for b in open_blockers),
            "pr": f"#{pr['number']}" if pr else "",
            "prUrl": pr["url"] if pr else "",
            "startedAt": started or "",
            "endedAt": ended or "",
            "wallMinutes": minutes(started, ended) if started else None,
            "attempts": run.get("attempts", 0),
            "tryCommand": try_command(i["body"]),
            "note": run.get("note", ""),
        })
    rows.sort(key=lambda r: r["number"])
    status = {
        "snapshotAt": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
        "run": json.load(open(os.path.join(HERE, "run.json"))) if os.path.exists(os.path.join(HERE, "run.json")) else {},
        "tickets": rows,
    }
    tmp = os.path.join(HERE, "status.json.tmp")
    with open(tmp, "w") as f:
        json.dump(status, f, indent=1)
    os.replace(tmp, os.path.join(HERE, "status.json"))
    # Ready numbers for the driver, in ticket order
    # ONLY="53 54": a driver that builds just these (the laptop); otherwise skip tickets labelled `local`
    only = {int(x) for x in os.environ.get("ONLY", "").split()}
    ready = [r["number"] for r in rows if r["status"] == "ready" and r["mode"]]
    print(" ".join(str(n) for n in ready if (n in only if only else n not in local)))


if __name__ == "__main__":
    sys.exit(main())
