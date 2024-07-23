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

import { Application, ApplicationsResponse } from './application';

export function fakeApplication(modifier?: Partial<Application> | ((baseApplication: Application) => Application)): Application {
  const base: Application = {
    id: 'b62f6e82-c39b-4edb-af6e-82c39b9edb46',
    name: 'AMO - OAuth2 with DCR',
    description: 'ezaf',
    applicationType: 'BACKEND_TO_BACKEND',
    hasClientId: true,
    owner: {
      id: '4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c',
      first_name: 'Admin',
      last_name: 'master',
      display_name: 'Admin master',
      email: 'no-reply@graviteesource.com',
      editable_profile: true,
      customFields: {
        city: 'zz',
        job_position: '1',
      },
      _links: {
        self: 'https://apim-master-api.team-apim.gravitee.dev/portal/environments/DEFAULT/users/4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c',
        avatar:
          'https://apim-master-api.team-apim.gravitee.dev/portal/environments/DEFAULT/users/4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c/avatar?1718186476114',
        notifications:
          'https://apim-master-api.team-apim.gravitee.dev/portal/environments/DEFAULT/users/4015f9f2-c0a4-4c0c-95f9-f2c0a4fc0c4c/notifications',
      },
    },
    created_at: '2022-05-03T12:36:08.85Z',
    groups: [
      {
        id: '1001f3a8-a0c8-4904-81f3-a8a0c81904bc',
        name: 'All App',
      },
    ],
    api_key_mode: 'SHARED',
    settings: {
      oauth: {
        client_secret: '3zEYOXPqqCyaq7os--Nf1-6jrHjL0AjumFz4CL78nwQ',
        client_id: 'zLgNDMUCbbCBDNnpBGb-WOV_lNrUlQlAlUiSditR9Es',
        redirect_uris: [],
        response_types: [],
        grant_types: ['client_credentials'],
        application_type: 'backend_to_backend',
        renew_client_secret_supported: false,
      },
      updated_at: '2022-05-03T12:36:08.85Z',
    },
    _links: {
      self: 'https://apim-master-api.team-apim.gravitee.dev/portal/environments/DEFAULT/applications/b62f6e82-c39b-4edb-af6e-82c39b9edb46',
      members:
        'https://apim-master-api.team-apim.gravitee.dev/portal/environments/DEFAULT/applications/b62f6e82-c39b-4edb-af6e-82c39b9edb46/members',
      notifications:
        'https://apim-master-api.team-apim.gravitee.dev/portal/environments/DEFAULT/applications/b62f6e82-c39b-4edb-af6e-82c39b9edb46/notifications',
      picture:
        'https://apim-master-api.team-apim.gravitee.dev/portal/environments/DEFAULT/applications/b62f6e82-c39b-4edb-af6e-82c39b9edb46/picture?1108291712',
      background:
        'https://apim-master-api.team-apim.gravitee.dev/portal/environments/DEFAULT/applications/b62f6e82-c39b-4edb-af6e-82c39b9edb46/background?1108291712',
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
export function fakeApplicationsResponse(
  modifier?: Partial<ApplicationsResponse> | ((baseApplication: ApplicationsResponse) => ApplicationsResponse),
): ApplicationsResponse {
  const base: ApplicationsResponse = {
    data: [fakeApplication()],
    metadata: {
      pagination: {
        current_page: 1,
        first: 1,
        last: 9,
        size: 9,
        total: 145,
        total_pages: 17,
      },
    },
    _links: {},
  };

  if (isFunction(modifier)) {
    return modifier(base);
  }

  return {
    ...base,
    ...modifier,
  };
}
