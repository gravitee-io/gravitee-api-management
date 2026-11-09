/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
 * Default GMD catalog tile.
 * Keep title/subtitle first — gmd-card always projects those slots above other content.
 */
export const DEFAULT_CATALOG_CARD_TEMPLATE = `<gmd-card>
  <gmd-card-title>{#api.name}</gmd-card-title>
  <gmd-card-subtitle>{#api.version}</gmd-card-subtitle>
  <gmd-md>
![{#api.name}]({#api.picture})

{#api.description}

**Type:** {#api.type} · **Status:** {#api.status} · **Owner:** {#api.owner}
  </gmd-md>
</gmd-card>
`;

export const SAMPLE_CATALOG_API_CONTEXT = {
  api: {
    name: 'Payments API',
    version: '1.2.0',
    description: 'Process and reconcile payments across channels.',
    type: 'API',
    status: 'Started',
    owner: 'Alex Morgan',
    // Sample data-URI so Console preview shows an image without a live API picture URL.
    picture:
      'data:image/svg+xml,' +
      encodeURIComponent(
        '<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64"><rect width="64" height="64" rx="12" fill="#eef2ff"/><text x="32" y="38" text-anchor="middle" font-size="20" fill="#275cf6">API</text></svg>',
      ),
    mcp: false,
    labels: ['payments', 'finance'],
    metadata: {
      ownerTeam: 'Platform',
    },
  },
};
