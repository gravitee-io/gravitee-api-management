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
for POM in "$POM_FILE" "$DISTRIBUTION_POM_FILE"; do
    sed -i.bak "s|<revision>${REVISION}</revision>|<revision>${NEXT_REVISION}</revision>|" "$POM"
    rm -f "$POM.bak"
done

# The pin follows the revision, or master keeps assembling a version it no longer publishes.
set_core_pin "${NEXT_REVISION}-SNAPSHOT"

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

# .mergify.yml: the new line gets its backport rule; no line loses one here.
add_mergify_rule "${BRANCH_NAME}" "${BRANCH_LABEL}"

# Commit
git -C "$REPO_ROOT" add pom.xml \
    gravitee-apim-distribution/pom.xml \
    gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml \
    helm/Chart.yaml \
    .mergify.yml

git -C "$REPO_ROOT" commit -m "chore: prepare next version (${MAJOR}.${NEXT_MINOR})"

git -C "$REPO_ROOT" push origin master

echo "Changes committed and pushed on master."
