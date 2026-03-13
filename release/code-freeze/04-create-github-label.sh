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

COMMON_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$COMMON_DIR/../.." && pwd)"

# Checkout the release branch so _common.sh reads the correct version from pom.xml
BRANCH_NAME=$(git -C "$REPO_ROOT" branch --list '*.x' | tr -d ' ' | sort -t. -k1,1n -k2,2n | tail -1)
git -C "$REPO_ROOT" checkout "$BRANCH_NAME"

source "$COMMON_DIR/_common.sh"

# =============================================================================
# Step 4: Create GitHub label for the new branch
# =============================================================================

# Authentication required to create labels via GitHub CLI
if ! gh auth status &> /dev/null; then
    echo "Not authenticated with GitHub CLI. Launching login..."
    gh auth login
fi

echo "Creating GitHub label '${BRANCH_LABEL}'..."

RANDOM_COLOR=$(printf '%06X' $((RANDOM * RANDOM % 16777216)))
gh label create "$BRANCH_LABEL" --repo "$GITHUB_REPO" --description "Mergify: apply on ${BRANCH_NAME}" --color "$RANDOM_COLOR"

echo "GitHub label '${BRANCH_LABEL}' created."
