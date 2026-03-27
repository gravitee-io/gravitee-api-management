#!/usr/bin/env bash
# =============================================================================
# generate-test-data.sh — Creates a v4 HTTP Proxy API and generates diverse
# test traffic to exercise all analytics dashboard widgets.
#
# Idempotent: can run multiple times without creating duplicate APIs.
# Compatible with macOS bash 3.x (no associative arrays).
# =============================================================================
set -euo pipefail

MGMT_URL="http://localhost:8083/management/v2"
GW_URL="http://localhost:8082"
ES_URL="http://localhost:9200"
AUTH="admin:admin"
ENV_ID="DEFAULT"
API_NAME="analytics-test-httpbin"
CONTEXT_PATH="/analytics-test"

TOTAL_SENT=0

echo "================================================================"
echo "  V4 Analytics Test Data Generator"
echo "================================================================"
echo ""

# ---------------------------------------------------------------------------
# Step 1: Check if API already exists (idempotency)
# ---------------------------------------------------------------------------
echo "[1/6] Checking for existing API..."

EXISTING_API_ID=$(curl -s -u "$AUTH" \
  "${MGMT_URL}/environments/${ENV_ID}/apis?page=1&perPage=100" \
  | python3 -c "
import sys, json
data = json.load(sys.stdin)
for api in data.get('data', []):
    if api.get('name') == '${API_NAME}':
        print(api['id'])
        break
" 2>/dev/null || echo "")

if [ -n "$EXISTING_API_ID" ]; then
  echo "  → API already exists: $EXISTING_API_ID"
  API_ID="$EXISTING_API_ID"
else
  echo "  → No existing API found, creating new one..."

  # -------------------------------------------------------------------------
  # Step 2: Create the v4 HTTP Proxy API
  # -------------------------------------------------------------------------
  echo "[2/6] Creating v4 HTTP Proxy API..."

  API_RESPONSE=$(curl -s -X POST -u "$AUTH" \
    -H "Content-Type: application/json" \
    "${MGMT_URL}/environments/${ENV_ID}/apis" \
    -d '{
      "name": "'"$API_NAME"'",
      "apiVersion": "1.0",
      "definitionVersion": "V4",
      "type": "PROXY",
      "description": "Test API for analytics dashboard — auto-generated",
      "listeners": [
        {
          "type": "HTTP",
          "paths": [
            {
              "path": "'"$CONTEXT_PATH"'"
            }
          ],
          "entrypoints": [
            {
              "type": "http-proxy"
            }
          ]
        }
      ],
      "endpointGroups": [
        {
          "name": "default-group",
          "type": "http-proxy",
          "endpoints": [
            {
              "name": "httpbin",
              "type": "http-proxy",
              "configuration": {
                "target": "https://httpbin.org"
              }
            }
          ]
        }
      ],
      "analytics": {
        "enabled": true
      },
      "flows": [],
      "flowExecution": {
        "mode": "DEFAULT"
      }
    }')

  API_ID=$(echo "$API_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)

  if [ -z "$API_ID" ]; then
    echo "  ✗ Failed to create API. Response:"
    echo "$API_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$API_RESPONSE"
    exit 1
  fi
  echo "  ✓ API created: $API_ID"
fi

# ---------------------------------------------------------------------------
# Step 3: Start (deploy) the API
# ---------------------------------------------------------------------------
echo "[3/6] Deploying API..."

curl -s -X POST -u "$AUTH" \
  "${MGMT_URL}/environments/${ENV_ID}/apis/${API_ID}/deployments" \
  -H "Content-Type: application/json" \
  -d '{}' -o /dev/null -w ""
echo "  ✓ Deployment triggered"

sleep 3

# Start the API lifecycle if not already started
curl -s -X POST -u "$AUTH" \
  "${MGMT_URL}/environments/${ENV_ID}/apis/${API_ID}/_start" \
  -o /dev/null -w "" 2>/dev/null || true
echo "  ✓ API started"

sleep 2

# ---------------------------------------------------------------------------
# Step 4: Generate diverse test traffic
# ---------------------------------------------------------------------------
echo "[4/6] Generating diverse test traffic..."

send_requests() {
  local method="$1"
  local path="$2"
  local count="$3"
  local body="${4:-}"

  for ((i=0; i<count; i++)); do
    if [ -n "$body" ]; then
      curl -s -o /dev/null -w "" \
        -X "$method" \
        -H "Content-Type: application/json" \
        -d "$body" \
        "${GW_URL}${CONTEXT_PATH}${path}" 2>/dev/null || true
    else
      curl -s -o /dev/null -w "" \
        -X "$method" \
        "${GW_URL}${CONTEXT_PATH}${path}" 2>/dev/null || true
    fi
    sleep 0.05
  done
  TOTAL_SENT=$((TOTAL_SENT + count))
}

# 2xx responses — ~80 requests
echo "  → GET /get (200) x30"
send_requests "GET" "/get" 30

echo "  → POST /post (200) x20"
send_requests "POST" "/post" 20 '{"test":"small payload"}'

echo "  → PUT /put (200) x10"
send_requests "PUT" "/put" 10 '{"test":"update","items":[1,2,3,4,5]}'

echo "  → DELETE /delete (200) x5"
send_requests "DELETE" "/delete" 5

echo "  → POST /status/201 (201) x10"
send_requests "POST" "/status/201" 10 '{"created":true}'

# 4xx responses — ~15 requests
echo "  → GET /status/400 (400) x8"
send_requests "GET" "/status/400" 8

echo "  → GET /status/404 (404) x7"
send_requests "GET" "/status/404" 7

# 5xx responses — ~10 requests
echo "  → POST /status/500 (500) x5"
send_requests "POST" "/status/500" 5 '{"error":"test"}'

echo "  → GET /status/502 (502) x3"
send_requests "GET" "/status/502" 3

echo "  → GET /status/503 (503) x2"
send_requests "GET" "/status/503" 2

# Large payloads for content-length variation
LARGE_PAYLOAD=$(python3 -c "import json; print(json.dumps({'data': 'x' * 5000}))")
echo "  → POST /post with large payload x5"
send_requests "POST" "/post" 5 "$LARGE_PAYLOAD"

# Burst of requests for time-series spike
echo "  → Burst GET /get (200) x15"
for i in {1..15}; do
  curl -s -o /dev/null "${GW_URL}${CONTEXT_PATH}/get" &
done
wait
TOTAL_SENT=$((TOTAL_SENT + 15))

echo "  ✓ Total requests sent: $TOTAL_SENT"

# ---------------------------------------------------------------------------
# Step 5: Wait for Elasticsearch indexing
# ---------------------------------------------------------------------------
echo "[5/6] Waiting 15 seconds for Elasticsearch bulk indexing..."
sleep 15

echo "  Checking Elasticsearch..."
ES_COUNT=$(curl -s "${ES_URL}/gravitee-v4-metrics-*/_count" 2>/dev/null \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('count', 0))" 2>/dev/null || echo "0")
echo "  ✓ Documents in ES (v4 metrics): $ES_COUNT"

# ---------------------------------------------------------------------------
# Step 6: Print summary
# ---------------------------------------------------------------------------
echo ""
echo "================================================================"
echo "  TEST DATA SUMMARY"
echo "================================================================"
echo ""
echo "  API ID:         $API_ID"
echo "  API Name:       $API_NAME"
echo "  Context Path:   $CONTEXT_PATH"
echo "  Total Sent:     $TOTAL_SENT"
echo "  ES Documents:   $ES_COUNT"
echo ""
echo "  Expected Status Code Distribution:"
echo "    200 → ~80 requests (GET/POST/PUT/DELETE + large + burst)"
echo "    201 → ~10 requests"
echo "    400 → ~8 requests"
echo "    404 → ~7 requests"
echo "    500 → ~5 requests"
echo "    502 → ~3 requests"
echo "    503 → ~2 requests"
echo ""
echo "  Console URL:"
echo "  http://localhost:4200/#!/environments/DEFAULT/apis/${API_ID}/v4/analytics"
echo ""
echo "================================================================"
echo "  ✓ Done! Open the Console URL above to see the dashboard."
echo "================================================================"
