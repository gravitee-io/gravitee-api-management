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
import type { ValidationErrorResponse } from './authz-api.types';

interface BootstrapConfig {
    /** e.g. "http://localhost:8083/gamma" — root of the Gamma REST API. */
    gammaBaseURL: string;
    /** e.g. "http://localhost:8083/management" — root of the legacy Management API. */
    managementBaseURL: string;
    organizationId: string;
}

function stripTrailingSlash(url: string): string {
    return url.endsWith('/') ? url.slice(0, -1) : url;
}

// CSRF token storage.
//
// TODO(authz-csrf): the token is currently kept in `localStorage` as a
// pragmatic session-bridge so the prototype works against the upstream
// Gravitee management cookie scheme. This is NOT the long-term plan —
// CSRF tokens should be issued by the server as a `Secure; SameSite=Strict`
// cookie (or read from a `Set-Cookie`-mirrored header) so they cannot be
// exfiltrated by XSS. Switch this once the backend cookie endpoint lands;
// at that point both helpers below should be replaced with cookie reads.
function getCsrfToken(): string | null {
    return localStorage.getItem('XSRF-TOKEN');
}

function setCsrfToken(value: string): void {
    localStorage.setItem('XSRF-TOKEN', value);
}

export class ApiError extends Error {
    constructor(
        public readonly status: number,
        message: string,
        public readonly validation?: ValidationErrorResponse,
    ) {
        super(message);
        this.name = 'ApiError';
    }
}

export interface AuthzApiClient {
    get: <T>(path: string) => Promise<T>;
    post: <T>(path: string, body?: unknown) => Promise<T>;
    put: <T>(path: string, body: unknown) => Promise<T>;
    delete: <T>(path: string) => Promise<T>;
}

/**
 * Returns the REST root used by this module.
 *
 *   - {@link AuthzApiClients.core}: the per-organization Gamma SPI tree at
 *     {@code {gammaBaseURL}/organizations/{org}}. The authz REST resources are
 *     mounted underneath by {@code GammaModulesResource} via the SPI, so the
 *     service layer composes paths as
 *     {@code /environments/{env}/modules/authz/...}.
 *
 * Shares a single bootstrap fetch, CSRF token bucket, and error
 * normalisation path with whatever else this module hits.
 */
export interface AuthzApiClients {
    readonly core: AuthzApiClient;
}

export function createAuthzApiClients(fetchFn: typeof fetch = fetch): AuthzApiClients {
    // We cache the in-flight PROMISE rather than just the resolved value.
    //
    // Caching only the resolved value let parallel callers (6–9 hooks mounting
    // simultaneously on a fresh page) each fire their own /constants.json +
    // /ui/bootstrap pair before any of them populated the cache, producing
    // the bootstrap-thrashing seen in APIM logs. Caching the promise
    // de-duplicates concurrent calls into a single network round-trip while
    // keeping the same observable behaviour (every caller still awaits a
    // BootstrapConfig).
    //
    // On error we clear the cache so a transient failure doesn't permanently
    // poison subsequent calls.
    let cachedPromise: Promise<BootstrapConfig> | null = null;

    function loadBootstrapConfig(): Promise<BootstrapConfig> {
        if (cachedPromise) {
            return cachedPromise;
        }

        cachedPromise = (async () => {
            const constantsRes = await fetchFn('/constants.json');
            if (!constantsRes.ok) {
                throw new Error(`Failed to fetch constants.json: ${constantsRes.status}`);
            }

            const { gammaBaseURL: gammaBaseURLFromConstants } = (await constantsRes.json()) as {
                gammaBaseURL: string;
            };

            const bootstrapRes = await fetchFn(`${stripTrailingSlash(gammaBaseURLFromConstants)}/ui/bootstrap`);
            if (!bootstrapRes.ok) {
                throw new Error(`Failed to fetch bootstrap config: ${bootstrapRes.status}`);
            }

            const bootstrap = (await bootstrapRes.json()) as {
                gammaBaseURL?: string;
                managementBaseURL: string;
                organizationId: string;
            };

            return {
                gammaBaseURL: stripTrailingSlash(bootstrap.gammaBaseURL ?? gammaBaseURLFromConstants),
                managementBaseURL: stripTrailingSlash(bootstrap.managementBaseURL),
                organizationId: bootstrap.organizationId,
            };
        })().catch(err => {
            cachedPromise = null;
            throw err;
        });

        return cachedPromise;
    }

    async function request<T>(buildUrl: (cfg: BootstrapConfig) => string, path: string, init?: RequestInit): Promise<T> {
        const config = await loadBootstrapConfig();
        const baseUrl = buildUrl(config);

        const headers = new Headers(init?.headers);
        headers.set('X-Requested-With', 'XMLHttpRequest');

        const csrf = getCsrfToken();
        if (csrf) {
            headers.set('X-Xsrf-Token', csrf);
        }

        const res = await fetchFn(`${baseUrl}${path}`, {
            ...init,
            headers,
            credentials: 'include',
        });

        const newCsrf = res.headers.get('X-Xsrf-Token');
        if (newCsrf) {
            setCsrfToken(newCsrf);
        }

        if (!res.ok) {
            let validation: ValidationErrorResponse | undefined;
            try {
                const text = await res.text();
                if (text) {
                    const parsed = JSON.parse(text) as ValidationErrorResponse;
                    if (typeof parsed.message === 'string') {
                        validation = parsed;
                    }
                }
            } catch {
                // ignore
            }
            throw new ApiError(res.status, validation?.message ?? `${init?.method ?? 'GET'} ${path} failed`, validation);
        }

        if (res.status === 204) {
            return undefined as T;
        }

        const text = await res.text();
        if (!text) {
            return undefined as T;
        }

        return JSON.parse(text) as T;
    }

    function clientFor(buildUrl: (cfg: BootstrapConfig) => string): AuthzApiClient {
        return {
            get: <T>(path: string) => request<T>(buildUrl, path),
            post: <T>(path: string, body?: unknown) =>
                request<T>(buildUrl, path, {
                    method: 'POST',
                    headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
                    body: body !== undefined ? JSON.stringify(body) : undefined,
                }),
            put: <T>(path: string, body: unknown) =>
                request<T>(buildUrl, path, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body),
                }),
            delete: <T>(path: string) =>
                request<T>(buildUrl, path, {
                    method: 'DELETE',
                }),
        };
    }

    return {
        // Authz is mounted by GammaModulesResource under the per-org SPI tree,
        // so requests go to /gamma/organizations/{org}/{path}. The service layer
        // prefixes `path` with `/environments/{env}/modules/authz/...` to match
        // the backend's environment-scoped sub-resource locator.
        core: clientFor(cfg => `${cfg.gammaBaseURL}/organizations/${cfg.organizationId}`),
    };
}

const clients = createAuthzApiClients();

/**
 * Canonical Gamma authz REST kernel ({@code /gamma/authz/*}). Use for
 * entity / policy / schema CRUD — these endpoints are shared by every Gamma
 * module, not just this one.
 */
export const authzCoreApiClient: AuthzApiClient = clients.core;
