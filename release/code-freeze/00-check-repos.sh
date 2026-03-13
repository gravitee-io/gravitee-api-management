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
# Step 0: Ensure all required repositories are cloned
# =============================================================================

REPOS=(
    "gravitee-api-management|https://github.com/gravitee-io/gravitee-api-management.git"
    "cloud-apim|https://github.com/gravitee-io/cloud-apim.git"
)

for REPO_ENTRY in "${REPOS[@]}"; do
    REPO_NAME="${REPO_ENTRY%%|*}"
    REPO_URL="${REPO_ENTRY##*|}"
    REPO_PATH="$PARENT_DIR/$REPO_NAME"

    if [ -d "$REPO_PATH/.git" ]; then
        echo "Repository '$REPO_NAME' found at $REPO_PATH"
    else
        echo "Repository '$REPO_NAME' not found. Cloning from $REPO_URL..."
        git clone "$REPO_URL" "$REPO_PATH"
        echo "Repository '$REPO_NAME' cloned."
    fi
done
