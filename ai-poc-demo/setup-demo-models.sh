#!/usr/bin/env bash
#
# AI Products PoC — demo model catalog (architect / SE script).
#
# Seeds a rich, offline-safe model catalog for customer demos:
#   • OpenAI GPT-5.4 mini        (enterprise gateway story)
#   • Qwen3.6 / Qwen3.7           (open-weight / Together catalog)
#   • Ollama-style Qwen2.5       (local inference story — mock or real Ollama)
#
# All mock-backed sources point at mock-llm (OpenAI wire format, free, no API key).
# Token burn is LOW by default (15 tokens/call) so 100-token budgets hit 429 quickly.
#
# Usage:
#   ./setup-demo-models.sh                    # mock only, fast token profile
#   MOCK_TOKEN_PROFILE=normal ./setup-demo-models.sh
#   OLLAMA_LIVE=1 ./setup-demo-models.sh      # also wire Ollama source (needs ollama profile)
#   OPENAI_API_KEY=sk-... ./setup-demo-models.sh   # optional live OpenAI source
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

MAPI="${MAPI:-http://localhost:8083}"
GAMMA="${GAMMA:-$MAPI/gamma}"
AUTH="${AUTH:-admin:admin}"
ORG_ID="${ORG_ID:-DEFAULT}"
ENV_ID="${ENV_ID:-DEFAULT}"
MOCK_PORT="${MOCK_PORT:-9099}"
MOCK_UPSTREAM="${MOCK_UPSTREAM:-http://mock-llm:${MOCK_PORT}/v1}"
MOCK_TOKEN_PROFILE="${MOCK_TOKEN_PROFILE:-fast}"
DEMO_PROVIDER_KEY="${DEMO_PROVIDER_KEY:-mock-key}"
OLLAMA_UPSTREAM="${OLLAMA_UPSTREAM:-http://ollama:11434/v1}"

CATALOG_BASE="$GAMMA/organizations/$ORG_ID/environments/$ENV_ID/modules/aim/catalog"

say()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
warn() { printf '\033[33m%s\033[0m\n' "$*"; }
die()  { printf '\033[31m%s\033[0m\n' "$*" >&2; exit 1; }

case "$MOCK_TOKEN_PROFILE" in
  fast)   PROMPT_T=5;  COMP_T=10;  TOTAL_T=15 ;;
  normal) PROMPT_T=40; COMP_T=60; TOTAL_T=100 ;;
  *) die "MOCK_TOKEN_PROFILE must be fast or normal (got: $MOCK_TOKEN_PROFILE)" ;;
esac

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
    say "Mock LLM reachable on :${MOCK_PORT}"
    return 0
  fi
  if docker compose -f "$SCRIPT_DIR/stack/compose.yaml" ps mock-llm 2>/dev/null | grep -qE 'running|healthy'; then
    for _ in $(seq 1 25); do
      curl -sf "http://127.0.0.1:${MOCK_PORT}/v1/models" -o /dev/null 2>/dev/null && return 0
      sleep 2
    done
  fi
  say "Starting mock LLM on host :${MOCK_PORT} (profile=$MOCK_TOKEN_PROFILE)"
  MOCK_TOKEN_PROFILE="$MOCK_TOKEN_PROFILE" nohup python3 "$SCRIPT_DIR/mock-llm.py" "$MOCK_PORT" >/tmp/ai-poc-mock-llm.log 2>&1 &
  for _ in $(seq 1 15); do
    curl -sf "http://127.0.0.1:${MOCK_PORT}/v1/models" -o /dev/null 2>/dev/null && return 0
    sleep 1
  done
  die "Mock failed — see /tmp/ai-poc-mock-llm.log"
}

# upsert_source <sourceKind> <display-name> <baseUrl> [format] [authType]
# authType: NONE | BEARER (default NONE for demo mock)
upsert_source() {
  local kind="$1" name="$2" base="$3" format="${4:-OPEN_AI}" auth_type="${5:-NONE}"
  local body id existing_base auth_json

  case "$auth_type" in
    NONE) auth_json='{ "type": "NONE" }' ;;
    BEARER) auth_json='{ "type": "BEARER" }' ;;
    *) die "Unsupported auth_type: $auth_type" ;;
  esac

  body=$(cat <<JSON
{
  "sourceKind": "$kind",
  "definition": {
    "type": "llm-provider",
    "name": "$name",
    "baseUrl": "$base",
    "format": "$format",
    "authMethod": $auth_json
  }
}
JSON
)

  id=$(curl -s -u "$AUTH" "$CATALOG_BASE/sources?sourceKind=$kind&perPage=50" \
    | python3 -c 'import json,sys; d=json.load(sys.stdin); print(next((x["id"] for x in d.get("data",[]) if x.get("sourceKind")=="'"$kind"'"), ""))' 2>/dev/null || true)

  if [ -n "$id" ]; then
    existing_base=$(curl -s -u "$AUTH" "$CATALOG_BASE/sources/$id" \
      | python3 -c 'import json,sys; print(json.load(sys.stdin).get("definition",{}).get("baseUrl",""))' 2>/dev/null || true)
    if [ "$existing_base" != "$base" ]; then
      curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X PUT "$CATALOG_BASE/sources/$id" -d "$body" -o /dev/null
      echo "  updated $kind → $base" >&2
    fi
  else
    id=$(curl -s -u "$AUTH" -H 'Content-Type: application/json' -X POST "$CATALOG_BASE/sources" -d "$body" \
      | python3 -c 'import json,sys; print(json.load(sys.stdin).get("id",""))' 2>/dev/null || true)
    [ -n "$id" ] || die "Failed to create catalog source $kind"
    echo "  created $kind → $base" >&2
  fi
  printf '%s' "$id"
}

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

say "Catalog sources (mock-backed — no provider API key required)"

OPENAI_ID=$(upsert_source "llm.provider.openai" "OpenAI (demo mock)" "$MOCK_UPSTREAM" "OPEN_AI" "NONE")
TOGETHER_ID=$(upsert_source "llm.provider.together" "Together / Qwen (demo mock)" "$MOCK_UPSTREAM" "OPEN_AI" "NONE")
FIREWORKS_ID=$(upsert_source "llm.provider.fireworks" "Fireworks / Qwen (demo mock)" "$MOCK_UPSTREAM" "OPEN_AI" "NONE")
OLLAMA_MOCK_ID=$(upsert_source "llm.provider.groq" "Ollama local (demo mock)" "$MOCK_UPSTREAM" "OPEN_AI" "NONE")

say "Importing demo models into Catalog"
import_models "$OPENAI_ID" "gpt-5.4-mini"
import_models "$TOGETHER_ID" "Qwen/Qwen3.6-Plus" "Qwen/Qwen3.7-Max"
import_models "$FIREWORKS_ID" "accounts/fireworks/models/qwen3p6-plus"
# Groq catalog ids stand in for Ollama model labels; mock answers any model name.
import_models "$OLLAMA_MOCK_ID" "openai/gpt-oss-20b"

if [ "${OLLAMA_LIVE:-0}" = "1" ]; then
  say "Optional live Ollama source"
  if docker compose -f "$SCRIPT_DIR/stack/compose.yaml" ps ollama 2>/dev/null | grep -q running; then
    OLLAMA_LIVE_ID=$(upsert_source "llm.provider.mistral" "Ollama (live)" "$OLLAMA_UPSTREAM")
    echo "  Live Ollama source id=$OLLAMA_LIVE_ID"
    warn "Pull a model first: docker compose exec ollama ollama pull qwen2.5:0.5b"
    warn "Pick model qwen2.5:0.5b in Create AI Product — Ollama live source uses auth NONE."
  else
    warn "OLLAMA_LIVE=1 but ollama container not running."
    echo "  Start: cd ai-poc-demo/stack && docker compose --profile ollama up -d ollama"
  fi
fi

if [ -n "${OPENAI_API_KEY:-}" ]; then
  REAL_ID=$(upsert_source "llm.provider.openai-live" "OpenAI (live)" "https://api.openai.com/v1" "OPEN_AI" "BEARER")
  import_models "$REAL_ID" "gpt-5.4-mini"
  echo "  Live OpenAI imported — configure provider credentials on the LLM proxy (Secure → LLM Router), not in AI Products."
fi

say "Token budget math (mock profile: $MOCK_TOKEN_PROFILE)"
cat <<EOF

  Per chat completion: ${PROMPT_T} prompt + ${COMP_T} completion = ${TOTAL_T} tokens

  User budget 100  → 429 after ~$(( (100 + TOTAL_T - 1) / TOTAL_T )) calls
  User budget 250  → 429 after ~$(( (250 + TOTAL_T - 1) / TOTAL_T )) calls

  No provider API key in Create AI Product — upstream auth comes from the catalog source (demo mock uses auth NONE).
  Live OpenAI/Gemini: platform ops configure credentials on the managed LLM proxy under Secure → LLM Router.

EOF

say "Recommended models to pick in Create AI Product"
cat <<'EOF'
  ┌──────────────────┬─────────────────────────────┐
  │ Demo story       │ Catalog model name          │
  ├──────────────────┼─────────────────────────────┤
  │ Enterprise GPT   │ GPT-5.4 mini                │
  │ Open Qwen        │ Qwen3.6 Plus                │
  │ Open Qwen (alt)  │ Qwen 3.7 Plus (Fireworks)   │
  │ Local Ollama     │ GPT OSS 20B (Groq/Ollama)   │
  └──────────────────┴─────────────────────────────┘

  Console: http://localhost:8085 → AI Products → Create → pick 1–3 models above
  Users tab: add developer with token budget 100 → curl until 429

  Example curl (replace path + key):
    curl -sS -X POST "http://localhost:8082/ai/YOUR-PRODUCT/v1/chat/completions" \\
      -H "X-Gravitee-Api-Key: YOUR-KEY" -H "Content-Type: application/json" \\
      -d '{"model":"Qwen/Qwen3.6-Plus","messages":[{"role":"user","content":"hi"}]}'
EOF

say "Done. Mock upstream: $MOCK_UPSTREAM  Gateway: http://localhost:8082"
