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
# Step 2: Update version to alpha.1 on the release branch and commit
# =============================================================================

echo "Updating versions to $ALPHA_VERSION_SNAPSHOT on branch '$BRANCH_NAME'..."

# pom.xml: set <sha1>-alpha.1</sha1> (version becomes ${revision}${sha1}${changelist} = 4.11.0-alpha.1-SNAPSHOT)
sed -i.bak "s|<sha1/>|<sha1>${ALPHA_SUFFIX}</sha1>|" "$POM_FILE"
rm -f "$POM_FILE.bak"

# portal-openapi.yaml: update version string
sed -i.bak "s|version: \"${REVISION}-SNAPSHOT\"|version: \"${ALPHA_VERSION_SNAPSHOT}\"|" "$PORTAL_OPENAPI"
rm -f "$PORTAL_OPENAPI.bak"

# helm/Chart.yaml: update version without -SNAPSHOT
sed -i.bak "s|version: ${REVISION}|version: ${ALPHA_VERSION}|" "$HELM_CHART"
rm -f "$HELM_CHART.bak"

# Commit
git -C "$REPO_ROOT" add pom.xml \
    gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml \
    helm/Chart.yaml

git -C "$REPO_ROOT" commit -m "chore: prepare first alpha version"

git -C "$REPO_ROOT" push -u origin "$BRANCH_NAME"

echo "Changes committed and pushed on branch '$BRANCH_NAME'."
