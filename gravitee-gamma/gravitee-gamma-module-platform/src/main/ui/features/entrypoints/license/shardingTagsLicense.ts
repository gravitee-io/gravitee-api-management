/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export const SHARDING_TAGS_LICENSE_FEATURE = 'apim-sharding-tags';

export const SHARDING_TAGS_UPGRADE = {
    title: 'Sharding Tags',
    description: 'Sharding tags let you route API traffic to specific gateway groups based on organization or environment needs.',
    features: [
        'Restrict entrypoints to dedicated gateway groups',
        'Control which groups can manage a given sharding tag',
        'Fine-grained routing for multi-tenant or multi-region deployments',
    ],
} as const;
