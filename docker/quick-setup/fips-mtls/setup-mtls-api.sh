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

# pipefail too: without it, `curl ... | jsonf` hides curl's status behind python's.
set -eo pipefail
cd "$(dirname "$0")"

MAPI="http://localhost:8083/management/v2/environments/DEFAULT"
MAPI_V1="http://localhost:8083/management/organizations/DEFAULT/environments/DEFAULT"
AUTH=(-u admin:admin -H "Content-Type: application/json" --fail-with-body)
# Exits non-zero on a missing key: an empty id would otherwise build URLs like /apis//plans
# and let the script report success while nothing was created.
jsonf() {
  python3 -c "import sys, json; value = json.load(sys.stdin).get('$1'); sys.exit(1) if not value else print(value)"
}

# Keeps the server's explanation instead of discarding it: --fail-with-body writes it to stdout, and
# every failure here is one the operator needs to read — a missing EE licence above all.
call() {
  local body
  if ! body=$(curl -s "${AUTH[@]}" "$@"); then
    echo "$body" >&2
    return 1
  fi
  printf '%s' "$body"
}

echo "0/6 waiting for the management API"
for attempt in $(seq 1 150); do
  if curl -sf -o /dev/null "http://localhost:8083/management/v2/ui/bootstrap"; then break; fi
  if [ "$attempt" -eq 150 ]; then
    echo "management API still not answering after 5 minutes; last attempt said:" >&2
    curl -sS "http://localhost:8083/management/v2/ui/bootstrap" >&2 || true
    echo "check 'docker compose logs management_api' — a missing licence and the very BouncyCastle failure this stack reproduces both land here." >&2
    exit 1
  fi
  sleep 2
done

echo "1/6 creating the API"
API_ID=$(call -X POST "$MAPI/apis" -d '{
  "definitionVersion":"V4","type":"PROXY","name":"fips-mtls-test","apiVersion":"1.0",
  "listeners":[{"type":"HTTP","paths":[{"path":"/fips-mtls"}],"entrypoints":[{"type":"http-proxy"}]}],
  "endpointGroups":[{"name":"default","type":"http-proxy","endpoints":[{"name":"backend","type":"http-proxy",
    "weight":1,"inheritConfiguration":false,"configuration":{"target":"https://api.gravitee.io/echo"}}]}]
}' | jsonf id)

echo "2/6 publishing an mTLS plan"
PLAN_ID=$(call -X POST "$MAPI/apis/$API_ID/plans" -d '{
  "definitionVersion":"V4","name":"mtls-plan","description":"mTLS","security":{"type":"MTLS"},
  "mode":"STANDARD","validation":"AUTO","flows":[]}' | jsonf id)
call -X POST "$MAPI/apis/$API_ID/plans/$PLAN_ID/_publish" >/dev/null

echo "3/6 creating the application"
APP_ID=$(call -X POST "$MAPI_V1/applications" \
  -d '{"name":"fips-mtls-client","description":"mTLS test client","settings":{"app":{"type":"SIMPLE"}}}' | jsonf id)

echo "4/6 registering the client certificate"
python3 -c "import json,pathlib;print(json.dumps({'name':'mtls-test-client','certificate':pathlib.Path('.certificates/client.crt').read_text()}))" \
  | call -X POST "$MAPI_V1/applications/$APP_ID/certificates" -d @- >/dev/null

echo "5/6 subscribing"
call -X POST "$MAPI/apis/$API_ID/subscriptions" \
  -d "{\"applicationId\":\"$APP_ID\",\"planId\":\"$PLAN_ID\"}" >/dev/null

echo "6/6 starting the API"
call -X POST "$MAPI/apis/$API_ID/_start" >/dev/null

cat <<TXT

Ready. The gateway picks the subscription up within its sync interval.

  curl -k --cert .certificates/client.crt --key .certificates/client.key https://localhost:8082/fips-mtls
TXT
