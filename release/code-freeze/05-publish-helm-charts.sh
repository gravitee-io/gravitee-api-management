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
