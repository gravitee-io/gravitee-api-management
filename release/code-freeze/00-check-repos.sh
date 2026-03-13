#!/usr/bin/env bash
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
