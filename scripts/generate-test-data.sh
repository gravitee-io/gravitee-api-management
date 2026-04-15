#!/usr/bin/env bash
#
# Copyright © 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -euo pipefail

# -----------------------------------------------------------------------------
# Configuration (override with env vars if needed)
# -----------------------------------------------------------------------------
MGMT_BASE_URL="${MGMT_BASE_URL:-http://localhost:8083/management/v2}"
ENV_ID="${ENV_ID:-DEFAULT}"
GATEWAY_BASE_URL="${GATEWAY_BASE_URL:-http://localhost:8082}"
ES_BASE_URL="${ES_BASE_URL:-http://localhost:9200}"

API_NAME="${API_NAME:-Workshop Analytics V4 Test API}"
API_PATH="${API_PATH:-/workshop-analytics-v4-test}"
API_VERSION="${API_VERSION:-1.0.0}"
BACKEND_TARGET="${BACKEND_TARGET:-https://httpbin.org}"

INDEX_WAIT_SECONDS="${INDEX_WAIT_SECONDS:-15}"
REQUEST_SLEEP_SECONDS="${REQUEST_SLEEP_SECONDS:-0.08}"

# Auth: prefer token, fallback to basic auth.
MGMT_TOKEN="${MGMT_TOKEN:-}"
MGMT_USERNAME="${MGMT_USERNAME:-admin}"
MGMT_PASSWORD="${MGMT_PASSWORD:-admin}"

# -----------------------------------------------------------------------------
# Helpers
# -----------------------------------------------------------------------------
log() {
  printf '[generate-test-data] %s\n' "$*"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

curl_auth_args() {
  if [[ -n "$MGMT_TOKEN" ]]; then
    printf -- '-H\nAuthorization: Bearer %s\n' "$MGMT_TOKEN"
  else
    printf -- '-u\n%s:%s\n' "$MGMT_USERNAME" "$MGMT_PASSWORD"
  fi
}

do_mgmt_request() {
  local method="$1"
  local url="$2"
  local payload="${3:-}"

  local response_file
  response_file="$(mktemp)"

  local status
  if [[ -n "$payload" ]]; then
    status="$(
      curl -sS -o "$response_file" -w "%{http_code}" \
        -X "$method" "$url" \
        -H "Content-Type: application/json" \
        $(curl_auth_args) \
        --data "$payload"
    )"
  else
    status="$(
      curl -sS -o "$response_file" -w "%{http_code}" \
        -X "$method" "$url" \
        $(curl_auth_args)
    )"
  fi

  cat "$response_file"
  rm -f "$response_file"
  printf '\n__HTTP_STATUS__=%s\n' "$status"
}

extract_http_status() {
  sed -n 's/^__HTTP_STATUS__=\([0-9][0-9][0-9]\)$/\1/p' | tail -n 1
}

extract_http_body() {
  sed '/^__HTTP_STATUS__=/d'
}

json_extract() {
  local expression="$1"
  python3 -c "import json,sys; data=json.load(sys.stdin); print($expression)"
}

find_api_id_by_name() {
  local list_url="${MGMT_BASE_URL}/environments/${ENV_ID}/apis?page=1&perPage=200"
  local raw
  raw="$(do_mgmt_request GET "$list_url")"
  local status body
  status="$(printf '%s' "$raw" | extract_http_status)"
  body="$(printf '%s' "$raw" | extract_http_body)"

  if [[ "$status" != "200" ]]; then
    echo "Failed to list APIs (status=$status)." >&2
    echo "$body" >&2
    exit 1
  fi

  printf '%s' "$body" | python3 - "$API_NAME" <<'PY'
import json,sys
target_name = sys.argv[1]
payload = json.load(sys.stdin)
items = payload.get("data", [])
for item in items:
    if item.get("name") == target_name:
        print(item.get("id", ""))
        break
PY
}

create_api() {
  local create_url="${MGMT_BASE_URL}/environments/${ENV_ID}/apis"
  local payload
  payload="$(cat <<JSON
{
  "name": "${API_NAME}",
  "apiVersion": "${API_VERSION}",
  "definitionVersion": "V4",
  "type": "PROXY",
  "description": "Synthetic analytics traffic generator API (idempotent workshop fixture)",
  "listeners": [
    {
      "type": "HTTP",
      "paths": [
        { "path": "${API_PATH}" }
      ],
      "entrypoints": [
        { "type": "http-proxy" }
      ]
    }
  ],
  "endpointGroups": [
    {
      "name": "default-group",
      "type": "http-proxy",
      "endpoints": [
        {
          "name": "httpbin-endpoint",
          "type": "http-proxy",
          "configuration": {
            "target": "${BACKEND_TARGET}"
          }
        }
      ]
    }
  ],
  "flows": [],
  "groups": [],
  "tags": [],
  "flowExecution": {
    "flowMode": "DEFAULT",
    "matchRequired": false
  }
}
JSON
)"

  local raw
  raw="$(do_mgmt_request POST "$create_url" "$payload")"
  local status body
  status="$(printf '%s' "$raw" | extract_http_status)"
  body="$(printf '%s' "$raw" | extract_http_body)"

  if [[ "$status" != "201" ]]; then
    echo "Failed to create API (status=$status)." >&2
    echo "$body" >&2
    exit 1
  fi

  printf '%s' "$body" | json_extract 'data.get("id","") if isinstance(data,dict) else ""'
}

deploy_api() {
  local api_id="$1"
  local deploy_url="${MGMT_BASE_URL}/environments/${ENV_ID}/apis/${api_id}/deployments"
  local payload='{"label":"workshop-analytics-test-data"}'
  local raw status body

  raw="$(do_mgmt_request POST "$deploy_url" "$payload")"
  status="$(printf '%s' "$raw" | extract_http_status)"
  body="$(printf '%s' "$raw" | extract_http_body)"

  if [[ "$status" != "202" && "$status" != "200" ]]; then
    echo "Failed to request deployment (status=$status)." >&2
    echo "$body" >&2
    exit 1
  fi
}

start_api() {
  local api_id="$1"
  local start_url="${MGMT_BASE_URL}/environments/${ENV_ID}/apis/${api_id}/_start"
  local raw status body

  raw="$(do_mgmt_request POST "$start_url")"
  status="$(printf '%s' "$raw" | extract_http_status)"
  body="$(printf '%s' "$raw" | extract_http_body)"

  # 204 => started, 400 can mean already started depending on current state.
  if [[ "$status" == "204" ]]; then
    return 0
  fi

  if [[ "$status" == "400" ]]; then
    if printf '%s' "$body" | rg -q "already STARTED|already started|STARTED"; then
      log "API already started."
      return 0
    fi
  fi

  echo "Failed to start API (status=$status)." >&2
  echo "$body" >&2
  exit 1
}

send_traffic() {
  local base_url="${GATEWAY_BASE_URL}${API_PATH}"
  local total=0

  local small_payload='{"kind":"small","note":"analytics test payload"}'
  local large_payload
  large_payload="$(python3 - <<'PY'
import json
payload = {"kind":"large","blob":"X"*12000}
print(json.dumps(payload))
PY
)"

  # Expected status distribution counters for deterministic traffic pattern.
  declare -Ag EXPECTED_STATUS=(
    ["200"]=0
    ["201"]=0
    ["400"]=0
    ["404"]=0
    ["500"]=0
  )

  # Optional actual counters to help debugging when an upstream behavior changes.
  declare -Ag ACTUAL_STATUS=(
    ["200"]=0
    ["201"]=0
    ["400"]=0
    ["404"]=0
    ["500"]=0
    ["other"]=0
  )

  send_one() {
    local method="$1"
    local path="$2"
    local expected="$3"
    local payload_kind="$4"
    local payload_data=""
    local code

    EXPECTED_STATUS["$expected"]=$((EXPECTED_STATUS["$expected"] + 1))

    case "$payload_kind" in
      small) payload_data="$small_payload" ;;
      large) payload_data="$large_payload" ;;
      none) payload_data="" ;;
      *)
        echo "Unknown payload kind: $payload_kind" >&2
        exit 1
        ;;
    esac

    if [[ -n "$payload_data" ]]; then
      code="$(curl -sS -o /dev/null -w "%{http_code}" \
        -X "$method" "${base_url}${path}" \
        -H "Content-Type: application/json" \
        --data "$payload_data")"
    else
      code="$(curl -sS -o /dev/null -w "%{http_code}" \
        -X "$method" "${base_url}${path}")"
    fi

    if [[ -n "${ACTUAL_STATUS[$code]+x}" ]]; then
      ACTUAL_STATUS["$code"]=$((ACTUAL_STATUS["$code"] + 1))
    else
      ACTUAL_STATUS["other"]=$((ACTUAL_STATUS["other"] + 1))
    fi

    total=$((total + 1))
    sleep "$REQUEST_SLEEP_SECONDS"
  }

  # 12 requests per loop * 10 loops = 120 requests total.
  for i in $(seq 1 10); do
    send_one GET "/get" 200 none
    send_one GET "/status/201" 201 none
    send_one GET "/status/400" 400 none
    send_one GET "/status/404" 404 none
    send_one GET "/status/500" 500 none

    send_one POST "/post" 200 small
    send_one PUT "/put" 200 large
    send_one DELETE "/delete" 200 none

    send_one POST "/status/201" 201 small
    send_one PUT "/status/400" 400 large
    send_one DELETE "/status/404" 404 none
    send_one POST "/status/500" 500 small

    # Burst separation creates visible histogram/line-chart shape transitions.
    sleep 0.7
  done

  TOTAL_REQUESTS_SENT="$total"
}

verify_es_count() {
  local count_url="${ES_BASE_URL}/gravitee-v4-metrics-*/_count"
  local body
  body="$(curl -sS "${count_url}")"
  ES_COUNT="$(printf '%s' "$body" | json_extract 'data.get("count","unknown")')"
}

print_summary() {
  echo
  echo "=============================================================="
  echo "Analytics test data generation complete"
  echo "=============================================================="
  echo "API name: ${API_NAME}"
  echo "API path: ${API_PATH}"
  echo "Backend:  ${BACKEND_TARGET}"
  echo "Total requests sent: ${TOTAL_REQUESTS_SENT}"
  echo
  echo "Expected status code distribution:"
  echo "  200: ${EXPECTED_STATUS["200"]}"
  echo "  201: ${EXPECTED_STATUS["201"]}"
  echo "  400: ${EXPECTED_STATUS["400"]}"
  echo "  404: ${EXPECTED_STATUS["404"]}"
  echo "  500: ${EXPECTED_STATUS["500"]}"
  echo
  echo "Observed status code distribution:"
  echo "  200: ${ACTUAL_STATUS["200"]}"
  echo "  201: ${ACTUAL_STATUS["201"]}"
  echo "  400: ${ACTUAL_STATUS["400"]}"
  echo "  404: ${ACTUAL_STATUS["404"]}"
  echo "  500: ${ACTUAL_STATUS["500"]}"
  echo "  other: ${ACTUAL_STATUS["other"]}"
  echo
  echo "Elasticsearch count endpoint:"
  echo "  ${ES_BASE_URL}/gravitee-v4-metrics-*/_count"
  echo "  count=${ES_COUNT}"
  echo "=============================================================="
}

main() {
  require_cmd curl
  require_cmd python3
  require_cmd rg

  log "Looking up existing API by name: ${API_NAME}"
  API_ID="$(find_api_id_by_name || true)"

  if [[ -z "${API_ID:-}" ]]; then
    log "API not found. Creating API."
    API_ID="$(create_api)"
    log "Created API with id: ${API_ID}"
  else
    log "Reusing existing API with id: ${API_ID}"
  fi

  log "Requesting deployment"
  deploy_api "$API_ID"

  log "Starting API"
  start_api "$API_ID"

  log "Generating diverse traffic against ${GATEWAY_BASE_URL}${API_PATH}"
  send_traffic

  log "Waiting ${INDEX_WAIT_SECONDS}s for Elasticsearch bulk indexing"
  sleep "$INDEX_WAIT_SECONDS"

  log "Verifying indexed data count in Elasticsearch"
  verify_es_count

  print_summary
}

main "$@"
