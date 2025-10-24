#!/bin/bash
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


# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration ---
export APIM_REGISTRY=${APIM_REGISTRY:-graviteeio.azurecr.io}
export APIM_VERSION=${APIM_VERSION:-master-latest}

# Match CONTAINER names
BRIDGE_SERVER_CONTAINER_NAME="gio_apim_gateway_server"
BRIDGE_CLIENT_1_CONTAINER_NAME="gio_apim_gateway_client"
BRIDGE_CLIENT_2_CONTAINER_NAME="gio_apim_gateway_client_2"
REDIS_CONTAINER_NAME="redis_stack"
MONGO_CONTAINER_NAME="gio_apim_mongodb"
MGMT_API_CONTAINER_NAME="gio_apim_management_api"
MGMT_UI_CONTAINER_NAME="gio_apim_management_ui"
ELASTIC_CONTAINER_NAME="gio_apim_elasticsearch"

# Match SERVICE names
REDIS_SERVICE_NAME="redis-stack"
MONGO_SERVICE_NAME="mongodb"
ELASTIC_SERVICE_NAME="elasticsearch"
BRIDGE_SERVER_SERVICE_NAME="gateway_server"
BRIDGE_CLIENT_1_SERVICE_NAME="gateway_client"
BRIDGE_CLIENT_2_SERVICE_NAME="gateway_client_2"
MGMT_API_SERVICE_NAME="management_api"
MGMT_UI_SERVICE_NAME="management_ui"

API_CONTEXT_PATH="testDSPWithBridge"
API_NAME="testDSPWithBridge" # Match the name in the payload

BRIDGE_SERVER_PORT="18092"
BRIDGE_CLIENT_1_PORT="8082"
BRIDGE_CLIENT_2_PORT="8081"
MGMT_API_URL="http://localhost:8083"
MGMT_API_USER="admin"
MGMT_API_PASS="admin"

WAIT_TIME_INITIAL=30
WAIT_TIME_SECONDARY=30

# --- Helper Functions ---
log_info() {
  echo
  echo "🔵 [INFO] $(date '+%Y-%m-%d %H:%M:%S') $1"
}

log_success() {
  echo "✅ [SUCCESS] $(date '+%Y-%m-%d %H:%M:%S') $1"
}

log_error() {
  echo "❌ [ERROR] $(date '+%Y-%m-%d %H:%M:%S') $1" >&2
  # (Error logging remains the same)
  echo "--- Last 20 lines of ${BRIDGE_SERVER_CONTAINER_NAME} ---"
  docker logs --tail 20 ${BRIDGE_SERVER_CONTAINER_NAME} 2>/dev/null || echo "Could not get logs for ${BRIDGE_SERVER_CONTAINER_NAME}"
  echo "--- Last 20 lines of ${BRIDGE_CLIENT_1_CONTAINER_NAME} ---"
  docker logs --tail 20 ${BRIDGE_CLIENT_1_CONTAINER_NAME} 2>/dev/null || echo "Could not get logs for ${BRIDGE_CLIENT_1_CONTAINER_NAME}"
  echo "--- Last 20 lines of ${BRIDGE_CLIENT_2_CONTAINER_NAME} ---"
  docker logs --tail 20 ${BRIDGE_CLIENT_2_CONTAINER_NAME} 2>/dev/null || echo "Could not get logs for ${BRIDGE_CLIENT_2_CONTAINER_NAME}"
  exit 1
}

cleanup() {
  log_info "Cleaning up Docker environment..."
  if docker compose ps -q &> /dev/null; then
      docker compose down -v --remove-orphans
  else
      log_info "No active docker compose project found to clean up."
  fi
  log_info "Cleanup complete."
}

# --- Main Test ---
trap cleanup EXIT

echo
log_info "Test that a new gateway in a cluster user bridge must sync if the bridge crashes"
echo

log_info "Starting Bridge Server, First Client, and Core Services..."
echo
export APIM_REGISTRY=graviteeio.azurecr.io && export APIM_VERSION=master-latest && docker compose up -d redis-stack mongodb elasticsearch gateway_server gateway_client management_api management_ui

log_info "Waiting ${WAIT_TIME_INITIAL}s for services (especially Gateway) to initialize..."
sleep $WAIT_TIME_INITIAL

log_info "Verifying initial containers started..."
echo
docker compose ps --filter "name=${BRIDGE_SERVER_CONTAINER_NAME}" --filter "status=running" | grep -q $BRIDGE_SERVER_CONTAINER_NAME || log_error "${BRIDGE_SERVER_CONTAINER_NAME} failed to start."
docker compose ps --filter "name=${BRIDGE_CLIENT_1_CONTAINER_NAME}" --filter "status=running" | grep -q $BRIDGE_CLIENT_1_CONTAINER_NAME || log_error "${BRIDGE_CLIENT_1_CONTAINER_NAME} failed to start."
docker compose ps --filter "name=${MGMT_API_CONTAINER_NAME}" --filter "status=running" | grep -q $MGMT_API_CONTAINER_NAME || log_error "${MGMT_API_CONTAINER_NAME} failed to start."

echo
log_success "Initial containers are running."
echo

SERVER_LOG_OUTPUT=$(docker logs $BRIDGE_SERVER_CONTAINER_NAME)

log_info "Printing last few log lines from Bridge Server (${BRIDGE_SERVER_CONTAINER_NAME})..."
echo "-----------------------------------------------------"
docker logs --tail 30 $BRIDGE_SERVER_CONTAINER_NAME
echo "-----------------------------------------------------"
echo

echo
if echo "$SERVER_LOG_OUTPUT" | grep -q "Sync service has been scheduled with delay"; then
log_success "Bridge Server started"; fi
echo
if echo "$SERVER_LOG_OUTPUT" | grep -q "A node joined the cluster"; then
log_success "Client joined the Bridge Server. Cluster is formed."; fi
echo
# --- Automated API Deployment using Import ---
log_info "Importing API definition for '${API_NAME}' with context path '/${API_CONTEXT_PATH}'..."
echo
AUTH_HEADER=$(echo -n "${MGMT_API_USER}:${MGMT_API_PASS}" | base64)

IMPORT_PAYLOAD=$(cat <<EOF | sed 's/"id": "[^"]*",//'
{
  "export": {
    "date": "2025-10-21T10:15:17.796427718Z",
    "apimVersion": "4.10.0-SNAPSHOT"
  },
  "api": {
    "definitionVersion": "V4",
    "type": "PROXY",
    "listeners": [
      {
        "type": "HTTP",
        "paths": [
          {
            "path": "/testDSPWithBridge/",
            "overrideAccess": false
          }
        ],
        "pathMappings": [],
        "entrypoints": [
          {
            "type": "http-proxy",
            "qos": "AUTO",
            "configuration": {}
          }
        ],
        "servers": []
      }
    ],
    "endpointGroups": [
      {
        "name": "Default HTTP proxy group",
        "type": "http-proxy",
        "loadBalancer": {
          "type": "ROUND_ROBIN"
        },
        "sharedConfiguration": "{\"proxy\":{\"useSystemProxy\":false,\"enabled\":false},\"http\":{\"keepAliveTimeout\":30000,\"keepAlive\":true,\"propagateClientHost\":false,\"followRedirects\":false,\"readTimeout\":10000,\"idleTimeout\":60000,\"connectTimeout\":3000,\"useCompression\":true,\"maxConcurrentConnections\":20,\"version\":\"HTTP_1_1\",\"pipelining\":false},\"ssl\":{\"keyStore\":{\"type\":\"\"},\"hostnameVerifier\":true,\"trustStore\":{\"type\":\"\"},\"trustAll\":false}}",
        "endpoints": [
          {
            "name": "Default HTTP proxy",
            "type": "http-proxy",
            "weight": 1,
            "inheritConfiguration": true,
            "configuration": {
              "target": "https://api.gravitee.io/echo"
            },
            "services": {},
            "secondary": false,
            "tenants": []
          }
        ],
        "services": {}
      }
    ],
    "analytics": {
      "enabled": true
    },
    "flowExecution": {
      "mode": "DEFAULT",
      "matchRequired": false
    },
    "flows": [],
    "id": "01735d86-7fd7-4e0a-b35d-867fd78e0a4a",
    "name": "testDSPWithBridge",
    "description": "",
    "apiVersion": "1",
    "deployedAt": "2025-10-21T10:15:14.71Z",
    "createdAt": "2025-10-21T10:15:14.501Z",
    "updatedAt": "2025-10-21T10:15:14.71Z",
    "disableMembershipNotifications": false,
    "groups": [],
    "state": "STARTED",
    "visibility": "PRIVATE",
    "labels": [],
    "lifecycleState": "CREATED",
    "tags": [],
    "primaryOwner": {
      "id": "b7c08c50-5c13-42bd-808c-505c13d2bd82",
      "displayName": "admin",
      "type": "USER"
    },
    "categories": [],
    "originContext": {
      "origin": "MANAGEMENT"
    },
    "responseTemplates": {}
  },
  "members": [
    {
      "id": "b7c08c50-5c13-42bd-808c-505c13d2bd82",
      "displayName": "admin",
      "roles": [
        {
          "name": "PRIMARY_OWNER",
          "scope": "API"
        }
      ]
    }
  ],
  "metadata": [
    {
      "key": "email-support",
      "name": "email-support",
      "format": "MAIL",
      "value": "abc@gmail.com",
      "defaultValue": "support@change.me"
    }
  ],
  "pages": [],
  "plans": [
    {
      "definitionVersion": "V4",
      "flows": [],
      "id": "e66237a9-2577-43e0-a237-a9257783e00f",
      "name": "Default Keyless (UNSECURED)",
      "description": "Default unsecured plan",
      "apiId": "01735d86-7fd7-4e0a-b35d-867fd78e0a4a",
      "security": {
        "type": "KEY_LESS",
        "configuration": {}
      },
      "mode": "STANDARD",
      "characteristics": [],
      "commentRequired": false,
      "createdAt": "2025-10-21T10:15:14.653Z",
      "excludedGroups": [],
      "order": 1,
      "publishedAt": "2025-10-21T10:15:14.678Z",
      "status": "PUBLISHED",
      "tags": [],
      "type": "API",
      "updatedAt": "2025-10-21T10:15:14.678Z",
      "validation": "MANUAL"
    }
  ],
  "apiMedia": []
}
EOF
)

# Call the Import API endpoint
API_IMPORT_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
     -X POST "${MGMT_API_URL}/management/v2/environments/DEFAULT/apis/_import/definition" \
     -H "Authorization: Basic ${AUTH_HEADER}" \
     -H 'Accept: application/json, text/plain, */*' \
     -H 'Content-Type: application/json' \
     --data-raw "${IMPORT_PAYLOAD}")

API_IMPORT_STATUS=$(echo "$API_IMPORT_RESPONSE" | tail -n1 | sed 's/HTTP_STATUS://')
API_IMPORT_BODY=$(echo "$API_IMPORT_RESPONSE" | sed '$d')

if [ "$API_IMPORT_STATUS" -ne 201 ]; then
    log_error "Failed to import API. Status: $API_IMPORT_STATUS, Response: $API_IMPORT_BODY"
    echo
fi

# Use grep and sed to extract the API ID from the import response body
API_ID=$(echo "$API_IMPORT_BODY" | grep '"id"' | head -n 1 | sed -e 's/.*"id" *: *"//' -e 's/".*//')

if [ -z "$API_ID" ]; then
    echo
    log_error "Could not extract API ID using grep/sed from import response: $API_IMPORT_BODY"
    echo
fi

echo
log_success "API imported successfully. ID: ${API_ID}"
echo

log_info "Starting API ID ${API_ID}..."
echo
START_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
     -X POST "${MGMT_API_URL}/management/v2/environments/DEFAULT/apis/${API_ID}/_start" \
     -H "Authorization: Basic ${AUTH_HEADER}" \
     -H 'Accept: application/json, text/plain, */*' \
     -H 'Content-Type: application/json' \
     --data-raw '{}') # Empty body for start

START_STATUS=$(echo "$START_RESPONSE" | tail -n1 | sed 's/HTTP_STATUS://')
if [ "$START_STATUS" -ne 200 ] && [ "$START_STATUS" -ne 204 ] ; then
    log_error "Failed to start API. Status: $START_STATUS, Response: $(echo "$START_RESPONSE" | sed '$d')"
fi
log_success "API start command sent successfully."
# --- End Automated API Deployment ---

log_info "Waiting 15s for API deployment to sync..."
sleep 15

log_info "Checking Bridge Server API list..."
BRIDGE_API_LIST=$(curl -s --max-time 5 http://localhost:${BRIDGE_SERVER_PORT}/_bridge/apis || echo "Curl failed")
echo
echo "--- Bridge Server Response (/apis) ---"
echo "$BRIDGE_API_LIST"
echo "--------------------------------------"
echo
if [[ "$BRIDGE_API_LIST" == "Curl failed" ]]; then
    log_error "Failed to connect to Bridge Server on port ${BRIDGE_SERVER_PORT}."
fi
log_success "Checked Bridge Server API list (verify '/${API_CONTEXT_PATH}' is present above)."

log_info "Verifying API access via First Bridge Client (port ${BRIDGE_CLIENT_1_PORT})..."
echo
HTTP_STATUS_CLIENT1=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost:${BRIDGE_CLIENT_1_PORT}/${API_CONTEXT_PATH} || echo "Curl failed")
if [[ "$HTTP_STATUS_CLIENT1" == "Curl failed" ]]; then
    log_error "Failed to connect to First Bridge Client on port ${BRIDGE_CLIENT_1_PORT}."
elif [ "$HTTP_STATUS_CLIENT1" -ne 200 ] && [ "$HTTP_STATUS_CLIENT1" -ne 401 ] && [ "$HTTP_STATUS_CLIENT1" -ne 403 ]; then
  log_error "API call via First Bridge Client failed with status code $HTTP_STATUS_CLIENT1."
else
  log_success "API call via First Bridge Client returned expected status code $HTTP_STATUS_CLIENT1."
fi

log_info "Checking keys in Redis..."
if ! docker exec $REDIS_CONTAINER_NAME redis-cli FT.INFO "gravitee-sync-events-idx" > /dev/null 2>&1; then
    log_info "Redis index not found yet, waiting 5s more..."
    sleep 5
    if ! docker exec $REDIS_CONTAINER_NAME redis-cli FT.INFO "gravitee-sync-events-idx" > /dev/null 2>&1; then
        log_error "Redis index 'gravitee-sync-events-idx' does not exist. Primary client likely failed to init DSP."
    fi
fi
REDIS_KEYS_OUTPUT=$(docker exec $REDIS_CONTAINER_NAME redis-cli KEYS '*' || echo "KEYS failed")
if [[ "$REDIS_KEYS_OUTPUT" == "KEYS failed" ]]; then
    log_error "Failed to execute KEYS command in Redis."
fi
echo
echo "--- Redis KEYS * Output ---"
echo "$REDIS_KEYS_OUTPUT"
echo "---------------------------"
echo
if ! echo "$REDIS_KEYS_OUTPUT" | grep -q -E 'distributed_event:|distributed_sync_state:'; then
  log_error "No 'distributed_event:*' or 'distributed_sync_state:*' keys found in Redis. DSP sync likely failed."
fi
log_success "Found keys in Redis (verify DSP keys are present above)."

log_info "Simulating Bridge failure: Stopping ${BRIDGE_SERVER_SERVICE_NAME} & ${MONGO_SERVICE_NAME}"
docker compose stop gateway_server mongodb
log_info "Waiting 5s..."
sleep 5

log_info "Attempting to start Second Bridge Client (${BRIDGE_CLIENT_2_SERVICE_NAME})..."
export APIM_REGISTRY=graviteeio.azurecr.io && export APIM_VERSION=master-latest && docker compose up -d gateway_client_2

log_info "Waiting ${WAIT_TIME_SECONDARY}s for Second Bridge Client to start and sync from Redis..."
sleep $WAIT_TIME_SECONDARY

log_info "Verifying Second Bridge Client started..."
echo
docker compose ps --filter "name=${BRIDGE_CLIENT_2_CONTAINER_NAME}" --filter "status=running" | grep -q $BRIDGE_CLIENT_2_CONTAINER_NAME || log_error "${BRIDGE_CLIENT_2_CONTAINER_NAME} failed to start."
log_success "Second Bridge Client (${BRIDGE_CLIENT_2_CONTAINER_NAME}) is running."

log_info "Printing last 15 log lines from Second Bridge Client (${BRIDGE_CLIENT_2_CONTAINER_NAME})..."
echo "-----------------------------------------------------"
docker logs --tail 15 $BRIDGE_CLIENT_2_CONTAINER_NAME
echo "-----------------------------------------------------"
echo

log_info "Verifying Second Bridge Client synced API '/${API_CONTEXT_PATH}' from Redis..."
echo
sleep 5
LOG_OUTPUT=$(docker logs $BRIDGE_CLIENT_2_CONTAINER_NAME)
SYNC_LOG_FOUND=false
DEPLOY_LOG_FOUND=false
BRIDGE_ERROR_DURING_SYNC_FOUND=false

if echo "$LOG_OUTPUT" | grep -q "i.g.g.s.s.p.d.s.AbstractDistributedSynchronizer"; then SYNC_LOG_FOUND=true; fi
if echo "$LOG_OUTPUT" | grep -q "API id\[.*\] name\[${API_NAME}\] version\[.*\] has been deployed"; then DEPLOY_LOG_FOUND=true; fi
if echo "$LOG_OUTPUT" | grep "Fetching event" -A 10 | grep -q -E "Failed|Error|Unable to sync.*http.*${BRIDGE_SERVER_NAME}"; then BRIDGE_ERROR_DURING_SYNC_FOUND=true; fi

if ! $SYNC_LOG_FOUND; then
    log_error "Second gateway logs do not show 'Fetching events from the distributed repository'."
echo
elif ! $DEPLOY_LOG_FOUND; then
     log_error "Second gateway logs show 'Fetching events' but NOT deployment of API '${API_NAME}'."
echo
elif $BRIDGE_ERROR_DURING_SYNC_FOUND; then
     log_info "NOTE: Second gateway might have logged errors trying to connect to the (down) bridge during startup/sync attempts, as expected."
echo
fi
log_success "Second Bridge Client appears to have synced API '${API_NAME}' from Redis."
echo

log_info "Testing API access via Second Bridge Client (port ${BRIDGE_CLIENT_2_PORT})..."
echo
HTTP_STATUS_CLIENT2=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost:${BRIDGE_CLIENT_2_PORT}/${API_CONTEXT_PATH} || echo "Curl failed")
if [[ "$HTTP_STATUS_CLIENT2" == "Curl failed" ]]; then
    log_error "Failed to connect to Second Bridge Client on port ${BRIDGE_CLIENT_2_PORT}."
elif [ "$HTTP_STATUS_CLIENT2" -ne 200 ] && [ "$HTTP_STATUS_CLIENT2" -ne 401 ] && [ "$HTTP_STATUS_CLIENT2" -ne 403 ]; then
  log_error "API call via Second Bridge Client failed with status code $HTTP_STATUS_CLIENT2."
else
  log_success "API call via Second Bridge Client returned expected status code $HTTP_STATUS_CLIENT2."
fi

echo
log_success "Bridge & Primary Client Failure Test Scenario Completed Successfully!"
echo

exit 0
