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
import {
    buildPlanName,
    buildPreviewGatewayUrl,
    GATEWAY_URL_PLACEHOLDER,
    mapFormToCreateRequest,
    mapFormToPlanRequest,
} from './apiProxyMapper';
import type { ApiProxyDraft } from '../types/apiCreation';

const BASE: ApiProxyDraft = {
    apiName: 'My API',
    apiVersion: '1.0.0',
    apiDescription: 'A test API',
    contextPath: '/my-api',
    virtualHostsEnabled: false,
    virtualHosts: [{ id: '1', host: 'api.example.com', path: '/v1', overrideAccess: false }],
    targetUrl: 'https://backend.example.com',
    authType: 'keyless',
    apiKeyPlanName: 'API Key Plan',
    jwtPlanName: 'JWT Plan',
    jwtSignature: 'RS256',
    jwtJwksResolver: 'JWKS_URL',
    jwtResolverParameter: 'https://jwks.example.com/.well-known/jwks.json',
    oauth2PlanName: 'OAuth2 Plan',
    oauth2Resource: 'generic-oauth2',
    mtlsPlanName: 'mTLS Plan',
    deployImmediately: true,
};

function form(overrides: Partial<ApiProxyDraft> = {}): ApiProxyDraft {
    return { ...BASE, ...overrides };
}

describe('buildPreviewGatewayUrl', () => {
    it('returns gateway placeholder + context path when virtual hosts are disabled', () => {
        expect(buildPreviewGatewayUrl(form())).toBe(`${GATEWAY_URL_PLACEHOLDER}/my-api`);
    });

    it('returns host + path when virtual hosts are enabled with a host value', () => {
        expect(buildPreviewGatewayUrl(form({ virtualHostsEnabled: true }))).toBe('api.example.com/v1');
    });

    it('falls back to the gateway placeholder when the virtual host has no host value', () => {
        const result = buildPreviewGatewayUrl(
            form({ virtualHostsEnabled: true, virtualHosts: [{ id: '1', host: '', path: '/v1', overrideAccess: false }] }),
        );
        expect(result).toBe(`${GATEWAY_URL_PLACEHOLDER}/v1`);
    });

    it('falls back to "/" when the virtual host has no path value', () => {
        const result = buildPreviewGatewayUrl(
            form({ virtualHostsEnabled: true, virtualHosts: [{ id: '1', host: 'api.example.com', path: '', overrideAccess: false }] }),
        );
        expect(result).toBe('api.example.com/');
    });

    it('falls back to "/your-api" placeholder when contextPath is empty and virtual hosts are disabled', () => {
        expect(buildPreviewGatewayUrl(form({ contextPath: '' }))).toBe(`${GATEWAY_URL_PLACEHOLDER}/your-api`);
    });
});

describe('mapFormToCreateRequest', () => {
    it('produces a path-based listener when virtual hosts are disabled', () => {
        const req = mapFormToCreateRequest(form());
        expect(req.listeners[0]).toEqual({ type: 'HTTP', paths: [{ path: '/my-api' }], entrypoints: [{ type: 'http-proxy' }] });
    });

    it('produces a host-based listener when virtual hosts are enabled', () => {
        const req = mapFormToCreateRequest(form({ virtualHostsEnabled: true }));
        expect(req.listeners[0]).toEqual({
            type: 'HTTP',
            hosts: [{ host: 'api.example.com', path: '/v1', overrideAccess: false }],
            entrypoints: [{ type: 'http-proxy' }],
        });
    });

    it('wires API name, version, description, and target URL correctly', () => {
        const req = mapFormToCreateRequest(form());
        expect(req.name).toBe('My API');
        expect(req.apiVersion).toBe('1.0.0');
        expect(req.description).toBe('A test API');
        expect(req.endpointGroups[0].endpoints[0].configuration.target).toBe('https://backend.example.com');
    });
});

describe('mapFormToPlanRequest', () => {
    it('maps each auth type to the correct security type and configuration', () => {
        expect(mapFormToPlanRequest(form({ authType: 'keyless' })).security).toEqual({ type: 'KEY_LESS' });
        expect(mapFormToPlanRequest(form({ authType: 'api-key' })).security).toEqual({ type: 'API_KEY' });
        expect(mapFormToPlanRequest(form({ authType: 'mtls' })).security).toEqual({ type: 'MTLS' });

        const jwtSecurity = mapFormToPlanRequest(form({ authType: 'jwt' })).security;
        expect(jwtSecurity.type).toBe('JWT');
        expect(jwtSecurity.configuration).toMatchObject({
            signature: 'RS256',
            publicKeyResolver: 'JWKS_URL',
            resolverParameter: 'https://jwks.example.com/.well-known/jwks.json',
        });

        const oauth2Security = mapFormToPlanRequest(form({ authType: 'oauth2' })).security;
        expect(oauth2Security.type).toBe('OAUTH2');
        expect(oauth2Security.configuration).toMatchObject({ authorizationServerResource: 'generic-oauth2' });
    });
});

describe('buildPlanName', () => {
    it('returns the correct plan name field for each auth type', () => {
        expect(buildPlanName(form({ authType: 'keyless' }))).toBe('Default keyless plan');
        expect(buildPlanName(form({ authType: 'api-key' }))).toBe('API Key Plan');
        expect(buildPlanName(form({ authType: 'jwt' }))).toBe('JWT Plan');
        expect(buildPlanName(form({ authType: 'oauth2' }))).toBe('OAuth2 Plan');
        expect(buildPlanName(form({ authType: 'mtls' }))).toBe('mTLS Plan');
    });
});
