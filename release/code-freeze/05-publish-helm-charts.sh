#!/usr/bin/env bash
source "$(cd "$(dirname "$0")" && pwd)/_common.sh"

# =============================================================================
# Step 5: Publish Helm charts to private registry
# =============================================================================

# Azure ACR auth is required to push Helm charts to the private registry
if ! az acr login --name graviteeio 2>/dev/null; then
    echo "Not authenticated with Azure. Launching login..."
    az login
    az acr login --name graviteeio
fi

echo "Publishing Helm charts..."

git -C "$REPO_ROOT" checkout "$BRANCH_NAME"

cd "$REPO_ROOT/helm"

helm dep up
helm package -d charts .
helm push "./charts/apim-${ALPHA_VERSION}.tgz" oci://graviteeio.azurecr.io/helm/

cd "$REPO_ROOT"

echo "Helm charts published successfully."
