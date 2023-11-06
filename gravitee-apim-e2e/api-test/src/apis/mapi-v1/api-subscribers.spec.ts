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
import { beforeAll, describe, expect, test } from '@jest/globals';
import { succeed } from '@lib/jest-utils';
import { APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { forManagementAsAdminUser, forPortalAsAdminUser } from '@gravitee/utils/configuration';
import { ApiEntity, UpdateApiEntityFromJSON } from '@gravitee/management-webclient-sdk/src/lib/models';
import { APISubscriptionsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APISubscriptionsApi';
import { ApiApi } from '@gravitee/portal-webclient-sdk/src/lib';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

// Portal resources
const portalApisAsAdminUser = new ApiApi(forPortalAsAdminUser());

// Management resources
const apisAsAdmin = new APIsApi(forManagementAsAdminUser());
const managementApiSubscriptionsAsAdminUser = new APISubscriptionsApi(forManagementAsAdminUser());

let createdApi: ApiEntity;

describe('Api subscribers tests', () => {
  beforeAll(async () => {
    // create an api
    createdApi = await apisAsAdmin.createApi({
      orgId,
      envId,
      newApiEntity: ApisFaker.newApi(),
    });

    // publish an api
    await apisAsAdmin.updateApi({
      api: createdApi.id,
      updateApiEntity: UpdateApiEntityFromJSON({
        ...createdApi,
        lifecycle_state: 'PUBLISHED',
        visibility: 'PUBLIC',
      }),
      orgId,
      envId,
    });
  });

  describe('GET subscribers', () => {
    test('should not get any subscriber from portal resource', async () => {
      const apiSubscribers = await succeed(
        portalApisAsAdminUser.getSubscriberApplicationsByApiIdRaw({
          apiId: createdApi.id,
        }),
      );

      expect(apiSubscribers).toBeTruthy();
      expect(apiSubscribers.data).toHaveLength(0);
    });

    test('should not get any subscriber from management resource', async () => {
      const apiSubscribers = await succeed(
        managementApiSubscriptionsAsAdminUser.getApiSubscribersRaw({
          api: createdApi.id,
          envId,
          orgId,
        }),
      );

      expect(apiSubscribers).toBeTruthy();
      expect(apiSubscribers).toHaveLength(0);
    });
  });
});
