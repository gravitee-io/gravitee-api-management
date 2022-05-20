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
import { describe, expect, test } from '@jest/globals';
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsAdminUser, forManagementAsSimpleUser } from '@client-conf/*';
import { created, fail, forbidden, noContent, notFound, succeed } from '@lib/jest-utils';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { ApiEntity, ApiEntityStateEnum } from '@management-models/ApiEntity';
import { ApiLifecycleState } from '@management-models/ApiLifecycleState';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanValidationType } from '@management-models/PlanValidationType';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { PlanStatus } from '@management-models/PlanStatus';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanEntity } from '@management-models/PlanEntity';
import { Visibility } from '@management-models/Visibility';

const apisManagementApiAsAdmin = new APIsApi(forManagementAsAdminUser());
const apiPlansManagementApiAsAdmin = new APIPlansApi(forManagementAsAdminUser());
const apisManagementApiAsSimpleUser = new APIsApi(forManagementAsSimpleUser());
const orgId = 'DEFAULT';
const envId = 'DEFAULT';

describe('API workflow', () => {
  let createdApi: ApiEntity;
  let createdPlan: PlanEntity;

  describe('API', () => {
    test('should create an API', async () => {
      createdApi = await created(
        apisManagementApiAsAdmin.createApiRaw({
          orgId,
          envId,
          newApiEntity: ApisFaker.newApi(),
        }),
      );
      expect(createdApi.state).toEqual(ApiEntityStateEnum.STOPPED);
      expect(createdApi.visibility).toEqual(Visibility.PRIVATE);
      expect(createdApi.lifecycle_state).toEqual(ApiLifecycleState.CREATED);
    });

    test('should fail to create an API as simple user', async () => {
      await forbidden(
        apisManagementApiAsSimpleUser.createApiRaw({
          orgId,
          envId,
          newApiEntity: ApisFaker.newApi(),
        }),
      );
    });

    test('should publish an API', async () => {
      const updateApiEntity = UpdateApiEntityFromJSON({
        ...createdApi,
        visibility: 'PUBLIC',
        lifecycle_state: ApiLifecycleState.PUBLISHED,
      });
      await succeed(
        apisManagementApiAsAdmin.updateApiRaw({
          orgId,
          envId,
          api: createdApi.id,
          updateApiEntity,
        }),
      );
    });

    test('should start an API', async () => {
      await noContent(
        apisManagementApiAsAdmin.doApiLifecycleActionRaw({
          orgId,
          envId,
          api: createdApi.id,
          action: LifecycleAction.START,
        }),
      );
    });

    test('should deploy an API', async () => {
      await succeed(apisManagementApiAsAdmin.deployApiRaw({ orgId, envId, api: createdApi.id }));
    });
  });

  describe('Plan', () => {
    test('should create a keyless plan', async () => {
      createdPlan = await created(
        apiPlansManagementApiAsAdmin.createApiPlanRaw({
          envId,
          orgId,
          api: createdApi.id,
          newPlanEntity: PlansFaker.newPlan({
            security: PlanSecurityType.KEYLESS,
            validation: PlanValidationType.MANUAL,
          }),
        }),
      );
      expect(createdPlan.status).toEqual(PlanStatus.STAGING);
      expect(createdPlan.security).toEqual(PlanSecurityType.KEYLESS);
    });

    test('should publish a plan ', async () => {
      await succeed(
        apiPlansManagementApiAsAdmin.publishApiPlanRaw({
          orgId,
          envId,
          api: createdApi.id,
          plan: createdPlan.id,
        }),
      );
    });
  });

  describe('Stop an API', () => {
    test('should fail to delete an API not stopped', async () => {
      await fail(
        apisManagementApiAsAdmin.deleteApiRaw({ orgId, envId, api: createdApi.id }),
        400,
        `API [${createdApi.id}] is still running and must be stopped before being deleted !`,
      );
    });

    test('should stop an API', async () => {
      await noContent(
        apisManagementApiAsAdmin.doApiLifecycleActionRaw({
          orgId,
          envId,
          api: createdApi.id,
          action: LifecycleAction.STOP,
        }),
      );
    });
  });

  describe('Delete a plan', () => {
    test('should fail to delete a stopped API with published plan', async () => {
      await fail(
        apisManagementApiAsAdmin.deleteApiRaw({ orgId, envId, api: createdApi.id }),
        400,
        `Plan(s) [${createdPlan.name}] must be closed before being able to delete the API !`,
      );
    });

    test('should delete a plan', async () => {
      await noContent(apiPlansManagementApiAsAdmin.deleteApiPlanRaw({ orgId, envId, api: createdApi.id, plan: createdPlan.id }));
    });
  });

  describe('Delete an API', () => {
    test('should fail to delete an API as simple user', async () => {
      await forbidden(apisManagementApiAsSimpleUser.deleteApiRaw({ orgId, envId, api: createdApi.id }));
    });

    test('should delete an API', async () => {
      await noContent(apisManagementApiAsAdmin.deleteApiRaw({ orgId, envId, api: createdApi.id }));
    });

    test('should fail to delete a non-existing API', async () => {
      await notFound(apisManagementApiAsAdmin.deleteApiRaw({ orgId, envId, api: '????' }));
    });
  });
});
