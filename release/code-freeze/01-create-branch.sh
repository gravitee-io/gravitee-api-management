#!/usr/bin/env bash
source "$(cd "$(dirname "$0")" && pwd)/_common.sh"

# =============================================================================
# Step 1: Create release branch from master
# =============================================================================

echo "Creating branch '$BRANCH_NAME' from master..."

git -C "$REPO_ROOT" checkout master
git -C "$REPO_ROOT" pull origin master
git -C "$REPO_ROOT" checkout -b "$BRANCH_NAME"

echo "Branch '$BRANCH_NAME' created successfully."
