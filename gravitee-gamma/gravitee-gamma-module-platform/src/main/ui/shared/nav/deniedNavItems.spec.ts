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
/** Module state, so each test needs its own copy of the store. */
async function freshStore() {
    jest.resetModules();
    return import('./deniedNavItems');
}

describe('denied nav items', () => {
    it('records a denied item', async () => {
        const store = await freshStore();
        store.markNavItemDenied('tenants');

        expect(store.getDeniedNavItemKeys().has('tenants')).toBe(true);
    });

    // Page effects run before the layout's, so the layout's first environment callback would otherwise
    // wipe a denial recorded during the same commit.
    it('keeps a denial recorded before the first environment is seen', async () => {
        const store = await freshStore();
        store.markNavItemDenied('tenants');
        store.resetDeniedNavItemsForEnvironment('env-1');

        expect(store.getDeniedNavItemKeys().has('tenants')).toBe(true);
    });

    it('clears denials when the environment changes', async () => {
        const store = await freshStore();
        store.resetDeniedNavItemsForEnvironment('env-1');
        store.markNavItemDenied('tenants');
        store.resetDeniedNavItemsForEnvironment('env-2');

        expect(store.getDeniedNavItemKeys().size).toBe(0);
    });

    it('keeps denials while the environment stays the same', async () => {
        const store = await freshStore();
        store.resetDeniedNavItemsForEnvironment('env-1');
        store.markNavItemDenied('tenants');
        store.resetDeniedNavItemsForEnvironment('env-1');

        expect(store.getDeniedNavItemKeys().has('tenants')).toBe(true);
    });
});
