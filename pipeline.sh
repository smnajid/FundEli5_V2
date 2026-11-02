#!/usr/bin/env bash
# The Rule Card pipeline (offline runtime, no web server, no Client Profile): ./pipeline.sh --isin=<ISIN> [--isin=<ISIN>...]
# Needs TYPESAFE_API_KEY for page selection. Draft cards land in backend/target/draft-rule-cards.
set -euo pipefail
: "${TYPESAFE_API_KEY:?TYPESAFE_API_KEY is not set (page selection needs it)}"
cd "$(dirname "$0")/backend"
./mvnw -q -DskipTests package
exec java -jar target/fundeli5-*.jar --spring.profiles.active=pipeline "$@"
