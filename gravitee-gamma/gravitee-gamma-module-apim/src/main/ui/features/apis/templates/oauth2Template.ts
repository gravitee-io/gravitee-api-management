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

export const oauth2Template: ApiCreationTemplate = {
    id: 'rest-oauth2',
    label: 'REST API with OAuth 2.0',
    headline: 'Token-based enterprise security',
    description:
        'Enforce OAuth 2.0 access tokens with token introspection. Ideal for enterprise APIs that require delegated authorization.',
    tags: ['REST', 'OAuth 2.0', 'Introspection', 'Enterprise'],
    icon: 'oauth2',
    steps: ['essentials', 'review-deploy'],
    defaults: {
        security: { type: 'oauth2', planName: 'Default OAuth2 plan', resource: '' },
    },
};
