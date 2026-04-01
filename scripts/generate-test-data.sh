#!/usr/bin/env bash
# generate-test-data.sh
#
# Creates a V4 HTTP Proxy API on the local Gravitee Management API, deploys it,
# generates 130+ diverse requests through the gateway to populate analytics data,
# waits for Elasticsearch bulk indexing, then verifies the document count.
#
# Idempotent: safe to run multiple times. Reuses an existing API by name.
#
# Requirements: curl, jq
# Optional:     python3 (used to generate large JSON payloads; falls back to printf)
#
# Usage:
#   ./scripts/generate-test-data.sh
#
# Override defaults via env vars:
#   MGMT_URL=http://localhost:8083  GATEWAY_URL=http://localhost:8082
#   ELASTIC_URL=http://localhost:9200
#   ADMIN_USER=admin  ADMIN_PASS=admin  ENV_ID=DEFAULT

set -euo pipefail

# ─────────────────────────────────────────────────────────────────────────────
# Colours
# ─────────────────────────────────────────────────────────────────────────────
if [[ -t 1 ]]; then
  RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
  BLUE='\033[0;34m'; BOLD='\033[1m'; NC='\033[0m'
else
  RED=''; GREEN=''; YELLOW=''; BLUE=''; BOLD=''; NC=''
fi

log()  { echo -e "${BLUE}[$(date '+%H:%M:%S')]${NC} $*"; }
ok()   { echo -e "${GREEN}[$(date '+%H:%M:%S')] ✓${NC} $*"; }
warn() { echo -e "${YELLOW}[$(date '+%H:%M:%S')] ⚠${NC}  $*"; }
err()  { echo -e "${RED}[$(date '+%H:%M:%S')] ✗${NC}  $*" >&2; }
die()  { err "$*"; exit 1; }

# ─────────────────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────────────────
MGMT_URL="${MGMT_URL:-http://localhost:8083}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8082}"
ELASTIC_URL="${ELASTIC_URL:-http://localhost:9200}"
ENV_ID="${ENV_ID:-DEFAULT}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin}"

API_NAME="Workshop Analytics Test"
API_CONTEXT_PATH="/workshop"
BACKEND_TARGET="https://httpbin.org"

BASE_URL="${MGMT_URL}/management/v2/environments/${ENV_ID}"
AUTH_HEADER="Authorization: Basic $(printf '%s:%s' "${ADMIN_USER}" "${ADMIN_PASS}" | base64 | tr -d '\n')"

# ─────────────────────────────────────────────────────────────────────────────
# Prerequisite check
# ─────────────────────────────────────────────────────────────────────────────
command -v curl >/dev/null 2>&1 || die "curl is required but not installed"
command -v jq   >/dev/null 2>&1 || die "jq is required but not installed (brew install jq)"

# ─────────────────────────────────────────────────────────────────────────────
# Management API helpers
# ─────────────────────────────────────────────────────────────────────────────
mgmt_get() {
  curl -sf -H "${AUTH_HEADER}" "${BASE_URL}${1}"
}

mgmt_post() {
  local path="$1"; local body="${2:-}"
  if [[ -n "${body}" ]]; then
    curl -sf -H "${AUTH_HEADER}" -H "Content-Type: application/json" \
         -X POST "${BASE_URL}${path}" -d "${body}"
  else
    curl -sf -H "${AUTH_HEADER}" -X POST "${BASE_URL}${path}"
  fi
}

# ─────────────────────────────────────────────────────────────────────────────
# Connectivity checks
# ─────────────────────────────────────────────────────────────────────────────
log "Checking Management API connectivity (${MGMT_URL})..."
if ! curl -sf -H "${AUTH_HEADER}" "${BASE_URL}/apis?page=1&perPage=1" > /dev/null 2>&1; then
  die "Cannot reach Management API at ${MGMT_URL}. Is it running? Check MGMT_URL / ADMIN_PASS."
fi
ok "Management API reachable"

log "Checking Elasticsearch connectivity (${ELASTIC_URL})..."
if ! curl -sf "${ELASTIC_URL}/_cluster/health" > /dev/null 2>&1; then
  warn "Elasticsearch not reachable at ${ELASTIC_URL} — ES verification will be skipped"
  ES_AVAILABLE=false
else
  ok "Elasticsearch reachable"
  ES_AVAILABLE=true
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 1: Idempotent API lookup / creation
# ─────────────────────────────────────────────────────────────────────────────
log "Searching for existing API '${API_NAME}'..."

SEARCH_RESP=$(mgmt_get "/apis?q=$(printf '%s' "${API_NAME}" | sed 's/ /+/g')&page=1&perPage=20")
API_ID=$(echo "${SEARCH_RESP}" | jq -r ".data[]? | select(.name == \"${API_NAME}\") | .id" 2>/dev/null || true)

if [[ -n "${API_ID}" ]]; then
  ok "Reusing existing API: ${API_ID}"
else
  log "Creating new V4 HTTP Proxy API..."
  CREATE_BODY=$(cat <<EOF
{
  "name": "${API_NAME}",
  "apiVersion": "1.0",
  "definitionVersion": "V4",
  "description": "Workshop analytics test API — proxies httpbin.org for data generation.",
  "type": "PROXY",
  "analytics": { "enabled": true },
  "listeners": [
    {
      "type": "HTTP",
      "paths": [{ "path": "${API_CONTEXT_PATH}" }],
      "entrypoints": [{ "type": "http-proxy", "configuration": {} }]
    }
  ],
  "endpointGroups": [
    {
      "name": "default-group",
      "type": "http-proxy",
      "loadBalancer": { "type": "ROUND_ROBIN" },
      "sharedConfiguration": {
        "http": {
          "keepAlive": true,
          "followRedirects": false,
          "readTimeout": 10000,
          "idleTimeout": 60000,
          "connectTimeout": 5000,
          "useCompression": true,
          "maxConcurrentConnections": 20,
          "version": "HTTP_1_1"
        }
      },
      "endpoints": [
        {
          "name": "httpbin",
          "type": "http-proxy",
          "weight": 1,
          "inheritConfiguration": true,
          "configuration": { "target": "${BACKEND_TARGET}" }
        }
      ]
    }
  ],
  "flows": []
}
EOF
  )

  CREATED=$(mgmt_post "/apis" "${CREATE_BODY}")
  API_ID=$(echo "${CREATED}" | jq -r '.id')
  ok "Created API: ${API_ID}"

  # Create keyless plan
  log "Creating keyless plan..."
  PLAN_BODY=$(cat <<'EOF'
{
  "name": "Free",
  "description": "Keyless plan for test traffic generation",
  "definitionVersion": "V4",
  "status": "STAGING",
  "security": { "type": "KEY_LESS" },
  "mode": "STANDARD",
  "type": "API"
}
EOF
  )
  PLAN=$(mgmt_post "/apis/${API_ID}/plans" "${PLAN_BODY}")
  PLAN_ID=$(echo "${PLAN}" | jq -r '.id')
  ok "Created plan: ${PLAN_ID}"

  # Publish plan
  log "Publishing plan..."
  mgmt_post "/apis/${API_ID}/plans/${PLAN_ID}/_publish" > /dev/null
  ok "Plan published"

  # Start API (triggers deploy + start)
  log "Starting API..."
  mgmt_post "/apis/${API_ID}/_start" > /dev/null || true
  ok "API started"

  log "Waiting 5s for gateway to sync API config..."
  sleep 5
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 2: Ensure API is STARTED
# ─────────────────────────────────────────────────────────────────────────────
LIFECYCLE=$(mgmt_get "/apis/${API_ID}" | jq -r '.lifecycleState // "UNKNOWN"')
if [[ "${LIFECYCLE}" != "STARTED" ]]; then
  log "API lifecycle is '${LIFECYCLE}' — starting now..."
  mgmt_post "/apis/${API_ID}/_start" > /dev/null || true
  sleep 3
  LIFECYCLE=$(mgmt_get "/apis/${API_ID}" | jq -r '.lifecycleState // "UNKNOWN"')
fi
ok "API lifecycle state: ${LIFECYCLE}"

# ─────────────────────────────────────────────────────────────────────────────
# Step 3: Smoke-test the gateway
# ─────────────────────────────────────────────────────────────────────────────
log "Smoke-testing gateway (${GATEWAY_URL}${API_CONTEXT_PATH}/get)..."
SMOKE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 \
        "${GATEWAY_URL}${API_CONTEXT_PATH}/get" 2>/dev/null || echo "000")

if [[ "${SMOKE}" == "000" ]]; then
  die "Gateway not reachable at ${GATEWAY_URL}. Is it running?"
elif [[ "${SMOKE}" == "404" ]]; then
  warn "Got 404 from gateway — API may not be fully deployed yet (context path stripping issue?)"
  warn "Proceeding anyway; traffic will still be logged in ES."
else
  ok "Gateway smoke test: HTTP ${SMOKE}"
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 4: Build payloads for varied content lengths
# ─────────────────────────────────────────────────────────────────────────────
SMALL_BODY='{"event":"click","userId":"user-001","session":"sess-abc","timestamp":1700000000}'

# Medium payload ~500 bytes
MEDIUM_FILLER=$(head -c 450 /dev/zero | tr '\0' 'm')
MEDIUM_BODY="{\"type\":\"medium\",\"data\":\"${MEDIUM_FILLER}\"}"

# Large payload ~5 KB
LARGE_FILLER=$(head -c 4900 /dev/zero | tr '\0' 'l')
LARGE_BODY="{\"type\":\"large\",\"data\":\"${LARGE_FILLER}\"}"

# ─────────────────────────────────────────────────────────────────────────────
# Step 5: Traffic generation
# ─────────────────────────────────────────────────────────────────────────────
COUNT_2XX=0
COUNT_201=0
COUNT_4XX=0
COUNT_5XX=0
COUNT_ERR=0
TOTAL=0

send() {
  local method="$1"
  local path="$2"
  local body="${3:-}"

  local status
  if [[ -n "${body}" ]]; then
    status=$(curl -s -X "${method}" "${GATEWAY_URL}${API_CONTEXT_PATH}${path}" \
      -H "Content-Type: application/json" -d "${body}" \
      -o /dev/null -w "%{http_code}" --max-time 15 2>/dev/null || echo "000")
  else
    status=$(curl -s -X "${method}" "${GATEWAY_URL}${API_CONTEXT_PATH}${path}" \
      -o /dev/null -w "%{http_code}" --max-time 15 2>/dev/null || echo "000")
  fi

  case "${status}" in
    201)       COUNT_201=$(( COUNT_201 + 1 )); COUNT_2XX=$(( COUNT_2XX + 1 )) ;;
    2[0-9][0-9]) COUNT_2XX=$(( COUNT_2XX + 1 )) ;;
    4[0-9][0-9]) COUNT_4XX=$(( COUNT_4XX + 1 )) ;;
    5[0-9][0-9]) COUNT_5XX=$(( COUNT_5XX + 1 )) ;;
    000)       COUNT_ERR=$(( COUNT_ERR + 1 )) ;;
  esac
  TOTAL=$(( TOTAL + 1 ))
  printf '.'
}

echo
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
log "Generating traffic in 8 batches (~130 requests total)"
log "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# ── Batch 1: GET /get  →  200  (25 requests)
# Exercises: Total Requests counter, Avg GW Response Time baseline
printf "\n[$(date '+%H:%M:%S')] Batch 1/8  GET /get (×25)  "
for _ in $(seq 1 25); do send GET /get; done

sleep 0.5

# ── Batch 2: POST /post with small payloads  →  200  (15 requests)
# Exercises: Avg Content Length (small bucket, ~80B)
printf "\n[$(date '+%H:%M:%S')] Batch 2/8  POST /post small (×15) "
for _ in $(seq 1 15); do send POST /post "${SMALL_BODY}"; done

sleep 0.5

# ── Batch 3: POST /post with medium payloads  →  200  (10 requests)
# Exercises: Avg Content Length (medium bucket, ~460B)
printf "\n[$(date '+%H:%M:%S')] Batch 3/8  POST /post medium (×10) "
for _ in $(seq 1 10); do send POST /post "${MEDIUM_BODY}"; done

sleep 0.5

# ── Batch 4: POST /post with large payloads  →  200  (5 requests)
# Exercises: Avg Content Length (large bucket, ~4.9KB)
printf "\n[$(date '+%H:%M:%S')] Batch 4/8  POST /post large (×5) "
for _ in $(seq 1 5); do send POST /post "${LARGE_BODY}"; done

sleep 0.5

# ── Batch 5: PUT, DELETE, mixed methods  →  200  (10 requests)
# Exercises: method variety
printf "\n[$(date '+%H:%M:%S')] Batch 5/8  PUT /put + DELETE /delete (×5 each) "
for _ in $(seq 1 5); do send PUT    /put    '{"name":"workshop","value":42}'; done
for _ in $(seq 1 5); do send DELETE /delete; done

sleep 0.5

# ── Batch 6: 2xx variants  →  201  (15 requests)
# Exercises: 2xx bucket in status pie chart
printf "\n[$(date '+%H:%M:%S')] Batch 6/8  GET /status/201 (×15) "
for _ in $(seq 1 15); do send GET /status/201; done

sleep 0.5

# ── Batch 7: 4xx errors  →  400, 404  (25 requests)
# Exercises: 4xx bucket in status pie chart
printf "\n[$(date '+%H:%M:%S')] Batch 7/8  GET /status/400 (×15) + GET /status/404 (×10) "
for _ in $(seq 1 15); do send GET /status/400; done
for _ in $(seq 1 10); do send GET /status/404; done

sleep 0.5

# ── Batch 8: 5xx errors  →  500  (15 requests)
# Exercises: 5xx bucket in status pie chart
printf "\n[$(date '+%H:%M:%S')] Batch 8/8  GET /status/500 (×15) "
for _ in $(seq 1 15); do send GET /status/500; done

echo
echo

ok "Traffic generation complete"

# ─────────────────────────────────────────────────────────────────────────────
# Step 6: Wait for Elasticsearch bulk indexing
# ─────────────────────────────────────────────────────────────────────────────
log "Waiting 15s for Elasticsearch bulk flush..."
for i in $(seq 15 -1 1); do
  printf "\r[$(date '+%H:%M:%S')] Flushing... %2d s remaining  " "${i}"
  sleep 1
done
printf "\r%-60s\n" ""

# ─────────────────────────────────────────────────────────────────────────────
# Step 7: Verify data in Elasticsearch
# ─────────────────────────────────────────────────────────────────────────────
ES_COUNT=0
if [[ "${ES_AVAILABLE}" == "true" ]]; then
  log "Verifying documents in Elasticsearch (gravitee-v4-metrics-*)..."
  ES_RESP=$(curl -sf "${ELASTIC_URL}/gravitee-v4-metrics-*/_count" 2>/dev/null || echo '{"count":0}')
  ES_COUNT=$(echo "${ES_RESP}" | jq -r '.count // 0')

  if [[ "${ES_COUNT}" -eq 0 ]]; then
    warn "No documents found in gravitee-v4-metrics-*"
    warn "  → The index may not exist yet if the gateway has never flushed to ES"
    warn "  → Retry after 30 more seconds: curl ${ELASTIC_URL}/gravitee-v4-metrics-*/_count"
  else
    ok "Elasticsearch: ${ES_COUNT} total documents in gravitee-v4-metrics-*"
  fi
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 8: Summary
# ─────────────────────────────────────────────────────────────────────────────
TOTAL_2XX_MINUS_201=$(( COUNT_2XX - COUNT_201 ))

echo
echo -e "${BOLD}════════════════════════════════════════════════════════${NC}"
echo -e "${BOLD}  TRAFFIC GENERATION SUMMARY${NC}"
echo -e "${BOLD}════════════════════════════════════════════════════════${NC}"
printf "  %-28s %s\n" "API Name:"       "${API_NAME}"
printf "  %-28s %s\n" "API ID:"         "${API_ID}"
printf "  %-28s %s\n" "Gateway URL:"    "${GATEWAY_URL}${API_CONTEXT_PATH}"
printf "  %-28s %s\n" "Backend target:" "${BACKEND_TARGET}"
echo
printf "  %-28s %d\n" "Total requests sent:"   "${TOTAL}"
echo "  ─────────────────────────────────────────────"
printf "  %-28s %d\n" "  2xx (non-201):"        "${TOTAL_2XX_MINUS_201}"
printf "  %-28s %d\n" "  201 Created:"           "${COUNT_201}"
printf "  %-28s %d\n" "  4xx Client errors:"     "${COUNT_4XX}"
printf "  %-28s %d\n" "  5xx Server errors:"     "${COUNT_5XX}"
[[ "${COUNT_ERR}" -gt 0 ]] && printf "  %-28s %d\n" "  Timeouts/conn errors:"  "${COUNT_ERR}"
echo
echo "  Expected dashboard values:"
echo "    Total Requests:          ~${TOTAL}"
echo "    Avg GW Response Time:    < 300ms typical (httpbin from your network)"
echo "    Avg Upstream Resp Time:  similar to GW response time"
echo "    Avg Content Length:      mixed (small ~80B, medium ~460B, large ~4.9KB)"
echo "    Response Status pie:     2xx dominant, 4xx/5xx minority slices"
echo
printf "  %-28s %s\n" "ES documents indexed:" "${ES_COUNT} (gravitee-v4-metrics-*)"
echo
echo "  ─────────────────────────────────────────────"
echo "  → Open the Console at http://localhost:4200 (or :8084)"
echo "  → APIs → '${API_NAME}' → Analytics tab"
echo "  → Switch timeframe to confirm live data loads"
echo -e "${BOLD}════════════════════════════════════════════════════════${NC}"
