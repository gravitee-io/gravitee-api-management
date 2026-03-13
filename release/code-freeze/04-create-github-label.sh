#!/usr/bin/env bash
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
