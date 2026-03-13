#!/usr/bin/env bash
source "$(cd "$(dirname "$0")" && pwd)/_common.sh"

# =============================================================================
# Step 8: Create CircleCI scheduled triggers for the new branch
# =============================================================================

CIRCLECI_PROJECT_SLUG="gh/gravitee-io/gravitee-api-management"
TRIGGER_NAMES=(
    "Helm tests"
    "Bridge Compatibility tests"
    "Repository tests"
)

echo "Creating CircleCI scheduled triggers for branch '${BRANCH_NAME}'..."

if [ -z "${CIRCLECI_TOKEN:-}" ]; then
    echo "ERROR: CIRCLECI_TOKEN environment variable is not set."
    exit 1
fi

# Fetch all existing schedules
ALL_SCHEDULES=$(curl -s \
    -H "Circle-Token: $CIRCLECI_TOKEN" \
    "https://circleci.com/api/v2/project/${CIRCLECI_PROJECT_SLUG}/schedule")

for TRIGGER_NAME in "${TRIGGER_NAMES[@]}"; do
    OLDEST_TRIGGER=$(echo "$ALL_SCHEDULES" | jq -r --arg name "$TRIGGER_NAME" '
        .items
        | map(select(.name | startswith($name) and (contains("master") | not)))
        | sort_by(
            .name
            | capture("(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.x")
            | (.major | tonumber) * 1000 + (.minor | tonumber)
        )
        | first
    ')

    if [ "$OLDEST_TRIGGER" = "null" ] || [ -z "$OLDEST_TRIGGER" ]; then
        echo "WARNING: No existing trigger found for '${TRIGGER_NAME}'. Skipping."
        continue
    fi

    NEW_TRIGGER=$(echo "$OLDEST_TRIGGER" | jq \
        --arg name "${TRIGGER_NAME} - ${BRANCH_NAME}" \
        --arg branch "$BRANCH_NAME" \
        '{
            name: $name,
            description: (.description // ""),
            "attribution-actor": "system",
            parameters: .parameters,
            timetable: .timetable
        } + {
            parameters: (.parameters + { branch: $branch })
        }')

    RESPONSE=$(curl -s -X POST \
        -H "Circle-Token: $CIRCLECI_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$NEW_TRIGGER" \
        "https://circleci.com/api/v2/project/${CIRCLECI_PROJECT_SLUG}/schedule")

    if echo "$RESPONSE" | jq -e '.id' > /dev/null 2>&1; then
        echo "Created trigger: '${TRIGGER_NAME} - ${BRANCH_NAME}'"
    else
        echo "ERROR creating trigger '${TRIGGER_NAME} - ${BRANCH_NAME}': $(echo "$RESPONSE" | jq -r '.message // .')"
        exit 1
    fi
done

echo "CircleCI scheduled triggers created."
