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

import { PLATFORM_ROUTE_CONFIG, ROUTES, ROUTE_KEYS, type RouteKey } from './routes';

/** Mirrors {@link resolveModulePath} activeNavKey resolution for Jest (SDK is ESM-only). */
function resolveActiveNavKey(pathname: string): RouteKey {
    const segments = pathname.split('/').filter(Boolean);
    let bestKey: RouteKey = PLATFORM_ROUTE_CONFIG.defaultRouteKey;
    let bestLength = 0;

    for (const key of ROUTE_KEYS) {
        const candidate = ROUTES[key].path.split('/');
        if (segments.length < candidate.length) {
            continue;
        }
        const start = segments.length - candidate.length;
        const matches = candidate.every((segment, index) => segments[start + index] === segment);
        if (matches && candidate.length > bestLength) {
            bestLength = candidate.length;
            bestKey = key;
        }
    }

    return bestKey;
}

describe('platform routes', () => {
    it('resolves environment CORS before org CORS on environment/cors paths', () => {
        expect(resolveActiveNavKey('/environments/dev/platform/environment/cors')).toBe('environment-cors');
    });

    it('resolves org CORS on platform/cors paths', () => {
        expect(resolveActiveNavKey('/environments/dev/platform/cors')).toBe('cors');
    });

    it('resolves environment SMTP before org SMTP on environment/smtp paths', () => {
        expect(resolveActiveNavKey('/environments/dev/platform/environment/smtp')).toBe('environment-smtp');
    });
});
