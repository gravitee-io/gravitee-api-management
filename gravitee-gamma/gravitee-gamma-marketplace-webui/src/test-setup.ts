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
import { act } from '@testing-library/react';

import { resetAllStores, seedBootstrap } from './testing/helpers';
import { server } from './testing/server';

window.matchMedia ??= (query: string): MediaQueryList =>
    ({
        matches: false,
        media: query,
        onchange: null,
        addEventListener: () => undefined,
        removeEventListener: () => undefined,
        addListener: () => undefined,
        removeListener: () => undefined,
        dispatchEvent: () => false,
    }) as MediaQueryList;

if (typeof Element !== 'undefined' && typeof Element.prototype.hasPointerCapture !== 'function') {
    Element.prototype.hasPointerCapture = () => false;
    Element.prototype.setPointerCapture = () => undefined;
    Element.prototype.releasePointerCapture = () => undefined;
}

window.ResizeObserver ??= class {
    observe() {
        return undefined;
    }
    unobserve() {
        return undefined;
    }
    disconnect() {
        return undefined;
    }
};

beforeAll(() => {
    server.listen({ onUnhandledRequest: 'error' });
});
beforeEach(() => seedBootstrap());
afterEach(() => {
    server.resetHandlers();
    act(() => {
        resetAllStores();
    });
});
afterAll(() => server.close());
