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
import { KeyIcon, KeyRoundIcon, LockIcon, ShieldCheckIcon, ShieldIcon } from '@gravitee/graphene-core/icons';
import type { LucideIcon } from '@gravitee/graphene-core/icons';
import type { CSSProperties } from 'react';

import type { AuthType } from '../types/wizard';

export const AUTH_LABEL: Record<AuthType, string> = {
    keyless: 'Keyless (Open)',
    'api-key': 'API Key',
    jwt: 'JWT',
    oauth2: 'OAuth 2.0',
    mtls: 'mTLS',
};

export interface AuthOption {
    id: AuthType;
    label: string;
    description: string;
    Icon: LucideIcon;
    iconStyle: CSSProperties;
}

export const AUTH_OPTIONS: AuthOption[] = [
    {
        id: 'keyless',
        label: 'Keyless (Open)',
        description: 'No authentication required. Any consumer can call this API.',
        Icon: KeyRoundIcon,
        iconStyle: { color: 'var(--color-success)' },
    },
    {
        id: 'api-key',
        label: 'API Key',
        description: 'Consumers must include a valid API key in requests.',
        Icon: KeyIcon,
        iconStyle: { color: 'var(--color-primary)' },
    },
    {
        id: 'jwt',
        label: 'JWT',
        description: 'Validate JSON Web Tokens from your identity provider.',
        Icon: ShieldCheckIcon,
        iconStyle: { color: '#8b5cf6' },
    },
    {
        id: 'oauth2',
        label: 'OAuth 2.0',
        description: 'Enforce OAuth 2.0 access tokens for enterprise security.',
        Icon: LockIcon,
        iconStyle: { color: '#d97706' },
    },
    {
        id: 'mtls',
        label: 'mTLS',
        description: 'Mutual TLS based on the client X.509 certificate.',
        Icon: ShieldIcon,
        iconStyle: { color: '#e11d48' },
    },
];

export const JWT_SIGNATURES = [
    { value: 'RS256', label: 'RS256 (RSA + SHA-256)' },
    { value: 'RS384', label: 'RS384 (RSA + SHA-384)' },
    { value: 'RS512', label: 'RS512 (RSA + SHA-512)' },
    { value: 'HS256', label: 'HS256 (HMAC + SHA-256)' },
    { value: 'HS384', label: 'HS384 (HMAC + SHA-384)' },
    { value: 'HS512', label: 'HS512 (HMAC + SHA-512)' },
] as const;

export const JWKS_RESOLVERS = [
    { value: 'GIVEN_KEY', label: 'Given key (PEM, single key)' },
    { value: 'GATEWAY_KEYS', label: 'Gateway keys (configured globally)' },
    { value: 'JWKS_URL', label: 'JWKS URL' },
] as const;

export const OAUTH2_RESOURCES = [
    { value: 'generic-oauth2', label: 'Generic OAuth2 Resource' },
    { value: 'am-oauth2', label: 'Gravitee Access Management' },
    { value: 'keycloak-oauth2', label: 'Keycloak Adapter' },
] as const;
