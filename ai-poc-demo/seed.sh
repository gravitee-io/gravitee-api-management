#!/usr/bin/env bash
#
# AI Products PoC — one-time seed.
# Creates a V4 LLM_PROXY API (allowed in API products), starts it, and deploys it.
# Everything else (AI Product, components, plans, consumers) is done live in the Gamma console.
#
# Usage:
#   LLM_API_KEY=sk-... [LLM_TARGET=https://api.openai.com/v1] [LLM_MODEL=gpt-4o-mini] ./seed.sh
#
set -euo pipefail

MAPI="${MAPI:-http://localhost:8083}"
AUTH="${AUTH:-admin:admin}"
ENV_ID="${ENV_ID:-DEFAULT}"
BASE="$MAPI/management/v2/environments/$ENV_ID"

LLM_TARGET="${LLM_TARGET:-https://api.openai.com/v1}"
LLM_MODEL="${LLM_MODEL:-gpt-4o-mini}"
LLM_API_KEY="${LLM_API_KEY:?Set LLM_API_KEY to the upstream provider key}"
CONTEXT_PATH="${CONTEXT_PATH:-/ai/openai}"

say() { printf '\n\033[1m%s\033[0m\n' "$*"; }

say "1) Creating LLM proxy API (type LLM_PROXY, path $CONTEXT_PATH, model $LLM_MODEL)"
API_ID=$(curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X POST "$BASE/apis" -d @- <<EOF | tee /tmp/seed-api.json | python3 -c 'import json,sys;print(json.load(sys.stdin)["id"])'
{
  "name": "OpenAI LLM Proxy",
  "apiVersion": "1.0.0",
  "definitionVersion": "V4",
  "type": "LLM_PROXY",
  "description": "Governed LLM proxy used by AI Products (PoC)",
  "allowedInApiProducts": true,
  "listeners": [
    {
      "type": "HTTP",
      "paths": [{ "path": "$CONTEXT_PATH" }],
      "entrypoints": [{ "type": "llm-proxy", "configuration": {}, "qos": "AUTO" }]
    }
  ],
  "endpointGroups": [
    {
      "name": "default-group",
      "type": "llm-proxy",
      "loadBalancer": { "type": "ROUND_ROBIN" },
      "endpoints": [
        {
          "name": "openai",
          "type": "llm-proxy",
          "weight": 1,
          "inheritConfiguration": false,
          "configuration": {
            "provider": "OPEN_AI",
            "target": "$LLM_TARGET",
            "authentication": { "type": "BEARER", "bearer": "$LLM_API_KEY" },
            "models": [
              { "name": "$LLM_MODEL", "inputPrice": 0.15, "outputPrice": 0.6 }
            ],
            "modelGovernance": { "aliasOnly": false }
          }
        }
      ]
    }
  ]
}
EOF
)
echo "API_ID=$API_ID"

say "2) Starting the API"
curl -sf -u "$AUTH" -X POST "$BASE/apis/$API_ID/_start" -o /dev/null && echo started

say "3) Deploying the API to the gateway"
curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X POST "$BASE/apis/$API_ID/deployments" -d '{}' -o /dev/null && echo deployed

say "Done. LLM proxy '$API_ID' is live at \${GATEWAY:-http://localhost:8082}$CONTEXT_PATH"
echo "Next (live in Gamma): create AI Product -> add this component -> create plan (rate limit + token budget) -> add consumer -> portal."
