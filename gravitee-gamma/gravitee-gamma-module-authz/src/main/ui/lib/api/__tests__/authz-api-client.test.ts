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
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, createAuthzApiClient } from '../authz-api-client';

function jsonResponse(body: object, status = 200, extraHeaders: Record<string, string> = {}): Response {
    return new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json', ...extraHeaders },
    });
}

const CONSTANTS = { gammaBaseURL: 'http://gamma.test' };
const BOOTSTRAP = {
    gammaBaseURL: 'http://gamma.test',
    managementBaseURL: 'http://mgmt.test/management',
    organizationId: 'org-1',
};

function makeFetch(apiHandler?: (url: string, init?: RequestInit) => Response | null) {
    return vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        if (url.includes('constants.json')) return Promise.resolve(jsonResponse(CONSTANTS));
        if (url.includes('/ui/bootstrap')) return Promise.resolve(jsonResponse(BOOTSTRAP));
        const result = apiHandler?.(url, init);
        return Promise.resolve(result ?? new Response('not found', { status: 404 }));
    });
}

describe('createAuthzApiClient', () => {
    afterEach(() => {
        localStorage.clear();
    });

    it('fetches constants and bootstrap once then caches', async () => {
        const mockFetch = makeFetch(() => jsonResponse({ data: [] }));
        const api = createAuthzApiClient(mockFetch);

        await api.get('/environments/DEFAULT/policies');
        await api.get('/environments/DEFAULT/policies');

        const bootstrapCalls = mockFetch.mock.calls.filter(([u]) => String(u).includes('/ui/bootstrap'));
        expect(bootstrapCalls).toHaveLength(1);
    });

    it('builds URL using gammaBaseURL + /organizations/{org}/modules/authz prefix', async () => {
        let captured: string | undefined;
        const mockFetch = makeFetch(url => {
            if (url.includes('/policies')) captured = url;
            return jsonResponse({});
        });

        await createAuthzApiClient(mockFetch).get('/environments/env-1/policies?page=1');

        expect(captured).toBe('http://gamma.test/organizations/org-1/modules/authz/environments/env-1/policies?page=1');
    });

    it('sends X-Requested-With and Content-Type on POST with body', async () => {
        let capturedInit: RequestInit | undefined;
        const mockFetch = makeFetch((url, init) => {
            if (url.includes('/policies')) capturedInit = init;
            return jsonResponse({});
        });

        await createAuthzApiClient(mockFetch).post('/environments/env/policies', { name: 'p' });

        const headers = capturedInit?.headers as Headers;
        expect(headers.get('X-Requested-With')).toBe('XMLHttpRequest');
        expect(headers.get('Content-Type')).toBe('application/json');
    });

    it('persists X-Xsrf-Token across requests', async () => {
        let capturedCsrf: string | null = null;
        const mockFetch = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
            const url = String(input);
            if (url.includes('constants.json')) return Promise.resolve(jsonResponse(CONSTANTS));
            if (url.includes('/ui/bootstrap')) return Promise.resolve(jsonResponse(BOOTSTRAP));
            if (url.includes('/policies')) {
                const h = init?.headers as Headers | undefined;
                capturedCsrf = h?.get('X-Xsrf-Token') ?? null;
            }
            return Promise.resolve(jsonResponse({}, 200, { 'X-Xsrf-Token': 'server-csrf' }));
        });

        const api = createAuthzApiClient(mockFetch);
        await api.get('/environments/env/policies');
        await api.get('/environments/env/policies');

        expect(capturedCsrf).toBe('server-csrf');
    });

    it('throws ApiError with parsed ValidationErrorResponse on 400', async () => {
        const mockFetch = makeFetch(() => jsonResponse({ message: 'Invalid schema', status: 400, errors: ['line 1'] }, 400));
        const api = createAuthzApiClient(mockFetch);

        let caught: unknown;
        try {
            await api.put('/environments/env/schema', {});
        } catch (e) {
            caught = e;
        }

        expect(caught).toBeInstanceOf(ApiError);
        const err = caught as ApiError;
        expect(err.status).toBe(400);
        expect(err.validation?.errors).toEqual(['line 1']);
        expect(err.message).toBe('Invalid schema');
    });

    it('throws ApiError on 404 without body', async () => {
        const mockFetch = makeFetch(() => new Response('', { status: 404 }));
        const api = createAuthzApiClient(mockFetch);

        await expect(api.get('/environments/env/policies/x')).rejects.toBeInstanceOf(ApiError);
    });

    it('deduplicates concurrent bootstrap fetches across parallel callers', async () => {
        // Bootstrap thrashing: APIM saw 6–9 calls to /ui/bootstrap per
        // navigation because every concurrent caller fired its own fetch
        // before any of them cached the resolved value. Caching the in-flight
        // promise should collapse N parallel callers into one round-trip.
        const mockFetch = makeFetch(() => jsonResponse({ data: [] }));
        const api = createAuthzApiClient(mockFetch);

        // Five concurrent requests, like five hooks mounting in parallel.
        await Promise.all([
            api.get('/environments/DEFAULT/policies'),
            api.get('/environments/DEFAULT/policies'),
            api.get('/environments/DEFAULT/policies'),
            api.get('/environments/DEFAULT/policies'),
            api.get('/environments/DEFAULT/policies'),
        ]);

        const constantsCalls = mockFetch.mock.calls.filter(([u]) => String(u).includes('constants.json'));
        const bootstrapCalls = mockFetch.mock.calls.filter(([u]) => String(u).includes('/ui/bootstrap'));
        expect(constantsCalls).toHaveLength(1);
        expect(bootstrapCalls).toHaveLength(1);
    });

    it('clears the cache after a bootstrap failure so a retry can succeed', async () => {
        let constantsCalls = 0;
        const mockFetch = vi.fn((input: RequestInfo | URL) => {
            const url = String(input);
            if (url.includes('constants.json')) {
                constantsCalls++;
                if (constantsCalls === 1) {
                    return Promise.resolve(new Response('boom', { status: 500 }));
                }
                return Promise.resolve(jsonResponse(CONSTANTS));
            }
            if (url.includes('/ui/bootstrap')) return Promise.resolve(jsonResponse(BOOTSTRAP));
            return Promise.resolve(jsonResponse({ ok: true }));
        });

        const api = createAuthzApiClient(mockFetch);
        await expect(api.get('/environments/DEFAULT/policies')).rejects.toThrow();
        // Second attempt should re-fetch (not return the failed promise).
        await api.get('/environments/DEFAULT/policies');
        expect(constantsCalls).toBe(2);
    });

    it('returns undefined for 204', async () => {
        const mockFetch = makeFetch(() => new Response(null, { status: 204 }));
        const api = createAuthzApiClient(mockFetch);

        const result = await api.delete<void>('/environments/env/schema');
        expect(result).toBeUndefined();
    });
});
