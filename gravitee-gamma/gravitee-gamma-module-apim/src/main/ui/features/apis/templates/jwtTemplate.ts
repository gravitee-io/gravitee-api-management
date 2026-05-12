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

export const jwtTemplate: ApiCreationTemplate = {
    id: 'jwt-default',
    label: 'REST API with JWT',
    headline: 'Enterprise identity provider',
    description:
        'Validate JWTs issued by your identity provider. Best for organizations with an existing IdP like Auth0, Okta, or Azure AD.',
    tags: ['REST', 'JWT', 'JWKS', 'Enterprise'],
    icon: 'jwt',
    steps: ['essentials', 'review-deploy'],
    defaults: {
        security: {
            type: 'jwt',
            planName: 'Default JWT plan',
            signature: 'RSA_RS256',
            jwksResolver: 'JWKS_URL',
            resolverParam: 'https://idp.example.com/.well-known/jwks.json',
        },
    },
};
