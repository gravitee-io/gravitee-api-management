/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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
import { AccessTokenFilteringStateStore } from './auth.service';

describe('AccessTokenFilteringStateStore', () => {
  let backingStore: Record<string, string>;
  let store: AccessTokenFilteringStateStore;

  beforeEach(() => {
    backingStore = {};
    const fakeStorage: Storage = {
      length: 0,
      clear: () => undefined,
      key: () => null,
      getItem: (key: string) => backingStore[key] ?? null,
      setItem: (key: string, value: string) => {
        backingStore[key] = value;
      },
      removeItem: (key: string) => {
        delete backingStore[key];
      },
    };
    store = new AccessTokenFilteringStateStore({ store: fakeStorage });
  });

  it('strips access_token from a serialised User object before persisting it', async () => {
    const user = JSON.stringify({
      access_token: 'super-secret-idp-token',
      id_token: 'some-id-token',
      expires_at: 123456789,
      token_type: 'Bearer',
    });

    await store.set('oidc.user:authority:client', user);

    const stored = JSON.parse(backingStore['oidc.user:authority:client']);
    expect(stored.access_token).toBe('');
    expect(stored.id_token).toBe('some-id-token');
    expect(stored.expires_at).toBe(123456789);
  });

  it('stores non-JSON values (e.g. OIDC state entries) unchanged', async () => {
    await store.set('oidc.state:abc123', 'not-json');

    expect(backingStore['oidc.state:abc123']).toBe('not-json');
  });
});
