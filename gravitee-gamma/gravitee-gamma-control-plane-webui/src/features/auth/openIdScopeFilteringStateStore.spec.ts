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
import { OpenIdScopeFilteringStateStore } from './openIdScopeFilteringStateStore';

describe('OpenIdScopeFilteringStateStore', () => {
    let backingStore: Record<string, string>;
    let store: OpenIdScopeFilteringStateStore;

    beforeEach(() => {
        backingStore = {};
        const fakeStorage = {
            getItem: (key: string) => backingStore[key] ?? null,
            setItem: (key: string, value: string) => {
                backingStore[key] = value;
            },
        } as unknown as Storage;
        store = new OpenIdScopeFilteringStateStore({ store: fakeStorage });
    });

    it('should remove the openid scope from a sign-in state and keep the other scopes and fields', async () => {
        await store.set('state-id', JSON.stringify({ id: 'state-id', scope: 'profile openid email', code_verifier: 'verifier' }));

        expect(JSON.parse(backingStore['oidc.state-id']!)).toEqual({ id: 'state-id', scope: 'profile email', code_verifier: 'verifier' });
    });

    it('should store a state without scope unchanged', async () => {
        const signoutState = JSON.stringify({ id: 'state-id', request_type: 'so:r' });

        await store.set('state-id', signoutState);

        expect(backingStore['oidc.state-id']).toBe(signoutState);
    });

    it('should store a JSON value that is not an object unchanged', async () => {
        await store.set('state-id', 'null');

        expect(backingStore['oidc.state-id']).toBe('null');
    });

    it('should store a value that is not JSON unchanged', async () => {
        await store.set('state-id', 'not-json');

        expect(backingStore['oidc.state-id']).toBe('not-json');
    });
});
