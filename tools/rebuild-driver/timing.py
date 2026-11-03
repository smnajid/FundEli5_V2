#!/usr/bin/env python3
"""Rewrites the ticket table of docs/exercise/timing-log.md from status.json at the end of the run."""
import json, sys

status_path, log_path = sys.argv[1:]
status = json.load(open(status_path))
run = status.get("run", {})
rows = [t for t in status["tickets"] if t["startedAt"]]
rows.sort(key=lambda t: t["startedAt"])

def hm(m):
    return "" if m is None else f"{m // 60} h {m % 60:02d}" if m >= 60 else f"{m} min"

lines = [
    "# Timing log",
    "",
    f"Unattended run on the VM ({run.get('vm', '')}), {run.get('model', '')}, at most {run.get('maxParallel', '')} tickets at once. "
    f"Started {run.get('startedAt', '')}, map charted {run.get('mapChartedAt', '')}, ended {run.get('endedAt', '')}.",
    "Wall time runs from the agent claiming the ticket to the merge. Attempts counts headless sessions, including resumes after a usage limit.",
    "",
    "| Ticket | Phase | Mode | Status | Started | Ended | Wall time | Attempts | PR | Note |",
    "|---|---|---|---|---|---|---|---|---|---|",
]
for t in rows:
    lines.append(f"| #{t['number']} {t['title']} | {t['phase']} | {t['mode']} | {t['status']} | {t['startedAt'][:16]} | "
                 f"{t['endedAt'][:16]} | {hm(t['wallMinutes'])} | {t['attempts']} | {t['pr']} | {t['note']} |")
open(log_path, "w").write("\n".join(lines) + "\n")
