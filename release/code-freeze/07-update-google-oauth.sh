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
# Step 7: Update Google OAuth 2.0 client with new environment URLs
# =============================================================================

# There is no public API to update standard OAuth 2.0 client redirect URIs.
# This step prints the URLs to add manually in the Google Cloud Console.

GOOGLE_CLIENT_ID="603595705788-8vdbn1keso28dh2r2n70mrt0953nu9m2.apps.googleusercontent.com"
PORTAL_URL="https://apim-${MAJOR}-${MINOR}-x-portal.team-apim.gravitee.dev"
CONSOLE_URL="https://apim-${MAJOR}-${MINOR}-x-console.team-apim.gravitee.dev"

echo "======================================================================"
echo " MANUAL ACTION REQUIRED: Update Google OAuth 2.0 client"
echo "======================================================================"
echo ""
echo " Open the following URL in your browser:"
echo "   https://console.cloud.google.com/apis/credentials/oauthclient/${GOOGLE_CLIENT_ID}?project=cluster-apim-hors-prod"
echo ""
echo " Add the following Authorized JavaScript origins:"
echo "   - ${PORTAL_URL}"
echo "   - ${CONSOLE_URL}"
echo ""
echo " Add the following Authorized redirect URIs:"
echo "   - ${CONSOLE_URL}"
echo "   - ${PORTAL_URL}/user/login"
echo "   - ${PORTAL_URL}/classic/user/login"
echo "   - ${PORTAL_URL}/next/log-in"
echo ""
echo "======================================================================"
echo ""

read -p "Press Enter once you have completed the manual step..."
echo "Google OAuth 2.0 step acknowledged."
