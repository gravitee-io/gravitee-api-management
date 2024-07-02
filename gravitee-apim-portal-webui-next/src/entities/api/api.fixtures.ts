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
import { isFunction } from 'rxjs/internal/util/isFunction';

import { Api } from './api';
import { ApisResponse } from './apis-response';

export function fakeApi(modifier?: Partial<Api> | ((baseApi: Api) => Api)): Api {
  const base: Api = {
    id: 'aee23b1e-34b1-4551-a23b-1e34b165516a',
    name: '\uD83E\uDE90 Planets',
    version: '1.0',
    description: 'The whole universe in your hand.',
    _public: true,
    running: true,
    entrypoints: ['https://api.company.com/planets'],
    listener_type: 'HTTP',
    labels: [],
    owner: {
      id: '8d4ce9b8-0efe-4d8b-8ce9-b80efe1d8bf1',
      email: 'gaetan.maisse@graviteesource.com',
      first_name: 'Hello',
      last_name: 'World',
      config: {},
      reference: 'user-reference',
      permissions: {
        USER: [],
        APPLICATION: [],
      },
      customFields: {},
      display_name: 'admin',
      editable_profile: false,
    },
    created_at: new Date(1630437434407),
    updated_at: new Date(1642675655553),
    categories: ['my-category'],
    _links: {
      picture:
        'https://master-apim-api.cloud.gravitee.io/management/organizations/DEFAULT/environments/DEFAULT/apis/aee23b1e-34b1-4551-a23b-1e34b165516a/picture?hash=1642675655553',
    },
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}

export function fakeApisResponse(modifier?: Partial<ApisResponse> | ((baseApi: ApisResponse) => ApisResponse)): ApisResponse {
  const base: ApisResponse = {
    data: [fakeApi()],
    metadata: {},
    links: {},
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
