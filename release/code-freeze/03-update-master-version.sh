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
# Step 3: Prepare next version on master and commit
# =============================================================================

echo "Switching back to master to prepare next version (${NEXT_REVISION})..."

git -C "$REPO_ROOT" checkout master

# pom.xml: bump <revision> to next minor
sed -i.bak "s|<revision>${REVISION}</revision>|<revision>${NEXT_REVISION}</revision>|" "$POM_FILE"
rm -f "$POM_FILE.bak"

# portal-openapi.yaml: bump version
sed -i.bak "s|version: \"${REVISION}-SNAPSHOT\"|version: \"${NEXT_REVISION}-SNAPSHOT\"|" "$PORTAL_OPENAPI"
rm -f "$PORTAL_OPENAPI.bak"

# helm/Chart.yaml: bump version and appVersion, clear artifacthub changes
sed -i.bak "s|version: ${REVISION}|version: ${NEXT_REVISION}|" "$HELM_CHART"
rm -f "$HELM_CHART.bak"
sed -i.bak "s|appVersion: ${REVISION}|appVersion: ${NEXT_REVISION}|" "$HELM_CHART"
rm -f "$HELM_CHART.bak"
awk '
  !in_block && /^  artifacthub.io\/changes:/ {
    print "  artifacthub.io/changes:"
    in_block = 1
    next
  }
  in_block && /^[[:space:]]{0,2}[^[:space:]]/ {
    in_block = 0
  }
  !in_block {
    print
  }
' "$HELM_CHART" > "${HELM_CHART}.tmp" && mv "${HELM_CHART}.tmp" "$HELM_CHART"

# .mergify.yml: replace the oldest branch with the newly created branch
OLDEST_BRANCH=$(grep -oE '[0-9]+\.[0-9]+\.x' "$MERGIFY_FILE" | sort -t. -k1,1n -k2,2n | head -1)
OLDEST_LABEL=$(echo "$OLDEST_BRANCH" | sed 's/\./-/g')
NEW_LABEL="${MAJOR}-${MINOR}-x"

echo "Replacing oldest mergify branch '${OLDEST_BRANCH}' with '${BRANCH_NAME}'..."

sed -i.bak "s|${OLDEST_BRANCH}|${BRANCH_NAME}|g" "$MERGIFY_FILE"
rm -f "$MERGIFY_FILE.bak"
sed -i.bak "s|${OLDEST_LABEL}|${NEW_LABEL}|g" "$MERGIFY_FILE"
rm -f "$MERGIFY_FILE.bak"

# Commit
git -C "$REPO_ROOT" add pom.xml \
    gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml \
    helm/Chart.yaml \
    .mergify.yml

git -C "$REPO_ROOT" commit -m "chore: prepare next version (${MAJOR}.${NEXT_MINOR})"

git -C "$REPO_ROOT" push origin master

echo "Changes committed and pushed on master."
