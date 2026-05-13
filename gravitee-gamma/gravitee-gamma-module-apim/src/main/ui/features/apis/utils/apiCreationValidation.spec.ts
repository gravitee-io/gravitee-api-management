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
import { validateContextPath, validateDetails, validateEntrypoints, validateEssentials, validateSecurity } from './apiCreationValidation';
import type { ApiProxyDraft } from '../types/apiCreation';

const BASE: ApiProxyDraft = {
    apiName: 'My API',
    apiVersion: '1.0.0',
    apiDescription: '',
    contextPath: '/my-api',
    virtualHostsEnabled: false,
    virtualHosts: [{ id: '1', host: 'api.example.com', path: '/v1', overrideAccess: false }],
    targetUrl: 'https://backend.example.com',
    authType: 'keyless',
    apiKeyPlanName: 'Default API Key plan',
    jwtPlanName: 'Default JWT plan',
    jwtSignature: 'RS256',
    jwtJwksResolver: 'JWKS_URL',
    jwtResolverParameter: '',
    oauth2PlanName: 'Default OAuth2 plan',
    oauth2Resource: '',
    mtlsPlanName: 'Default mTLS plan',
    deployImmediately: true,
};

function form(overrides: Partial<ApiProxyDraft> = {}): ApiProxyDraft {
    return { ...BASE, ...overrides };
}

describe('validateContextPath', () => {
    it('returns an error when empty', () => {
        expect(validateContextPath('')).toBe('Context path is required.');
        expect(validateContextPath('   ')).toBe('Context path is required.');
    });

    it('returns an error when path is 3 characters or fewer', () => {
        expect(validateContextPath('/')).toBe('Context path has to be more than 3 characters long.');
        expect(validateContextPath('/ab')).toBe('Context path has to be more than 3 characters long.');
        expect(validateContextPath('/ab')).toBe('Context path has to be more than 3 characters long.');
    });

    it('returns an error for paths containing double slashes', () => {
        expect(validateContextPath('//api')).toBe('Context path is not valid.');
        expect(validateContextPath('/my//api')).toBe('Context path is not valid.');
    });

    it('returns an error for paths with invalid characters', () => {
        expect(validateContextPath('/my api')).toBe('Context path is not valid.');
        expect(validateContextPath('/my@api')).toBe('Context path is not valid.');
        expect(validateContextPath('no-leading-slash')).toBe('Context path is not valid.');
    });

    it('returns null for valid paths (at least 4 chars, starts with /, allowed chars)', () => {
        expect(validateContextPath('/abc')).toBeNull();
        expect(validateContextPath('/my-api')).toBeNull();
        expect(validateContextPath('/my_api/v2.0')).toBeNull();
        expect(validateContextPath('/api/v1')).toBeNull();
    });
});

describe('validateDetails', () => {
    it('returns an error for each missing required field', () => {
        expect(validateDetails(form({ apiName: '' }))).toEqual({ apiName: 'API name is required.' });
        expect(validateDetails(form({ apiVersion: '' }))).toEqual({ apiVersion: 'Version is required.' });
        expect(validateDetails(form({ apiName: '', apiVersion: '' }))).toEqual({
            apiName: 'API name is required.',
            apiVersion: 'Version is required.',
        });
    });

    it('returns no errors when all required fields are filled', () => {
        expect(validateDetails(form())).toEqual({});
    });
});

describe('validateEntrypoints', () => {
    it('returns errors for missing targetUrl and invalid path / virtual-host values', () => {
        expect(validateEntrypoints(form({ targetUrl: '' }))).toHaveProperty('targetUrl');

        expect(
            validateEntrypoints(
                form({ virtualHostsEnabled: true, virtualHosts: [{ id: '1', host: '', path: '/', overrideAccess: false }] }),
            ),
        ).toHaveProperty('virtualHosts');

        expect(validateEntrypoints(form({ contextPath: '' }))).toHaveProperty('contextPath');
    });

    it('returns no errors for valid entrypoint combinations', () => {
        expect(validateEntrypoints(form())).toEqual({});

        expect(
            validateEntrypoints(
                form({
                    virtualHostsEnabled: true,
                    virtualHosts: [{ id: '1', host: 'api.example.com', path: '/v1', overrideAccess: false }],
                }),
            ),
        ).toEqual({});
    });
});

describe('validateSecurity', () => {
    it('returns a plan name error for each non-keyless auth type with an empty plan name', () => {
        expect(validateSecurity(form({ authType: 'api-key', apiKeyPlanName: '' }))).toHaveProperty('apiKeyPlanName');
        expect(validateSecurity(form({ authType: 'jwt', jwtPlanName: '' }))).toHaveProperty('jwtPlanName');
        expect(validateSecurity(form({ authType: 'oauth2', oauth2PlanName: '' }))).toHaveProperty('oauth2PlanName');
        expect(validateSecurity(form({ authType: 'mtls', mtlsPlanName: '' }))).toHaveProperty('mtlsPlanName');
    });

    it('returns no errors when auth is keyless or the plan name is filled', () => {
        expect(validateSecurity(form({ authType: 'keyless' }))).toEqual({});
        expect(validateSecurity(form({ authType: 'api-key', apiKeyPlanName: 'My Plan' }))).toEqual({});
        expect(validateSecurity(form({ authType: 'jwt', jwtPlanName: 'My JWT Plan' }))).toEqual({});
    });
});

describe('validateEssentials', () => {
    it('returns exactly the errors for whichever fields are missing', () => {
        const allMissing = validateEssentials(form({ apiName: '', apiVersion: '', contextPath: '', targetUrl: '' }));
        expect(Object.keys(allMissing)).toHaveLength(4);
        expect(allMissing).toHaveProperty('apiName');
        expect(allMissing).toHaveProperty('apiVersion');
        expect(allMissing).toHaveProperty('contextPath');
        expect(allMissing).toHaveProperty('targetUrl');

        expect(validateEssentials(form({ apiName: '' }))).toEqual({ apiName: 'API name is required.' });
        expect(validateEssentials(form())).toEqual({});
    });
});
