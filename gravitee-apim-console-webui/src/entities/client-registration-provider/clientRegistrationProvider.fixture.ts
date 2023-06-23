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

import { ClientRegistrationProvider } from './clientRegistrationProvider';

export function fakeClientRegistrationProvider(attributes?: Partial<ClientRegistrationProvider>): ClientRegistrationProvider {
  const base: ClientRegistrationProvider = {
    ...fakeNewClientRegistrationProvider(),
    id: '61840ad7-7a93-4c3b-840a-d77a937b4bff',
    updated_at: 1636536375600,
  } as ClientRegistrationProvider;

  return {
    ...base,
    ...attributes,
  };
}

export function fakeNewClientRegistrationProvider(attributes?: Partial<ClientRegistrationProvider>): Partial<ClientRegistrationProvider> {
  const base: Partial<ClientRegistrationProvider> = {
    name: 'Fake ClientRegistrationProvider',
    description: 'My ClientRegistrationProvider',
    discovery_endpoint: 'https://localhost:8080',
    initial_access_token_type: 'CLIENT_CREDENTIALS',
    client_id: '123456',
    client_secret: 'abcdef',
    scopes: [],
    initial_access_token: 'token',
    renew_client_secret_support: false,
    renew_client_secret_endpoint: 'https://localhost:8080',
    renew_client_secret_method: 'GET',
    software_id: 'softId',
  };

  return {
    ...base,
    ...attributes,
  };
}
