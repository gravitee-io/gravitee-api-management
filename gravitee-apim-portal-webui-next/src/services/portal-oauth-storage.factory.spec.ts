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
import { AccessTokenFilteringOAuthStorage } from './access-token-filtering-oauth-storage';
import { ConfigService } from './config.service';
import { createPortalOAuthStorage } from './portal-oauth-storage.factory';

describe('createPortalOAuthStorage', () => {
  beforeEach(() => {
    sessionStorage.removeItem('access_token');
  });

  it('uses plain session storage when IdP access_token is exposed', () => {
    const configService = { stripIdpAccessTokenFromStorage: () => false } as ConfigService;
    const storage = createPortalOAuthStorage(configService);

    storage.setItem('access_token', 'secret');
    expect(storage.getItem('access_token')).toBe('secret');
  });

  it('filters access_token when installation hides it', () => {
    const configService = { stripIdpAccessTokenFromStorage: () => true } as ConfigService;
    const storage = createPortalOAuthStorage(configService);

    expect(storage).toBeInstanceOf(AccessTokenFilteringOAuthStorage);
    storage.setItem('access_token', 'secret');
    expect(storage.getItem('access_token')).toBeFalsy();
  });
});
