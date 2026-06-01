#!/usr/bin/env bash
# E2E test: deploy AuthZen API def, create entities + policy, force sync, evaluate.
# Assumes APIM rest-api on :8083 and gateway on :8082 (gamma.enabled=true).
set -u

MGMT="http://localhost:8083/management/v2"
GAMMA="http://localhost:8083/gamma/organizations/DEFAULT/environments/DEFAULT/modules/authz"
GW="http://localhost:8082"
AUTH="-u admin:admin"
JSON='-H Content-Type:application/json -H Accept:application/json'
FIX=/tmp/authzen-e2e

section() { echo; echo "── $* ──"; }
post() { local label=$1 url=$2 file=${3:-}
  echo "▶ POST $url"
  [ -n "$file" ] && echo "  body: $file"
  if [ -n "$file" ]; then
    curl -s $AUTH $JSON -X POST "$url" --data @"$file" -w "\nHTTP %{http_code}\n"
  else
    curl -s $AUTH $JSON -X POST "$url" -w "\nHTTP %{http_code}\n"
  fi
  echo
}

# 1. Verify mgmt API up
# 0. Cleanup any prior runs (same name)
section "0. cleanup prior APIs"
EXISTING=$(curl -s $AUTH $JSON -X POST "$MGMT/environments/DEFAULT/apis/_search?perPage=100" -d '{"query":"AuthZen evaluation"}' \
  | python3 -c 'import sys,json; d=json.load(sys.stdin); print(" ".join(a["id"] for a in d.get("data",[]) if a.get("name")=="AuthZen evaluation"))' 2>/dev/null)
for id in $EXISTING; do
  echo "  → stop+delete $id"
  curl -s $AUTH $JSON -X POST "$MGMT/environments/DEFAULT/apis/$id/_stop" -o /dev/null
  curl -s $AUTH $JSON -X POST "$MGMT/environments/DEFAULT/apis/$id/plans/_search" -d '{}' \
    | python3 -c 'import sys,json; d=json.load(sys.stdin); print(" ".join(p["id"] for p in d.get("data",[])))' 2>/dev/null \
    | while read pid; do for p in $pid; do curl -s $AUTH $JSON -X POST "$MGMT/environments/DEFAULT/apis/$id/plans/$p/_close" -o /dev/null; done; done
  curl -s $AUTH $JSON -X DELETE "$MGMT/environments/DEFAULT/apis/$id?closePlans=true" -w "  delete HTTP %{http_code}\n"
done

section "1. verify rest-api up"
curl -sf $AUTH "$MGMT/environments/DEFAULT" -o /dev/null -w "rest-api env DEFAULT: HTTP %{http_code}\n" || { echo "rest-api not ready"; exit 1; }

# 2. Verify gamma module loaded
section "2. verify gamma authz module"
curl -s $AUTH "http://localhost:8083/gamma/organizations/DEFAULT/modules" | head -c 600; echo
echo

# 3. Create AuthZen API definition
section "3. POST /apis/_import/definition"
CREATE_RESP=$(curl -s $AUTH $JSON -X POST "$MGMT/environments/DEFAULT/apis/_import/definition" --data @$FIX/api-def.json)
echo "$CREATE_RESP" | head -c 2000; echo
API_ID=$(echo "$CREATE_RESP" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("id",""))' 2>/dev/null)
echo "→ API_ID=$API_ID"
if [ -z "$API_ID" ]; then echo "Failed to create API"; exit 1; fi

# 3b. Create + publish a Keyless plan (plan import in _import/definition is finicky)
section "3b. create + publish Keyless plan"
PLAN_RESP=$(curl -s $AUTH $JSON -X POST "$MGMT/environments/DEFAULT/apis/$API_ID/plans" --data @$FIX/plan.json)
echo "$PLAN_RESP" | head -c 600; echo
PLAN_ID=$(echo "$PLAN_RESP" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("id",""))' 2>/dev/null)
echo "→ PLAN_ID=$PLAN_ID"
if [ -z "$PLAN_ID" ]; then echo "plan create failed"; fi
curl -s $AUTH $JSON -X POST "$MGMT/environments/DEFAULT/apis/$API_ID/plans/$PLAN_ID/_publish" -w "\n  publish HTTP %{http_code}\n"

# 4. Deploy API to gateway
section "4. deploy API"
curl -s $AUTH $JSON -X POST "$MGMT/environments/DEFAULT/apis/$API_ID/deployments" -d '{}' -w "\nHTTP %{http_code}\n"

# 5. Start API
section "5. start API"
curl -s $AUTH $JSON -X POST "$MGMT/environments/DEFAULT/apis/$API_ID/_start" -w "\nHTTP %{http_code}\n"

# 6. Create entities via Gamma authz REST
section "6. create entities (alice, bob, doc-1)"
post entity-alice "$GAMMA/entities" $FIX/entity-alice.json
post entity-bob   "$GAMMA/entities" $FIX/entity-bob.json
post entity-doc1  "$GAMMA/entities" $FIX/entity-doc1.json

# 7. Create + deploy policy
section "7. create policy"
POL_RESP=$(curl -s $AUTH $JSON -X POST "$GAMMA/policies" --data @$FIX/policy-alice-read-doc1.json)
echo "$POL_RESP" | head -c 2000; echo
POL_ID=$(echo "$POL_RESP" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("id",""))' 2>/dev/null)
echo "→ POL_ID=$POL_ID"

section "8. deploy policy"
if [ -n "$POL_ID" ]; then
  curl -s $AUTH $JSON -X POST "$GAMMA/policies/$POL_ID/deploy" -w "\nHTTP %{http_code}\n"
fi

# 9. Force reload of entities to PDP
section "9. force /entities/reload"
curl -s $AUTH $JSON -X POST "$GAMMA/entities/reload" -w "\nHTTP %{http_code}\n"

# 10. Wait for gateway sync (kicks every few seconds)
section "10. wait 6s for gateway sync"
sleep 6

# 11. Sanity: gateway should serve /authz/access/v1/evaluation
section "11. PERMIT evaluation (alice → read doc-1)"
echo "▶ POST $GW/authz/access/v1/evaluation"
curl -s $JSON -X POST "$GW/authz/access/v1/evaluation" --data @$FIX/eval-permit.json -w "\nHTTP %{http_code}\n"

section "12. DENY evaluation (bob → read doc-1)"
echo "▶ POST $GW/authz/access/v1/evaluation"
curl -s $JSON -X POST "$GW/authz/access/v1/evaluation" --data @$FIX/eval-deny-bob.json -w "\nHTTP %{http_code}\n"

echo
echo "═══════════════════════════════════════════════════════════════"
echo "done."
