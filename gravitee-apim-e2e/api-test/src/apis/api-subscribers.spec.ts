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
import {
  forManagementAsAdminUser,
  forManagementAsApiUser,
  forManagementAsAppUser,
  forPortalAsAdminUser,
  forPortalAsAppUser,
} from '@client-conf/*';
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ApiEntity } from '@management-models/ApiEntity';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { ApplicationApi as PortalApplicationApi } from '@portal-apis/ApplicationApi';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { ApplicationsFaker } from '@management-fakers/ApplicationsFaker';
import fetchApi from 'node-fetch';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanValidationType } from '@management-models/PlanValidationType';
import { PlanEntity } from '@management-models/PlanEntity';
import { Api } from '@portal-models/Api';
import { ApplicationEntity } from '@management-models/ApplicationEntity';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { ApiApi } from '@portal-apis/ApiApi';
import { Visibility } from '@management-models/Visibility';
import { succeed } from '@lib/jest-utils';
import { GetSubscriptionByIdIncludeEnum, SubscriptionApi } from '@portal-apis/SubscriptionApi';
import faker from '@faker-js/faker';
import { Subscription } from '@portal-models/Subscription';
import { APISubscriptionsApi } from '@management-apis/APISubscriptionsApi';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';

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
        lifecycle_state: ApiLifecycleState.PUBLISHED,
        visibility: Visibility.PUBLIC,
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
