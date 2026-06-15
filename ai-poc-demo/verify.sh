#!/usr/bin/env bash
#
# AI Products PoC — runtime verification ladder.
# Proves: product-plan SECURITY (401/200) and product-plan FLOWS (token budget 429) on an LLM proxy.
#
# Prereqs: stack up (compose), mock upstream running (python3 ai-poc-demo/mock-llm.py 9099),
# and the LLM proxy seeded (seed.sh with LLM_TARGET=http://host.docker.internal:9099/v1).
#
# Usage: API_ID=<llm proxy api id> ./verify.sh
#
set -euo pipefail

MAPI="${MAPI:-http://localhost:8083}"
GW="${GW:-http://localhost:8082}"
AUTH="${AUTH:-admin:admin}"
ENV_ID="${ENV_ID:-DEFAULT}"
BASE="$MAPI/management/v2/environments/$ENV_ID"
V1_BASE="$MAPI/management/organizations/DEFAULT/environments/$ENV_ID"
API_ID="${API_ID:?Set API_ID to the seeded LLM proxy API id}"
CONTEXT_PATH="${CONTEXT_PATH:-/ai/openai}"

say() { printf '\n\033[1m%s\033[0m\n' "$*"; }
jqr() { python3 -c "import json,sys;print(json.load(sys.stdin)$1)"; }

# Helper: add a user (application + subscription with a personal token budget AND rate limit) and echo their key.
add_developer() { # $1=name $2=tokenLimit $3=rateLimit(req/min, default 60) -> echoes API key
  local name="$1" limit="$2" rate="${3:-60}"
  local appId subId
  appId=$(curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X POST "$V1_BASE/applications" \
    -d "{\"name\":\"$name-$(date +%s)\",\"description\":\"AI user $name\",\"settings\":{\"app\":{\"type\":\"Other\"}}}" | jqr '["id"]')
  subId=$(curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X POST "$BASE/api-products/$PRODUCT_ID/subscriptions" \
    -d "{\"planId\":\"$PLAN_ID\",\"applicationId\":\"$appId\",\"metadata\":{\"tokenLimit\":\"$limit\",\"rateLimit\":\"$rate\"}}" | jqr '["id"]')
  curl -sf -u "$AUTH" "$BASE/api-products/$PRODUCT_ID/subscriptions/$subId/api-keys?page=1&perPage=10" | jqr '["data"][0]["key"]'
}

say "1) Create AI Product (type AI_PRODUCT) wrapping the LLM proxy"
CREATED=$(curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X POST "$BASE/api-products" \
  -d "{\"name\":\"Verify AI Product $(date +%s)\",\"version\":\"1.0.0\",\"type\":\"AI_PRODUCT\",\"description\":\"PoC verification\",\"apiIds\":[\"$API_ID\"]}")
PRODUCT_ID=$(echo "$CREATED" | jqr '["id"]')
PRODUCT_TYPE=$(echo "$CREATED" | jqr '.get("type","<missing>")')
echo "PRODUCT_ID=$PRODUCT_ID  type=$PRODUCT_TYPE"
[ "$PRODUCT_TYPE" = "AI_PRODUCT" ] || echo "  WARNING: product 'type' did not round-trip (expected AI_PRODUCT) — AI/API lists won't separate."

say "2) Create plan: API key + PER-DEVELOPER token limit (dynamicLimit from subscription metadata)"
PLAN_ID=$(curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X POST "$BASE/api-products/$PRODUCT_ID/plans" -d @- <<'EOF' | jqr '["id"]'
{
  "name": "Developer Access",
  "description": "API key; each developer's token limit comes from their subscription metadata",
  "definitionVersion": "V4",
  "mode": "STANDARD",
  "validation": "AUTO",
  "security": { "type": "API_KEY", "configuration": {} },
  "flows": [
    {
      "name": "Restrictions",
      "enabled": true,
      "request": [
        {
          "name": "Token budget (per user)",
          "enabled": true,
          "policy": "token-ratelimit",
          "configuration": {
            "strategy": "BLOCK_ON_INTERNAL_ERROR",
            "addHeaders": true,
            "rate": { "limit": 0, "dynamicLimit": "{#subscription.metadata['tokenLimit'] ?: 1000000}", "periodTime": 1, "periodTimeUnit": "MINUTES" }
          }
        },
        {
          "name": "Request rate limit (per user)",
          "enabled": true,
          "policy": "rate-limit",
          "configuration": {
            "errorStrategy": "BLOCK_ON_INTERNAL_ERROR",
            "addHeaders": true,
            "rate": { "limit": 0, "dynamicLimit": "{#subscription.metadata['rateLimit'] ?: 1000000}", "periodTime": 1, "periodTimeUnit": "MINUTES" }
          }
        }
      ]
    }
  ]
}
EOF
)
echo "PLAN_ID=$PLAN_ID"

say "3) Publish the plan + deploy the product"
curl -sf -u "$AUTH" -X POST "$BASE/api-products/$PRODUCT_ID/plans/$PLAN_ID/_publish" -o /dev/null && echo published
curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X POST "$BASE/api-products/$PRODUCT_ID/deployments" -d '{}' -o /dev/null && echo deployed

say "4) Add TWO developers with DIFFERENT limits — Bob (250) and Alice (2000)"
BOB_KEY=$(add_developer Bob 250)
ALICE_KEY=$(add_developer Alice 2000)
echo "BOB_KEY=$BOB_KEY (limit 250)"
echo "ALICE_KEY=$ALICE_KEY (limit 2000)"

say "5) Gateway ladder (wait a few seconds for sync) — each mock call = 100 tokens"
sleep 8
BODY='{"model":"gpt-4o-mini","messages":[{"role":"user","content":"hi"}]}'
call() { curl -s -o /dev/null -w '%{http_code}' -H "X-Gravitee-Api-Key: $1" -H 'Content-Type: application/json' -d "$BODY" "$GW$CONTEXT_PATH/chat/completions"; }

echo -n "  a) no key                 -> "
curl -s -o /dev/null -w '%{http_code}\n' -H 'Content-Type: application/json' -d "$BODY" "$GW$CONTEXT_PATH/chat/completions"

echo "  b) Bob with key (expect 200 + X-Token-Rate-Limit-* headers):"
curl -s -D- -o /dev/null -H "X-Gravitee-Api-Key: $BOB_KEY" -H 'Content-Type: application/json' -d "$BODY" "$GW$CONTEXT_PATH/chat/completions" | grep -iE "^HTTP|token-rate-limit"

echo "  c) Burn Bob's 250-token budget (3 calls = 300 tokens)…"
for i in 1 2 3; do call "$BOB_KEY" >/dev/null; done
echo -n "  d) Bob next call          -> "; call "$BOB_KEY"; echo "   (expect 429 — Bob over HIS limit)"
echo -n "  e) Alice (limit 2000)     -> "; call "$ALICE_KEY"; echo "   (expect 200 — limits are PER DEVELOPER, not shared)"

say "Done. PRODUCT_ID=$PRODUCT_ID PLAN_ID=$PLAN_ID  BOB_KEY=$BOB_KEY  ALICE_KEY=$ALICE_KEY"
