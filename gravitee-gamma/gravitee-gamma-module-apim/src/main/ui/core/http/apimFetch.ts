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

export class ApimHttpError extends Error {
    constructor(
        public readonly status: number,
        message: string,
        public readonly body?: unknown,
    ) {
        super(message);
        this.name = 'ApimHttpError';
    }
}

function getXsrfToken(): string | null {
    if (typeof localStorage === 'undefined') return null;
    return localStorage.getItem('XSRF-TOKEN');
}

function setXsrfToken(value: string) {
    if (typeof localStorage === 'undefined') return;
    localStorage.setItem('XSRF-TOKEN', value);
}

function readErrorMessage(body: unknown): string | undefined {
    if (body && typeof body === 'object' && 'message' in body) {
        const m = (body as { message?: unknown }).message;
        if (typeof m === 'string') return m;
    }
    return undefined;
}

export async function apimFetchJson<T>(url: string, init?: RequestInit): Promise<T> {
    const headers = new Headers(init?.headers);
    headers.set('X-Requested-With', 'XMLHttpRequest');
    const csrf = getXsrfToken();
    if (csrf) headers.set('X-Xsrf-Token', csrf);
    if (init?.body && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }

    const res = await fetch(url, {
        ...init,
        headers,
        credentials: 'include',
    });

    const newCsrf = res.headers.get('X-Xsrf-Token');
    if (newCsrf) setXsrfToken(newCsrf);

    if (res.status === 204) {
        return undefined as T;
    }

    const text = await res.text();
    let parsed: unknown = undefined;
    if (text) {
        try {
            parsed = JSON.parse(text) as unknown;
        } catch {
            parsed = text;
        }
    }

    if (!res.ok) {
        throw new ApimHttpError(res.status, readErrorMessage(parsed) ?? `${init?.method ?? 'GET'} ${url} failed (${res.status})`, parsed);
    }

    return parsed as T;
}
