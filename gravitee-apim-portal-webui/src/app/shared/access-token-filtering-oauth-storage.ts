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
import { OAuthStorage } from 'angular-oauth2-oidc';

/**
 * An OAuthStorage that ignores writes to the `access_token` key, so the IdP access token - a live bearer
 * credential at the IdP - never reaches Web Storage.
 *
 * `id_token` and the other OIDC bookkeeping keys are left untouched: they are needed for single logout
 * and session-expiry checks.
 *
 * See APIM-14822.
 */
export class AccessTokenFilteringOAuthStorage implements OAuthStorage {
  private static readonly FILTERED_KEY = 'access_token';

  constructor(private readonly delegate: OAuthStorage) {}

  getItem(key: string): string | null {
    return this.delegate.getItem(key);
  }

  removeItem(key: string): void {
    this.delegate.removeItem(key);
  }

  setItem(key: string, data: string): void {
    if (key === AccessTokenFilteringOAuthStorage.FILTERED_KEY) {
      return;
    }
    this.delegate.setItem(key, data);
  }
}
