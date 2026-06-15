#!/usr/bin/env bash
#
# AI Products PoC — create 2 clean, working LLM Proxy APIs.
#
# Creates two V4 LLM_PROXY APIs (allowed in API products), starts and deploys each,
# then smoke-tests them through the gateway. Both point at the local OpenAI-compatible
# mock (ai-poc-demo/mock-llm.py on :9099) so they return real 200s with token usage —
# which is what makes per-user token budgets actually enforce (429) in the demo.
#
# They differ by name / context-path / model catalog so the AI Product "Models" view
# shows a varied catalog. Both speak the OpenAI wire protocol on purpose: the mock only
# answers OpenAI-shaped calls, so provider=OPEN_AI / OPEN_AI_COMPATIBLE returns 200,
# whereas provider=ANTHROPIC|GEMINI would make the connector translate to a wire format
# the mock can't answer (→ 500). To demo a *real* second provider, point that proxy at a
# real Anthropic/Gemini key and switch its provider (the connector handles translation).
#
# Usage:
#   ./create-llm-proxies.sh                      # both -> local mock (offline, always works)
#   LLM_TARGET=https://api.openai.com/v1 LLM_API_KEY=sk-... ./create-llm-proxies.sh
#
set -euo pipefail

MAPI="${MAPI:-http://localhost:8083}"
GATEWAY="${GATEWAY:-http://localhost:8082}"
AUTH="${AUTH:-admin:admin}"
ENV_ID="${ENV_ID:-DEFAULT}"
BASE="$MAPI/management/v2/environments/$ENV_ID"

# Upstream both proxies talk to. Defaults to the local mock (always-on, no real key needed).
LLM_TARGET="${LLM_TARGET:-http://localhost:9099/v1}"
LLM_API_KEY="${LLM_API_KEY:-mock-key}"

say()  { printf '\n\033[1m%s\033[0m\n' "$*"; }
warn() { printf '\033[33m%s\033[0m\n' "$*"; }
die()  { printf '\033[31m%s\033[0m\n' "$*" >&2; exit 1; }

# Preflight: MAPI must be up (otherwise the create calls just hang/000).
curl -sf -u "$AUTH" "$BASE/apis/_search" -X POST -H 'Content-Type: application/json' -d '{}' -o /dev/null \
  || die "MAPI not reachable at $MAPI — start the Management API (IntelliJ) and retry."

# Delete any existing API serving <context-path> so re-runs are clean and the
# proxy always ends up with the exact config below (allowedInApiProducts, models…).
delete_existing_at_path() {
  local path="$1" ids id
  ids=$(curl -s -u "$AUTH" -X POST "$BASE/apis/_search?perPage=200" -H 'Content-Type: application/json' -d '{}' \
        | python3 -c '
import json,sys
try: d=json.load(sys.stdin)
except Exception: sys.exit(0)
for a in d.get("data",[]): print(a.get("id",""))' 2>/dev/null)
  for id in $ids; do
    [ -n "$id" ] || continue
    if curl -s -u "$AUTH" "$BASE/apis/$id" \
         | python3 -c '
import json,sys
d=json.load(sys.stdin)
paths=[p.get("path") for l in d.get("listeners",[]) for p in l.get("paths",[])]
sys.exit(0 if any((p or "").rstrip("/")=="'"${path%/}"'" for p in paths) else 1)' 2>/dev/null; then
      warn "  removing existing API at $path (id=$id)"
      curl -s -u "$AUTH" -X POST "$BASE/apis/$id/_stop" -o /dev/null 2>/dev/null || true
      curl -s -u "$AUTH" -X DELETE "$BASE/apis/$id?closePlans=true" -o /dev/null 2>/dev/null || true
    fi
  done
}

# create_proxy <display-name> <context-path> <provider> <models-json-array>
# Echoes the created API id on stdout. Starts + deploys it.
create_proxy() {
  local name="$1" path="$2" provider="$3" models="$4"
  say "Creating LLM proxy: $name  ($path, provider $provider)"
  delete_existing_at_path "$path"

  local body resp api_id
  body=$(cat <<JSON
{
  "name": "$name",
  "apiVersion": "1.0.0",
  "definitionVersion": "V4",
  "type": "LLM_PROXY",
  "description": "Governed LLM proxy for AI Products (PoC) — upstream $provider",
  "allowedInApiProducts": true,
  "listeners": [
    {
      "type": "HTTP",
      "paths": [{ "path": "$path" }],
      "entrypoints": [{ "type": "llm-proxy", "configuration": {}, "qos": "AUTO" }]
    }
  ],
  "endpointGroups": [
    {
      "name": "default-group",
      "type": "llm-proxy",
      "loadBalancer": { "type": "ROUND_ROBIN" },
      "sharedConfiguration": {
        "proxy": { "enabled": false, "useSystemProxy": false },
        "http": {
          "connectTimeout": 3000, "readTimeout": 30000, "idleTimeout": 60000,
          "keepAlive": true, "keepAliveTimeout": 30000, "maxConcurrentConnections": 20,
          "useCompression": true, "followRedirects": false, "pipelining": false,
          "propagateClientHost": false, "version": "HTTP_1_1"
        },
        "ssl": { "trustAll": true, "hostnameVerifier": false }
      },
      "endpoints": [
        {
          "name": "upstream",
          "type": "llm-proxy",
          "weight": 1,
          "inheritConfiguration": true,
          "configuration": {
            "provider": "$provider",
            "target": "$LLM_TARGET",
            "authentication": { "type": "API_KEY", "headerName": "Authorization", "apiKey": "Bearer $LLM_API_KEY" },
            "models": $models,
            "modelGovernance": { "aliasOnly": false, "modelPattern": "*", "prefixPolicy": { "policy": "NO_PREFIX" } }
          }
        }
      ]
    }
  ]
}
JSON
)

  resp=$(curl -s -u "$AUTH" -H 'Content-Type: application/json' -X POST "$BASE/apis" -d "$body")
  api_id=$(printf '%s' "$resp" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("id",""))' 2>/dev/null || true)
  [ -n "$api_id" ] || { warn "Create failed for '$name'. Response:"; printf '%s\n' "$resp" | head -c 1200 >&2; die "Aborting."; }
  echo "  id=$api_id" >&2

  curl -sf -u "$AUTH" -X POST "$BASE/apis/$api_id/_start" -o /dev/null && echo "  started" >&2
  curl -sf -u "$AUTH" -H 'Content-Type: application/json' -X POST "$BASE/apis/$api_id/deployments" -d '{}' -o /dev/null && echo "  deployed" >&2

  printf '%s' "$api_id"
}

# All six proxies speak the OpenAI wire protocol against the local mock, so they all genuinely
# return 200 + token usage. The model NAMES are labels for a rich catalog; the mock answers any of
# them. (To make one truly hit a real provider, point its target/key/provider at that provider.)
OPENAI_MODELS='[{ "name": "gpt-4o-mini", "inputPrice": 0.15, "outputPrice": 0.60 },{ "name": "gpt-4o", "inputPrice": 2.50, "outputPrice": 10.0 },{ "name": "o3-mini", "inputPrice": 1.10, "outputPrice": 4.40 }]'
ANTHROPIC_MODELS='[{ "name": "claude-sonnet-4-5", "inputPrice": 3.00, "outputPrice": 15.0 },{ "name": "claude-opus-4-1", "inputPrice": 15.0, "outputPrice": 75.0 },{ "name": "claude-haiku-4-5", "inputPrice": 0.80, "outputPrice": 4.00 }]'
MISTRAL_MODELS='[{ "name": "mistral-large-latest", "inputPrice": 2.00, "outputPrice": 6.00 },{ "name": "mistral-small-latest", "inputPrice": 0.20, "outputPrice": 0.60 }]'
GEMINI_MODELS='[{ "name": "gemini-2.5-pro", "inputPrice": 1.25, "outputPrice": 10.0 },{ "name": "gemini-2.5-flash", "inputPrice": 0.30, "outputPrice": 2.50 }]'
LLAMA_MODELS='[{ "name": "llama-3.3-70b", "inputPrice": 0.60, "outputPrice": 0.60 },{ "name": "llama-3.1-8b", "inputPrice": 0.10, "outputPrice": 0.10 }]'
COHERE_MODELS='[{ "name": "command-r-plus", "inputPrice": 2.50, "outputPrice": 10.0 },{ "name": "command-r", "inputPrice": 0.15, "outputPrice": 0.60 }]'

API1=$(create_proxy "OpenAI Proxy"    "/ai/openai"    "OPEN_AI"            "$OPENAI_MODELS")
API2=$(create_proxy "Anthropic Proxy" "/ai/anthropic" "OPEN_AI_COMPATIBLE" "$ANTHROPIC_MODELS")
API3=$(create_proxy "Mistral Proxy"   "/ai/mistral"   "OPEN_AI_COMPATIBLE" "$MISTRAL_MODELS")
API4=$(create_proxy "Gemini Proxy"    "/ai/gemini"    "OPEN_AI_COMPATIBLE" "$GEMINI_MODELS")
API5=$(create_proxy "Llama Proxy"     "/ai/llama"     "OPEN_AI_COMPATIBLE" "$LLAMA_MODELS")
API6=$(create_proxy "Cohere Proxy"    "/ai/cohere"    "OPEN_AI_COMPATIBLE" "$COHERE_MODELS")

say "Smoke-testing through the gateway ($GATEWAY)"
sleep 3  # give the gateway a moment to pick up the deployment
for p in /ai/openai /ai/anthropic /ai/mistral /ai/gemini /ai/llama /ai/cohere; do
  code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$GATEWAY$p/v1/chat/completions" \
    -H 'Content-Type: application/json' \
    -d '{"model":"x","messages":[{"role":"user","content":"ping"}]}' || echo 000)
  # 401 (needs a key) means it's deployed & routing; 404 means not yet synced.
  printf '  %s -> %s\n' "$p" "$code"
done

say "Done. Created 6 LLM proxies (all live against the mock):"
echo "  OpenAI Proxy     /ai/openai     gpt-4o-mini, gpt-4o, o3-mini"
echo "  Anthropic Proxy  /ai/anthropic  claude-sonnet-4-5, claude-opus-4-1, claude-haiku-4-5"
echo "  Mistral Proxy    /ai/mistral    mistral-large-latest, mistral-small-latest"
echo "  Gemini Proxy     /ai/gemini     gemini-2.5-pro, gemini-2.5-flash"
echo "  Llama Proxy      /ai/llama      llama-3.3-70b, llama-3.1-8b"
echo "  Cohere Proxy     /ai/cohere     command-r-plus, command-r"
echo
echo "Next (in Gamma): AI Products -> create -> Components -> add some of these -> Overview ->"
echo "Publish access plan -> Developer Portal: subscribe -> Subscribers: approve + set token budget."
