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
import { forManagementAsApiUser, forPortal } from '@client-conf/*';
import { afterAll, beforeAll, describe, expect, it } from '@jest/globals';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ApiApi } from '@portal-apis/ApiApi';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceUser = new APIsApi(forManagementAsApiUser());
const apiResource = new ApiApi(forPortal());

let userApi;

describe('API - Publishing', () => {
  describe('Not published', () => {
    beforeAll(async () => {
      userApi = await apisResourceUser.createApi({
        orgId,
        envId,
        newApiEntity: ApisFaker.newApi(),
      });
      return userApi;
    });

    afterAll(async () => {
      return await apisResourceUser.deleteApi({ orgId, envId, api: userApi.id });
    });

    describe('As ANONYMOUS user', () => {
      it('Get APIs should not contain created api', async () => {
        const apisResponse = await apiResource.getApis({});
        expect(apisResponse.data.find((api) => api.id === userApi.id)).not.toBeDefined();
      });
    });
  });
});
