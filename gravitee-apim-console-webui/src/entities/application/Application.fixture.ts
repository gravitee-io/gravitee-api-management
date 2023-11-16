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
import { ApiKeyMode, Application, ApplicationType } from './application';

export function fakeApplication(attributes?: Partial<Application>): Application {
  const base: Application = {
    api_key_mode: ApiKeyMode.UNSPECIFIED,
    created_at: 1636536375600,
    description: 'My default application',
    id: '61840ad7-7a93-4b5b-840a-d77a937b5bff',
    name: 'Default application',
    owner: {},
    picture_url:
      'https://apim-master-api.cloud.gravitee.io/management/organizations/DEFAULT/environments/DEFAULT/applications/61840ad7-7a93-4b5b-840a-d77a937b5bff/picture?hash=1636536375600',

    settings: {
      oauth: {
        client_id: 'test_client_id',
        client_secret: 'test_client_secret',
        redirect_uris: ['https://apim-master-console.team-apim.gravitee.dev/'],
        response_types: ['code', 'token', 'id_token'],
        grant_types: ['authorization_code', 'refresh_token', 'password', 'implicit'],
        application_type: 'NATIVE',
        renew_client_secret_supported: false,
      },
    },
    status: 'ACTIVE',
    type: 'SIMPLE',
    updated_at: 1636536375600,
    origin: 'MANAGEMENT',
  };

  return {
    ...base,
    ...attributes,
  };
}

export function fakeApplicationType(attributes?: Partial<ApplicationType>): ApplicationType {
  const base: ApplicationType = {
    id: 'id_test',
    name: 'name_test',
    requires_redirect_uris: true,
    description: 'description_test',
    allowed_grant_types: [
      {
        type: 'allowed_type',
        name: 'allowed_name',
        response_types: ['allowed_response_type'],
      },
    ],
    default_grant_types: [
      {
        type: 'default_type',
        name: 'default_name',
        response_types: ['default_response_type'],
      },
    ],
    mandatory_grant_types: [
      {
        type: 'mandatory_type',
        name: 'mandatory_name',
        response_types: ['mandatory_response_type'],
      },
    ],
  };

  return {
    ...base,
    ...attributes,
  };
}
