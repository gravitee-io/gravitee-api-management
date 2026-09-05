/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { MemoryStorage } from 'angular-oauth2-oidc';

import { AccessTokenFilteringOAuthStorage } from './access-token-filtering-oauth-storage';

describe('AccessTokenFilteringOAuthStorage', () => {
  it('ignores writes to the access_token key', () => {
    const delegate = new MemoryStorage();
    const storage = new AccessTokenFilteringOAuthStorage(delegate);

    storage.setItem('access_token', 'super-secret-idp-token');

    expect(storage.getItem('access_token')).toBeNull();
    expect(delegate.getItem('access_token')).toBeNull();
  });

  it('still writes and reads every other key, e.g. id_token, unaffected', () => {
    const delegate = new MemoryStorage();
    const storage = new AccessTokenFilteringOAuthStorage(delegate);

    storage.setItem('id_token', 'some-id-token');
    storage.setItem('expires_at', '123456789');

    expect(storage.getItem('id_token')).toBe('some-id-token');
    expect(storage.getItem('expires_at')).toBe('123456789');
  });

  it('delegates removeItem for every key, including access_token', () => {
    const delegate = new MemoryStorage();
    delegate.setItem('id_token', 'some-id-token');
    const storage = new AccessTokenFilteringOAuthStorage(delegate);

    storage.removeItem('id_token');

    expect(storage.getItem('id_token')).toBeNull();
  });
});
