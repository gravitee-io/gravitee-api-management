#!/usr/bin/env bash
#
# AI Products PoC — console-first E2E test (API ladder).
# Creates product via API (same policies as UI), adds user, curls gateway until 429.
#
# Prereqs:
#   stack up, ./setup-demo-models.sh run
#
# Usage:
#   ./test-ai-product.sh
#   CONTEXT_PATH=/ai/demo PRODUCT_NAME="Demo Product" TOKEN_BUDGET=100 ./test-ai-product.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

MAPI="${MAPI:-http://localhost:8083}"
GW="${GW:-http://localhost:8082}"
GAMMA="${GAMMA:-$MAPI/gamma}"
AUTH="${AUTH:-admin:admin}"
ORG_ID="${ORG_ID:-DEFAULT}"
ENV_ID="${ENV_ID:-DEFAULT}"
BASE="$MAPI/management/v2/environments/$ENV_ID"
CATALOG_BASE="$GAMMA/organizations/$ORG_ID/environments/$ENV_ID/modules/aim/catalog"

PRODUCT_NAME="${PRODUCT_NAME:-AI Product E2E $(date +%s)}"
CONTEXT_PATH="${CONTEXT_PATH:-/ai/e2e-$(date +%s)}"
TOKEN_BUDGET="${TOKEN_BUDGET:-100}"
BUDGET_WINDOW="${BUDGET_WINDOW:-DAY}"
MODEL="${MODEL:-Qwen/Qwen3.6-Plus}"

say() { printf '\n\033[1m%s\033[0m\n' "$*"; }
die() { printf '\033[31m%s\033[0m\n' "$*" >&2; exit 1; }
jqr() { python3 -c "import json,sys;print(json.load(sys.stdin)$1)"; }

wait_mapi() {
  for _ in $(seq 1 60); do
    curl -sf -u "$AUTH" "$MAPI/management/v2/environments/$ENV_ID/apis/_search" \
      -X POST -H 'Content-Type: application/json' -d '{}' -o /dev/null 2>/dev/null && return 0
    sleep 2
  done
  die "Management API not reachable at $MAPI"
}

pick_catalog_model() {
  curl -sf -u "$AUTH" "$CATALOG_BASE/models?perPage=50" \
    | python3 -c "
import json,sys
d=json.load(sys.stdin)
for m in d.get('data',[]):
    if m.get('name') == sys.argv[1] or m.get('definition',{}).get('queryName') == sys.argv[1]:
        print(json.dumps({'catalogId':m['id'],'catalogSourceId':m['sourceId'],'name':m.get('name','')}))
        break
" "$MODEL"
}

window_minutes() {
  case "$1" in
    DAY) echo 1440 ;;
    WEEK) echo 10080 ;;
    MONTH) echo 43200 ;;
    *) die "BUDGET_WINDOW must be DAY, WEEK, or MONTH" ;;
  esac
}

say "Waiting for Management API"
wait_mapi

say "Resolve catalog model: $MODEL"
MODEL_JSON="$(pick_catalog_model)"
[[ -n "$MODEL_JSON" ]] || die "Model not in catalog — run: ./setup-demo-models.sh"
CATALOG_ID=$(echo "$MODEL_JSON" | jqr '["catalogId"]')
SOURCE_ID=$(echo "$MODEL_JSON" | jqr '["catalogSourceId"]')
MODEL_NAME=$(echo "$MODEL_JSON" | jqr '["name"]')
echo "  catalogId=$CATALOG_ID sourceId=$SOURCE_ID"

say "1) Create AI Product"
PRODUCT_ID=$(curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X POST "$BASE/api-products" \
  -d "{\"name\":\"$PRODUCT_NAME\",\"version\":\"1.0.0\",\"type\":\"AI_PRODUCT\",\"description\":\"E2E test\"}" | jqr '["id"]')
echo "  PRODUCT_ID=$PRODUCT_ID"

say "2) Create managed LLM proxy (no provider API key — catalog auth NONE)"
PROXY=$(curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X POST "$GAMMA/organizations/$ORG_ID/environments/$ENV_ID/modules/aim/llm-proxies" -d @- <<EOF
{
  "name": "$PRODUCT_NAME — LLM",
  "version": "1.0.0",
  "description": "E2E managed proxy",
  "type": "universal",
  "deploy": true,
  "providers": [{
    "kind": "catalog",
    "catalogSourceId": "$SOURCE_ID",
    "authSecret": "",
    "models": [{ "catalogId": "$CATALOG_ID", "name": "$MODEL_NAME" }],
    "governance": { "mode": "REGISTERED_ONLY", "prefixPolicy": "NO_PREFIX", "aliasOnly": false }
  }],
  "entrypointConfig": {
    "contextPath": "$CONTEXT_PATH",
    "trackTokensInStream": true,
    "injectTokenUsageHeaders": true
  },
  "plans": [{
    "name": "Internal",
    "validation": "AUTO",
    "commentRequired": false,
    "security": { "type": "KEY_LESS", "configuration": {} }
  }]
}
EOF
)
PROXY_ID=$(echo "$PROXY" | jqr '["id"]')
echo "  PROXY_ID=$PROXY_ID"

curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X PUT "$BASE/api-products/$PRODUCT_ID" \
  -d "{\"name\":\"$PRODUCT_NAME\",\"version\":\"1.0.0\",\"apiIds\":[\"$PROXY_ID\"]}" -o /dev/null

PERIOD=$(window_minutes "$BUDGET_WINDOW")
PLAN_NAME="Developer Access (per $(echo "$BUDGET_WINDOW" | tr '[:upper:]' '[:lower:]'))"

say "3) Create + publish access plan (token budget $TOKEN_BUDGET / $BUDGET_WINDOW)"
PLAN_ID=$(curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X POST "$BASE/api-products/$PRODUCT_ID/plans" -d @- <<EOF | jqr '["id"]'
{
  "name": "$PLAN_NAME",
  "description": "E2E plan",
  "definitionVersion": "V4",
  "mode": "STANDARD",
  "validation": "AUTO",
  "security": { "type": "API_KEY", "configuration": {} },
  "flows": [{
    "name": "Per-user limits",
    "enabled": true,
    "request": [
      {
        "name": "Token budget",
        "enabled": true,
        "policy": "token-ratelimit",
        "configuration": {
          "strategy": "BLOCK_ON_INTERNAL_ERROR",
          "addHeaders": true,
          "rate": {
            "limit": 0,
            "dynamicLimit": "{#subscription.metadata['tokenLimit'] ?: $TOKEN_BUDGET}",
            "periodTime": $PERIOD,
            "periodTimeUnit": "MINUTES"
          }
        }
      }
    ]
  }]
}
EOF
)
curl -sf -u "$AUTH" -X POST "$BASE/api-products/$PRODUCT_ID/plans/$PLAN_ID/_publish" -o /dev/null
curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X POST "$BASE/api-products/$PRODUCT_ID/deployments" -d '{}' -o /dev/null
echo "  PLAN_ID=$PLAN_ID"

say "4) Add user with token budget $TOKEN_BUDGET"
APP_ID=$(curl -sf -u "$AUTH" -H 'Content-Type: application/json' \
  -X POST "$MAPI/management/organizations/$ORG_ID/environments/$ENV_ID/applications" \
  -d "{\"name\":\"e2e-user-$(date +%s)\",\"description\":\"E2E\",\"settings\":{\"app\":{\"type\":\"Other\"}}}" | jqr '["id"]')
SUB_ID=$(curl -sf -u "$AUTH" -H 'Content-Type: application/json' \
  -X POST "$BASE/api-products/$PRODUCT_ID/subscriptions" \
  -d "{\"planId\":\"$PLAN_ID\",\"applicationId\":\"$APP_ID\",\"metadata\":{\"tokenLimit\":\"$TOKEN_BUDGET\",\"rateLimit\":\"1000\"}}" | jqr '["id"]')
API_KEY=$(curl -sf -u "$AUTH" "$BASE/api-products/$PRODUCT_ID/subscriptions/$SUB_ID/api-keys?page=1&perPage=10" | jqr '["data"][0]["key"]')
echo "  API_KEY=$API_KEY"

say "5) Gateway ladder (mock ~15 tokens/call, budget $TOKEN_BUDGET)"
sleep 8
BODY=$(python3 -c "import json; print(json.dumps({'model':'$MODEL','messages':[{'role':'user','content':'hi'}]}))")
URL="$GW$CONTEXT_PATH/v1/chat/completions"
call() { curl -s -o /dev/null -w '%{http_code}' -H "X-Gravitee-Api-Key: $API_KEY" -H 'Content-Type: application/json' -d "$BODY" "$URL"; }

echo -n "  no key        -> "
curl -s -o /dev/null -w '%{http_code}\n' -H 'Content-Type: application/json' -d "$BODY" "$URL"

echo -n "  first call    -> "; call "$API_KEY"; echo " (expect 200)"

n=$(( (TOKEN_BUDGET + 14) / 15 + 2 ))
echo "  burning budget (~$n calls)…"
for _ in $(seq 1 "$n"); do call "$API_KEY" >/dev/null; done
echo -n "  after burn    -> "; STATUS=$(call "$API_KEY"); echo "$STATUS (expect 429)"

[[ "$STATUS" == "429" ]] || die "Expected 429 after budget exhausted, got $STATUS"

say "PASS — AI Product E2E"
echo "  Console: http://localhost:8085 → AI Products → $PRODUCT_NAME"
echo "  Gateway: curl -H 'X-Gravitee-Api-Key: $API_KEY' $URL"
