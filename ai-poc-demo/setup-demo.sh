#!/usr/bin/env bash
#
# AI Products PoC — console-first demo bootstrap.
#
# 1. Starts the local OpenAI-compatible mock upstream (if not already listening).
# 2. Registers catalog LLM provider sources + imports demo models via the AIM catalog API.
# 3. Prints which models work offline (mock) vs which need a real provider API key.
#
# Usage:
#   ./setup-demo.sh
#   MOCK_PORT=9099 GOOGLE_API_KEY=... OPENAI_API_KEY=sk-... ./setup-demo.sh
#
# After this script, use the Gamma console (http://localhost:8085):
#   AI Products → Create → set entrypoint + pick imported models → Create & deploy
#   Users tab → add user + token budget → curl the gateway entrypoint
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

MAPI="${MAPI:-http://localhost:8083}"
GAMMA="${GAMMA:-$MAPI/gamma}"
AUTH="${AUTH:-admin:admin}"
ORG_ID="${ORG_ID:-DEFAULT}"
ENV_ID="${ENV_ID:-DEFAULT}"
MOCK_PORT="${MOCK_PORT:-9099}"
# Gateway containers reach the compose mock at mock-llm; host.docker.internal works on Docker Desktop too.
MOCK_UPSTREAM="${MOCK_UPSTREAM:-http://mock-llm:${MOCK_PORT}/v1}"

CATALOG_BASE="$GAMMA/organizations/$ORG_ID/environments/$ENV_ID/modules/aim/catalog"

say()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
warn() { printf '\033[33m%s\033[0m\n' "$*"; }
die()  { printf '\033[31m%s\033[0m\n' "$*" >&2; exit 1; }
jqr()  { python3 -c "import json,sys;print(json.load(sys.stdin)$1)"; }

wait_mapi() {
  say "Waiting for Management API at $MAPI"
  for _ in $(seq 1 60); do
    if curl -sf -u "$AUTH" "$MAPI/management/v2/environments/$ENV_ID/apis/_search" \
         -X POST -H 'Content-Type: application/json' -d '{}' -o /dev/null 2>/dev/null; then
      return 0
    fi
    sleep 2
  done
  die "Management API not reachable — run: cd ai-poc-demo/stack && docker compose up -d"
}

ensure_mock() {
  if curl -sf "http://127.0.0.1:${MOCK_PORT}/v1/models" -o /dev/null 2>/dev/null; then
    say "Mock LLM reachable on :${MOCK_PORT} (compose service or host process)"
    return 0
  fi
  if docker compose -f "$SCRIPT_DIR/stack/compose.yaml" ps mock-llm 2>/dev/null | grep -qE 'running|healthy'; then
    warn "mock-llm container is up but :${MOCK_PORT} not ready yet — waiting…"
    for _ in $(seq 1 20); do
      curl -sf "http://127.0.0.1:${MOCK_PORT}/v1/models" -o /dev/null 2>/dev/null && return 0
      sleep 2
    done
  fi
  say "Starting mock LLM upstream on host :${MOCK_PORT} (fallback when not using compose mock-llm service)"
  nohup python3 "$SCRIPT_DIR/mock-llm.py" "$MOCK_PORT" >/tmp/ai-poc-mock-llm.log 2>&1 &
  for _ in $(seq 1 15); do
    if curl -sf "http://127.0.0.1:${MOCK_PORT}/v1/models" -o /dev/null 2>/dev/null; then
      echo "  pid=$!  log=/tmp/ai-poc-mock-llm.log"
      return 0
    fi
    sleep 1
  done
  die "Mock failed to start — see /tmp/ai-poc-mock-llm.log"
}

# create_source <sourceKind> <json-body>  → echoes source UUID
create_source() {
  local kind="$1" body="$2" resp id
  resp=$(curl -s -u "$AUTH" -H 'Content-Type: application/json' -X POST "$CATALOG_BASE/sources" -d "$body")
  id=$(printf '%s' "$resp" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("id",""))' 2>/dev/null || true)
  if [ -z "$id" ]; then
    # getOrCreate may return 200 for existing — try list + match sourceKind
    id=$(curl -s -u "$AUTH" "$CATALOG_BASE/sources?sourceKind=$kind&perPage=50" \
      | python3 -c 'import json,sys; d=json.load(sys.stdin); print(next((x["id"] for x in d.get("data",[]) if x.get("sourceKind")=="'"$kind"'"), ""))' 2>/dev/null || true)
  fi
  [ -n "$id" ] || { warn "Create source failed for $kind:"; printf '%s\n' "$resp" | head -c 800; die "Aborting."; }
  printf '%s' "$id"
}

# import_models <source-id> <candidate-id>...
import_models() {
  local source_id="$1"; shift
  local ids_json
  ids_json=$(python3 -c 'import json,sys; print(json.dumps(sys.argv[1:]))' "$@")
  curl -sf -u "$AUTH" -H 'Content-Type: application/json' \
    -X POST "$CATALOG_BASE/sources/$source_id/imports/models" \
    -d "{\"candidateIds\":$ids_json}" \
    | python3 -c 'import json,sys; d=json.load(sys.stdin); print("imported=%s skipped=%s" % (d.get("imported",0), d.get("skipped",0)))'
}

wait_mapi
ensure_mock

say "Registering catalog sources"

MOCK_OPENAI_BODY=$(cat <<JSON
{
  "sourceKind": "llm.provider.openai",
  "definition": {
    "type": "llm-provider",
    "name": "OpenAI (PoC mock)",
    "baseUrl": "$MOCK_UPSTREAM",
    "format": "OPEN_AI",
    "authMethod": { "type": "NONE" }
  }
}
JSON
)
MOCK_SOURCE_ID=$(create_source "llm.provider.openai" "$MOCK_OPENAI_BODY")
echo "  OpenAI mock source id=$MOCK_SOURCE_ID  baseUrl=$MOCK_UPSTREAM"

say "Importing demo models (mock-safe — any provider API key works)"
import_models "$MOCK_SOURCE_ID" "gpt-5.4-mini" "gpt-4o-mini"
echo "  Use model name gpt-5.4-mini or gpt-4o-mini in create form / curl"

if [ -n "${OPENAI_API_KEY:-}" ]; then
  REAL_OPENAI_BODY=$(cat <<JSON
{
  "sourceKind": "llm.provider.openai-real",
  "definition": {
    "type": "llm-provider",
    "name": "OpenAI (live)",
    "baseUrl": "https://api.openai.com/v1",
    "format": "OPEN_AI",
    "authMethod": { "type": "BEARER" }
  }
}
JSON
)
  REAL_SOURCE_ID=$(create_source "llm.provider.openai-real" "$REAL_OPENAI_BODY")
  echo "  OpenAI live source id=$REAL_SOURCE_ID"
  import_models "$REAL_SOURCE_ID" "gpt-5.4-mini"
  echo "  Real OpenAI: configure credentials on the managed LLM proxy (Secure → LLM Router), not in AI Products"
else
  warn "OPENAI_API_KEY not set — skipping live OpenAI catalog source (mock-only demo)"
fi

if [ -n "${GOOGLE_API_KEY:-}" ]; then
  GOOGLE_BODY=$(cat <<JSON
{
  "sourceKind": "llm.provider.google",
  "definition": {
    "type": "llm-provider",
    "name": "Google Gemini (live)",
    "baseUrl": "https://generativelanguage.googleapis.com/v1beta",
    "format": "GEMINI",
    "authMethod": { "type": "API_KEY", "headerName": "x-goog-api-key" }
  }
}
JSON
)
  GOOGLE_SOURCE_ID=$(create_source "llm.provider.google" "$GOOGLE_BODY")
  echo "  Google source id=$GOOGLE_SOURCE_ID"
  import_models "$GOOGLE_SOURCE_ID" "gemini-2.5-flash" "gemini-2.5-pro"
  echo "  Real Gemini: configure credentials on the managed LLM proxy (Secure → LLM Router), not in AI Products"
else
  warn "GOOGLE_API_KEY not set — skipping live Gemini (needs a Google AI Studio key)"
fi

say "Model testing guide"
cat <<'EOF'

┌─────────────────────────────────────────────────────────────────────────────┐
│ WITHOUT MOCK (real upstream)                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ • OpenAI (gpt-5.4-mini, gpt-4o-mini) — needs OPENAI_API_KEY                 │
│ • Google Gemini (gemini-2.5-flash)   — needs GOOGLE_API_KEY                 │
│   Re-run: GOOGLE_API_KEY=... ./setup-demo.sh to import Gemini catalog       │
├─────────────────────────────────────────────────────────────────────────────┤
│ WITH MOCK (offline, token-budget 429 demo)                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ • OpenAI source pointing at mock (created above, authMethod NONE)           │
│ • No provider API key in Create AI Product                                  │
│ • ~15 tokens per chat call (fast profile) → budget 100 ≈ 7 calls to 429     │
└─────────────────────────────────────────────────────────────────────────────┘

Console flow (http://localhost:8085 → AI Products):
  1. Create product — name, entrypoint /ai/your-product, pick imported models
  2. Set default token budget + reset window (day / week / month)
  3. Create & deploy — auto-provisions LLM proxy + syncs gateway
  4. Users tab — add user + token budget; copy API key
  5. curl -X POST http://localhost:8082<entrypoint>/v1/chat/completions \
       -H "X-Gravitee-Api-Key: <key>" -H "Content-Type: application/json" \
       -d '{"model":"gpt-5.4-mini","messages":[{"role":"user","content":"hi"}]}'

Legacy API ladder (optional): API_ID=<proxy-id> ai-poc-demo/verify.sh
EOF

say "Done. Mock: http://127.0.0.1:${MOCK_PORT}/v1  Gateway: http://localhost:8082"
