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
import { isTcpForm } from './protocol';
import { DEFAULT_OAUTH2_CONFIG } from '../pages/detail/plans/plan-form/security/oauth2Config';
import type {
    CreateApiPlanRequest,
    CreateApiProxyRequest,
    HttpEndpointGroup,
    HttpListener,
    PlanSecurity,
    TcpEndpointGroup,
    TcpListener,
} from '../types';
import type { ApiProxyDraft } from '../types/apiCreation';
import type { ApiResource } from '../types/resource';

export const GATEWAY_URL_PLACEHOLDER = 'https://gateway.company.com';
export const TCP_HOST_PLACEHOLDER = 'host.example.com';

export const OAUTH2_RESOURCE_NAME = 'OAuth2 Authorization Server';

export function buildPreviewGatewayUrl(form: ApiProxyDraft, gatewayPrefix = GATEWAY_URL_PLACEHOLDER): string {
    if (isTcpForm(form)) {
        return form.tcpHosts.find(h => h.host.trim())?.host.trim() || TCP_HOST_PLACEHOLDER;
    }
    if (form.virtualHostsEnabled && form.virtualHosts.length > 0) {
        const first = form.virtualHosts[0];
        const host = first.host || gatewayPrefix;
        const path = first.path || '/';
        return `${host}${path}`;
    }
    const path = form.contextPath || '/your-api';
    return `${gatewayPrefix}${path}`;
}

/** Upstream target display, shared by the review step and the flow visualization. */
export function buildPreviewUpstream(form: ApiProxyDraft): string {
    if (isTcpForm(form)) {
        const host = form.tcpTargetHost.trim() || 'upstream';
        const port = form.tcpTargetPort.trim() || 'port';
        return `${host}:${port}`;
    }
    return form.targetUrl.trim() || 'upstream:port';
}

function buildTcpListener(form: ApiProxyDraft): TcpListener {
    return {
        type: 'TCP',
        hosts: form.tcpHosts.map(h => h.host.trim()).filter(Boolean),
        entrypoints: [{ type: 'tcp-proxy' }],
    };
}

function buildHttpListener(form: ApiProxyDraft): HttpListener {
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

const LISTENER_BUILDERS: Record<ApiProxyDraft['protocol'], (form: ApiProxyDraft) => HttpListener | TcpListener> = {
    HTTP: buildHttpListener,
    TCP: buildTcpListener,
};

function buildListener(form: ApiProxyDraft): HttpListener | TcpListener {
    return LISTENER_BUILDERS[form.protocol](form);
}

function buildTcpEndpointGroups(form: ApiProxyDraft): TcpEndpointGroup[] {
    return [
        {
            name: 'Default TCP Proxy group',
            type: 'tcp-proxy',
            sharedConfiguration: {},
            endpoints: [
                {
                    name: 'Default TCP Proxy',
                    type: 'tcp-proxy',
                    weight: 1,
                    inheritConfiguration: false,
                    configuration: {
                        target: {
                            host: form.tcpTargetHost.trim(),
                            port: Number(form.tcpTargetPort.trim()),
                            secured: form.tcpTargetSecured,
                        },
                    },
                },
            ],
        },
    ];
}

function buildHttpEndpointGroups(form: ApiProxyDraft): HttpEndpointGroup[] {
    return [
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
    ];
}

const ENDPOINT_GROUP_BUILDERS: Record<ApiProxyDraft['protocol'], (form: ApiProxyDraft) => (HttpEndpointGroup | TcpEndpointGroup)[]> = {
    HTTP: buildHttpEndpointGroups,
    TCP: buildTcpEndpointGroups,
};

function buildEndpointGroups(form: ApiProxyDraft): (HttpEndpointGroup | TcpEndpointGroup)[] {
    return ENDPOINT_GROUP_BUILDERS[form.protocol](form);
}

function buildPlanSecurity(form: ApiProxyDraft): PlanSecurity {
    if (isTcpForm(form)) return { type: 'KEY_LESS' };
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
            return { type: 'OAUTH2', configuration: { ...DEFAULT_OAUTH2_CONFIG, oauthResource: OAUTH2_RESOURCE_NAME } };
        case 'mtls':
            return { type: 'MTLS' };
    }
}

export function buildPlanName(form: ApiProxyDraft): string {
    if (isTcpForm(form)) return 'Default Keyless (UNSECURED)';
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
        endpointGroups: buildEndpointGroups(form),
    };
}

export function buildApiResources(form: ApiProxyDraft): ApiResource[] {
    if (isTcpForm(form) || form.authType !== 'oauth2' || !form.oauth2ResourceType) return [];
    return [
        {
            name: OAUTH2_RESOURCE_NAME,
            type: form.oauth2ResourceType,
            enabled: true,
            configuration: form.oauth2ResourceConfig,
        },
    ];
}

export function mapFormToPlanRequest(form: ApiProxyDraft): CreateApiPlanRequest {
    return {
        name: buildPlanName(form),
        security: buildPlanSecurity(form),
        definitionVersion: 'V4',
        mode: 'STANDARD',
    };
}
