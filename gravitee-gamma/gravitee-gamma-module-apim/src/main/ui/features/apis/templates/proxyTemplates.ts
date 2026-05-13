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
import { KeyIcon, LockIcon, KeyRoundIcon, ShieldCheckIcon } from '@gravitee/graphene-core/icons';
import type { CSSProperties } from 'react';

import type { ProxyTemplate, TemplateColor } from '../types/apiCreation';
import { AUTH_ICON_COLORS, TEMPLATE_CARD_BG } from '../utils/securityFormatters';

export const PROXY_TEMPLATES: ProxyTemplate[] = [
    {
        id: 'rest-api-key',
        title: 'REST API with API Key',
        subtitle: 'Most common pattern',
        description: 'Protect your REST API with simple API key authentication. Consumers receive a key when they subscribe to the plan.',
        Icon: KeyIcon,
        color: 'blue',
        tags: ['REST', 'API Key', 'Simple onboarding'],
        defaults: {
            authType: 'api-key',
            apiKeyPlanName: 'Default API Key plan',
        },
    },
    {
        id: 'rest-jwt',
        title: 'REST API with JWT',
        subtitle: 'Enterprise identity provider',
        description:
            'Validate JWTs issued by your identity provider. Best for organisations with an existing IdP like Auth0, Okta, or Azure AD.',
        Icon: ShieldCheckIcon,
        color: 'violet',
        tags: ['REST', 'JWT', 'JWKS', 'Enterprise'],
        defaults: {
            authType: 'jwt',
            jwtPlanName: 'Default JWT plan',
            jwtSignature: 'RS256',
            jwtJwksResolver: 'JWKS_URL',
            jwtResolverParameter: '',
        },
    },
    {
        id: 'rest-oauth2',
        title: 'REST API with OAuth 2.0',
        subtitle: 'Token-based enterprise security',
        description:
            'Enforce OAuth 2.0 access tokens with token introspection. Ideal for enterprise APIs that require delegated authorization.',
        Icon: LockIcon,
        color: 'amber',
        tags: ['REST', 'OAuth 2.0', 'Enterprise'],
        defaults: {
            authType: 'oauth2',
            oauth2PlanName: 'Default OAuth2 plan',
            oauth2Resource: 'generic-oauth2',
        },
    },
    {
        id: 'rest-keyless',
        title: 'REST API with Keyless plan',
        subtitle: 'Not recommended',
        description: 'Creates a REST proxy with a keyless (open) plan so traffic is accepted without API keys or subscriptions.',
        Icon: KeyRoundIcon,
        color: 'rose',
        tags: ['REST', 'Keyless', 'Demo / sandbox'],
        notRecommended: true,
        warningMessage:
            'For demos, workshops, and local testing only. The API is publicly reachable without subscriptions or API keys. Do not use for production or sensitive data.',
        defaults: {
            authType: 'keyless',
        },
    },
];

export const TEMPLATE_COLOR_STYLES: Record<TemplateColor, { iconBg: CSSProperties; iconColor: CSSProperties }> = {
    blue: {
        iconBg: { backgroundColor: TEMPLATE_CARD_BG.blue },
        iconColor: { color: AUTH_ICON_COLORS.blue },
    },
    violet: {
        iconBg: { backgroundColor: TEMPLATE_CARD_BG.violet },
        iconColor: { color: AUTH_ICON_COLORS.violet },
    },
    amber: {
        iconBg: { backgroundColor: TEMPLATE_CARD_BG.amber },
        iconColor: { color: AUTH_ICON_COLORS.amber },
    },
    rose: {
        iconBg: { backgroundColor: TEMPLATE_CARD_BG.rose },
        iconColor: { color: AUTH_ICON_COLORS.rose },
    },
};
