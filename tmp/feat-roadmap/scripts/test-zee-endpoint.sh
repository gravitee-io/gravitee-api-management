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

# ==============================================================================
# Zee Backend Integration Test Procedure
# ==============================================================================
# This script documents how to run a live end-to-end integration test against 
# the Zee AI Generation endpoint in a local development environment.

# ------------------------------------------------------------------------------
# 1. Start the APIM REST API
# ------------------------------------------------------------------------------
# In your terminal, navigate to the APIM repository root, then start the REST API 
# with the Azure OpenAI environment variables set:
#
# cd gravitee-apim-rest-api
# export OPENAI_API_URL="https://your-azure-openai-endpoint..."
# export OPENAI_API_KEY="your-azure-api-key"
# mvn clean compile exec:java -Pdev -pl gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-container

# ------------------------------------------------------------------------------
# 2. Get an Authentication Token
# ------------------------------------------------------------------------------
# You will need a valid JWT token to authorize the request. 
# Option A: Start Console UI (http://localhost:4000), log in with admin/admin, 
#           and copy the Bearer token from your browser's Developer Tools Network tab.
# Option B: Or call the local Auth endpoint if DB is seeded:
#
# TOKEN=$(curl -s -X POST http://localhost:8083/management/auth/login \
#    -u admin:admin -H "Content-Type: application/json" | jq -r .token)

export TOKEN="YOUR_JWT_TOKEN_HERE"

# ------------------------------------------------------------------------------
# 3. Execute the Integration Test
# ------------------------------------------------------------------------------
# Run the following curl command. We use the 'DEFAULT' environment which is standard
# for local development.

echo "Running Zee generation request..."

curl -s -X POST http://localhost:8083/management/v2/environments/DEFAULT/ai/generate \
  -H "Authorization: Bearer $TOKEN" \
  -F 'request={"resourceType":"FLOW","prompt":"Create a rate-limiting flow for /api/v1/* that limits to 100 requests per minute","contextData":{"apiId":"test-api"}};type=application/json' \
  | jq .

# ------------------------------------------------------------------------------
# Expected Output Structure
# ------------------------------------------------------------------------------
# If successful, Azure OpenAI will generate the structured Flow, the backend will 
# rehydrate it, and you'll receive a 200 OK with this shape:
#
# {
#   "resourceType": "FLOW",
#   "generated": {
#     "name": "Rate Limiting Flow (100 req/min)",
#     "enabled": true,
#     "selectors": [
#       {
#         "type": "HTTP",
#         "path": "/api/v1/*",
#         "pathOperator": "STARTS_WITH"
#       }
#     ],
#     "request": [
#       {
#         "name": "Rate Limiting",
#         "policy": "rate-limit",
#         "configuration": {
#           "rate": {
#             "limit": 100,
#             "periodTime": 1,
#             "periodTimeUnit": "MINUTES"
#           }
#         }
#       }
#     ],
#     "response": [],
#     "subscribe": [],
#     "publish": []
#   },
#   "metadata": {
#     "model": "gpt-4o-mini",       // Or actual deployed model name
#     "tokensUsed": 850,
#     "ragContextUsed": false
#   }
# }
