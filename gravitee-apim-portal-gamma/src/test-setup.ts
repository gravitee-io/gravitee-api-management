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
import '@testing-library/jest-dom';
import { act } from '@testing-library/react';

import { installFakeIndexedDB, resetFakeIndexedDB } from './testing/fake-indexeddb';
import { resetAllStores, seedBootstrap } from './testing/helpers';
import { server } from './testing/server';

if (typeof globalThis.DOMRect === 'undefined') {
    globalThis.DOMRect = class DOMRect {
        readonly x: number;
        readonly y: number;
        readonly width: number;
        readonly height: number;

        constructor(x = 0, y = 0, width = 0, height = 0) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        get top() {
            return this.y;
        }

        get left() {
            return this.x;
        }

        get right() {
            return this.x + this.width;
        }

        get bottom() {
            return this.y + this.height;
        }

        static fromRect(rect?: DOMRectInit) {
            return new DOMRect(rect?.x ?? 0, rect?.y ?? 0, rect?.width ?? 0, rect?.height ?? 0);
        }
    } as typeof DOMRect;
}

Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: () => undefined,
        removeListener: () => undefined,
        addEventListener: () => undefined,
        removeEventListener: () => undefined,
        dispatchEvent: () => false,
    }),
});

installFakeIndexedDB();

beforeAll(() => {
    server.listen({ onUnhandledRequest: 'error' });
});
beforeEach(() => {
    resetFakeIndexedDB();
    installFakeIndexedDB();
    seedBootstrap();
});
afterEach(() => {
    server.resetHandlers();
    act(() => {
        resetAllStores();
    });
});
afterAll(() => server.close());
