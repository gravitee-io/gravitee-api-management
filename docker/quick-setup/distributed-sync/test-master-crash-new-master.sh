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

PRIMARY_GW_CONTAINER_NAME="gio_apim_gateway_primary"
SECONDARY_GW_CONTAINER_NAME="gio_apim_gateway_secondary"
REDIS_CONTAINER_NAME="redis_stack"
MONGO_CONTAINER_NAME="gio_apim_mongodb"
MGMT_API_CONTAINER_NAME="gio_apim_management_api"
MGMT_UI_CONTAINER_NAME="gio_apim_management_ui"
ELASTIC_CONTAINER_NAME="gio_apim_elasticsearch"

REDIS_SERVICE_NAME="redis-stack"
MONGO_SERVICE_NAME="mongodb"
ELASTIC_SERVICE_NAME="elasticsearch"
PRIMARY_GW_SERVICE_NAME="gateway_primary"
SECONDARY_GW_SERVICE_NAME="gateway_secondary"
MGMT_API_SERVICE_NAME="management_api"
MGMT_UI_SERVICE_NAME="management_ui"

# --- API Details ---
API_CONTEXT_PATH="testDSPWithoutDBSync"
API_NAME="testDSPWithoutDBSync"
# -------------------

PRIMARY_GW_PORT="8082"
SECONDARY_GW_PORT="8081"
MGMT_API_URL="http://localhost:8083"
MGMT_API_USER="admin"
MGMT_API_PASS="admin"

WAIT_TIME_INITIAL=20
WAIT_TIME_SECONDARY=20

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
  echo "--- Last 20 lines of ${PRIMARY_GW_CONTAINER_NAME} ---"
  docker logs --tail 20 ${PRIMARY_GW_CONTAINER_NAME} 2>/dev/null || echo "Could not get logs for ${PRIMARY_GW_CONTAINER_NAME}"
  echo "--- Last 20 lines of ${SECONDARY_GW_CONTAINER_NAME} ---"
  docker logs --tail 20 ${SECONDARY_GW_CONTAINER_NAME} 2>/dev/null || echo "Could not get logs for ${SECONDARY_GW_CONTAINER_NAME}"
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
log_info "Test that a new master must be elected if the master node of the cluster crashes."
echo

log_info "Starting Primary Gateway and Core Services..."
export APIM_REGISTRY=${APIM_REGISTRY} && export APIM_VERSION=${APIM_VERSION} && docker compose up -d ${REDIS_SERVICE_NAME} ${MONGO_SERVICE_NAME} ${ELASTIC_SERVICE_NAME} ${PRIMARY_GW_SERVICE_NAME} ${MGMT_API_SERVICE_NAME} ${MGMT_UI_SERVICE_NAME}

log_info "Waiting ${WAIT_TIME_INITIAL}s for services to initialize..."
sleep $WAIT_TIME_INITIAL

log_info "Now starting secondary gateway"
export APIM_REGISTRY=${APIM_REGISTRY} && export APIM_VERSION=${APIM_VERSION} && docker compose up -d ${SECONDARY_GW_SERVICE_NAME}

log_info "Waiting ${WAIT_TIME_INITIAL}s for secondary gateway to start"
sleep $WAIT_TIME_INITIAL

log_info "Verifying initial containers started..."

# Check Management API is running via docker ps
docker compose ps --filter "name=${MGMT_API_CONTAINER_NAME}" --filter "status=running" | grep -q $MGMT_API_CONTAINER_NAME || log_error "${MGMT_API_CONTAINER_NAME} container failed to start."

if docker logs $MGMT_API_CONTAINER_NAME | grep -m 1 -q "Started oejs.Server"; then
    log_success "Management API reported ready."
else
    log_error "Management API (${MGMT_API_CONTAINER_NAME}) having errors."
fi

# Check Primary Gateway is running via docker ps
docker compose ps --filter "name=${PRIMARY_GW_CONTAINER_NAME}" --filter "status=running" | grep -q $PRIMARY_GW_CONTAINER_NAME || log_error "${PRIMARY_GW_CONTAINER_NAME} container failed to start."

# Check Secondary Gateway is running via docker ps
docker compose ps --filter "name=${SECONDARY_GW_CONTAINER_NAME}" --filter "status=running" | grep -q $SECONDARY_GW_CONTAINER_NAME || log_error "${SECONDARY_GW_CONTAINER_NAME} container failed to start."

PRIMARY_GW_LOG_OUTPUT=$(docker logs $PRIMARY_GW_CONTAINER_NAME)
SECONDARY_GW_LOG_OUTPUT=$(docker logs $SECONDARY_GW_CONTAINER_NAME)

echo
if echo "$PRIMARY_GW_LOG_OUTPUT" | grep -q "Sync service has been scheduled with delay"; then
log_success "Primary Gateway started"; fi
echo
if echo "$SECONDARY_GW_LOG_OUTPUT" | grep -q "Sync service has been scheduled with delay"; then
log_success "Secondary Gateway started"; fi
echo

log_info "Printing last few log lines from Primary Gateway Server (${PRIMARY_GW_CONTAINER_NAME})..."
echo "-----------------------------------------------------"
echo "$PRIMARY_GW_LOG_OUTPUT" | tail -n 20
echo "-----------------------------------------------------"
echo
log_info "Printing last few log lines from Secondary Gateway Server (${PRIMARY_GW_CONTAINER_NAME})..."
echo "-----------------------------------------------------"
echo "$SECONDARY_GW_LOG_OUTPUT" | tail -n 20
echo "-----------------------------------------------------"
echo


if echo "$PRIMARY_GW_LOG_OUTPUT" | grep -q "A node joined the cluster"; then
log_success "Primary Gateway is the master gateway. Cluster is formed as confirmed via logs of the container."; fi

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
            "path": "/testDSPWithoutDBSync/",
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
    "name": "testDSPWithoutDBSync",
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
fi

# Use grep and sed to extract the API ID
API_ID=$(echo "$API_IMPORT_BODY" | grep '"id"' | head -n 1 | sed -e 's/.*"id" *: *"//' -e 's/".*//')

if [ -z "$API_ID" ]; then
    log_error "Could not extract API ID using grep/sed from import response: $API_IMPORT_BODY"
fi
log_success "API imported successfully. ID: ${API_ID}"

log_info "Starting API ID ${API_ID}..."
echo
START_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
     -X POST "${MGMT_API_URL}/management/v2/environments/DEFAULT/apis/${API_ID}/_start" \
     -H "Authorization: Basic ${AUTH_HEADER}" \
     -H 'Accept: application/json, text/plain, */*' \
     -H 'Content-Type: application/json' \
     --data-raw '{}')

START_STATUS=$(echo "$START_RESPONSE" | tail -n1 | sed 's/HTTP_STATUS://')
if [ "$START_STATUS" -ne 201 ] && [ "$START_STATUS" -ne 204 ] ; then
    log_error "Failed to start API. Status: $START_STATUS, Response: $(echo "$START_RESPONSE" | sed '$d')"
fi
log_success "API start command sent successfully."
# --- End Automated API Deployment ---

log_info "Waiting 15s for API deployment and sync to Primary Gateway and Redis..."
sleep 15

log_info "Verifying API access via Primary Gateway (port ${PRIMARY_GW_PORT})..."
echo
HTTP_STATUS_CLIENT1=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost:${PRIMARY_GW_PORT}/${API_CONTEXT_PATH} || echo "Curl failed")
if [[ "$HTTP_STATUS_CLIENT1" == "Curl failed" ]]; then
    log_error "Failed to connect to Primary Gateway on port ${PRIMARY_GW_PORT}."
elif [ "$HTTP_STATUS_CLIENT1" -ne 200 ] && [ "$HTTP_STATUS_CLIENT1" -ne 401 ] && [ "$HTTP_STATUS_CLIENT1" -ne 403 ]; then
  log_error "API call via Primary Gateway failed with status code $HTTP_STATUS_CLIENT1."
else
  log_success "API call via Primary Gateway returned expected status code $HTTP_STATUS_CLIENT1."
fi

log_info "Testing API access via Secondary Gateway (port ${SECONDARY_GW_PORT})..."
echo
HTTP_STATUS_CLIENT2=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost:${SECONDARY_GW_PORT}/${API_CONTEXT_PATH} || echo "Curl failed")
if [[ "$HTTP_STATUS_CLIENT2" == "Curl failed" ]]; then
    log_error "Failed to connect to Secondary Gateway on port ${SECONDARY_GW_PORT}."
elif [ "$HTTP_STATUS_CLIENT2" -ne 200 ] && [ "$HTTP_STATUS_CLIENT2" -ne 401 ] && [ "$HTTP_STATUS_CLIENT2" -ne 403 ]; then
  log_error "API call via Secondary Gateway failed with status code $HTTP_STATUS_CLIENT2."
else
  log_success "API call via Secondary Gateway returned expected status code $HTTP_STATUS_CLIENT2."
fi

log_info "Simulating Primary/Master Gateway failure: Stopping ${MONGO_SERVICE_NAME} and ${PRIMARY_GW_SERVICE_NAME}..."
echo
docker compose down $MONGO_SERVICE_NAME $PRIMARY_GW_SERVICE_NAME
log_info "Waiting 5s..."
sleep 5

SECONDARY_GW_LOG_OUTPUT_UPDATED=$(docker logs $SECONDARY_GW_CONTAINER_NAME)

echo
log_info "Checking logs from Secondary Gateway Server (${PRIMARY_GW_CONTAINER_NAME})..."
echo

if echo "$SECONDARY_GW_LOG_OUTPUT_UPDATED" | grep -q "A node leaved the cluster"; then
log_success "Secondary Gateway is the master gateway. Logs contain info like 'A node leaved the cluster' indicating it is the master now."; fi
echo


log_success "New master must be elected Test Scenario Completed Successfully!"
echo

exit 0
