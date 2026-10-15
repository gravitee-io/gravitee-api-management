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
import { afterAll, beforeAll, describe, expect, test } from '@jest/globals';
import { ApiEntity } from '@gravitee/management-webclient-sdk/src/lib/models/ApiEntity';
import { ApiLifecycleState } from '@gravitee/management-webclient-sdk/src/lib/models/ApiLifecycleState';
import { PlanSecurityType } from '@gravitee/management-webclient-sdk/src/lib/models/PlanSecurityType';
import { PlanStatus } from '@gravitee/management-webclient-sdk/src/lib/models/PlanStatus';
import { Visibility } from '@gravitee/management-webclient-sdk/src/lib/models/Visibility';
import { APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { CurrentUserApi } from '@gravitee/management-webclient-sdk/src/lib/apis/CurrentUserApi';
import { APIMembersApi } from '@gravitee/management-v2-webclient-sdk/src/lib';
import { Application } from '@gravitee/portal-webclient-sdk/src/lib/models/Application';
import { Subscription as PortalSubscription } from '@gravitee/portal-webclient-sdk/src/lib/models/Subscription';
import { ApplicationApi } from '@gravitee/portal-webclient-sdk/src/lib/apis/ApplicationApi';
import { SubscriptionApi } from '@gravitee/portal-webclient-sdk/src/lib/apis/SubscriptionApi';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { PortalApplicationFaker } from '@gravitee/fixtures/portal/PortalApplicationFaker';
import {
  forManagementAsApiUser,
  forManagementAsAppUser,
  forManagementV2AsApiUser,
  forPortalAsAppUser,
} from '@gravitee/utils/configuration';
import { teardownApisAndApplications } from '@gravitee/utils/management';
import { notFound, succeed } from '@lib/jest-utils';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResource = new APIsApi(forManagementAsApiUser());
const apiMembersResource = new APIMembersApi(forManagementV2AsApiUser());
const currentUserResourceAsAppUser = new CurrentUserApi(forManagementAsAppUser());
const portalApplicationResource = new ApplicationApi(forPortalAsAppUser());
const portalSubscriptionResource = new SubscriptionApi(forPortalAsAppUser());

describe('Subscribe from the portal to a private API', () => {
  let createdApi: ApiEntity;
  let createdApplication: Application;
  let createdSubscription: PortalSubscription;

  beforeAll(async () => {
    // a private API, published to the catalog, with a published API Key plan
    createdApi = await apisResource.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport({
        visibility: Visibility.PRIVATE,
        plans: [PlansFaker.plan({ security: PlanSecurityType.API_KEY, status: PlanStatus.PUBLISHED })],
      }),
    });

    await apisResource.updateApi({
      envId,
      orgId,
      api: createdApi.id,
      updateApiEntity: {
        lifecycle_state: ApiLifecycleState.PUBLISHED,
        description: createdApi.description,
        name: createdApi.name,
        proxy: createdApi.proxy,
        version: createdApi.version,
        visibility: Visibility.PRIVATE,
      },
    });

    createdApplication = await portalApplicationResource.createApplication({
      applicationInput: PortalApplicationFaker.newApplicationInput(),
    });
  });

  test('should answer 404 when the user is not a member of the API', async () => {
    await notFound(
      portalSubscriptionResource.createSubscriptionRaw({
        subscriptionInput: {
          application: createdApplication.id,
          plan: createdApi.plans[0].id,
        },
      }),
    );
  });

  test('should subscribe once the user is a member of the API', async () => {
    const appUser = await currentUserResourceAsAppUser.getCurrentUser({ orgId });

    await apiMembersResource.addApiMember({
      envId,
      apiId: createdApi.id,
      addMember: {
        userId: appUser.id,
        roleName: 'USER',
      },
    });

    createdSubscription = await succeed(
      portalSubscriptionResource.createSubscriptionRaw({
        subscriptionInput: {
          application: createdApplication.id,
          plan: createdApi.plans[0].id,
        },
      }),
    );
    expect(createdSubscription.id).toBeTruthy();
  });

  afterAll(async () => {
    if (createdApplication) {
      await portalApplicationResource.deleteApplicationByApplicationId({ applicationId: createdApplication.id });
    }
    await teardownApisAndApplications(orgId, envId, createdApi ? [createdApi.id] : [], []);
  });
});
