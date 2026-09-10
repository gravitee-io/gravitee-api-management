#!/usr/bin/env bash
# Seed Portal Next catalog data so every catalog filter has values to show.
#
# Creates portal categories, a published TOP_NAVBAR folder, banking-themed A2A
# proxy agents, a few other API types, plans (keyless / API key auto / API key
# manual), and published portal navigation items assigned to those categories.
#
#
# Prerequisites: curl, python3, Management API at MGMT_URL (default localhost:8083).
#
# Usage:
#   ./scripts/seed-catalog-filters.sh            # create (idempotent)
#   ./scripts/seed-catalog-filters.sh --drop      # delete previous seed data, then create
#   ./scripts/seed-catalog-filters.sh --drop-only  # delete only, don't re-create
#
# Optional env:
#   MGMT_URL   http://localhost:8083/management
#   ENV_ID     DEFAULT
#   ORG_ID     DEFAULT
#   MGMT_USER  admin
#   MGMT_PASS  admin

set -euo pipefail

MGMT_URL="${MGMT_URL:-http://localhost:8083/management}"
ENV_ID="${ENV_ID:-DEFAULT}"
ORG_ID="${ORG_ID:-DEFAULT}"
MGMT_USER="${MGMT_USER:-admin}"
MGMT_PASS="${MGMT_PASS:-admin}"

V2="${MGMT_URL}/v2/environments/${ENV_ID}"
V1_ORG="${MGMT_URL}/organizations/${ORG_ID}"

DROP=false
DROP_ONLY=false
for arg in "$@"; do
  case "$arg" in
    --drop)      DROP=true ;;
    --drop-only) DROP=true; DROP_ONLY=true ;;
    *) echo "Unknown option: $arg" >&2; exit 1 ;;
  esac
done

FOLDER_TITLE="Catalog Seed"
SECOND_OWNER_EMAIL="treasury.desk@example.com"
SECOND_OWNER_NAME="Treasury Desk"

SEED_API_NAMES=(
  "Mortgage Calculator Agent"
  "Loan Eligibility Agent"
  "Credit Score Agent"
  "Investment Advisor Agent"
  "Insurance Quote Agent"
  "Fraud Detection Agent"
  "Account Summary Agent"
  "Payment Processing Agent"
  "Account REST API"
  "Market Data Stream API"
  "LLM Document Analyzer API"
  "MCP Data Connector API"
  "Market Data Stream"
  "LLM Document Analyzer"
  "MCP Data Connector"
)

SEED_CATEGORY_TITLES=(
  "Banking"
  "Loans"
  "Insurance"
  "Investments"
  "Customer Service"
)

if ! command -v curl >/dev/null || ! command -v python3 >/dev/null; then
  echo "This script needs curl and python3 on PATH." >&2
  exit 1
fi

json_get() {
  python3 -c 'import json,sys; data=json.load(sys.stdin)
path=sys.argv[1].split(".")
cur=data
for key in path:
  if cur is None:
    break
  if isinstance(cur, list):
    cur=cur[int(key)]
  else:
    cur=cur.get(key)
print("" if cur is None else cur)' "$1"
}

json_find_id_by_title() {
  local title="$1"
  python3 -c 'import json,sys
title=sys.argv[1]
raw=sys.stdin.read()
data=json.loads(raw) if raw.strip() else []
items=data.get("items", data) if isinstance(data, dict) else data
if not isinstance(items, list):
    items=[]
for item in items:
    if item.get("title")==title:
        print(item.get("id",""))
        break' "$title"
}

update_api_payload() {
  python3 -c 'import json,sys
api=json.load(sys.stdin)
labels=json.loads(sys.argv[1])
keep=("name","apiVersion","definitionVersion","description","visibility","type",
      "listeners","endpointGroups","tags","groups","analytics","flowExecution",
      "flows","failover","resources","properties")
out={k: api[k] for k in keep if k in api}
out["labels"]=labels
out["lifecycleState"]="PUBLISHED"
out["visibility"]="PUBLIC"
json.dump(out, sys.stdout)' "$1"
}

request() {
  local method="$1"
  local url="$2"
  local body="${3:-}"
  local tmp
  tmp="$(mktemp)"
  local args=(-sS -u "${MGMT_USER}:${MGMT_PASS}" -H "Content-Type: application/json" -w "\n%{http_code}" -o "$tmp" -X "$method")
  if [[ -n "$body" ]]; then
    args+=(-d "$body")
  fi
  local status
  status="$(curl "${args[@]}" "$url" | tail -n1)"
  RESPONSE_BODY="$(cat "$tmp")"
  rm -f "$tmp"
  HTTP_STATUS="$status"
}

must() {
  local expected="$1"
  local what="$2"
  if [[ "$HTTP_STATUS" != "$expected" ]]; then
    echo "Failed: ${what} (HTTP ${HTTP_STATUS})" >&2
    echo "$RESPONSE_BODY" >&2
    exit 1
  fi
}

echo "Checking Management API at ${MGMT_URL} ..."
request GET "${V2}/portal-categories"
if [[ "$HTTP_STATUS" != "200" ]]; then
  echo "Cannot reach ${V2}. Start the Management API and retry." >&2
  echo "$RESPONSE_BODY" >&2
  exit 1
fi

if [[ "$DROP" == "true" ]]; then
  echo "Dropping previous seed data ..."

  # 1. Delete nav items inside category folders, then delete the folders
  request GET "${V2}/portal-navigation-items?area=TOP_NAVBAR&loadChildren=true"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    NAV_BODY_DROP="$RESPONSE_BODY"
    for folder_title in "${SEED_CATEGORY_TITLES[@]}"; do
      folder_id_drop="$(printf '%s' "$NAV_BODY_DROP" | json_find_id_by_title "$folder_title")"
      if [[ -z "$folder_id_drop" ]]; then
        continue
      fi
      child_ids="$(printf '%s' "$NAV_BODY_DROP" | python3 -c 'import json,sys
data=json.load(sys.stdin)
items=data.get("items",data) if isinstance(data,dict) else data
if not isinstance(items,list): items=[]
folder_id=sys.argv[1]
for item in items:
    if item.get("id")==folder_id:
        for child in item.get("children",[]):
            print(child.get("id",""))
        break' "$folder_id_drop")"
      for cid in $child_ids; do
        request DELETE "${V2}/portal-navigation-items/${cid}"
        echo "  deleted nav item ${cid} (HTTP ${HTTP_STATUS})"
      done
      request DELETE "${V2}/portal-navigation-items/${folder_id_drop}"
      echo "  deleted folder '${folder_title}' ${folder_id_drop} (HTTP ${HTTP_STATUS})"
    done

    # Also clean up legacy single "Catalog Seed" folder if present
    legacy_folder_id="$(printf '%s' "$NAV_BODY_DROP" | json_find_id_by_title "$FOLDER_TITLE")"
    if [[ -n "$legacy_folder_id" ]]; then
      child_ids="$(printf '%s' "$NAV_BODY_DROP" | python3 -c 'import json,sys
data=json.load(sys.stdin)
items=data.get("items",data) if isinstance(data,dict) else data
if not isinstance(items,list): items=[]
folder_id=sys.argv[1]
for item in items:
    if item.get("id")==folder_id:
        for child in item.get("children",[]):
            print(child.get("id",""))
        break' "$legacy_folder_id")"
      for cid in $child_ids; do
        request DELETE "${V2}/portal-navigation-items/${cid}"
        echo "  deleted legacy nav item ${cid} (HTTP ${HTTP_STATUS})"
      done
      request DELETE "${V2}/portal-navigation-items/${legacy_folder_id}"
      echo "  deleted legacy folder '${FOLDER_TITLE}' ${legacy_folder_id} (HTTP ${HTTP_STATUS})"
    fi
  fi

  # 2. Delete seed APIs (stop, then delete)
  for api_name in "${SEED_API_NAMES[@]}"; do
    encoded_name="$(python3 -c 'import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))' "$api_name")"
    request GET "${V2}/apis?query=${encoded_name}&page=1&perPage=5"
    if [[ "$HTTP_STATUS" == "200" ]]; then
      matched_ids="$(printf '%s' "$RESPONSE_BODY" | python3 -c 'import json,sys
data=json.load(sys.stdin)
name=sys.argv[1]
items=data.get("data",data) if isinstance(data,dict) else data
if not isinstance(items,list): items=[]
for item in items:
    if item.get("name")==name:
        print(item.get("id",""))' "$api_name")"
      for aid in $matched_ids; do
        request POST "${V2}/apis/${aid}/_stop"
        request DELETE "${V2}/apis/${aid}"
        echo "  deleted API '${api_name}' ${aid} (HTTP ${HTTP_STATUS})"
      done
    fi
  done

  # 3. Delete seed categories
  for cat_title in "${SEED_CATEGORY_TITLES[@]}"; do
    request GET "${V2}/portal-categories"
    if [[ "$HTTP_STATUS" == "200" ]]; then
      cat_id="$(printf '%s' "$RESPONSE_BODY" | json_find_id_by_title "$cat_title")"
      if [[ -n "$cat_id" ]]; then
        request DELETE "${V2}/portal-categories/${cat_id}"
        echo "  deleted category '${cat_title}' ${cat_id} (HTTP ${HTTP_STATUS})"
      fi
    fi
  done

  echo "Drop complete."
  if [[ "$DROP_ONLY" == "true" ]]; then
    exit 0
  fi
  echo
fi

echo "Creating portal categories ..."
CATEGORY_MAP_FILE="$(mktemp)"
printf '{}' >"$CATEGORY_MAP_FILE"
trap 'rm -f "$CATEGORY_MAP_FILE"' EXIT  # updated below after FOLDER_MAP_FILE is created

remember_category() {
  python3 -c 'import json,sys
path, title, category_id = sys.argv[1:4]
with open(path) as handle:
    data=json.load(handle)
data[title]=category_id
with open(path, "w") as handle:
    json.dump(data, handle)' "$CATEGORY_MAP_FILE" "$1" "$2"
}

while IFS='|' read -r title description; do
  request GET "${V2}/portal-categories"
  must 200 "list portal categories"
  existing_id="$(printf '%s' "$RESPONSE_BODY" | json_find_id_by_title "$title")"
  if [[ -n "$existing_id" ]]; then
    echo "  reuse category: ${title} (${existing_id})"
    remember_category "$title" "$existing_id"
    continue
  fi
  request POST "${V2}/portal-categories" "$(python3 -c 'import json,sys; print(json.dumps({"title":sys.argv[1],"description":sys.argv[2],"visible":True}))' "$title" "$description")"
  if [[ "$HTTP_STATUS" == "409" ]]; then
    request GET "${V2}/portal-categories"
    must 200 "list portal categories after conflict"
    existing_id="$(printf '%s' "$RESPONSE_BODY" | json_find_id_by_title "$title")"
    remember_category "$title" "$existing_id"
    echo "  reuse category: ${title} (${existing_id})"
    continue
  fi
  must 201 "create category ${title}"
  existing_id="$(printf '%s' "$RESPONSE_BODY" | json_get id)"
  remember_category "$title" "$existing_id"
  echo "  created category: ${title} (${existing_id})"
done <<'EOF'
Banking|Retail and commercial banking APIs and agents
Loans|Mortgage, personal, and commercial lending
Insurance|Quotes, claims, and underwriting
Investments|Portfolio, markets, and wealth
Customer Service|Account servicing and support
EOF

echo "Creating TOP_NAVBAR category folders ..."
FOLDER_MAP_FILE="$(mktemp)"
printf '{}' >"$FOLDER_MAP_FILE"
trap 'rm -f "$CATEGORY_MAP_FILE" "$FOLDER_MAP_FILE"' EXIT

remember_folder() {
  python3 -c 'import json,sys
path, title, folder_id = sys.argv[1:4]
with open(path) as handle:
    data=json.load(handle)
data[title]=folder_id
with open(path, "w") as handle:
    json.dump(data, handle)' "$FOLDER_MAP_FILE" "$1" "$2"
}

request GET "${V2}/portal-navigation-items?area=TOP_NAVBAR&loadChildren=false"
must 200 "list top navbar items"
NAV_ITEMS_BODY="$RESPONSE_BODY"

FOLDER_ORDER=0
for folder_title in "${SEED_CATEGORY_TITLES[@]}"; do
  folder_id="$(printf '%s' "$NAV_ITEMS_BODY" | json_find_id_by_title "$folder_title")"
  if [[ -z "$folder_id" ]]; then
    request POST "${V2}/portal-navigation-items" "$(python3 -c 'import json,sys; print(json.dumps({"title":sys.argv[1],"type":"FOLDER","area":"TOP_NAVBAR","visibility":"PUBLIC"}))' "$folder_title")"
    must 201 "create folder ${folder_title}"
    folder_id="$(printf '%s' "$RESPONSE_BODY" | json_get id)"
  fi
  request PUT "${V2}/portal-navigation-items/${folder_id}" "$(python3 -c 'import json,sys; print(json.dumps({"title":sys.argv[1],"type":"FOLDER","area":"TOP_NAVBAR","order":int(sys.argv[2]),"published":True,"visibility":"PUBLIC"}))' "$folder_title" "$FOLDER_ORDER")"
  must 200 "publish folder ${folder_title}"
  remember_folder "$folder_title" "$folder_id"
  echo "  folder: ${folder_title} (${folder_id}) order=${FOLDER_ORDER}"
  FOLDER_ORDER=$((FOLDER_ORDER + 1))
done

echo "Creating second publisher '${SECOND_OWNER_NAME}' ..."
SECOND_OWNER_ID=""
request POST "${V1_ORG}/users" "$(python3 -c 'import json,sys; print(json.dumps({"lastname":sys.argv[1],"email":sys.argv[2],"source":"memory","sourceId":sys.argv[2],"service":True}))' "$SECOND_OWNER_NAME" "$SECOND_OWNER_EMAIL")"
if [[ "$HTTP_STATUS" == "200" || "$HTTP_STATUS" == "201" ]]; then
  SECOND_OWNER_ID="$(printf '%s' "$RESPONSE_BODY" | json_get id)"
  echo "  created user ${SECOND_OWNER_ID}"
else
  request GET "${V1_ORG}/users?q=${SECOND_OWNER_EMAIL}&page=1&size=10"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    SECOND_OWNER_ID="$(printf '%s' "$RESPONSE_BODY" | python3 -c 'import json,sys
data=json.load(sys.stdin)
email=sys.argv[1]
items=data.get("data", data) if isinstance(data, dict) else data
if not isinstance(items, list):
    items=[]
for item in items:
    if item.get("email")==email or item.get("sourceId")==email:
        print(item.get("id",""))
        break' "$SECOND_OWNER_EMAIL")"
  fi
  if [[ -n "$SECOND_OWNER_ID" ]]; then
    echo "  reuse user ${SECOND_OWNER_ID}"
  else
    echo "  could not create or find a second publisher; Published by may stay hidden if every API has the same owner."
  fi
fi

PATH_PREFIX="/seed-catalog-$(date +%s)"

create_http_api() {
  local name="$1"
  local description="$2"
  local api_type="$3"
  local connector="$4"
  local path="$5"
  local target="${6:-https://api.gravitee.io/echo}"
  python3 -c 'import json,sys
name, description, api_type, connector, path, target = sys.argv[1:7]
payload={
  "name": name,
  "apiVersion": "1.0",
  "definitionVersion": "V4",
  "description": description,
  "visibility": "PUBLIC",
  "type": api_type,
  "listeners": [{
    "type": "HTTP",
    "paths": [{"path": path}],
    "entrypoints": [{"type": connector, "configuration": {}}]
  }],
  "endpointGroups": [{
    "name": "default-group",
    "type": connector,
    "endpoints": [{
      "name": "default-endpoint",
      "type": connector,
      "inheritConfiguration": False,
      "configuration": {"target": target}
    }]
  }]
}
print(json.dumps(payload))' "$name" "$description" "$api_type" "$connector" "$path" "$target"
}

create_message_api() {
  local name="$1"
  local description="$2"
  local path="$3"
  python3 -c 'import json,sys
name, description, path = sys.argv[1:4]
payload={
  "name": name,
  "apiVersion": "1.0",
  "definitionVersion": "V4",
  "description": description,
  "visibility": "PUBLIC",
  "type": "MESSAGE",
  "listeners": [{
    "type": "HTTP",
    "paths": [{"path": path}],
    "entrypoints": [{
      "type": "http-get",
      "qos": "AUTO",
      "configuration": {"messagesLimitCount": 40, "messagesLimitDurationMs": 2000}
    }]
  }],
  "endpointGroups": [{
    "name": "default-group",
    "type": "mock",
    "endpoints": [{
      "name": "default-endpoint",
      "type": "mock",
      "inheritConfiguration": False,
      "configuration": {"messageContent": "tick", "messageInterval": 1000}
    }]
  }]
}
print(json.dumps(payload))' "$name" "$description" "$path"
}

create_plan_payload() {
  python3 -c 'import json,sys
name, security, validation = sys.argv[1:4]
print(json.dumps({
  "name": name,
  "description": name,
  "definitionVersion": "V4",
  "validation": validation,
  "security": {"type": security, "configuration": {}},
  "mode": "STANDARD",
  "order": 1,
  "characteristics": [],
  "flows": []
}))' "$1" "$2" "$3"
}

publish_nav_item() {
  local nav_id="$1"
  local title="$2"
  local nav_type="$3"
  local category_json="$4"
  local api_id="$5"
  local parent_folder_id="$6"
  python3 -c 'import json,sys
nav_id, title, nav_type, folder_id = sys.argv[1:5]
categories=json.loads(sys.argv[5])
api_id=sys.argv[6] if len(sys.argv)>6 and sys.argv[6] else None
payload={
  "title": title,
  "type": nav_type,
  "parentId": folder_id,
  "order": 0,
  "published": True,
  "visibility": "PUBLIC",
  "categoryIds": categories
}
if api_id:
    payload["apiId"]=api_id
print(json.dumps(payload))' "$nav_id" "$title" "$nav_type" "$parent_folder_id" "$category_json" "$api_id"
}

seed_api() {
  local name="$1"
  local description="$2"
  local api_type="$3"
  local connector="$4"
  local slug="$5"
  local labels_csv="$6"
  local categories_csv="$7"
  local nav_type="$8"
  local plan_security="$9"
  local plan_validation="${10}"
  local transfer="${11:-false}"

  local labels_json category_json
  labels_json="$(python3 -c 'import json,sys; print(json.dumps([s.strip() for s in sys.argv[1].split(",") if s.strip()]))' "$labels_csv")"
  category_json="$(python3 -c 'import json,sys
titles=[t.strip() for t in sys.argv[1].split(",") if t.strip()]
ids=json.load(open(sys.argv[2]))
print(json.dumps([ids[title] for title in titles if title in ids]))' "$categories_csv" "$CATEGORY_MAP_FILE")"

  echo "Creating ${name} (${api_type}) ..."
  local payload
  if [[ "$api_type" == "MESSAGE" ]]; then
    payload="$(create_message_api "$name" "$description" "${PATH_PREFIX}/${slug}")"
  else
    payload="$(create_http_api "$name" "$description" "$api_type" "$connector" "${PATH_PREFIX}/${slug}")"
  fi
  request POST "${V2}/apis" "$payload"
  must 201 "create API ${name}"
  local api_id
  api_id="$(printf '%s' "$RESPONSE_BODY" | json_get id)"

  request GET "${V2}/apis/${api_id}"
  must 200 "get API ${name}"
  request PUT "${V2}/apis/${api_id}" "$(printf '%s' "$RESPONSE_BODY" | update_api_payload "$labels_json")"
  must 200 "publish and label API ${name}"

  request POST "${V2}/apis/${api_id}/plans" "$(create_plan_payload "Catalog ${plan_security}" "$plan_security" "$plan_validation")"
  must 201 "create plan for ${name}"
  local plan_id
  plan_id="$(printf '%s' "$RESPONSE_BODY" | json_get id)"
  request POST "${V2}/apis/${api_id}/plans/${plan_id}/_publish"
  must 200 "publish plan for ${name}"

  request POST "${V2}/apis/${api_id}/deployments" '{"deploymentLabel":"catalog-seed"}'
  if [[ "$HTTP_STATUS" != "202" && "$HTTP_STATUS" != "200" ]]; then
    echo "  deploy skipped for ${name} (HTTP ${HTTP_STATUS})"
  fi
  request POST "${V2}/apis/${api_id}/_start"
  if [[ "$HTTP_STATUS" != "204" && "$HTTP_STATUS" != "200" ]]; then
    echo "  start skipped for ${name} (HTTP ${HTTP_STATUS})"
  fi

  if [[ "$transfer" == "true" && -n "$SECOND_OWNER_ID" ]]; then
    request POST "${V2}/apis/${api_id}/_transfer-ownership" "$(python3 -c 'import json,sys; print(json.dumps({"userId":sys.argv[1],"userType":"USER","poRole":"USER"}))' "$SECOND_OWNER_ID")"
    if [[ "$HTTP_STATUS" != "204" && "$HTTP_STATUS" != "200" ]]; then
      echo "  transfer ownership skipped for ${name} (HTTP ${HTTP_STATUS})"
    else
      echo "  transferred ownership to ${SECOND_OWNER_NAME}"
    fi
  fi

  local first_category parent_folder_id
  first_category="$(python3 -c 'import sys; print([t.strip() for t in sys.argv[1].split(",") if t.strip()][0])' "$categories_csv")"
  parent_folder_id="$(python3 -c 'import json,sys
ids=json.load(open(sys.argv[1]))
print(ids.get(sys.argv[2],""))' "$FOLDER_MAP_FILE" "$first_category")"

  if [[ -z "$parent_folder_id" ]]; then
    echo "  WARNING: no folder for category '${first_category}', skipping nav item for ${name}"
    return
  fi

  request POST "${V2}/portal-navigation-items" "$(python3 -c 'import json,sys
print(json.dumps({
  "title": sys.argv[1],
  "type": sys.argv[2],
  "area": "TOP_NAVBAR",
  "visibility": "PUBLIC",
  "parentId": sys.argv[3],
  "apiId": sys.argv[4],
  "categoryIds": json.loads(sys.argv[5])
}))' "$name" "$nav_type" "$parent_folder_id" "$api_id" "$category_json")"
  must 201 "create nav item for ${name}"
  local nav_id
  nav_id="$(printf '%s' "$RESPONSE_BODY" | json_get id)"
  request PUT "${V2}/portal-navigation-items/${nav_id}" "$(publish_nav_item "$nav_id" "$name" "$nav_type" "$category_json" "$api_id" "$parent_folder_id")"
  must 200 "publish nav item for ${name}"

  seed_docs "$nav_id" "$slug"

  echo "  catalog item ready: ${name} → ${first_category}"
}

create_page() {
  local parent_nav_id="$1"
  local page_title="$2"
  local page_order="${3:-0}"
  local content
  content="$(cat)"

  # 1. Create a PAGE nav item under the parent
  local create_payload
  create_payload="$(python3 -c 'import json,sys
print(json.dumps({
  "title": sys.argv[1],
  "type": "PAGE",
  "area": "TOP_NAVBAR",
  "visibility": "PUBLIC",
  "parentId": sys.argv[2]
}))' "$page_title" "$parent_nav_id")"

  request POST "${V2}/portal-navigation-items" "$create_payload"
  if [[ "$HTTP_STATUS" != "201" ]]; then
    echo "    page '${page_title}' create failed (HTTP ${HTTP_STATUS})" >&2
    echo "    $RESPONSE_BODY" >&2
    return
  fi
  local page_nav_id content_id
  page_nav_id="$(printf '%s' "$RESPONSE_BODY" | json_get id)"
  content_id="$(printf '%s' "$RESPONSE_BODY" | json_get portalPageContentId)"

  # 2. PUT the markdown content
  local content_payload
  content_payload="$(python3 -c 'import json,sys; print(json.dumps({"content": sys.stdin.read()}))' <<< "$content")"
  request PUT "${V2}/portal-page-contents/${content_id}" "$content_payload"
  if [[ "$HTTP_STATUS" != "200" ]]; then
    echo "    page '${page_title}' content update failed (HTTP ${HTTP_STATUS})" >&2
    echo "    $RESPONSE_BODY" >&2
    return
  fi

  # 3. Publish the nav item
  request PUT "${V2}/portal-navigation-items/${page_nav_id}" "$(python3 -c 'import json,sys
print(json.dumps({
  "title": sys.argv[1],
  "type": "PAGE",
  "parentId": sys.argv[2],
  "order": int(sys.argv[3]),
  "published": True,
  "visibility": "PUBLIC"
}))' "$page_title" "$parent_nav_id" "$page_order")"
  if [[ "$HTTP_STATUS" != "200" ]]; then
    echo "    page '${page_title}' publish failed (HTTP ${HTTP_STATUS})" >&2
    echo "    $RESPONSE_BODY" >&2
    return
  fi
  echo "    page '${page_title}' published (order=${page_order})"
}

seed_docs() {
  local parent_nav_id="$1"
  local slug="$2"
  echo "  adding docs for ${slug} ..."

  case "$slug" in
    mortgage-calculator)
      create_page "$parent_nav_id" "Overview" 0 <<'MDOC'
# Mortgage Calculator Agent

The Mortgage Calculator Agent provides real-time mortgage payment estimates, amortization schedules, and refinancing comparisons for retail banking customers and internal loan officers.

## Key Capabilities

| Capability | Description |
|---|---|
| **Payment estimation** | Fixed-rate, adjustable-rate, and interest-only monthly payment calculations |
| **Amortization schedules** | Full principal-and-interest breakdown over the life of the loan |
| **Refinance comparison** | Side-by-side comparison of current vs. proposed terms |
| **Affordability check** | Calculates maximum loan amount from income, debts, and target DTI ratio |

## How It Works

The agent accepts a natural-language or structured request describing the loan scenario and returns a comprehensive payment breakdown. Under the hood it applies standard financial formulas (PMT, PV, FV) and the bank's current rate sheet.

```
User → "What would my monthly payment be on a $450,000 loan at 6.25% for 30 years?"
Agent → { monthlyPayment: 2,770.98, totalInterest: 547,553.28, ... }
```

## Rate Sources

Rates are sourced from the bank's internal Rate Engine API, refreshed every 15 minutes during market hours. The agent always states the effective date of the rate used in its response.

## Limits and Constraints

- Maximum loan amount: $10,000,000
- Supported terms: 10, 15, 20, 25, and 30 years
- Currencies: USD only (EUR and GBP planned for Q3)
- Rate lock quotes are informational only — binding rate locks require a separate approval workflow
MDOC

      create_page "$parent_nav_id" "Input Parameters" 1 <<'MDOC'
# Input Parameters

## Required Fields

| Parameter | Type | Description |
|---|---|---|
| `loanAmount` | number | Principal amount in dollars |
| `interestRate` | number | Annual interest rate as a decimal (e.g., 6.25) |
| `termYears` | integer | Loan term in years |

## Optional Fields

| Parameter | Type | Default | Description |
|---|---|---|---|
| `downPayment` | number | 0 | Down payment amount |
| `propertyTax` | number | — | Annual property tax for escrow calculation |
| `insurance` | number | — | Annual homeowner's insurance premium |
| `pmi` | boolean | auto | Include PMI when LTV > 80% |
| `startDate` | date | today | Amortization start date |
| `extraPayment` | number | 0 | Additional monthly principal payment |
| `scenario` | string | `FIXED` | One of `FIXED`, `ARM_5_1`, `ARM_7_1`, `INTEREST_ONLY` |

## Example Request (Natural Language)

```
Calculate monthly payments for a $350,000 home with 20% down,
30-year fixed at today's rate. Include property tax of $4,200/year
and insurance of $1,800/year.
```

## Example Request (Structured)

```json
{
  "loanAmount": 280000,
  "interestRate": 6.25,
  "termYears": 30,
  "downPayment": 70000,
  "propertyTax": 4200,
  "insurance": 1800,
  "scenario": "FIXED"
}
```

## Response Format

The agent returns a JSON object containing:

- `monthlyPayment` — total monthly payment including escrow
- `principalAndInterest` — P&I portion only
- `amortizationSchedule` — array of monthly breakdowns (first 12 months by default)
- `totalInterest` — total interest paid over the life of the loan
- `payoffDate` — estimated final payment date
- `effectiveRate` — the rate used, with its effective date
MDOC

      create_page "$parent_nav_id" "Error Handling" 2 <<'MDOC'
# Error Handling

The agent uses structured error responses so consuming applications can surface meaningful messages to end users.

## Error Codes

| Code | HTTP Status | Description | Recommended Action |
|---|---|---|---|
| `INVALID_LOAN_AMOUNT` | 400 | Loan amount is zero, negative, or exceeds the $10M cap | Validate input range before calling |
| `UNSUPPORTED_TERM` | 400 | Term is not one of the supported values | Show a dropdown of valid terms |
| `RATE_UNAVAILABLE` | 503 | Rate Engine is unreachable or stale (>30 min) | Retry after 60 seconds; fall back to last-known rate |
| `CALCULATION_ERROR` | 500 | Internal arithmetic failure | Contact support with the correlation ID |
| `UNAUTHORIZED` | 401 | Missing or invalid API credentials | Check API key or token expiry |

## Retry Policy

- `503` responses include a `Retry-After` header (in seconds).
- Clients should implement exponential back-off starting at 1 second, capped at 60 seconds.
- Do not retry `400`-level errors — they indicate a client-side issue.

## Correlation IDs

Every response includes an `X-Correlation-Id` header. Include this value in any support ticket to enable fast diagnosis.
MDOC
      ;;

    loan-eligibility)
      create_page "$parent_nav_id" "Overview" 0 <<'MDOC'
# Loan Eligibility Agent

The Loan Eligibility Agent performs preliminary screening of loan applications against the bank's lending policy. It evaluates income, employment history, existing obligations, and credit indicators to produce a pass/refer/decline recommendation.

## Purpose

Branch staff and digital channels use this agent as a first-pass filter before routing applications to underwriting. It reduces underwriter workload by up to 40% by catching policy violations early and providing clear explanations.

## Decision Factors

| Factor | Weight | Source |
|---|---|---|
| Debt-to-income ratio (DTI) | High | Applicant-declared income and bureau data |
| Employment tenure | Medium | Applicant declaration, verified at underwriting |
| Loan-to-value ratio (LTV) | High | Requested amount vs. collateral valuation |
| Credit score band | High | Credit Score Agent |
| Residency status | Low | KYC data |
| Existing relationship | Low | Core banking — account tenure and product holdings |

## Output

The agent returns one of three outcomes:

- **ELIGIBLE** — the application meets all policy thresholds and can proceed to automated approval or fast-track underwriting.
- **REFER** — one or more factors are marginal; the application must be reviewed by a human underwriter. The response includes the specific factors that triggered referral.
- **DECLINE** — a hard policy violation exists (e.g., DTI above 55%). The response includes a customer-safe explanation and suggested next steps.
MDOC

      create_page "$parent_nav_id" "Integration Guide" 1 <<'MDOC'
# Integration Guide

## Authentication

This agent requires an API key passed in the `X-Gravitee-Api-Key` header. Keys are provisioned through the Developer Portal and scoped to your application.

## Request Flow

```
1. Collect applicant data (income, employment, loan request)
2. POST to Loan Eligibility Agent
3. Receive ELIGIBLE / REFER / DECLINE decision
4. Route to appropriate downstream process
```

## Rate Limits

| Plan | Requests/min | Burst |
|---|---|---|
| Standard | 60 | 10 |
| Premium | 300 | 50 |

Exceeding the limit returns `429 Too Many Requests` with a `Retry-After` header.

## Idempotency

Requests are **not** idempotent. Each call creates a screening record in the audit log. If you need to re-screen the same applicant, send a new request — the agent will return a fresh decision based on current policy and data.

## Data Retention

Screening results are retained for 7 years in compliance with lending regulations. The response includes a `screeningId` that can be used to retrieve the decision from the Audit API.

## Sandbox Environment

A sandbox environment is available at `sandbox.acmebank.example.com`. It uses synthetic data and always returns deterministic results for test applicant IDs documented in the sandbox guide.
MDOC
      ;;

    credit-score)
      create_page "$parent_nav_id" "Overview" 0 <<'MDOC'
# Credit Score Agent

The Credit Score Agent retrieves consumer credit scores from authorised credit bureaus and returns a normalised score with an explanatory breakdown. It supports Equifax, Experian, and TransUnion, and can merge tri-bureau results into a single composite view.

## Features

- **Single-bureau lookup** — query one bureau by name
- **Tri-bureau merge** — request all three and receive a merged report with the median score, high, low, and per-bureau detail
- **Score factors** — top 4 positive and negative factors influencing the score, using standardised reason codes
- **Trend data** — optional 12-month score history when the bureau supports it

## Use Cases

| Consumer | Use Case |
|---|---|
| Loan Eligibility Agent | Automated DTI + credit pre-screening |
| Branch staff | Manual credit review during appointment |
| Mobile app | Customer self-serve credit health dashboard |

## Consent and Compliance

A valid consent token must accompany every request. The token is obtained through the bank's Consent Management API and encodes the customer's agreement to a hard or soft credit pull. Requests without a valid consent token are rejected with `403 CONSENT_REQUIRED`.
MDOC

      create_page "$parent_nav_id" "Bureau Configuration" 1 <<'MDOC'
# Bureau Configuration

## Supported Bureaus

| Bureau | Code | Soft Pull | Hard Pull | Trend Data |
|---|---|---|---|---|
| Equifax | `EFX` | ✔ | ✔ | ✔ |
| Experian | `XPN` | ✔ | ✔ | ✗ |
| TransUnion | `TU` | ✔ | ✔ | ✔ |

## Pull Types

- **Soft pull** — does not affect the consumer's score. Used for pre-qualification and account reviews. Requires `inquiry.type = SOFT`.
- **Hard pull** — recorded on the consumer's credit file. Used for formal credit applications. Requires `inquiry.type = HARD` and a valid consent token with hard-pull scope.

## Timeout and Fallback

Each bureau request has a 10-second timeout. If a bureau does not respond:

1. The agent marks that bureau as `UNAVAILABLE` in the response.
2. If at least one bureau returned data, the response is still successful with a partial result.
3. If all bureaus time out, the agent returns `503 SERVICE_UNAVAILABLE`.

## Score Normalisation

All scores are normalised to a 300–850 range. The response includes both the raw bureau score and the normalised value. Score bands:

| Band | Range | Description |
|---|---|---|
| Excellent | 750–850 | Best rates available |
| Good | 700–749 | Most products available |
| Fair | 650–699 | Limited product range, higher rates |
| Poor | 550–649 | Secured products or guarantor required |
| Very Poor | 300–549 | Decline likely |
MDOC
      ;;

    investment-advisor)
      create_page "$parent_nav_id" "Overview" 0 <<'MDOC'
# Investment Advisor Agent

The Investment Advisor Agent generates personalised portfolio allocation recommendations based on a client's risk profile, investment horizon, and financial goals. It is designed to assist wealth advisors and power self-directed investment tools in the mobile banking app.

## Methodology

The agent uses Modern Portfolio Theory (MPT) to optimise expected return for a given level of risk. Inputs are mapped to one of five model portfolios, then adjusted for the client's specific constraints (e.g., ESG preferences, sector exclusions, liquidity needs).

## Model Portfolios

| Profile | Equities | Fixed Income | Alternatives | Cash |
|---|---|---|---|---|
| Conservative | 20% | 55% | 10% | 15% |
| Moderate Conservative | 35% | 45% | 10% | 10% |
| Moderate | 50% | 30% | 15% | 5% |
| Moderate Aggressive | 65% | 20% | 10% | 5% |
| Aggressive | 80% | 10% | 8% | 2% |

## Regulatory Compliance

All recommendations include a suitability assessment reference and a disclaimer. The agent does not constitute financial advice — it produces suggestions that must be reviewed by a licensed advisor before execution.

## Output

The response includes:
- Recommended allocation percentages by asset class
- Specific fund/ETF suggestions with ISIN codes
- Expected annual return range (5th–95th percentile, backtested)
- Risk metrics: standard deviation, Sharpe ratio, maximum drawdown
- Rebalancing schedule recommendation
MDOC

      create_page "$parent_nav_id" "Risk Profiling" 1 <<'MDOC'
# Risk Profiling

## Questionnaire Mapping

The agent accepts either a completed risk questionnaire or a pre-computed risk score (1–10). When a questionnaire is provided, the agent scores it internally.

| Question Area | Weight | Example |
|---|---|---|
| Investment objective | 25% | Growth vs. income vs. preservation |
| Time horizon | 20% | < 3 years, 3–7 years, 7–15 years, 15+ years |
| Loss tolerance | 25% | Acceptable portfolio decline in a downturn |
| Income stability | 15% | Fixed salary vs. variable/commission vs. retired |
| Existing portfolio | 15% | Current asset mix and experience level |

## Risk Score to Profile Mapping

| Score Range | Profile |
|---|---|
| 1–2 | Conservative |
| 3–4 | Moderate Conservative |
| 5–6 | Moderate |
| 7–8 | Moderate Aggressive |
| 9–10 | Aggressive |

## Constraints

Clients may specify additional constraints that override the model allocation:

- **ESG only** — exclude non-ESG-rated instruments
- **Sector exclusion** — e.g., no tobacco, no weapons
- **Geography preference** — e.g., domestic only, global ex-emerging
- **Liquidity floor** — minimum cash allocation regardless of profile
MDOC
      ;;

    insurance-quote)
      create_page "$parent_nav_id" "Overview" 0 <<'MDOC'
# Insurance Quote Agent

The Insurance Quote Agent produces indicative premium quotes for home and auto insurance products. It connects to the bank's insurance underwriting engine and returns pricing within seconds, enabling real-time quote display in digital channels.

## Supported Products

| Product | Line | Key Rating Factors |
|---|---|---|
| Homeowner's | Property | Location, rebuild cost, construction type, security features |
| Renter's | Property | Location, contents value, liability limit |
| Auto — comprehensive | Motor | Vehicle age/value, driver age/experience, claims history, annual mileage |
| Auto — third party | Motor | Driver profile, vehicle class |

## Quote Lifecycle

1. **Request** — submit risk details and coverage preferences
2. **Indicative quote** — agent returns premium, excess, and coverage summary (valid for 30 days)
3. **Bind** (out of scope) — binding the policy is handled by the Policy Administration API after identity verification

## Premium Factors

The agent explains the top rating factors that influenced the premium, helping front-end applications display a transparent breakdown to the customer. Each factor includes its direction (increase/decrease) and relative impact.
MDOC

      create_page "$parent_nav_id" "Coverage Options" 1 <<'MDOC'
# Coverage Options

## Home Insurance

| Coverage | Standard | Premium |
|---|---|---|
| Building | Rebuild cost | Rebuild cost + 25% contingency |
| Contents | Up to $50,000 | Up to $150,000 |
| Liability | $1,000,000 | $5,000,000 |
| Temporary accommodation | 12 months | 24 months |
| Accidental damage | ✗ | ✔ |
| Home office equipment | ✗ | ✔ (up to $10,000) |

## Auto Insurance

| Coverage | Third Party | Comprehensive |
|---|---|---|
| Third-party property | $20,000,000 | $20,000,000 |
| Third-party injury | Unlimited | Unlimited |
| Own vehicle damage | ✗ | Market value / agreed value |
| Windscreen | ✗ | ✔ (no excess) |
| Rental car | ✗ | ✔ (up to 30 days) |
| Roadside assist | ✗ | ✔ |

## Excess Structure

All quotes include a standard excess (deductible). Customers may elect a higher voluntary excess to reduce the premium. The agent returns quotes at three excess levels:

- **Low** — minimum excess, highest premium
- **Standard** — recommended balance
- **High** — maximum excess, lowest premium

Each level shows the annual premium and the total excess payable at claim time.
MDOC
      ;;

    fraud-detection)
      create_page "$parent_nav_id" "Overview" 0 <<'MDOC'
# Fraud Detection Agent

The Fraud Detection Agent monitors payment and account activity for suspicious patterns. It combines rule-based screening with a machine-learning scoring model to flag potentially fraudulent transactions in real time.

## Detection Capabilities

| Category | Examples |
|---|---|
| Payment fraud | Unusual amount, atypical recipient, velocity spike, geographic anomaly |
| Account takeover | Login from new device + immediate high-value transfer, credential stuffing patterns |
| Application fraud | Synthetic identity indicators, inconsistent applicant data |
| Card fraud | Card-not-present anomalies, compromised BIN ranges, test-amount patterns |

## Risk Scoring

Every transaction receives a risk score between 0 (benign) and 100 (almost certainly fraudulent). The score drives the action:

| Score Range | Action | SLA |
|---|---|---|
| 0–30 | Allow | Instant |
| 31–60 | Flag for review | Queued for analyst within 4 hours |
| 61–80 | Hold and alert | Transaction held; customer contacted within 1 hour |
| 81–100 | Block | Transaction declined; case opened automatically |

## Feedback Loop

Analysts mark flagged transactions as `CONFIRMED_FRAUD` or `FALSE_POSITIVE`. This feedback is ingested nightly to retrain the ML model, improving precision over time.
MDOC

      create_page "$parent_nav_id" "Integration Patterns" 1 <<'MDOC'
# Integration Patterns

## Real-Time Screening

The primary integration is inline screening during payment processing:

```
Payment Service → Fraud Detection Agent → Allow / Hold / Block → Payment Gateway
```

The agent responds within 150 ms at the 99th percentile, meeting the bank's real-time payment SLA.

## Batch Screening

For back-office review, the agent accepts batch submissions of up to 10,000 transactions via a CSV or JSON array. Results are returned asynchronously via webhook or polling.

## Event-Driven Integration

The agent can consume transaction events from a Kafka topic (`payments.completed`). Suspicious transactions are published to `fraud.alerts` for downstream consumption by case management systems.

## Webhooks

Configure a webhook URL to receive real-time alerts when a transaction scores above a configurable threshold. The payload includes:

- Transaction ID and amount
- Risk score and top contributing factors
- Recommended action
- Link to the case in the fraud analyst console

## Testing

The sandbox environment includes test card numbers and account IDs that trigger specific fraud scenarios:

| Test ID | Scenario | Expected Score |
|---|---|---|
| `4111-0000-FRAUD-0001` | High-value geographic anomaly | 85 |
| `4111-0000-FRAUD-0002` | Velocity spike (10 txns in 2 min) | 72 |
| `4111-0000-FRAUD-0003` | Clean transaction | 5 |
MDOC
      ;;

    account-summary)
      create_page "$parent_nav_id" "Overview" 0 <<'MDOC'
# Account Summary Agent

The Account Summary Agent provides a consolidated view of a customer's accounts, including balances, recent transactions, upcoming payments, and key account alerts. It powers the "home screen" experience in mobile and web banking.

## Data Aggregated

| Data Point | Source | Refresh |
|---|---|---|
| Current / savings balances | Core Banking | Real-time |
| Credit card balance and available credit | Card Processing | Near real-time (< 30 s) |
| Mortgage outstanding balance | Loan Servicing | Daily |
| Upcoming direct debits and standing orders | Payment Hub | Real-time |
| Recent transactions (last 30 days) | Transaction Store | Real-time |
| Pending transactions | Card Processing / Payment Hub | Real-time |

## Response Structure

The agent returns a single JSON object grouping accounts by type:

- `currentAccounts[]` — balance, available funds, overdraft limit
- `savingsAccounts[]` — balance, interest rate, maturity date (for term deposits)
- `creditCards[]` — balance, available credit, minimum payment, due date
- `loans[]` — outstanding principal, next payment amount, next payment date
- `alerts[]` — low balance warnings, upcoming large debits, rate changes

## Personalisation

The agent tailors the summary based on the customer's preferences:
- **Favourite account** shown first
- **Hidden accounts** excluded
- **Nicknames** used instead of product names when set
- **Currency conversion** applied for multi-currency customers
MDOC

      create_page "$parent_nav_id" "Data Freshness" 1 <<'MDOC'
# Data Freshness and Caching

## Freshness Guarantees

| Data Type | Max Staleness | Notes |
|---|---|---|
| Account balance | 0 s (real-time) | Sourced from core banking ledger |
| Pending transactions | 30 s | Card processor push interval |
| Mortgage balance | 24 h | Batch update overnight |
| Interest accrual | 24 h | Calculated during end-of-day processing |

## Caching Behaviour

The agent caches aggregated responses for 60 seconds per customer to reduce load on downstream systems. The `Cache-Control` header in the response indicates the remaining TTL.

To force a fresh fetch, pass `Cache-Control: no-cache` in the request header. Use sparingly — forced refreshes bypass the cache for all downstream sources.

## Partial Failures

If one data source is unavailable, the agent returns a partial response with the available data and includes a `warnings[]` array listing the unavailable sources. The HTTP status remains `200` — the `warnings` array is the signal to the UI to show a degraded-data banner.

```json
{
  "currentAccounts": [ ... ],
  "warnings": [
    { "source": "CardProcessing", "message": "Temporarily unavailable", "retryAfterSeconds": 120 }
  ]
}
```
MDOC
      ;;

    payment-processing)
      create_page "$parent_nav_id" "Overview" 0 <<'MDOC'
# Payment Processing Agent

The Payment Processing Agent initiates domestic and international transfers, standing orders, and scheduled payments on behalf of authenticated customers. It orchestrates the end-to-end payment flow from validation through fraud screening to settlement.

## Supported Payment Types

| Type | Scheme | Currency | Cut-off |
|---|---|---|---|
| Domestic instant | FedNow / RTP | USD | 24/7 |
| Domestic same-day | ACH same-day | USD | 14:00 ET |
| Domestic next-day | ACH | USD | 17:00 ET |
| International wire | SWIFT gpi | Multi | 15:00 ET |
| Internal transfer | Book transfer | USD | Instant |
| Standing order | ACH recurring | USD | T-1 business day |

## Payment Flow

```
1. Validate payee and amount
2. Check sufficient funds / available credit
3. Screen via Fraud Detection Agent
4. Obtain authorisation (SCA if required)
5. Submit to payment scheme
6. Return payment reference and estimated arrival
```

## Idempotency

Every payment request must include an `Idempotency-Key` header (UUID v4). The agent guarantees that replaying the same key within 24 hours will not create a duplicate payment. The original response is returned instead.

## Status Tracking

After submission, the payment status can be tracked via the `paymentId` returned in the response. Statuses progress through: `ACCEPTED` → `PROCESSING` → `SETTLED` (or `FAILED` / `RETURNED`).
MDOC

      create_page "$parent_nav_id" "Limits and Controls" 1 <<'MDOC'
# Limits and Controls

## Transaction Limits

| Channel | Single Payment | Daily Aggregate | Requires SCA |
|---|---|---|---|
| Mobile app | $25,000 | $50,000 | > $1,000 |
| Web banking | $50,000 | $100,000 | > $5,000 |
| API (trusted partner) | $500,000 | $1,000,000 | Always |

Limits are configurable per customer segment. Temporary limit increases can be requested through the Customer Service channel and take effect within 15 minutes.

## Strong Customer Authentication (SCA)

Payments above the SCA threshold require a second factor. The agent returns a `challenge` object when SCA is needed:

```json
{
  "status": "SCA_REQUIRED",
  "challenge": {
    "type": "PUSH_NOTIFICATION",
    "expiresIn": 300,
    "challengeId": "ch_abc123"
  }
}
```

The client must complete the challenge and resubmit with the `challengeId` and approval token.

## Sanctions Screening

All international payments are screened against OFAC, EU, and UN sanctions lists. Matches result in an automatic hold and escalation to the compliance team. The agent returns `HELD_FOR_REVIEW` — the payment is neither completed nor rejected until compliance clears it.

## Cut-off Times

Payments submitted after the scheme cut-off are queued for the next processing window. The response indicates the expected settlement date. Customers can cancel queued payments before the processing window opens.
MDOC
      ;;

    account-rest)
      create_page "$parent_nav_id" "Overview" 0 <<'MDOC'
# Account REST API

The Account REST API provides standard CRUD operations for retail and commercial bank accounts. It exposes core banking data through a RESTful interface compliant with the Open Banking standard.

## Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/accounts` | List accounts for the authenticated customer |
| `GET` | `/accounts/{id}` | Account detail including balance and product info |
| `GET` | `/accounts/{id}/transactions` | Paginated transaction history |
| `GET` | `/accounts/{id}/statements` | Monthly statement list (PDF download links) |
| `POST` | `/accounts/{id}/beneficiaries` | Add a new payment beneficiary |
| `GET` | `/accounts/{id}/beneficiaries` | List registered beneficiaries |
| `GET` | `/accounts/{id}/direct-debits` | Active direct debit mandates |

## Pagination

Transaction and statement endpoints support cursor-based pagination:

- `?cursor=<token>&limit=50` — default limit is 25, maximum is 100
- The response includes `nextCursor` when more results are available

## Filtering

Transactions can be filtered by:
- `from` / `to` — date range (ISO 8601)
- `type` — `CREDIT`, `DEBIT`, or `ALL`
- `category` — merchant category code
- `minAmount` / `maxAmount`
MDOC

      create_page "$parent_nav_id" "Authentication" 1 <<'MDOC'
# Authentication and Authorization

## OAuth 2.0

The Account REST API uses OAuth 2.0 with the Authorization Code Grant flow. Tokens are issued by the bank's Identity Provider and must be passed in the `Authorization: Bearer <token>` header.

## Scopes

| Scope | Access |
|---|---|
| `accounts:read` | List accounts, view balances and transactions |
| `accounts:write` | Manage beneficiaries and preferences |
| `statements:read` | Download statements |
| `direct-debits:read` | View direct debit mandates |

## Consent

Third-party applications must obtain explicit customer consent for each scope. Consent records are managed by the Consent API and are revocable at any time. Accessing data without a valid consent returns `403 CONSENT_EXPIRED`.

## Rate Limits

| Client Type | Requests/min |
|---|---|
| First-party apps | 600 |
| Third-party (standard) | 120 |
| Third-party (premium) | 300 |
MDOC
      ;;

    market-data)
      create_page "$parent_nav_id" "Overview" 0 <<'MDOC'
# Market Data Stream

The Market Data Stream provides real-time and delayed price ticks for listed instruments across major global exchanges. It uses an asynchronous messaging pattern — clients subscribe via an HTTP streaming endpoint and receive a continuous flow of JSON-formatted tick events.

## Supported Exchanges

| Exchange | Code | Latency | Hours (ET) |
|---|---|---|---|
| NYSE | `XNYS` | Real-time | 09:30–16:00 |
| NASDAQ | `XNAS` | Real-time | 09:30–16:00 |
| LSE | `XLON` | 15 min delay | 08:00–16:30 GMT |
| Tokyo | `XTKS` | 15 min delay | 09:00–15:00 JST |

## Tick Format

Each tick is a JSON object delivered as a server-sent event (SSE):

```json
{
  "symbol": "AAPL",
  "exchange": "XNAS",
  "price": 187.42,
  "change": 1.23,
  "changePct": 0.66,
  "volume": 45123,
  "timestamp": "2025-07-15T14:32:01.456Z"
}
```

## Subscription

Subscribe by specifying one or more symbols as query parameters:

```
GET /market-data?symbols=AAPL,GOOGL,MSFT
Accept: text/event-stream
```

The connection remains open and ticks are pushed as they arrive. Reconnect with `Last-Event-ID` to resume without missing ticks.
MDOC

      create_page "$parent_nav_id" "Data Policies" 1 <<'MDOC'
# Data Policies

## Redistribution

Market data received through this stream is licensed for internal use only. Redistribution to external parties, display on public websites, or resale requires a separate market data license from the relevant exchange.

## Throttling

| Plan | Max Symbols | Max Connections |
|---|---|---|
| Standard | 50 | 2 |
| Professional | 500 | 10 |
| Enterprise | Unlimited | 50 |

Exceeding the symbol limit returns `400 TOO_MANY_SYMBOLS`. Exceeding the connection limit drops the oldest connection.

## Historical Data

This stream provides live/delayed ticks only. For historical OHLCV (Open-High-Low-Close-Volume) data, use the Market History REST API (separate subscription required).

## Corporate Actions

The stream includes corporate action events (splits, dividends, symbol changes) as special tick types with `"type": "CORPORATE_ACTION"`. Clients should handle these to keep local caches consistent.
MDOC
      ;;

    llm-documents)
      create_page "$parent_nav_id" "Overview" 0 <<'MDOC'
# LLM Document Analyzer

The LLM Document Analyzer extracts structured data from uploaded loan documents using large language model technology. It processes scanned PDFs, photographed documents, and digital files to pull out key fields needed for loan origination workflows.

## Supported Document Types

| Document | Extracted Fields |
|---|---|
| Pay stubs | Employer name, gross pay, net pay, pay period, YTD earnings |
| W-2 forms | Employer EIN, wages, federal/state tax withheld |
| Bank statements | Account holder, institution, period, ending balance, average balance |
| Tax returns (1040) | Filing status, AGI, taxable income, total tax |
| Property appraisals | Property address, appraised value, comparables, condition rating |
| Title documents | Legal description, encumbrances, ownership chain |

## Processing Pipeline

1. **Upload** — document submitted as multipart/form-data or base64-encoded
2. **OCR** — optical character recognition (for scanned/photographed documents)
3. **Classification** — the model identifies the document type
4. **Extraction** — structured fields are extracted with confidence scores
5. **Validation** — cross-field consistency checks (e.g., YTD earnings vs. pay period)
6. **Response** — extracted data returned as structured JSON

## Confidence Scores

Every extracted field includes a confidence score (0.0–1.0). Fields below 0.85 are flagged for human review. The response groups fields into `highConfidence`, `mediumConfidence`, and `lowConfidence` arrays for easy triaging.
MDOC

      create_page "$parent_nav_id" "Security and Privacy" 1 <<'MDOC'
# Security and Privacy

## Data Handling

- Uploaded documents are encrypted at rest (AES-256) and in transit (TLS 1.3).
- Documents are processed in an isolated environment and **deleted within 24 hours** of extraction.
- No document content is used for model training.
- Processing logs retain metadata (document type, page count, processing time) but not content.

## PII Redaction

The analyzer detects and can optionally redact PII (Social Security Numbers, account numbers, dates of birth) from the extracted output. Enable redaction by setting `"redactPii": true` in the request.

## Access Control

| Role | Permissions |
|---|---|
| Loan Officer | Upload documents, view extractions |
| Underwriter | Upload, view, override extracted values |
| Auditor | View extraction history (read-only) |
| API Consumer | Upload and receive extraction results only |

## Audit Trail

Every document upload and extraction is logged with:
- Timestamp, user ID, and client application
- Document type and page count
- Fields extracted and confidence scores
- Any manual overrides applied downstream

Audit records are retained for 7 years in compliance with lending regulations.
MDOC
      ;;

    mcp-data)
      create_page "$parent_nav_id" "Overview" 0 <<'MDOC'
# MCP Data Connector

The MCP Data Connector exposes core banking data through the Model Context Protocol (MCP), enabling AI agents and LLM-powered tools to query customer, account, and product data in a structured, permission-controlled manner.

## Available Tools

| Tool Name | Description | Parameters |
|---|---|---|
| `get_customer` | Retrieve customer profile | `customerId` |
| `get_accounts` | List accounts for a customer | `customerId`, `type?` |
| `get_transactions` | Query transaction history | `accountId`, `from?`, `to?`, `limit?` |
| `get_product_catalog` | List available banking products | `category?` |
| `get_branch_info` | Branch details and hours | `branchId` or `zipCode` |
| `get_exchange_rates` | Current FX rates | `baseCurrency`, `targetCurrency?` |

## How It Works

The connector translates MCP tool calls into queries against the bank's core systems. AI agents interact with banking data using natural tool-calling semantics without needing to understand the underlying API structure.

```
Agent: "What accounts does customer C-12345 have?"
→ MCP tool call: get_accounts({ customerId: "C-12345" })
→ Returns: [ { type: "CURRENT", balance: 4521.30, ... }, { type: "SAVINGS", balance: 12000.00, ... } ]
```

## Data Scope

The connector enforces the same access controls as the underlying APIs. The calling agent's identity determines which customers and accounts are visible. Cross-customer queries are not permitted.
MDOC

      create_page "$parent_nav_id" "Configuration" 1 <<'MDOC'
# Configuration and Deployment

## Connection Setup

The MCP Data Connector is registered as an MCP server in your agent's configuration:

```json
{
  "mcpServers": {
    "acme-banking": {
      "url": "https://api.acmebank.example.com/mcp-data",
      "apiKey": "<your-acme-api-key>"
    }
  }
}
```

## Tool Permissions

Not all tools are available to all API consumers. Permissions are configured at the application level:

| Permission Set | Tools Included |
|---|---|
| `banking:basic` | `get_customer`, `get_accounts`, `get_product_catalog`, `get_branch_info` |
| `banking:transactions` | All basic + `get_transactions` |
| `banking:fx` | All basic + `get_exchange_rates` |
| `banking:full` | All tools |

## Rate Limits

| Plan | Tool Calls/min | Concurrent |
|---|---|---|
| Standard | 120 | 5 |
| Premium | 600 | 20 |

## Error Handling

Tool call errors follow MCP conventions with structured error objects:

```json
{
  "error": {
    "code": "CUSTOMER_NOT_FOUND",
    "message": "No customer found with ID C-99999"
  }
}
```

Common error codes: `CUSTOMER_NOT_FOUND`, `ACCOUNT_NOT_FOUND`, `PERMISSION_DENIED`, `RATE_LIMITED`, `UPSTREAM_TIMEOUT`.
MDOC
      ;;
  esac
}

# A2A proxy agents — banking focus
seed_api "Mortgage Calculator Agent" "Calculates mortgage payments and repayment scenarios." A2A_PROXY a2a-proxy mortgage-calculator "mortgage,calculator" "Banking,Loans" AGENT KEY_LESS AUTO false
seed_api "Loan Eligibility Agent" "Screens applicants against lending policy." A2A_PROXY a2a-proxy loan-eligibility "loans,eligibility" "Loans" AGENT API_KEY AUTO true
seed_api "Credit Score Agent" "Retrieves and explains consumer credit scores." A2A_PROXY a2a-proxy credit-score "credit,scoring" "Banking" AGENT API_KEY MANUAL false
seed_api "Investment Advisor Agent" "Suggests portfolio allocations from a risk profile." A2A_PROXY a2a-proxy investment-advisor "investments,portfolio" "Investments" AGENT KEY_LESS AUTO true
seed_api "Insurance Quote Agent" "Produces indicative quotes for home and auto cover." A2A_PROXY a2a-proxy insurance-quote "insurance,quotes" "Insurance" AGENT API_KEY AUTO false
seed_api "Fraud Detection Agent" "Flags suspicious payment and account activity." A2A_PROXY a2a-proxy fraud-detection "fraud,security" "Banking,Insurance" AGENT API_KEY MANUAL true
seed_api "Account Summary Agent" "Summarizes balances, recent activity, and next payments." A2A_PROXY a2a-proxy account-summary "accounts,balance" "Banking,Customer Service" AGENT KEY_LESS AUTO false
seed_api "Payment Processing Agent" "Initiates transfers and payment instructions." A2A_PROXY a2a-proxy payment-processing "payments,transfers" "Banking" AGENT API_KEY AUTO true

# Other protocol types for Protocol + APIs tab coverage
seed_api "Account REST API" "REST account enquiry and servicing." PROXY http-proxy account-rest "accounts,rest" "Banking" API KEY_LESS AUTO false
seed_api "Market Data Stream API" "Async market ticks for listed instruments." MESSAGE mock market-data "market-data,streaming" "Investments" API API_KEY AUTO false
seed_api "LLM Document Analyzer API" "Extracts loan-file data from uploaded documents." PROXY http-proxy llm-documents "documents,ai" "Loans" API API_KEY MANUAL true
seed_api "MCP Data Connector API" "MCP tools for core-banking data lookup." PROXY http-proxy mcp-data "data,integration" "Banking" API KEY_LESS AUTO false

echo
echo "Catalog seed complete."
echo "Open Portal Next Catalog and confirm filters: Access, Category, Protocol, Tags, Published by."
echo "Agents tab should list the A2A (and LLM/MCP) items; APIs tab should list the REST and async items."
