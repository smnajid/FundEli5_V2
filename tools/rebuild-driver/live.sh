#!/bin/bash
# Serves the live page on port 8090 (localhost only) and refreshes its data: activity every 10 s, tickets every 60 s.
cd "$(dirname "$0")"
mkdir -p live
ln -sf ../status.json live/status.json
ln -sf ../activity.json live/activity.json
(cd live && python3 -m http.server 8090 --bind 127.0.0.1 >/dev/null 2>&1) &
n=0
while :; do
  python3 activity.py 2>>logs/activity.err
  if [ $((n % 6)) -eq 0 ]; then python3 snapshot.py >/dev/null 2>>logs/snapshot.err; fi
  n=$((n+1))
  sleep 10
done
