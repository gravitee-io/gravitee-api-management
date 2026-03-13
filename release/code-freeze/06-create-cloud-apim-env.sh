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

# Add the new environment path to the 3 applicationset files
for APPSET in apim.applicationset.yaml apim.common.applicationset.yaml apim.logstash.applicationset.yaml; do
    APPSET_FILE="$CLOUD_APIM_REPO/application/$APPSET"
    sed -i.bak "/${PREV_ENV_DIR_NAME}/a\\
\\          - path: \"${ENV_DIR_NAME}\"
" "$APPSET_FILE"
    rm -f "$APPSET_FILE.bak"
done

# Commit and create pull request
CLOUD_APIM_BRANCH="feat/deploy-${ENV_DIR_NAME}-environment"

git -C "$CLOUD_APIM_REPO" checkout main
git -C "$CLOUD_APIM_REPO" pull origin main
git -C "$CLOUD_APIM_REPO" checkout -b "$CLOUD_APIM_BRANCH"

git -C "$CLOUD_APIM_REPO" add \
    "$ENV_DIR_NAME/" \
    application/apim.applicationset.yaml \
    application/apim.common.applicationset.yaml \
    application/apim.logstash.applicationset.yaml

git -C "$CLOUD_APIM_REPO" commit -m "feat: deploy ${ENV_DIR_NAME} environment"
git -C "$CLOUD_APIM_REPO" push -u origin "$CLOUD_APIM_BRANCH"

gh pr create \
    --repo gravitee-io/cloud-apim \
    --base main \
    --head "$CLOUD_APIM_BRANCH" \
    --title "feat: deploy ${ENV_DIR_NAME} environment" \
    --body "Add dev environment for ${BRANCH_NAME} branch (code freeze)."

echo "Dev environment '${ENV_DIR_NAME}' created in cloud-apim with pull request."
