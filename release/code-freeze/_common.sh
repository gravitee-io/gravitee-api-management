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

# =============================================================================
# Common variables and helpers for code freeze scripts
# =============================================================================

set -euo pipefail

# Add gcloud to PATH if installed via brew
if [ -d "/opt/homebrew/share/google-cloud-sdk/bin" ]; then
    export PATH="/opt/homebrew/share/google-cloud-sdk/bin:$PATH"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
POM_FILE="$REPO_ROOT/pom.xml"
# The distribution carries its own <revision>/<sha1>/<changelist> since it left the product
# reactor, and the two no longer have to agree. What it assembles is its own apim.core.version.
DISTRIBUTION_POM_FILE="$REPO_ROOT/gravitee-apim-distribution/pom.xml"
PARENT_DIR="$(dirname "$REPO_ROOT")"

# Load environment variables from .env
if [ -f "$SCRIPT_DIR/../.env" ]; then
    source "$SCRIPT_DIR/../.env"
fi

# Check required tools
REQUIRED_TOOLS=(git gh az helm jq curl)
MISSING_TOOLS=()
for TOOL in "${REQUIRED_TOOLS[@]}"; do
    if ! command -v "$TOOL" &> /dev/null; then
        MISSING_TOOLS+=("$TOOL")
    fi
done
if [ ${#MISSING_TOOLS[@]} -gt 0 ]; then
    echo "ERROR: The following required tools are not installed: ${MISSING_TOOLS[*]}"
    exit 1
fi

# Extract version components from pom.xml
REVISION=$(grep '<revision>' "$POM_FILE" | sed 's/.*<revision>\(.*\)<\/revision>.*/\1/')
CHANGELIST=$(grep '<changelist>' "$POM_FILE" | sed 's/.*<changelist>\(.*\)<\/changelist>.*/\1/')

FULL_VERSION="${REVISION}${CHANGELIST}"

MAJOR=$(echo "$REVISION" | cut -d. -f1)
MINOR=$(echo "$REVISION" | cut -d. -f2)
BRANCH_NAME="${MAJOR}.${MINOR}.x"

ALPHA_SUFFIX="-alpha.1"
ALPHA_VERSION="${REVISION}${ALPHA_SUFFIX}"
ALPHA_VERSION_SNAPSHOT="${ALPHA_VERSION}-SNAPSHOT"

NEXT_MINOR=$((MINOR + 1))
NEXT_REVISION="${MAJOR}.${NEXT_MINOR}.0"

PREV_MINOR=$((MINOR - 1))
ENV_DIR_NAME="${MAJOR}-${MINOR}-x"
PREV_ENV_DIR_NAME="${MAJOR}-${PREV_MINOR}-x"

PORTAL_OPENAPI="$REPO_ROOT/gravitee-apim-rest-api/gravitee-apim-rest-api-portal/gravitee-apim-rest-api-portal-rest/src/main/resources/portal-openapi.yaml"
HELM_CHART="$REPO_ROOT/helm/Chart.yaml"
MERGIFY_FILE="$REPO_ROOT/.mergify.yml"
CLOUD_APIM_REPO="$PARENT_DIR/cloud-apim"

GITHUB_REPO="gravitee-io/gravitee-api-management"
BRANCH_LABEL="apply-on-${MAJOR}-${MINOR}-x"

# Each branch pins the core it publishes itself, so the pin follows the version being written: a
# branch that keeps master's pin cannot release — a SNAPSHOT core is refused — and master that keeps
# the branch's pin assembles a version it no longer publishes.
#
# Verified after writing, because a sed that matches nothing exits 0: the pin would silently stay
# where it was, and the freeze would report success.
set_core_pin() {
    local version="$1"

    sed -i.bak "s|<apim.core.version>.*</apim.core.version>|<apim.core.version>${version}</apim.core.version>|" "$DISTRIBUTION_POM_FILE"
    rm -f "$DISTRIBUTION_POM_FILE.bak"

    if ! grep -q "<apim.core.version>${version}</apim.core.version>" "$DISTRIBUTION_POM_FILE"; then
        echo "ERROR: apim.core.version was not set to ${version} in $DISTRIBUTION_POM_FILE." >&2
        echo "       It currently reads: $(grep -o '<apim.core.version>[^<]*</apim.core.version>' "$DISTRIBUTION_POM_FILE" || echo 'nothing')" >&2
        exit 1
    fi
    echo "Pinned core ${version}"
}

echo "Code freeze context: version=${FULL_VERSION} branch=${BRANCH_NAME}"
