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
import type { CredentialStrategy } from '../types/credential';

export interface CredentialField {
    readonly key: string;
    readonly label: string;
    readonly placeholder?: string;
    readonly optional?: boolean;
    readonly multiline?: boolean;
    /** Typed as comma-separated text, sent as a list. */
    readonly list?: boolean;
}

export interface CredentialStrategyDefinition {
    readonly label: string;
    readonly settings: readonly CredentialField[];
    readonly secrets: readonly CredentialField[];
}

/** Mirrors the backend `CredentialStrategy`: its secret fields and the plain settings it accepts. */
export const CREDENTIAL_STRATEGY_DEFINITIONS: Readonly<Record<CredentialStrategy, CredentialStrategyDefinition>> = {
    BEARER: {
        label: 'Bearer token',
        settings: [],
        secrets: [{ key: 'token', label: 'Token' }],
    },
    API_KEY: {
        label: 'API key',
        settings: [{ key: 'headerName', label: 'Header name', placeholder: 'X-Api-Key' }],
        secrets: [{ key: 'value', label: 'Value' }],
    },
    OAUTH2: {
        label: 'OAuth2 client credentials',
        settings: [
            { key: 'tokenUrl', label: 'Token URL', placeholder: 'https://idp.example.com/oauth/token' },
            { key: 'scopes', label: 'Scopes', placeholder: 'read, write', optional: true, list: true },
            { key: 'audience', label: 'Audience', optional: true },
        ],
        secrets: [
            { key: 'clientId', label: 'Client ID' },
            { key: 'clientSecret', label: 'Client secret' },
        ],
    },
    BASIC: {
        label: 'Basic auth',
        settings: [],
        secrets: [
            { key: 'username', label: 'Username' },
            { key: 'password', label: 'Password' },
        ],
    },
    SERVICE_ACCOUNT: {
        label: 'Service account',
        settings: [],
        secrets: [{ key: 'serviceAccountKey', label: 'Service account key', placeholder: 'Paste the JSON key file', multiline: true }],
    },
};

// The server can return a strategy this console was built before.
export function isKnownCredentialStrategy(strategy: string): strategy is CredentialStrategy {
    return Object.hasOwn(CREDENTIAL_STRATEGY_DEFINITIONS, strategy);
}

export function credentialStrategyLabel(strategy: CredentialStrategy): string {
    return CREDENTIAL_STRATEGY_DEFINITIONS[strategy]?.label ?? strategy;
}
