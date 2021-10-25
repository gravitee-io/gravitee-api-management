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
import { UserMembership } from './userMembership';

export function fakeUserMembership<T extends 'application' | 'api' | string = string>(
  type?: T,
  attributes?: Partial<UserMembership<T>>,
): UserMembership<T> {
  if (type === 'api') {
    const baseApi: UserMembership<'api'> = {
      memberships: [
        {
          reference: '9f14c9a7-6077-4e09-94c9-a760773e0998',
          type: 'API' as const,
          roles: {
            API: 'OWNER',
          },
          source: 'system',
        },
        {
          reference: 'aee23b1e-34b1-4551-a23b-1e34b165516a',
          type: 'API' as const,
          roles: {
            API: 'REVIEWER',
          },
          source: 'system',
        },
      ],
      metadata: {
        '9f14c9a7-6077-4e09-94c9-a760773e0998': {
          visibility: 'PUBLIC',
          name: '\uD83D\uDC82‍♀️Planets API Validator',
          version: '1.0',
        },
        'aee23b1e-34b1-4551-a23b-1e34b165516a': {
          visibility: 'PUBLIC',
          name: '\uD83E\uDE90 Planets',
          version: '1.0',
        },
      },
    };

    return {
      ...baseApi,
      ...attributes,
    };
  }

  if (type === 'application') {
    const baseApplication: UserMembership<'application'> = {
      memberships: [
        {
          reference: '00783aa6-d0db-45d1-b83a-a6d0db05d1e1',
          type: 'APPLICATION',
          roles: {
            APPLICATION: 'PRIMARY_OWNER',
          },
          source: 'system',
        },
      ],
      metadata: {
        '00783aa6-d0db-45d1-b83a-a6d0db05d1e1': {
          name: 'Default application',
        },
      },
    };

    return {
      ...baseApplication,
      ...attributes,
    };
  }

  const base: UserMembership<T> = {
    memberships: [
      {
        reference: '00783aa6-d0db-45d1-b83a-a6d0db05d1e1',
        type: 'APPLICATION',
        roles: {
          APPLICATION: 'PRIMARY_OWNER',
        },
        source: 'system',
      },
    ],
    metadata: {
      '00783aa6-d0db-45d1-b83a-a6d0db05d1e1': {
        name: 'Default application',
      },
    },
  };

  return {
    ...base,
    ...attributes,
  };
}
