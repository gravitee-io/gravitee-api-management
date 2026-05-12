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
import type { ApiCreationTemplate } from '../types/template.types';

export const keylessTemplate: ApiCreationTemplate = {
    id: 'keyless',
    label: 'REST API with Keyless plan',
    headline: 'Not recommended',
    description: 'Creates a REST proxy with a keyless (open) plan so traffic is accepted without API keys or subscriptions.',
    tags: ['REST', 'Keyless', 'Demo / sandbox'],
    icon: 'keyless',
    caution: {
        label: 'Demo and testing only',
        description:
            'For demos, workshops, and local testing only. The API is publicly reachable without subscriptions or API keys, and you will not get plan-based quotas or access rules. Do not use for production or sensitive data.',
    },
    steps: ['essentials', 'review-deploy'],
    defaults: {
        security: { type: 'keyless' },
    },
};
