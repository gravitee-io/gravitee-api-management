#!/usr/bin/env bash
set -euo pipefail

# =============================================================================
# Code Freeze Script for Gravitee API Management
# =============================================================================
#
# This script automates the code freeze process. It reads the current version
# from pom.xml and performs the following actions:
#
#   00 - Verify that all required git repositories are cloned locally
#   01 - Create a release branch (e.g. 4.11.x) from master
#   02 - Set the alpha version on the release branch and push
#   03 - Bump master to the next minor version and push
#   04 - Create a GitHub label for Mergify backports (e.g. apply-on-4-11-x)
#   05 - Package and publish Helm charts to the Azure OCI registry
#   06 - Create a dev environment in cloud-apim and open a PR
#   07 - (Manual) Update Google OAuth 2.0 client with new redirect URIs
#   08 - Create CircleCI scheduled triggers for the new branch
#
# Prerequisites:
#   - git, gh, az, helm, jq, curl must be installed
#   - CIRCLECI_TOKEN must be set in release/.env
#   - Docker must be running (for Helm chart push via Azure ACR)
#
# Usage:
#   ./release/code-freeze.sh           # Run all steps from the beginning
#   ./release/code-freeze.sh 5         # Resume from step 05
#
# Each step can also be run independently:
#   ./release/code-freeze/05-publish-helm-charts.sh
#
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)/code-freeze"
START_FROM="${1:-0}"

STEPS=(
    "00-check-repos.sh"
    "01-create-branch.sh"
    "02-update-branch-version.sh"
    "03-update-master-version.sh"
    "04-create-github-label.sh"
    "05-publish-helm-charts.sh"
    "06-create-cloud-apim-env.sh"
    "07-update-google-oauth.sh"
    "08-create-circleci-triggers.sh"
)

for STEP in "${STEPS[@]}"; do
    STEP_NUMBER="${STEP%%-*}"  # e.g. "05" from "05-publish-helm-charts.sh"
    if [ "$((10#$STEP_NUMBER))" -lt "$((10#$START_FROM))" ]; then
        echo ""
        echo "  Skipping: $STEP"
        continue
    fi
    echo ""
    echo "======================================================================"
    echo " Running: $STEP"
    echo "======================================================================"
    echo ""
    bash "$SCRIPT_DIR/$STEP"
done

# =============================================================================
# Summary
# =============================================================================

source "$SCRIPT_DIR/_common.sh"

echo ""
echo "======================================================================"
echo " Code Freeze Summary"
echo "======================================================================"
echo ""
echo " Version detected:        ${FULL_VERSION}"
echo " Release branch created:  ${BRANCH_NAME}"
echo " Alpha version:           ${ALPHA_VERSION_SNAPSHOT}"
echo " Next version on master:  ${NEXT_REVISION}-SNAPSHOT"
echo ""
echo " What was done:"
echo "  [gravitee-api-management]"
echo "   - Created branch '${BRANCH_NAME}' from master"
echo "   - Set version to ${ALPHA_VERSION_SNAPSHOT} on '${BRANCH_NAME}'"
echo "   - Bumped master to ${NEXT_REVISION}-SNAPSHOT"
echo "   - Updated .mergify.yml"
echo ""
echo "  [GitHub]"
echo "   - Created label '${BRANCH_LABEL}'"
echo ""
echo "  [Helm]"
echo "   - Published chart apim-${ALPHA_VERSION}.tgz to oci://graviteeio.azurecr.io/helm/"
echo ""
echo "  [cloud-apim]"
echo "   - Created environment '${ENV_DIR_NAME}' (copied from '${PREV_ENV_DIR_NAME}')"
echo "   - Updated applicationset files"
echo ""
echo "  [Google OAuth 2.0]"
echo "   - Added JS origins and redirect URIs for ${ENV_DIR_NAME}"
echo ""
echo "  [CircleCI]"
echo "   - Created scheduled triggers for '${BRANCH_NAME}'"
echo ""
echo "======================================================================"
echo " Manual steps required"
echo "======================================================================"
echo ""
echo "  1. Update plugins that depend on the APIM BOM"
echo "  2. Release each library and plugin from alpha to official version"
echo "  3. Update dependencies in the APIM pom.xml"
echo ""
echo "======================================================================"
