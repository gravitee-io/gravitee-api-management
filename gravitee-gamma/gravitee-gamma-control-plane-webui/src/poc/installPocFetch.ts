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

import { handlePocApiRequest } from './pocApiHandlers';
import { loadPocApiStore } from './pocApiStore';

function resolveUrl(input: RequestInfo | URL): URL {
    if (typeof input === 'string') {
        return new URL(input, window.location.origin);
    }
    if (input instanceof URL) {
        return input;
    }
    return new URL(input.url, window.location.origin);
}

/**
 * When POC_MODE is enabled, intercept mutable API management calls and serve them
 * from localStorage so create/list/open works without a real backend (local + Vercel).
 */
export function installPocFetch(): void {
    if (process.env.POC_MODE !== 'true') {
        return;
    }
    if (typeof window === 'undefined' || typeof window.fetch !== 'function') {
        return;
    }

    // Ensure demo API seed exists before any request.
    loadPocApiStore();

    const originalFetch = window.fetch.bind(window);

    window.fetch = async (input: RequestInfo | URL, init?: RequestInit): Promise<Response> => {
        const url = resolveUrl(input);
        if (!url.pathname.startsWith('/management/')) {
            return originalFetch(input, init);
        }

        try {
            // Clone Request inputs so fall-through can still read the original body.
            const request = new Request(input instanceof Request ? input.clone() : input, init);
            const handled = await handlePocApiRequest(request, url.pathname, url.searchParams);
            if (handled) {
                return handled;
            }
        } catch (err) {
            console.warn('[poc-fetch] handler error, falling through to network', err);
        }

        return originalFetch(input, init);
    };
}
