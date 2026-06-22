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
import type { CreateApiPlanRequest, CreateApiProxyRequest, HttpListener, PlanSecurity } from '../types';
import type { ApiProxyDraft } from '../types/apiCreation';

export const GATEWAY_URL_PLACEHOLDER = 'https://gateway.company.com';

export function buildPreviewGatewayUrl(form: ApiProxyDraft, gatewayPrefix = GATEWAY_URL_PLACEHOLDER): string {
    if (form.virtualHostsEnabled && form.virtualHosts.length > 0) {
        const first = form.virtualHosts[0];
        const host = first.host || gatewayPrefix;
        const path = first.path || '/';
        return `${host}${path}`;
    }
    const path = form.contextPath || '/your-api';
    return `${gatewayPrefix}${path}`;
}

function buildListener(form: ApiProxyDraft): HttpListener {
    if (form.virtualHostsEnabled && form.virtualHosts.length > 0) {
        return {
            type: 'HTTP',
            hosts: form.virtualHosts.map(vh => ({
                host: vh.host,
                path: vh.path,
                overrideAccess: vh.overrideAccess,
            })),
            entrypoints: [{ type: 'http-proxy' }],
        };
    }
    return { type: 'HTTP', paths: [{ path: form.contextPath }], entrypoints: [{ type: 'http-proxy' }] };
}

function buildPlanSecurity(form: ApiProxyDraft): PlanSecurity {
    switch (form.authType) {
        case 'keyless':
            return { type: 'KEY_LESS' };
        case 'api-key':
            return { type: 'API_KEY' };
        case 'jwt':
            return {
                type: 'JWT',
                configuration: {
                    signature: form.jwtSignature,
                    publicKeyResolver: form.jwtJwksResolver,
                    resolverParameter: form.jwtResolverParameter,
                },
            };
        case 'oauth2':
            return { type: 'OAUTH2', configuration: { authorizationServerResource: form.oauth2Resource } };
        case 'mtls':
            return { type: 'MTLS' };
    }
}

export function buildPlanName(form: ApiProxyDraft): string {
    switch (form.authType) {
        case 'keyless':
            return 'Default keyless plan';
        case 'api-key':
            return form.apiKeyPlanName;
        case 'jwt':
            return form.jwtPlanName;
        case 'oauth2':
            return form.oauth2PlanName;
        case 'mtls':
            return form.mtlsPlanName;
    }
}

export function mapFormToCreateRequest(form: ApiProxyDraft): CreateApiProxyRequest {
    return {
        name: form.apiName,
        apiVersion: form.apiVersion,
        description: form.apiDescription,
        type: 'PROXY',
        definitionVersion: 'V4',
        visibility: 'PRIVATE',
        listeners: [buildListener(form)],
        allowedInApiProducts: false,
        endpointGroups: [
            {
                name: 'Default endpoint group',
                type: 'http-proxy',
                sharedConfiguration: {},
                endpoints: [
                    {
                        name: 'Default endpoint',
                        type: 'http-proxy',
                        weight: 1,
                        inheritConfiguration: false,
                        configuration: { target: form.targetUrl },
                    },
                ],
            },
        ],
    };
}

export function mapFormToPlanRequest(form: ApiProxyDraft): CreateApiPlanRequest {
    return {
        name: buildPlanName(form),
        security: buildPlanSecurity(form),
        definitionVersion: 'V4',
        mode: 'STANDARD',
    };
}
