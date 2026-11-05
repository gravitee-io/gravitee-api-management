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

import { API_PRODUCT_RESOURCE_PLUGIN_IDS, isApiProductResourcePlugin } from './api-product-resource-plugins';

describe('API Product resource plugins', () => {
  it('allows only the resource plugins the API Product update accepts', () => {
    expect([...API_PRODUCT_RESOURCE_PLUGIN_IDS].sort()).toEqual(
      [
        'auth-provider-http-resource',
        'auth-provider-inline-resource',
        'auth-provider-ldap-resource',
        'cache',
        'cache-redis',
        'content-provider-inline-resource',
        'oauth2',
        'oauth2-am-resource',
        'oauth2-auth0',
        'oauth2-entra-id',
        'oauth2-keycloak-resource',
      ].sort(),
    );
  });

  it('rejects a plugin that is not on the allow-list', () => {
    expect(isApiProductResourcePlugin('cache')).toBe(true);
    expect(isApiProductResourcePlugin('cache-cloud-redis')).toBe(false);
  });
});
