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

source "$(cd "$(dirname "$0")" && pwd)/_common.sh"

# =============================================================================
# Step 8: Create the CircleCI scheduled pipelines for the new branch
# =============================================================================
#
# Declared here rather than copied from another branch: a clone carries whatever the line it was
# taken from happens to hold today, and nothing in the repository said what that was. Written out,
# the hours are readable, reviewable, and the step works on a project with no schedules at all.
#
# The hours below are 4.12.x's, and each line shifts them by an offset of its own: two hours per
# step, cycling over the four minors that are supported at a time, so no two supported lines — and
# never master, which sits one hour after the base — start the same suite together. Cloning used to
# spread the lines by accident; this spreads them on purpose.
#
# Every schedule is named "<Suite> - <branch>", and its description says the same thing in the same
# words. One of master's predates the convention and reads `bridge_compatibility_tests_master`;
# nothing here reproduces that.

CIRCLECI_PROJECT_SLUG="gh/gravitee-io/gravitee-api-management"

# name | gio_action | base hour (UTC) | days
SCHEDULES=(
    "Bridge Compatibility tests|bridge_compatibility_tests|2|MON"
    "Repository tests|repositories_tests|3|MON,TUE,WED,THU,FRI"
    "Helm tests|helm_tests|21|WED,SUN"
    "Nightly|nightly|23|MON,TUE,WED,THU,FRI"
)

# Even offsets only: master runs each suite at base + 1, and skipping the odd ones keeps a line off
# master's hour. Four slots, which is how many lines are supported at a time.
HOUR_OFFSET=$(( (MINOR % 4) * 2 ))

echo "Creating CircleCI scheduled pipelines for branch '${BRANCH_NAME}'..."

if [ -z "${CIRCLECI_TOKEN:-}" ]; then
    echo "ERROR: CIRCLECI_TOKEN environment variable is not set." >&2
    exit 1
fi

# Every page of the listing, validated. `curl -s` alone exits 0 on a 401 as on a 500, and the
# idempotence guard below reads a failed listing as "nothing exists yet" — which is precisely when
# it would create a second copy of all four schedules. The project carries seventeen of them today,
# enough to page.
fetch_schedules() {
    local url="https://circleci.com/api/v2/project/${CIRCLECI_PROJECT_SLUG}/schedule"
    local token="" page="" items="[]"

    while :; do
        if ! page=$(curl -sS --fail -H "Circle-Token: $CIRCLECI_TOKEN" "${url}${token:+?page-token=${token}}"); then
            echo "ERROR: could not list the project's schedules." >&2
            exit 1
        fi
        if ! echo "$page" | jq -e '.items | type == "array"' > /dev/null; then
            echo "ERROR: the schedule listing answered without items: $(echo "$page" | jq -c '.message // .')" >&2
            exit 1
        fi

        items=$(jq -n --argjson acc "$items" --argjson page "$(echo "$page" | jq '.items')" '$acc + $page')
        token=$(echo "$page" | jq -r '.next_page_token // empty')
        [ -z "$token" ] && break
    done

    echo "$items"
}

ALL_SCHEDULES=$(fetch_schedules)

for SCHEDULE in "${SCHEDULES[@]}"; do
    IFS='|' read -r NAME ACTION BASE_HOUR DAYS <<< "$SCHEDULE"
    HOUR=$(( (BASE_HOUR + HOUR_OFFSET) % 24 ))
    TARGET_NAME="${NAME} - ${BRANCH_NAME}"

    # Resuming the freeze from a step number is supported, and CircleCI accepts a second schedule
    # under the same name — the branch would then run everything twice.
    if echo "$ALL_SCHEDULES" | jq -e --arg name "$TARGET_NAME" 'any(.name == $name)' > /dev/null; then
        echo "Schedule '${TARGET_NAME}' already exists. Leaving it alone."
        continue
    fi

    PAYLOAD=$(jq -n \
        --arg name "$TARGET_NAME" \
        --arg label "$NAME" \
        --arg branch "$BRANCH_NAME" \
        --arg action "$ACTION" \
        --argjson hour "$HOUR" \
        --arg days "$DAYS" \
        '{
            name: $name,
            description: "\($label) for \($branch)",
            "attribution-actor": "system",
            parameters: { branch: $branch, gio_action: $action },
            timetable: {
                "per-hour": 1,
                "hours-of-day": [$hour],
                "days-of-week": ($days | split(",")),
                "months": ["JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"]
            }
        }')

    RESPONSE=$(curl -s -X POST \
        -H "Circle-Token: $CIRCLECI_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD" \
        "https://circleci.com/api/v2/project/${CIRCLECI_PROJECT_SLUG}/schedule")

    # Not a warning: a branch short of a schedule runs fewer tests than every other one, and nothing
    # turns red to say so.
    if echo "$RESPONSE" | jq -e '.id' > /dev/null 2>&1; then
        echo "Created schedule: '${TARGET_NAME}' — ${ACTION}, ${HOUR}:00 UTC on ${DAYS}"
    else
        echo "ERROR creating schedule '${TARGET_NAME}': $(echo "$RESPONSE" | jq -r '.message // .')" >&2
        exit 1
    fi
done

echo "CircleCI scheduled pipelines created."
