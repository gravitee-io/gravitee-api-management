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
import {
  forManagementAsAdminUser,
  forManagementAsApiUser,
  forManagementAsAppUser,
  forPortalAsAppUser,
  forPortalAsSimpleUser,
} from '@client-conf/*';
import { afterAll, beforeAll, describe, expect } from '@jest/globals';
import { APIsApi } from '@management-apis/APIsApi';
import { ApiEntity } from '@management-models/ApiEntity';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { Visibility } from '@management-models/Visibility';
import { GroupsFaker } from '@management-fakers/GroupsFaker';
import { SearchableUser } from '@management-models/SearchableUser';
import { RoleScope } from '@management-models/RoleScope';
import { GroupEntity } from '@management-models/GroupEntity';
import { ConfigurationApi } from '@management-apis/ConfigurationApi';
import { UsersApi } from '@management-apis/UsersApi';
import { find, cloneDeep } from 'lodash';
import { ApplicationsApi } from '@management-apis/ApplicationsApi';
import { ApplicationsFaker } from '@management-fakers/ApplicationsFaker';
import { ApplicationEntity } from '@management-models/ApplicationEntity';
import { ApiApi } from '@portal-apis/ApiApi';
import { succeed } from '@lib/jest-utils';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanEntity } from '@management-models/PlanEntity';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { PlanStatus } from '@management-models/PlanStatus';
import { SubscriptionApi } from '@portal-apis/SubscriptionApi';
import { Subscription } from '@portal-models/Subscription';
import { APISubscriptionsApi } from '@management-apis/APISubscriptionsApi';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanValidationType } from '@management-models/PlanValidationType';
import { UpdatePlanEntity } from '@management-models/UpdatePlanEntity';
import { SubscriptionStatus } from '@management-models/SubscriptionStatus';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

// admin resources
const configurationResourceAsAdmin = new ConfigurationApi(forManagementAsAdminUser());
const usersResourceAsAdmin = new UsersApi(forManagementAsAdminUser());

// api publisher resources
const apisResourceAsApiPublisher = new APIsApi(forManagementAsApiUser());
const apiPlansResourceAsApiPublisher = new APIPlansApi(forManagementAsApiUser());
const apiSubscriptionsResourceAsApiPublisher = new APISubscriptionsApi(forManagementAsApiUser());

// api consumer resources
const applicationsResourceAsApiConsumer = new ApplicationsApi(forManagementAsAppUser());
const portalApiResourceAsApiConsumer = new ApiApi(forPortalAsAppUser());
const portalSubscriptionResourceAsApiConsumer = new SubscriptionApi(forPortalAsAppUser());

// simple user resources
const portalApiResourceAsSimpleUser = new ApiApi(forPortalAsSimpleUser());

let createdApi: ApiEntity;
let createdPlan: PlanEntity;
let updatedPlan: UpdatePlanEntity;
let createdApplication: ApplicationEntity;
let createdSubscription: Subscription;
let excludedGroup: GroupEntity;
let userMember: SearchableUser;

describe('API - Plan', () => {
  beforeAll(async () => {
    // create an API
    createdApi = await apisResourceAsApiPublisher.importApiDefinition({
      envId,
      orgId,
      body: ApisFaker.apiImport(),
    });

    // publish API
    await apisResourceAsApiPublisher.updateApi({
      envId,
      orgId,
      api: createdApi.id,
      updateApiEntity: UpdateApiEntityFromJSON({
        ...createdApi,
        lifecycle_state: ApiLifecycleState.PUBLISHED,
        visibility: Visibility.PUBLIC,
      }),
    });

    // create EXCLUDED group
    excludedGroup = await configurationResourceAsAdmin.createGroup({
      envId,
      orgId,
      newGroupEntity: GroupsFaker.newGroup({ event_rules: [{ event: 'API_CREATE' }] }),
    });

    // get user member
    const users: SearchableUser[] = await usersResourceAsAdmin.searchUsers({
      envId,
      orgId,
      q: process.env.SIMPLE_USERNAME,
    });
    userMember = find(users, (user) => user.displayName === process.env.SIMPLE_USERNAME);

    // add EXCLUDED member to group
    await configurationResourceAsAdmin.addOrUpdateGroupMember({
      envId,
      orgId,
      group: excludedGroup.id,
      groupMembership: [
        {
          reference: userMember.reference,
          roles: [
            {
              scope: RoleScope.API,
              name: 'REVIEWER',
            },
            {
              scope: RoleScope.APPLICATION,
              name: 'USER',
            },
          ],
        },
      ],
    });

    // create application for subscription
    createdApplication = await applicationsResourceAsApiConsumer.createApplication({
      envId,
      orgId,
      newApplicationEntity: ApplicationsFaker.newApplication(),
    });
  });

  describe('API consumer checks initial situation on portal', () => {
    test('Get API plans should return empty', async () => {
      let plans = await succeed(portalApiResourceAsApiConsumer.getApiPlansByApiIdRaw({ apiId: createdApi.id }));

      expect(plans).toBeTruthy();
      expect(plans.data).toBeTruthy();
      expect(plans.data.length).toBe(0);
    });
  });

  describe('API publisher creates plan', () => {
    test('Create plan should succeed', async () => {
      createdPlan = await succeed(
        apisResourceAsApiPublisher.createApiPlanRaw({
          envId,
          orgId,
          api: createdApi.id,
          newPlanEntity: PlansFaker.newPlan({ security: PlanSecurityType.APIKEY, validation: PlanValidationType.MANUAL }),
        }),
        201,
      );
    });

    test('API consumer should not see plan in portal yet', async () => {
      let plans = await succeed(portalApiResourceAsApiConsumer.getApiPlansByApiIdRaw({ apiId: createdApi.id }));

      expect(plans).toBeTruthy();
      expect(plans.data).toBeTruthy();
      expect(plans.data.length).toBe(0);
    });
  });

  describe('Publish plan, accept and create subscription', () => {
    test('API publisher publishes plan from management API', async () => {
      let publishedPlan = await succeed(
        apiPlansResourceAsApiPublisher.publishApiPlanRaw({ envId, orgId, plan: createdPlan.id, api: createdApi.id }),
      );

      expect(publishedPlan).toBeTruthy();
      expect(publishedPlan.status).toBe(PlanStatus.PUBLISHED);
    });

    test('API consumer creates subscription from portal API', async () => {
      createdSubscription = await succeed(
        portalSubscriptionResourceAsApiConsumer.createSubscriptionRaw({
          subscriptionInput: {
            application: createdApplication.id,
            plan: createdPlan.id,
          },
        }),
      );
    });

    test('API publisher accepts subscription from management API', async () => {
      await succeed(
        apiSubscriptionsResourceAsApiPublisher.processApiSubscriptionRaw({
          envId,
          orgId,
          subscription: createdSubscription.id,
          api: createdApi.id,
          processSubscriptionEntity: {
            accepted: true,
          },
        }),
      );
    });

    test('API consumer should see published plan in portal now', async () => {
      let plans = await succeed(portalApiResourceAsApiConsumer.getApiPlansByApiIdRaw({ apiId: createdApi.id }));
      expect(plans).toBeTruthy();
      expect(plans.data).toBeTruthy();
      expect(plans.data.length).toBeGreaterThan(0);

      let foundPlan = find(plans.data, (plan) => plan.id === createdPlan.id);
      expect(foundPlan).toBeTruthy();
    });
  });

  describe('Exclude group', () => {
    test('Simple user should see plan in portal', async () => {
      let plans = await succeed(portalApiResourceAsSimpleUser.getApiPlansByApiIdRaw({ apiId: createdApi.id }));
      expect(plans).toBeTruthy();
      expect(plans.data).toBeTruthy();
      expect(plans.data.length).toBeGreaterThan(0);

      let foundPlan = find(plans.data, (plan) => plan.id === createdPlan.id);
      expect(foundPlan).toBeTruthy();
    });

    test('Api publisher excludes simple user group from plan', async () => {
      updatedPlan = { ...cloneDeep(createdPlan), order: 1, excluded_groups: [excludedGroup.id] };

      await succeed(
        apiPlansResourceAsApiPublisher.updateApiPlanRaw({
          envId,
          orgId,
          plan: createdPlan.id,
          api: createdApi.id,
          updatePlanEntity: updatedPlan,
        }),
      );
    });

    test('Simple user should no more see plan in portal', async () => {
      let plans = await succeed(portalApiResourceAsSimpleUser.getApiPlansByApiIdRaw({ apiId: createdApi.id }));

      expect(plans).toBeTruthy();
      expect(plans.data).toBeTruthy();
      expect(plans.data.length).toBe(0);
    });

    test('Api publisher cancel group exclusion', async () => {
      updatedPlan.excluded_groups = null;

      await succeed(
        apiPlansResourceAsApiPublisher.updateApiPlanRaw({
          envId,
          orgId,
          plan: createdPlan.id,
          api: createdApi.id,
          updatePlanEntity: updatedPlan,
        }),
      );
    });

    test('Simple user should see again the plan in portal', async () => {
      let plans = await succeed(portalApiResourceAsSimpleUser.getApiPlansByApiIdRaw({ apiId: createdApi.id }));
      expect(plans).toBeTruthy();
      expect(plans.data).toBeTruthy();
      expect(plans.data.length).toBeGreaterThan(0);

      let foundPlan = find(plans.data, (plan) => plan.id === createdPlan.id);
      expect(foundPlan).toBeTruthy();
    });
  });

  describe('Deprecate plan', () => {
    test('API publisher should deprecate plan', async () => {
      let deprecatedPlan = await succeed(
        apiPlansResourceAsApiPublisher.deprecateApiPlanRaw({
          envId,
          orgId,
          plan: createdPlan.id,
          api: createdApi.id,
        }),
      );

      expect(deprecatedPlan).toBeTruthy();
      expect(deprecatedPlan.status).toBe(PlanStatus.DEPRECATED);
    });

    test('API consumer should not see the deprecated plan in portal', async () => {
      let plans = await succeed(portalApiResourceAsApiConsumer.getApiPlansByApiIdRaw({ apiId: createdApi.id }));

      expect(plans).toBeTruthy();
      expect(plans.data).toBeTruthy();
      expect(plans.data.length).toBe(0);
    });
  });

  describe('Get subscription', () => {
    test('API publisher should get subscription', async () => {
      let foundSubscriptions = await succeed(
        apiSubscriptionsResourceAsApiPublisher.getApiSubscriptionsRaw({
          envId,
          orgId,
          api: createdApi.id,
          plan: [createdPlan.id],
          status: [SubscriptionStatus.ACCEPTED, SubscriptionStatus.PENDING, SubscriptionStatus.REJECTED, SubscriptionStatus.CLOSED],
        }),
      );

      expect(foundSubscriptions.data).toBeTruthy();
      expect(foundSubscriptions.data.length).toBeGreaterThan(0);

      let foundSubscription = find(foundSubscriptions.data, (sub) => sub.id === createdSubscription.id);
      expect(foundSubscription).toBeTruthy();
      expect(foundSubscription.api).toBe(createdApi.id);
      expect(foundSubscription.application).toBe(createdApplication.id);
      expect(foundSubscription.plan).toBe(createdPlan.id);
    });
  });

  describe('Close plan', () => {
    test('API publisher should close plan', async () => {
      let deprecatedPlan = await succeed(
        apiPlansResourceAsApiPublisher.closeApiPlanRaw({
          envId,
          orgId,
          plan: createdPlan.id,
          api: createdApi.id,
        }),
      );

      expect(deprecatedPlan).toBeTruthy();
      expect(deprecatedPlan.status).toBe(PlanStatus.CLOSED);
    });

    test('API consumer should not see the closed plan in portal', async () => {
      let plans = await succeed(portalApiResourceAsApiConsumer.getApiPlansByApiIdRaw({ apiId: createdApi.id }));

      expect(plans).toBeTruthy();
      expect(plans.data).toBeTruthy();
      expect(plans.data.length).toBe(0);
    });
  });

  afterAll(async () => {
    // delete created application
    if (createdApplication) {
      await applicationsResourceAsApiConsumer.deleteApplication({ orgId, envId, application: createdApplication.id });
    }

    // delete created API
    if (createdApi) {
      await apisResourceAsApiPublisher.deleteApi({ orgId, envId, api: createdApi.id });
    }

    // delete EXCLUDED group
    if (excludedGroup) {
      await configurationResourceAsAdmin.deleteGroup({ envId, orgId, group: excludedGroup.id });
    }
  });
});
