#!/bin/bash
# Copyright (C) 2015 The Gravitee team (http://gravitee.io)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Script to build gravitee-markdown library for console-webui
# This script navigates to portal-webui-next, installs dependencies, and builds the library

set -e  # Exit on any error

echo "🚀   Building gravitee-markdown library for console-webui..."

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Navigate to portal-webui-next directory
PORTAL_DIR="$SCRIPT_DIR/../../gravitee-apim-portal-webui-next"

if [ ! -d "$PORTAL_DIR" ]; then
    echo "❌  Error: portal-webui-next directory not found at $PORTAL_DIR"
    exit 1
fi

echo "📁  Navigating to portal-webui-next directory..."
cd "$PORTAL_DIR"

echo "📦  Installing dependencies..."
yarn install

echo "🔨  Building gravitee-markdown library with console configuration..."
yarn build:gravitee-markdown:console

echo "✅  gravitee-markdown library built successfully!"
