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

interface BootstrapCache {
    managementBaseURL: string;
    organizationId: string;
}

let _cache: BootstrapCache | undefined;
let _bootstrapPromise: Promise<BootstrapCache> | undefined;

async function resolveBootstrap(): Promise<BootstrapCache> {
    if (_cache) return _cache;
    _bootstrapPromise ??= (async () => {
        try {
            const constants = await fetch('/constants.json').then(r => r.json() as Promise<{ gammaBaseURL: string }>);
            const gammaBase = constants.gammaBaseURL.replace(/\/$/, '');
            const bootstrap = await fetch(`${gammaBase}/ui/bootstrap`).then(
                r => r.json() as Promise<{ managementBaseURL: string; organizationId: string }>,
            );
            _cache = {
                managementBaseURL: bootstrap.managementBaseURL.replace(/\/$/, ''),
                organizationId: bootstrap.organizationId,
            };
            return _cache;
        } catch (err) {
            _bootstrapPromise = undefined;
            throw err;
        }
    })();
    return _bootstrapPromise;
}

export class ApimApiError extends Error {
    constructor(
        public readonly status: number,
        message: string,
        public readonly body?: unknown,
    ) {
        super(message);
        this.name = 'ApimApiError';
    }
}

async function doFetch<T>(url: string, path: string, init?: RequestInit): Promise<T> {
    const headers = new Headers(init?.headers);
    headers.set('X-Requested-With', 'XMLHttpRequest');
    if (init?.body !== undefined && init.body !== null) {
        headers.set('Content-Type', 'application/json');
    }
    const csrf = localStorage.getItem('XSRF-TOKEN');
    if (csrf) headers.set('X-Xsrf-Token', csrf);

    const res = await fetch(url, { ...init, headers, credentials: 'include' });

    const newCsrf = res.headers.get('X-Xsrf-Token');
    if (newCsrf) localStorage.setItem('XSRF-TOKEN', newCsrf);

    if (!res.ok) {
        const text = await res.text().catch(() => '');
        let parsed: unknown;
        try {
            parsed = JSON.parse(text);
        } catch {
            parsed = undefined;
        }
        const message = (parsed as Record<string, string> | undefined)?.message ?? text ?? `${path} → ${res.status}`;
        throw new ApimApiError(res.status, message, parsed);
    }
    if (res.status === 204) return undefined as T;
    const text = await res.text();
    return text ? (JSON.parse(text) as T) : (undefined as T);
}

/** Org-scoped v1: `/organizations/{orgId}{path}` */
export async function apimFetchJson<T>(organizationId: string, path: string, init?: RequestInit): Promise<T> {
    const { managementBaseURL } = await resolveBootstrap();
    return doFetch<T>(`${managementBaseURL}/organizations/${organizationId}${path}`, path, init);
}

/** v2 env-scoped: `/v2/environments/{envId}{path}` */
export async function apimFetchJsonV2<T>(environmentId: string, path: string, init?: RequestInit): Promise<T> {
    const { managementBaseURL } = await resolveBootstrap();
    return doFetch<T>(`${managementBaseURL}/v2/environments/${environmentId}${path}`, path, init);
}

/** v1 env-scoped (auto-resolves orgId): `/organizations/{orgId}/environments/{envId}{path}` */
export async function apimFetchJsonV1Env<T>(environmentId: string, path: string, init?: RequestInit): Promise<T> {
    const { managementBaseURL, organizationId } = await resolveBootstrap();
    return doFetch<T>(`${managementBaseURL}/organizations/${organizationId}/environments/${environmentId}${path}`, path, init);
}

/** Org-scoped v1 (auto-resolves orgId): `/organizations/{orgId}{path}` */
export async function apimFetchJsonOrg<T>(path: string, init?: RequestInit): Promise<T> {
    const { managementBaseURL, organizationId } = await resolveBootstrap();
    return doFetch<T>(`${managementBaseURL}/organizations/${organizationId}${path}`, path, init);
}
