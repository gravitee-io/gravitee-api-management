#!/usr/bin/env bash
#
# Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
#
# Builds Gamma Console + portal-gamma + APIM module for the offline POC and
# assembles them into dist-poc/ for static hosting (e.g. Vercel CLI deploy).
#
# Usage:
#   yarn poc:build
#   SKIP_BUILD=1 yarn poc:build   # re-assemble only (requires prior build outputs)
#
# Deploy:
#   cd dist-poc && vercel --prod
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

DIST_POC="${ROOT}/dist-poc"
CONSOLE_DIST="${ROOT}/gravitee-gamma/gravitee-gamma-control-plane-webui/dist"
PORTAL_DIST="${ROOT}/gravitee-apim-portal-gamma/dist"
APIM_DIST="${ROOT}/gravitee-gamma/gravitee-gamma-module-apim/target/classes/ui"

export POC_MODE=true
export DEV_MODULE_ENTRIES="${DEV_MODULE_ENTRIES:-portals=/portal-editor/mf-manifest.json,apim=/mf-apim/mf-manifest.json}"
export NODE_OPTIONS="${NODE_OPTIONS:---max_old_space_size=8192}"
export NX_TUI=false

echo "==> POC build env"
echo "    POC_MODE=${POC_MODE}"
echo "    DEV_MODULE_ENTRIES=${DEV_MODULE_ENTRIES}"
echo "    NODE_OPTIONS=${NODE_OPTIONS}"

if [[ "${SKIP_BUILD:-}" != "1" ]]; then
    echo "==> Building gamma-console (production, POC_MODE)"
    yarn nx build gamma-console --configuration=production

    echo "==> Building portal-gamma (production, /portal-editor/)"
    POC_ASSET_PREFIX=/portal-editor yarn nx build portal-gamma --configuration=production

    echo "==> Building gravitee-gamma-module-apim (production, /mf-apim/)"
    POC_ASSET_PREFIX=/mf-apim yarn nx build gravitee-gamma-module-apim --configuration=production
else
    echo "==> SKIP_BUILD=1 — reusing existing build outputs"
fi

for label_path in \
    "gamma-console:${CONSOLE_DIST}" \
    "portal-gamma:${PORTAL_DIST}" \
    "APIM module:${APIM_DIST}"; do
    label="${label_path%%:*}"
    path="${label_path#*:}"
    if [[ ! -d "${path}" ]] || [[ -z "$(ls -A "${path}" 2>/dev/null || true)" ]]; then
        echo "ERROR: missing or empty build output for ${label}: ${path}" >&2
        echo "Run without SKIP_BUILD=1 to build first." >&2
        exit 1
    fi
done

if [[ ! -f "${CONSOLE_DIST}/index.html" ]]; then
    echo "ERROR: ${CONSOLE_DIST}/index.html not found" >&2
    exit 1
fi
if [[ ! -f "${PORTAL_DIST}/mf-manifest.json" ]]; then
    echo "ERROR: ${PORTAL_DIST}/mf-manifest.json not found" >&2
    exit 1
fi
if [[ ! -f "${APIM_DIST}/mf-manifest.json" ]]; then
    echo "ERROR: ${APIM_DIST}/mf-manifest.json not found" >&2
    exit 1
fi

echo "==> Assembling ${DIST_POC}"
rm -rf "${DIST_POC}"
mkdir -p "${DIST_POC}/portal-editor" "${DIST_POC}/mf-apim" "${DIST_POC}/mock"

cp -R "${CONSOLE_DIST}/." "${DIST_POC}/"
cp -R "${PORTAL_DIST}/." "${DIST_POC}/portal-editor/"
cp -R "${APIM_DIST}/." "${DIST_POC}/mf-apim/"

cp "${ROOT}/poc-mock/fixtures/"*.json "${DIST_POC}/mock/"
printf '%s\n' '{}' >"${DIST_POC}/mock/empty.json"

cp "${ROOT}/poc-mock/vercel.json" "${DIST_POC}/vercel.json"

# Point editorBaseURL at the same-origin portal-editor path for static hosting.
if [[ -f "${DIST_POC}/portal-editor/constants.json" ]]; then
    DIST_POC="${DIST_POC}" node --input-type=module <<'EOF'
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const path = join(process.env.DIST_POC, 'portal-editor/constants.json');
const constants = JSON.parse(readFileSync(path, 'utf8'));
constants.editorBaseURL = '/portal-editor';
constants.appBasePath = constants.appBasePath ?? '/portal-editor';
writeFileSync(path, `${JSON.stringify(constants, null, 4)}\n`);
EOF
fi

cat >"${DIST_POC}/README.md" <<'EOF'
# Portal Gamma offline POC (static)

Built by `yarn poc:build`.

## Layout

| Path | Content |
|------|---------|
| `/` | Gamma Console |
| `/portal-editor/` | Developer Portals MF remote |
| `/mf-apim/` | APIM MF remote |
| `/mock/*` + rewrites | Mock management/gamma API |

## Deploy (Vercel CLI, no Git link)

```bash
cd dist-poc
npx vercel login
npx vercel          # preview
npx vercel --prod   # production
```
EOF

echo ""
echo "==> Done. Output: ${DIST_POC}"
echo "    Deploy: cd dist-poc && npx vercel --prod"
