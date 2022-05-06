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
import { APIsApi } from '@management-apis/APIsApi';
import { ApiApi } from '@portal-apis/ApiApi';
import { forManagementAsApiUser, forManagementWithWrongPassword, forPortalAsApiUser, forPortalWithWrongPassword } from '@client-conf/*';
import { describe } from '@jest/globals';
import { fail, succeed } from '../../lib/jest-utils';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const managementApisResourceAsApiUser = new APIsApi(forManagementAsApiUser());
const managementApisResourceAsWrongUser = new APIsApi(forManagementWithWrongPassword());

const portalApiResourceAsApiUser = new ApiApi(forPortalAsApiUser());
const portalApiResourceAsWrongUser = new ApiApi(forPortalWithWrongPassword());

describe('Basic authentication', () => {
  describe('Management API', () => {
    test('Valid auth should return 200 OK', async () => {
      await succeed(managementApisResourceAsApiUser.getApisRaw({ envId, orgId }));
    });

    test('Invalid auth should return 401 unauthorized', async () => {
      await fail(managementApisResourceAsWrongUser.getApis({ envId, orgId }), 401);
    });
  });

  describe('Portal API', () => {
    test('Valid auth should return 200 OK', async () => {
      await succeed(portalApiResourceAsApiUser.getApisRaw({}));
    });

    test('Invalid auth should return 401 unauthorized', async () => {
      await fail(portalApiResourceAsWrongUser.getApis({}), 401);
    });
  });
});
