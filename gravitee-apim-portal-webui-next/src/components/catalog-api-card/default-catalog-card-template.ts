/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Default GMD catalog tile (classic-style: picture + name/version/owner/status).
 * Prefer GMD components over raw HTML — the viewer sanitizes inline styles.
 */
export const DEFAULT_CATALOG_CARD_TEMPLATE = `<gmd-card backgroundColor="white">
  <gmd-grid columns="2">
    <gmd-cell>
      <gmd-md>
![{#api.name}]({#api.picture})
      </gmd-md>
    </gmd-cell>
    <gmd-cell>
      <gmd-md>
**{#api.name}** · \`v{#api.version}\`

👤 {#api.owner}

\`{#api.status}\`
      </gmd-md>
    </gmd-cell>
  </gmd-grid>
</gmd-card>
`;
