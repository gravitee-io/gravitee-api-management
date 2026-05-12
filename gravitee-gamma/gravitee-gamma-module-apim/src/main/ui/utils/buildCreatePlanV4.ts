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
import type { CreatePlanV4Payload } from '../features/apis/types/api.types';
import type { ApiCreationState } from '../features/apis/types/models';

function mapJwtPublicKeyResolver(label: string): string {
    const l = label.trim().toUpperCase();
    if (l === 'GIVEN_KEY' || l === 'GATEWAY_KEYS' || l === 'JWKS_URL') return l;
    if (l.includes('JWKS')) return 'JWKS_URL';
    if (l.includes('GATEWAY')) return 'GATEWAY_KEYS';
    if (l.includes('KID')) return 'KID';
    return 'JWKS_URL';
}

/**
 * Builds plans to create after API exists — mirrors Console defaults for keyless
 * and maps Gamma simplified security fields to plan security configuration.
 */
export function buildCreatePlansV4(data: ApiCreationState): CreatePlanV4Payload[] {
    const s = data.security;
    switch (s.type) {
        case 'keyless':
            return [
                {
                    definitionVersion: 'V4',
                    name: 'Default Keyless (UNSECURED)',
                    description: 'Default unsecured plan',
                    mode: 'STANDARD',
                    security: {
                        type: 'KEY_LESS',
                        configuration: {},
                    },
                    validation: 'MANUAL',
                },
            ];
        case 'api-key':
            return [
                {
                    definitionVersion: 'V4',
                    name: s.planName.trim() || 'API Key plan',
                    description: 'API Key plan',
                    mode: 'STANDARD',
                    security: {
                        type: 'API_KEY',
                        configuration: {},
                    },
                    validation: 'MANUAL',
                },
            ];
        case 'jwt':
            return [
                {
                    definitionVersion: 'V4',
                    name: s.planName.trim() || 'JWT plan',
                    description: 'JWT plan',
                    mode: 'STANDARD',
                    security: {
                        type: 'JWT',
                        configuration: {
                            signature: s.signature.trim(),
                            publicKeyResolver: mapJwtPublicKeyResolver(s.jwksResolver),
                            resolverParameter: s.resolverParam.trim(),
                        },
                    },
                    validation: 'MANUAL',
                },
            ];
        case 'oauth2':
            return [
                {
                    definitionVersion: 'V4',
                    name: s.planName.trim() || 'OAuth2 plan',
                    description: 'OAuth2 plan',
                    mode: 'STANDARD',
                    security: {
                        type: 'OAUTH2',
                        configuration: {
                            oauthResource: s.resource.trim(),
                        },
                    },
                    validation: 'MANUAL',
                },
            ];
        case 'mtls':
            return [
                {
                    definitionVersion: 'V4',
                    name: s.planName.trim() || 'mTLS plan',
                    description: 'mTLS plan',
                    mode: 'STANDARD',
                    security: {
                        type: 'MTLS',
                        configuration: {},
                    },
                    validation: 'MANUAL',
                },
            ];
        default:
            return [];
    }
}
