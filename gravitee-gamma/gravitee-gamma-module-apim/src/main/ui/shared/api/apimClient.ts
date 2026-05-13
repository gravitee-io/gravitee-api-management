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

let _managementBaseUrl: string | undefined;
let _bootstrapPromise: Promise<string> | undefined;

async function resolveManagementBaseUrl(): Promise<string> {
    if (_managementBaseUrl) return _managementBaseUrl;
    _bootstrapPromise ??= (async () => {
        try {
            const constants = await fetch('/constants.json').then(r => r.json() as Promise<{ gammaBaseURL: string }>);
            const gammaBase = constants.gammaBaseURL.replace(/\/$/, '');
            const bootstrap = await fetch(`${gammaBase}/ui/bootstrap`).then(r => r.json() as Promise<{ managementBaseURL: string }>);
            _managementBaseUrl = bootstrap.managementBaseURL.replace(/\/$/, '');
            return _managementBaseUrl;
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

export async function apimFetchJson<T>(organizationId: string, path: string, init?: RequestInit): Promise<T> {
    const base = await resolveManagementBaseUrl();
    return doFetch<T>(`${base}/organizations/${organizationId}${path}`, path, init);
}

export async function apimFetchJsonV2<T>(environmentId: string, path: string, init?: RequestInit): Promise<T> {
    const base = await resolveManagementBaseUrl();
    return doFetch<T>(`${base}/v2/environments/${environmentId}${path}`, path, init);
}
