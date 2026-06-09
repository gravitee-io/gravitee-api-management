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

import { getCsrfToken, setCsrfToken } from './csrf';

/**
 * Generic HTTP client for talking to the Platform module's own plugin REST surface.
 *
 * Unlike the gamma-ui-shared apimClient (which targets the management API), this client targets
 * the gamma rest-api where module resources live. Callers pass a `baseUrl` per request relative to
 * the bootstrap-resolved `gammaBaseURL` (e.g. `/organizations/.../modules/platform/am`); the client
 * prepends the absolute host resolved from `/constants.json` → `/gamma/ui/bootstrap`.
 */

function stripTrailingSlash(url: string): string {
    return url.endsWith('/') ? url.slice(0, -1) : url;
}

export class ModuleApiError extends Error {
    readonly status: number;
    readonly body?: string;
    readonly code?: string;
    constructor(status: number, message: string, body?: string, code?: string) {
        super(message);
        this.name = 'ModuleApiError';
        this.status = status;
        this.body = body;
        this.code = code;
    }

    get isAmUnavailable(): boolean {
        return this.status === 503 || this.code === 'am_not_configured';
    }
}

function tryExtractMessage(text: string): string | undefined {
    if (!text) return undefined;
    try {
        const parsed = JSON.parse(text);
        if (parsed && typeof parsed.message === 'string') return parsed.message;
    } catch {
        // not JSON; fall through and return the raw text
    }
    return text;
}

function tryExtractCode(text: string): string | undefined {
    if (!text) return undefined;
    try {
        const parsed = JSON.parse(text);
        if (parsed && typeof parsed.code === 'string') return parsed.code;
    } catch {
        // not JSON; no code available
    }
    return undefined;
}

export function createAmManagementApi(fetchFn: typeof fetch = fetch) {
    let cachedGammaBaseURL: string | null = null;
    let cachedOrganizationId: string | null = null;

    async function resolveBootstrap(): Promise<{ gammaBaseURL: string; organizationId: string | null }> {
        if (cachedGammaBaseURL) return { gammaBaseURL: cachedGammaBaseURL, organizationId: cachedOrganizationId };

        const constantsRes = await fetchFn('/constants.json');
        if (!constantsRes.ok) {
            throw new ModuleApiError(constantsRes.status, `Failed to fetch constants.json: ${constantsRes.status}`);
        }
        const { gammaBaseURL } = (await constantsRes.json()) as { gammaBaseURL: string };

        const bootstrapRes = await fetchFn(`${stripTrailingSlash(gammaBaseURL)}/ui/bootstrap`);
        if (!bootstrapRes.ok) {
            throw new ModuleApiError(bootstrapRes.status, `Failed to fetch bootstrap config: ${bootstrapRes.status}`);
        }
        const bootstrap = (await bootstrapRes.json()) as { gammaBaseURL: string; organizationId?: string };

        cachedGammaBaseURL = stripTrailingSlash(bootstrap.gammaBaseURL);
        cachedOrganizationId = bootstrap.organizationId ?? null;
        return { gammaBaseURL: cachedGammaBaseURL, organizationId: cachedOrganizationId };
    }

    async function request<T>(baseUrl: string, path: string, init: RequestInit = {}): Promise<T> {
        const headers: Record<string, string> = {
            Accept: 'application/json',
            ...((init.headers ?? {}) as Record<string, string>),
        };
        const csrf = getCsrfToken();
        if (csrf) headers['X-Xsrf-Token'] = csrf;

        const { gammaBaseURL } = await resolveBootstrap();
        const url = `${gammaBaseURL}${baseUrl}${path}`;
        let res: Response;
        try {
            res = await fetchFn(url, { ...init, credentials: 'include', headers });
        } catch (e) {
            console.error(`Module request network error: ${init.method ?? 'GET'} ${url}`, e);
            throw new ModuleApiError(0, `Network error calling module: ${String(e)}`);
        }

        const nextXsrf = res.headers.get('x-xsrf-token');
        if (nextXsrf) setCsrfToken(nextXsrf);

        if (!res.ok) {
            let text = '';
            try {
                text = await res.text();
            } catch (e) {
                console.error(`Module request: failed to read error body for ${url}`, e);
            }
            let message: string;
            if (res.status >= 500) message = `Module server error (${res.status})`;
            else if (res.status === 401 || res.status === 403) message = `Unauthorized (${res.status})`;
            else if (res.status === 404) message = `Not found (${res.status})`;
            else message = `Module request failed (${res.status})`;
            const detail = tryExtractMessage(text) ?? res.statusText;
            const code = tryExtractCode(text);
            throw new ModuleApiError(res.status, detail ? `${message}: ${detail}` : message, text, code);
        }
        if (res.status === 204) return undefined as T;
        return (await res.json()) as T;
    }

    const jsonBody = (body: unknown) => ({
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });

    return {
        resolveOrganizationId: async (): Promise<string | null> => (await resolveBootstrap()).organizationId,

        get: <T>(baseUrl: string, path: string) => request<T>(baseUrl, path),

        post: <T>(baseUrl: string, path: string, body?: unknown) =>
            request<T>(baseUrl, path, { method: 'POST', ...(body !== undefined ? jsonBody(body) : {}) }),

        put: <T>(baseUrl: string, path: string, body: unknown) => request<T>(baseUrl, path, { method: 'PUT', ...jsonBody(body) }),

        delete: <T>(baseUrl: string, path: string) => request<T>(baseUrl, path, { method: 'DELETE' }),
    };
}

export const amManagementApi = createAmManagementApi();
