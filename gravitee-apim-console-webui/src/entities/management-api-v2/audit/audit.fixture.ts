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

import { isFunction } from 'lodash';

import { Audit } from './audit';

export function fakeAudit(modifier?: Partial<Audit> | ((base: Audit) => Audit)): Audit {
  const base: Audit = {
    id: 'audit-id',
    organizationId: 'organization-id',
    environmentId: 'environment-id',
    reference: {
      id: 'api-id',
      type: 'API',
      name: 'API name',
    },
    createdAt: '2021-07-01T00:00:00.000Z',
    user: {
      id: 'user-id',
      displayName: 'John Doe',
    },
    event: 'APIKEY_REVOKED',
    properties: [
      {
        key: 'API_KEY',
        value: 'd2df9def-fd47-491b-90be-ebf1829adb5b',
        name: 'd2df9def-fd47-491b-90be-ebf1829adb5b',
      },
    ],
    patch: '[{"op":"add","path":"/revokedAt","value":1698406835.71},{"op":"replace","path":"/revoked","value":true}]',
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
