#!/usr/bin/env bash
# One command: build the Angular screens and the backend, start the dealer app on fixtures in replay mode.
#   ./start.sh            build and start, prints the URL
#   PORT=9090 ./start.sh  another port (default 8080)
# Presidio (ADR 0009) is used when it answers on $PRESIDIO_URL (default http://localhost:5002; start it with
#   docker build -t fundeli5-presidio docker/presidio && docker run -p 5002:3000 fundeli5-presidio, which loads
#   English, French and German); otherwise the fixture detector runs and says so.
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$PWD"
PORT="${PORT:-8080}"
PRESIDIO_URL="${PRESIDIO_URL:-http://localhost:5002}"

echo ">> Building the screens"
(cd frontend && { [ -d node_modules ] || npm ci; } && npm run build)

echo ">> Building the backend"
(cd backend && ./mvnw -q -DskipTests package)

detector=fixture
if curl -fs -m 2 -o /dev/null "$PRESIDIO_URL/health"; then
  detector=presidio
else
  echo ">> Presidio not reachable at $PRESIDIO_URL: using the fixture detector (knows only the demo names)"
fi

echo ">> Dealer screen: http://localhost:$PORT/dealer-screen   (validation: /validation-screen, API docs: /swagger-ui.html)"
cd backend
exec java -jar target/fundeli5-*.jar \
  --server.port="$PORT" \
  --fundeli5.screens.dir="$ROOT/frontend/dist/frontend/browser" \
  --fundeli5.llm.endpoint=replay \
  --fundeli5.privacygate.detector="$detector" \
  --fundeli5.privacygate.presidio.url="$PRESIDIO_URL"
