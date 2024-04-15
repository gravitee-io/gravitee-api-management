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

import { ApplicationSubscriptionApiKey } from './ApplicationSubscriptionApiKey';

import { ApiKeyMode } from '../application/Application';

export function fakeApplicationSubscriptionApiKey(
  modifier?: Partial<ApplicationSubscriptionApiKey> | ((baseApi: ApplicationSubscriptionApiKey) => ApplicationSubscriptionApiKey),
): ApplicationSubscriptionApiKey {
  const base: ApplicationSubscriptionApiKey = {
    id: '12f73b0a-59e6-4d23-b73b-0a59e62d2369',
    key: '240760d9-7a50-4e7c-8406-657cdee57fde',
    subscriptions: [
      {
        id: '990486be-f554-4e97-8486-bef5549e97c0',
        api: 'eb93b824-a70b-46dd-93b8-24a70be6ddfa',
        plan: '9a53d2e3-9ffc-492f-93d2-e39ffc892fb1',
        application: 'b62f6e82-c39b-4edb-af6e-82c39b9edb46',
        status: 'CLOSED',
        metadata: {},
        consumerStatus: 'STARTED',
        processed_at: 1711020274143,
        processed_by: 'b78eae83-fbfb-4ff7-8eae-83fbfb8ff770',
        subscribed_by: 'b78eae83-fbfb-4ff7-8eae-83fbfb8ff770',
        starting_at: 1711020274135,
        created_at: 1711020272645,
        updated_at: 1712062118650,
        closed_at: 1712062118650,
      },
      {
        id: '2a49e151-0845-4556-89e1-51084515563f',
        api: 'c42f51dd-fa20-4e68-af51-ddfa20be682c',
        plan: '363a92e8-611c-42d3-ba92-e8611c32d3f1',
        application: 'b62f6e82-c39b-4edb-af6e-82c39b9edb46',
        status: 'ACCEPTED',
        request: '',
        reason: 'good job',
        metadata: {},
        consumerStatus: 'STARTED',
        processed_at: 1711535552014,
        processed_by: '4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c',
        subscribed_by: '4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c',
        starting_at: 1711535552014,
        created_at: 1711535493015,
        updated_at: 1711535552014,
      },
      {
        id: '7aaf5841-d0cf-4860-af58-41d0cf08607d',
        api: '46980d82-fc35-4798-980d-82fc357798cb',
        plan: '34823aa0-1a35-400d-823a-a01a35700dba',
        application: 'b62f6e82-c39b-4edb-af6e-82c39b9edb46',
        status: 'ACCEPTED',
        request: '',
        metadata: {},
        consumerStatus: 'STARTED',
        processed_at: 1711708933789,
        processed_by: '4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c',
        subscribed_by: '4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c',
        starting_at: 1711708933789,
        created_at: 1711708920416,
        updated_at: 1711708933789,
      },
    ],
    application: {
      id: 'b62f6e82-c39b-4edb-af6e-82c39b9edb46',
      name: 'TAR - OAuth2 with DCR',
      description: "I'm a fox",
      groups: ['1001f3a8-a0c8-4904-81f3-a8a0c81904bc', '1ea0297e-bdfe-43c5-a029-7ebdfe93c565', '37f48db8-6fca-4e3e-b48d-b86fca4e3e24'],
      status: 'ACTIVE',
      type: 'BACKEND_TO_BACKEND',
      origin: 'MANAGEMENT',
      created_at: 1651581368850,
      updated_at: 1651581368850,
      disable_membership_notifications: false,
      api_key_mode: ApiKeyMode.SHARED,
      owner: {
        id: '4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c',
        email: 'no-reply@graviteesource.com',
        displayName: 'Admin master',
        type: 'USER',
      },
      settings: {
        oauth: {
          client_id: 'zLgNDMUCbbCBDNnpBGb-WOV_lNrUlQlAlUiSditR9Es',
          client_secret: '3zEYOXPqqCyaq7os--Nf1-6jrHjL0AjumFz4CL78nwQ',
          redirect_uris: [],
          response_types: [],
          grant_types: ['client_credentials'],
          application_type: 'backend_to_backend',
          renew_client_secret_supported: false,
        },
      },
    },
    revoked: false,
    expired: false,
    created_at: 1711020274185,
    updated_at: 1711020274185,
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
