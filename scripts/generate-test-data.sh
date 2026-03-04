#!/usr/bin/env bash
#
# Copyright (C) 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Generates diverse test traffic for the V4 API Analytics Dashboard.
# Idempotent: re-run to reuse the same API and add more traffic (or redeploy).
#
# Prerequisites: curl, jq. Management API and Gateway running; Elasticsearch for verification.
# Optional env: BASE_URL_MGMT, GATEWAY_URL, ENV_ID, ES_URL, AUTH_HEADER (e.g. "Authorization: Bearer ...")
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

BASE_URL_MGMT="${BASE_URL_MGMT:-http://localhost:8083}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8082}"
ENV_ID="${ENV_ID:-DEFAULT}"
ES_URL="${ES_URL:-http://localhost:9200}"
API_NAME="analytics-test-api"
CONTEXT_PATH="/analytics-test"
BACKEND_TARGET="https://httpbin.org"

# Optional: set AUTH_HEADER for Management API (e.g. "Authorization: Bearer <token>" or leave unset for no auth)
CURL_MGMT=(curl -s -w "\n%{http_code}" ${AUTH_HEADER:+-H "$AUTH_HEADER"})
CURL_GW=(curl -s -o /dev/null -w "%{http_code}")

log() { echo "[$(date +%H:%M:%S)] $*"; }
err() { echo "[$(date +%H:%M:%S)] ERROR: $*" >&2; }

# Response: body on stdout, status code on last line
req() {
  local method="$1"
  local url="$2"
  shift 2
  "${CURL_MGMT[@]}" -X "$method" "$url" "$@"
}

# Returns HTTP status code (last line of response)
status_of() {
  echo "$1" | tail -n1
}

# Returns response body (all but last line)
body_of() {
  echo "$1" | sed '$d'
}

# --- 1) Find or create API (idempotent) ---
log "Resolving API: $API_NAME (env=$ENV_ID)"
LIST_RESP=$(req GET "$BASE_URL_MGMT/management/v2/environments/$ENV_ID/apis?page=1&perPage=100")
LIST_STATUS=$(status_of "$LIST_RESP")
LIST_BODY=$(body_of "$LIST_RESP")

if [[ "$LIST_STATUS" != "200" ]]; then
  err "List APIs failed ($LIST_STATUS). Check Management API ($BASE_URL_MGMT) and auth."
  echo "$LIST_BODY" | jq . 2>/dev/null || echo "$LIST_BODY"
  exit 1
fi

API_ID=$(echo "$LIST_BODY" | jq -r --arg name "$API_NAME" '.data[] | select(.name == $name) | .id // empty')

if [[ -z "${API_ID:-}" ]]; then
  log "Creating API: $API_NAME at $CONTEXT_PATH -> $BACKEND_TARGET"
  CREATE_BODY=$(jq -n \
    --arg name "$API_NAME" \
    --arg path "$CONTEXT_PATH" \
    --arg target "$BACKEND_TARGET" \
    '{
      name: $name,
      apiVersion: "1.0",
      definitionVersion: "V4",
      type: "PROXY",
      analytics: { enabled: true },
      listeners: [
        {
          type: "HTTP",
          paths: [{ path: $path }],
          entrypoints: [{ type: "http-proxy" }]
        }
      ],
      endpointGroups: [
        {
          name: "default-group",
          type: "http-proxy",
          endpoints: [
            {
              name: "default",
              type: "http-proxy",
              weight: 1,
              inheritConfiguration: false,
              configuration: { target: $target }
            }
          ]
        }
      ],
      flows: []
    }')
  CREATE_RESP=$(req POST "$BASE_URL_MGMT/management/v2/environments/$ENV_ID/apis" \
    -H "Content-Type: application/json" \
    -d "$CREATE_BODY")
  CREATE_STATUS=$(status_of "$CREATE_RESP")
  CREATE_BODY=$(body_of "$CREATE_RESP")
  if [[ "$CREATE_STATUS" != "201" ]]; then
    err "Create API failed ($CREATE_STATUS)"
    echo "$CREATE_BODY" | jq . 2>/dev/null || echo "$CREATE_BODY"
    exit 1
  fi
  API_ID=$(echo "$CREATE_BODY" | jq -r '.id')
  log "Created API id=$API_ID"
else
  log "Using existing API id=$API_ID"
fi

# --- 2) Deploy API ---
log "Deploying API..."
DEPLOY_RESP=$(req POST "$BASE_URL_MGMT/management/v2/environments/$ENV_ID/apis/$API_ID/deployments" \
  -H "Content-Type: application/json" \
  -d '{}')
DEPLOY_STATUS=$(status_of "$DEPLOY_RESP")
if [[ "$DEPLOY_STATUS" != "202" && "$DEPLOY_STATUS" != "204" ]]; then
  err "Deploy failed ($DEPLOY_STATUS). Body: $(body_of "$DEPLOY_RESP")"
  exit 1
fi
log "Deployment requested (202/204). Waiting 3s for gateway sync..."
sleep 3

# --- 3) Generate diverse traffic (100+ requests) ---
BASE="$GATEWAY_URL$CONTEXT_PATH"
TOTAL=0
declare -A STATUS_COUNTS

# Helper: run request and count status
do_req() {
  local method="$1"
  local path="$2"
  local extra=("${@:3}")
  local code
  code=$("${CURL_GW[@]}" -X "$method" "$BASE$path" "${extra[@]}")
  ((STATUS_COUNTS[$code]=${STATUS_COUNTS[$code]:-0}+1)) || true
  ((TOTAL+=1))
}

log "Generating traffic to $BASE (methods, paths, sizes, spaced over time)..."

# Batches spaced by 0.5–1.5s to create time-series pattern
batch_200() {
  do_req GET "/get"
  do_req POST "/post" -d '{"small":1}'
  do_req PUT "/put" -d '{"small":1}'
  do_req DELETE "/delete"
  do_req GET "/get"
  do_req GET "/status/200"
  do_req GET "/status/201"
}
batch_4xx_5xx() {
  do_req GET "/status/400"
  do_req GET "/status/404"
  do_req GET "/status/500"
  do_req GET "/status/503"
}
batch_large() {
  # Larger payload (httpbin /post echoes back)
  local payload
  payload=$(printf '{"data":"%s"}' "$(head -c 2000 < /dev/urandom | base64 | tr -d '\n' | head -c 2000)")
  do_req POST "/post" -H "Content-Type: application/json" -d "$payload"
  do_req POST "/post" -d "small"
}

# ~110+ requests: mix of 200/201, 400/404/500/503, and sizes; spaced to create time-series
for i in 1 2 3 4 5; do
  batch_200
  batch_4xx_5xx
  batch_200
  batch_large
  batch_4xx_5xx
  [[ "$i" -lt 5 ]] && sleep 0.8
done
# Extra to guarantee 100+
batch_200
batch_200

log "Sent $TOTAL requests."

# --- 4) Wait for Elasticsearch bulk indexing ---
log "Waiting 15s for Elasticsearch bulk indexing..."
sleep 15

# --- 5) Verify data in ES ---
log "Verifying data in Elasticsearch ($ES_URL)..."
ES_RESP=$(curl -s -w "\n%{http_code}" "$ES_URL/gravitee-v4-metrics-*/_count" 2>/dev/null || true)
ES_STATUS=$(status_of "$ES_RESP")
ES_BODY=$(body_of "$ES_RESP")
ES_COUNT=""
if [[ "$ES_STATUS" == "200" ]]; then
  ES_COUNT=$(echo "$ES_BODY" | jq -r '.count // empty')
fi
if [[ -z "$ES_COUNT" ]]; then
  err "Could not read ES count (status=$ES_STATUS). Is Elasticsearch at $ES_URL and index gravitee-v4-metrics-* present?"
  echo "$ES_BODY" | jq . 2>/dev/null || echo "$ES_BODY"
else
  log "Elasticsearch gravitee-v4-metrics-* count: $ES_COUNT"
fi

# --- 6) Summary ---
echo ""
echo "========== Summary =========="
echo "Total requests sent:    $TOTAL"
echo "Expected status distribution (approximate):"
for code in 200 201 400 404 500 503; do
  printf "  %s: %s\n" "$code" "${STATUS_COUNTS[$code]:-0}"
done
echo "API ID:                 $API_ID"
echo "Context path:           $CONTEXT_PATH"
echo "Gateway:                $GATEWAY_URL"
echo "Management API:         $BASE_URL_MGMT"
if [[ -n "${ES_COUNT:-}" ]]; then
  echo "ES v4-metrics count:    $ES_COUNT"
fi
echo "=============================="
echo ""
echo "In Console: open API -> API Traffic -> Analytics. Select timeframe (e.g. Last 24 hours) to see widgets."
echo "See docs/workshop/TEST_DATA_PROFILE.md for expected widget behaviour."
