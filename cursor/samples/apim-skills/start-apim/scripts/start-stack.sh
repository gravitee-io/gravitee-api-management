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

# Start Gravitee APIM Stack Infrastructure
# This script starts the infrastructure (MongoDB, Elasticsearch) for the Gravitee APIM
set -e

APIM_ROOT="${APIM_ROOT:-$HOME/workspace/Gravitee/gravitee-api-management}"

echo "🚀 Starting Gravitee APIM Stack"
echo "================================"

# Check if APIM root exists
if [ ! -d "$APIM_ROOT" ]; then
    echo "❌ Error: APIM root directory not found: $APIM_ROOT"
    echo "Set APIM_ROOT environment variable or update this script"
    exit 1
fi

# Step 1: Start Infrastructure
echo ""
echo "📦 Step 1: Starting Infrastructure (MongoDB + Elasticsearch)"
echo "-----------------------------------------------------------"

# Check if containers exist
if docker ps -a --format '{{.Names}}' | grep -q "gio_apim_mongodb"; then
    echo "▶️  Starting existing containers..."
    docker start gio_apim_mongodb gio_apim_elasticsearch
else
    echo "🆕 Creating new containers..."
    cd "$APIM_ROOT"
    docker compose -f docker/quick-setup/mongodb/docker-compose.yml up mongodb elasticsearch -d
fi

# Wait for containers to be healthy
echo "⏳ Waiting for containers to be ready..."
sleep 5

# Verify containers
echo ""
echo "✅ Container Status:"
docker ps --filter "name=gio_apim" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Step 2: Check if distribution exists
echo ""
echo "📦 Step 2: Checking Build Distribution"
echo "--------------------------------------"

GATEWAY_BIN="$APIM_ROOT/gravitee-apim-gateway/gravitee-apim-gateway-standalone/gravitee-apim-gateway-standalone-distribution/target/distribution/bin"
RESTAPI_BIN="$APIM_ROOT/gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target/distribution/bin"

if [ ! -d "$GATEWAY_BIN" ] || [ ! -d "$RESTAPI_BIN" ]; then
    echo "❌ Distribution folders not found. Please run /build-apim first."
    echo ""
    echo "Gateway bin: $GATEWAY_BIN"
    echo "REST API bin: $RESTAPI_BIN"
    exit 1
fi

echo "✅ Distribution folders found"

# Step 3: Instructions for starting services
echo ""
echo "🎯 Next Steps - Start Services Manually"
echo "========================================"
echo ""
echo "Open 3 separate terminals and run:"
echo ""
echo "Terminal 1 - Gateway (Port 8082):"
echo "  cd $GATEWAY_BIN"
echo "  ./gravitee"
echo ""
echo "Terminal 2 - REST API (Port 8083):"
echo "  cd $RESTAPI_BIN"
echo "  ./gravitee"
echo ""
echo "Terminal 3 - Console UI (Port 4000):"
echo "  cd $APIM_ROOT/gravitee-apim-console-webui"
echo "  nvm use && yarn && yarn serve"
echo ""
echo "🌐 URLs:"
echo "  Gateway:    http://localhost:8082"
echo "  REST API:   http://localhost:8083"
echo "  Console UI: http://localhost:4000"
echo "  Credentials: admin / admin"
echo ""
echo "✅ Infrastructure started successfully!"
