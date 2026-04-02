/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { useBootstrapStore } from '../config/bootstrap.store';

export type Backend = 'management' | 'gamma';

function resolveBaseUrl(backend: Backend): string {
    const config = useBootstrapStore.getState().config;
    if (!config) throw new Error('Bootstrap not initialized');

    switch (backend) {
        case 'management':
            return `${config.managementBaseURL}/organizations/${config.organizationId}`;
        case 'gamma':
            return `${config.gammaBaseURL}/organizations/${config.organizationId}`;
    }
}

function getCsrfToken(): string | null {
    return localStorage.getItem('XSRF-TOKEN');
}

function setCsrfToken(value: string) {
    localStorage.setItem('XSRF-TOKEN', value);
}

export async function request<T>(backend: Backend, path: string, init?: RequestInit): Promise<T> {
    const headers = new Headers(init?.headers);
    headers.set('X-Requested-With', 'XMLHttpRequest');
    const csrf = getCsrfToken();
    if (csrf) headers.set('X-Xsrf-Token', csrf);

    const res = await fetch(`${resolveBaseUrl(backend)}${path}`, {
        ...init,
        headers,
        credentials: 'include',
    });

    const newCsrf = res.headers.get('X-Xsrf-Token');
    if (newCsrf) setCsrfToken(newCsrf);

    if (!res.ok) {
        throw new ApiError(res.status, `${init?.method ?? 'GET'} ${path} failed`);
    }

    if (res.status === 204) return undefined as T;
    const text = await res.text();
    if (!text) return undefined as T;
    return JSON.parse(text) as T;
}

export class ApiError extends Error {
    constructor(
        public readonly status: number,
        message: string,
    ) {
        super(message);
        this.name = 'ApiError';
    }
}

function createClient(backend: Backend) {
    return {
        get: <T>(path: string) => request<T>(backend, path),

        post: <T>(path: string, body?: unknown, extraHeaders?: Record<string, string>) =>
            request<T>(backend, path, {
                method: 'POST',
                headers: {
                    ...(body ? { 'Content-Type': 'application/json' } : {}),
                    ...extraHeaders,
                },
                body: body ? JSON.stringify(body) : undefined,
            }),

        put: <T>(path: string, body: unknown) =>
            request<T>(backend, path, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body),
            }),

        delete: <T>(path: string) => request<T>(backend, path, { method: 'DELETE' }),
    };
}

export const managementApi = createClient('management');
export const gammaApi = createClient('gamma');
