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
import { http, HttpResponse, type JsonBodyType } from 'msw';

import { buildBootstrapConfig, TEST_PORTAL_API } from './factories';
import { server } from './server';
import { useAuthStore } from '../app/auth/auth.store';
import { useBootstrapStore } from '../shared/config/bootstrap.store';
import type { BootstrapConfig } from '../shared/config/bootstrap.store';

export interface TrackedRequest {
    url: string;
    method: string;
    headers: Headers;
    body: unknown;
}

export interface RequestTracker {
    readonly calls: TrackedRequest[];
    readonly callCount: number;
    readonly lastCall: TrackedRequest | null;
}

export function trackHandler(
    method: 'get' | 'post' | 'put' | 'delete',
    url: string,
    responseBody: JsonBodyType = undefined,
    status = 200,
): RequestTracker {
    const requests: TrackedRequest[] = [];

    server.use(
        http[method](url, async ({ request }) => {
            const cloned = request.clone();
            requests.push({
                url: request.url,
                method: request.method,
                headers: new Headers(request.headers),
                body: request.method !== 'GET' ? await cloned.json().catch(() => null) : null,
            });
            return status === 204 ? new HttpResponse(null, { status }) : HttpResponse.json(responseBody, { status });
        }),
    );

    return {
        get calls() {
            return requests;
        },
        get callCount() {
            return requests.length;
        },
        get lastCall() {
            return requests[requests.length - 1] ?? null;
        },
    };
}

export function respondWith(method: 'get' | 'post' | 'put' | 'delete', url: string, body: JsonBodyType, status = 200) {
    server.use(http[method](url, () => (status === 204 ? new HttpResponse(null, { status }) : HttpResponse.json(body, { status }))));
}

export function respondWithError(method: 'get' | 'post' | 'put' | 'delete', url: string, status: number) {
    server.use(http[method](url, () => HttpResponse.json({ message: `Error ${status}` }, { status })));
}

function matchesPortalPath(request: Request, path: string): boolean {
    const url = new URL(request.url);
    return `${url.origin}${url.pathname}` === `${TEST_PORTAL_API}${path}`;
}

export function trackPortalPath(
    method: 'get' | 'post' | 'put' | 'delete',
    path: string,
    responseBody: JsonBodyType = undefined,
    status = 200,
): RequestTracker {
    const requests: TrackedRequest[] = [];

    server.use(
        http[method](
            ({ request }) => matchesPortalPath(request, path),
            async ({ request }) => {
                const cloned = request.clone();
                requests.push({
                    url: request.url,
                    method: request.method,
                    headers: new Headers(request.headers),
                    body: request.method !== 'GET' ? await cloned.json().catch(() => null) : null,
                });
                return status === 204 ? new HttpResponse(null, { status }) : HttpResponse.json(responseBody, { status });
            },
        ),
    );

    return {
        get calls() {
            return requests;
        },
        get callCount() {
            return requests.length;
        },
        get lastCall() {
            return requests[requests.length - 1] ?? null;
        },
    };
}

export function respondToPortalPath(method: 'get' | 'post' | 'put' | 'delete', path: string, body: JsonBodyType, status = 200) {
    server.use(
        http[method](
            ({ request }) => matchesPortalPath(request, path),
            () => (status === 204 ? new HttpResponse(null, { status }) : HttpResponse.json(body, { status })),
        ),
    );
}

export function respondToPortalPathError(method: 'get' | 'post' | 'put' | 'delete', path: string, status: number) {
    server.use(
        http[method](
            ({ request }) => matchesPortalPath(request, path),
            () => HttpResponse.json({ message: `Error ${status}` }, { status }),
        ),
    );
}

export function resetAllStores() {
    useBootstrapStore.setState({ config: null, loading: false, error: null });
    useAuthStore.setState({ user: null, loading: false, initialized: false, oauthRedirectUrl: null });
    localStorage.clear();
}

export function seedBootstrap(overrides: Partial<BootstrapConfig> = {}) {
    useBootstrapStore.setState({
        config: buildBootstrapConfig(overrides),
        loading: false,
        error: null,
    });
}
