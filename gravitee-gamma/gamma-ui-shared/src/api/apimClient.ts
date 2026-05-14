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

export interface ApimBootstrap {
    managementBaseURL: string;
    organizationId: string;
}

let _bootstrap: ApimBootstrap | undefined;
let _bootstrapPromise: Promise<ApimBootstrap> | undefined;

function requireNonEmptyString(value: unknown, field: string): string {
    if (typeof value !== 'string' || !value.trim()) {
        throw new ApimApiError(0, `Bootstrap config is missing required field: ${field}`);
    }
    return value;
}

function trimTrailingSlash(url: string): string {
    return url.replace(/\/$/, '');
}

async function resolveBootstrap(): Promise<ApimBootstrap> {
    if (_bootstrap) return _bootstrap;
    _bootstrapPromise ??= (async () => {
        try {
            const constantsRes = await fetch('/constants.json');
            if (!constantsRes.ok) {
                throw new ApimApiError(constantsRes.status, `Failed to fetch constants.json: ${constantsRes.status}`);
            }
            const constants = (await constantsRes.json()) as Record<string, unknown>;
            const gammaBase = trimTrailingSlash(requireNonEmptyString(constants.gammaBaseURL, 'gammaBaseURL'));

            const bootstrapRes = await fetch(`${gammaBase}/ui/bootstrap`);
            if (!bootstrapRes.ok) {
                throw new ApimApiError(bootstrapRes.status, `Failed to fetch bootstrap config: ${bootstrapRes.status}`);
            }
            const bootstrap = (await bootstrapRes.json()) as Record<string, unknown>;

            _bootstrap = {
                managementBaseURL: trimTrailingSlash(requireNonEmptyString(bootstrap.managementBaseURL, 'managementBaseURL')),
                organizationId: requireNonEmptyString(bootstrap.organizationId, 'organizationId'),
            };
            return _bootstrap;
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
    const { organizationId } = await resolveBootstrap();
    return apimFetchJson<T>(organizationId, path, init);
}
