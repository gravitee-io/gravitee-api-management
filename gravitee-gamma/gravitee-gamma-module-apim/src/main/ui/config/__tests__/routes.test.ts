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
jest.mock('../observability', () => ({
    observability: {
        resolveRouteKey: () => null,
    },
}));

import { getActiveNavKey, isRouteKey, ROUTES } from '../routes';

describe('getActiveNavKey', () => {
    it('returns quick-start for root path', () => {
        expect(getActiveNavKey('/')).toBe('quick-start');
    });

    it('returns quick-start for /quick-start', () => {
        expect(getActiveNavKey('/quick-start')).toBe('quick-start');
    });

    it('returns apis for /apis', () => {
        expect(getActiveNavKey('/apis')).toBe('apis');
    });

    it('handles host-prefixed standard routes', () => {
        expect(getActiveNavKey('/environments/DEFAULT/apim/apis', 'environments/DEFAULT/apim')).toBe('apis');
        expect(getActiveNavKey('/environments/DEFAULT/apim/quick-start', 'environments/DEFAULT/apim')).toBe('quick-start');
    });
});

describe('isRouteKey', () => {
    it('recognises quick-start', () => {
        expect(isRouteKey('quick-start')).toBe(true);
    });

    it('rejects dashboard', () => {
        expect(isRouteKey('dashboard')).toBe(false);
    });
});

describe('ROUTES', () => {
    it('has a path and label for the quick start route', () => {
        expect(ROUTES['quick-start']).toEqual({ path: 'quick-start', label: 'Quick Start' });
    });
});
