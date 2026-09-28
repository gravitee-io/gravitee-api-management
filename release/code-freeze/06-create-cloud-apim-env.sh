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
# Step 6: Create dev environment in cloud-apim repository
# =============================================================================

echo "Setting up new dev environment '${ENV_DIR_NAME}' in cloud-apim..."

# Each environment reads its own Keeper record (its Mongo URL among others); keeping the previous
# one would point the new environment at the previous one's database.
PREV_KEEPER_KEY=$(awk -v name="name: apim-${PREV_ENV_DIR_NAME}-external-secret" '
    index($0, name) { found = 1 }
    found && /key:/ { gsub(/.*key: *"?|".*/, ""); print; exit }
' "$CLOUD_APIM_REPO/$PREV_ENV_DIR_NAME/values.yaml")
if [ -z "$PREV_KEEPER_KEY" ]; then
    echo "ERROR: no Keeper key found for apim-${PREV_ENV_DIR_NAME}-external-secret in ${PREV_ENV_DIR_NAME}/values.yaml." >&2
    exit 1
fi

read -r -p "Keeper record UID for ${ENV_DIR_NAME} (${PREV_ENV_DIR_NAME} uses ${PREV_KEEPER_KEY}): " KEEPER_KEY < /dev/tty
if ! [[ "$KEEPER_KEY" =~ ^[A-Za-z0-9_-]+$ ]]; then
    echo "ERROR: '${KEEPER_KEY}' is not a Keeper record UID." >&2
    exit 1
fi
if [ "$KEEPER_KEY" = "$PREV_KEEPER_KEY" ]; then
    echo "ERROR: this is ${PREV_ENV_DIR_NAME}'s record, not a new one." >&2
    exit 1
fi

# Copy previous environment as base
rm -rf "$CLOUD_APIM_REPO/$ENV_DIR_NAME"
cp -r "$CLOUD_APIM_REPO/$PREV_ENV_DIR_NAME" "$CLOUD_APIM_REPO/$ENV_DIR_NAME"

# Update Chart.yaml: name, description, and apim dependency versions
sed -i.bak "s|name: ${PREV_ENV_DIR_NAME}|name: ${ENV_DIR_NAME}|" "$CLOUD_APIM_REPO/$ENV_DIR_NAME/Chart.yaml"
rm -f "$CLOUD_APIM_REPO/$ENV_DIR_NAME/Chart.yaml.bak"
sed -i.bak "s|description: ${PREV_ENV_DIR_NAME}|description: ${ENV_DIR_NAME}|" "$CLOUD_APIM_REPO/$ENV_DIR_NAME/Chart.yaml"
rm -f "$CLOUD_APIM_REPO/$ENV_DIR_NAME/Chart.yaml.bak"
sed -i.bak "s|version: ${MAJOR}\.${PREV_MINOR}\.\*|version: ${ALPHA_VERSION}|g" "$CLOUD_APIM_REPO/$ENV_DIR_NAME/Chart.yaml"
rm -f "$CLOUD_APIM_REPO/$ENV_DIR_NAME/Chart.yaml.bak"

# values.yaml names the version in every separator the tooling around it needs: 4-12-x for hosts and
# secrets, 4_12_x for indices, 4.12.x for image tags. The lookarounds keep 14.12 or 4.120 untouched.
VALUES_FILE="$CLOUD_APIM_REPO/$ENV_DIR_NAME/values.yaml"
perl -pi -e "s/(?<![\d.])${MAJOR}([._-])${PREV_MINOR}(?!\d)/${MAJOR}\${1}${MINOR}/g" "$VALUES_FILE"
if grep -nE "(^|[^0-9.])${MAJOR}[._-]${PREV_MINOR}([^0-9]|$)" "$VALUES_FILE"; then
    echo "ERROR: ${VALUES_FILE} still names ${MAJOR}.${PREV_MINOR} on the lines above." >&2
    exit 1
fi

sed -i.bak "s|key: \"${PREV_KEEPER_KEY}\"|key: \"${KEEPER_KEY}\"|" "$VALUES_FILE"
rm -f "$VALUES_FILE.bak"
if ! grep -q "key: \"${KEEPER_KEY}\"" "$VALUES_FILE" || grep -q "${PREV_KEEPER_KEY}" "$VALUES_FILE"; then
    echo "ERROR: the Keeper key of apim-${ENV_DIR_NAME}-external-secret was not replaced in ${VALUES_FILE}." >&2
    exit 1
fi
echo "Keeper record: ${KEEPER_KEY}"

# values.yaml exports traces and logs to apim-otel-collector-<env>, which exists only once the
# environment has its own collector values and is listed in the otel-collector applicationset.
OTEL_VALUES_FILE="$CLOUD_APIM_REPO/otel-collector/values-${ENV_DIR_NAME}.yaml"
perl -pe "s/(?<![\d.])${MAJOR}([._-])${PREV_MINOR}(?!\d)/${MAJOR}\${1}${MINOR}/g" \
    "$CLOUD_APIM_REPO/otel-collector/values-${PREV_ENV_DIR_NAME}.yaml" > "$OTEL_VALUES_FILE"
if grep -nE "(^|[^0-9.])${MAJOR}[._-]${PREV_MINOR}([^0-9]|$)" "$OTEL_VALUES_FILE"; then
    echo "ERROR: ${OTEL_VALUES_FILE} still names ${MAJOR}.${PREV_MINOR} on the lines above." >&2
    exit 1
fi

# Add the new environment path to the applicationset files
for APPSET in apim.applicationset.yaml apim.logstash.applicationset.yaml apim.otel-collector.applicationset.yaml; do
    APPSET_FILE="$CLOUD_APIM_REPO/application/$APPSET"
    # The new entry copies the previous one's line, so it keeps that file's indentation and quotes.
    if ! grep -qE "path: [\"']${ENV_DIR_NAME}[\"']" "$APPSET_FILE"; then
        perl -pi -e "\$_ .= \$1 . \$2 . '${ENV_DIR_NAME}' . \$2 . \"\\n\" if /^(\s*- path: )([\"'])${PREV_ENV_DIR_NAME}\2\s*\$/" "$APPSET_FILE"
    fi
    if ! grep -qE "path: [\"']${ENV_DIR_NAME}[\"']" "$APPSET_FILE"; then
        echo "ERROR: ${ENV_DIR_NAME} was not added to ${APPSET_FILE}: no '- path:' line for ${PREV_ENV_DIR_NAME}." >&2
        exit 1
    fi
done

# Commit and create pull request
CLOUD_APIM_BRANCH="feat/deploy-${ENV_DIR_NAME}-environment"

git -C "$CLOUD_APIM_REPO" checkout main
git -C "$CLOUD_APIM_REPO" pull origin main
git -C "$CLOUD_APIM_REPO" checkout -b "$CLOUD_APIM_BRANCH"

git -C "$CLOUD_APIM_REPO" add \
    "$ENV_DIR_NAME/" \
    "otel-collector/values-${ENV_DIR_NAME}.yaml" \
    application/apim.applicationset.yaml \
    application/apim.logstash.applicationset.yaml \
    application/apim.otel-collector.applicationset.yaml

git -C "$CLOUD_APIM_REPO" commit -m "feat: deploy ${ENV_DIR_NAME} environment"
git -C "$CLOUD_APIM_REPO" --no-pager show --stat HEAD
run_confirmed git -C "$CLOUD_APIM_REPO" push -u origin "$CLOUD_APIM_BRANCH"

run_confirmed gh pr create \
    --repo gravitee-io/cloud-apim \
    --base main \
    --head "$CLOUD_APIM_BRANCH" \
    --title "feat: deploy ${ENV_DIR_NAME} environment" \
    --body "Add dev environment for ${BRANCH_NAME} branch (code freeze)."

echo "Dev environment '${ENV_DIR_NAME}' created in cloud-apim with pull request."
