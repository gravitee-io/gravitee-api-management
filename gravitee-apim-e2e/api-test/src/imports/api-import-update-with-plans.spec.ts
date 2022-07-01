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
import { afterAll, describe, expect, test } from '@jest/globals';

import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsAdminUser } from '@client-conf/*';
import { succeed } from '@lib/jest-utils';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PlanStatus } from '@management-models/PlanStatus';
import { PlanSecurityType } from '@management-models/PlanSecurityType';
import { PlanType } from '@management-models/PlanType';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { PlanValidationType } from '@management-models/PlanValidationType';

const apisResource = new APIsApi(forManagementAsAdminUser());

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

describe('Update API by importing it', () => {
  describe('Update API from import with plans', () => {
    describe('Update API with plans without ID', () => {
      const expectedApiId = '2ce4fa7c-8c75-31a2-83a9-73ccc6773b13';

      const fakePlan1 = PlansFaker.aPlan({ name: 'test plan 1', description: 'this is a test plan', order: 1 });
      const fakePlan2 = PlansFaker.aPlan({ name: 'test plan 2', description: 'this is a test plan', order: 2 });
      const fakeApi = ApisFaker.apiImport({
        id: '08a92f8c-e133-42ec-a92f-8ce13382ec73',
      });

      // this update API, creating 2 plans
      const updatedFakeApi = ApisFaker.apiImport({ id: expectedApiId, plans: [fakePlan1, fakePlan2] });

      test('should create the API', async () => {
        await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      });

      test('should update the API', async () => {
        await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: updatedFakeApi }));
      });

      test('should get 2 plans created on API', async () => {
        let plans = await succeed(apisResource.getApiPlansRaw({ envId, orgId, api: expectedApiId, status: [PlanStatus.STAGING] }));

        expect(plans).toHaveLength(2);
        expect(plans[0]).toEqual(
          expect.objectContaining({
            description: 'this is a test plan',
            validation: 'AUTO',
            security: PlanSecurityType.KEYLESS,
            type: PlanType.API,
            status: PlanStatus.STAGING,
            order: 1,
          }),
        );
        expect(plans[1]).toEqual(
          expect.objectContaining({
            description: 'this is a test plan',
            validation: 'AUTO',
            security: PlanSecurityType.KEYLESS,
            type: PlanType.API,
            status: PlanStatus.STAGING,
            order: 2,
          }),
        );
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      });
    });

    describe('Update API with plans with ID', () => {
      const expectedApiId = 'e4998d06-6518-316e-a47b-e5112498c718';

      const fakePlan1 = PlansFaker.aPlan({
        id: '08a92f8c-e133-42ec-a92f-8ce139999999',
        name: 'test plan 1',
        description: 'this is a test plan',
        status: PlanStatus.CLOSED,
      });
      const fakePlan2 = PlansFaker.aPlan({
        id: '08a92f8c-e133-42ec-a92f-8ce138888888',
        name: 'test plan 2',
        description: 'this is a test plan',
        status: PlanStatus.CLOSED,
      });
      const fakeApi = ApisFaker.apiImport({ id: '6d94ad00-2878-44bc-aacc-5d9c2ac35034' });

      // this update API, creating 2 plans
      const updatedFakeApi = ApisFaker.apiImport({ id: expectedApiId, plans: [fakePlan1, fakePlan2] });

      test('should create the API', async () => {
        await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      });

      test('should update the API', async () => {
        await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: updatedFakeApi }));
      });

      test('should get 2 plans created on API, with specified status', async () => {
        let plans = await succeed(apisResource.getApiPlansRaw({ envId, orgId, api: expectedApiId, status: [PlanStatus.CLOSED] }));
        expect(plans).toHaveLength(2);
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      });
    });

    describe('Update API with plan without ID matching name of one existing plan', () => {
      const expectedApiId = '8f2ef0a4-27ca-3373-ba0c-c24b81f35ce0';

      const fakePlan1 = PlansFaker.aPlan({ name: 'test plan', description: 'this is a test plan' });
      const fakeApi = ApisFaker.apiImport({ id: 'd166c30a-0500-40a0-b414-a4853dc4bad8', plans: [fakePlan1] });

      // this update will update the plan of the existing API, cause it has the same name
      const updateFakePlan = PlansFaker.aPlan({ name: 'test plan', description: 'this is the updated description' });
      const updatedFakeApi = ApisFaker.apiImport({ id: expectedApiId, plans: [updateFakePlan] });

      test('should create the API', async () => {
        await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      });

      test('should update the API', async () => {
        await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: updatedFakeApi }));
      });

      test('should get the API plan, which has been updated', async () => {
        let plans = await succeed(apisResource.getApiPlansRaw({ envId, orgId, api: expectedApiId, status: [PlanStatus.STAGING] }));
        expect(plans).toHaveLength(1);
        expect(plans[0].description).toBe('this is the updated description');
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      });
    });

    describe('Update API with plan with missing data, from gateway event (aka rollback API feature)', () => {
      let createdApi;

      const existingPlan = PlansFaker.plan({
        crossId: 'my-plan',
        status: PlanStatus.STAGING,
        security: PlanSecurityType.APIKEY,
        validation: PlanValidationType.AUTO,
        name: 'my old plan name',
        description: 'my old plan description',
      });

      const updatePlanWithMissingData = PlansFaker.plan({
        crossId: 'my-plan',
        name: 'my new plan name',
        security: PlanSecurityType.APIKEY,
      });
      // plan from gateway event misses for example, validation, and description
      delete updatePlanWithMissingData.validation;
      delete updatePlanWithMissingData.description;

      const existingApi = ApisFaker.apiImport({ crossId: 'my-api', plans: [existingPlan] });
      const updatedApi = ApisFaker.apiImport({ crossId: 'my-api', plans: [updatePlanWithMissingData] });

      test('should create the API', async () => {
        createdApi = await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: existingApi }));
      });

      test('should update the API', async () => {
        await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: createdApi.id, body: updatedApi }));
      });

      test('should get 1 plan on API : existing data have been updated, and missing data are kept unchanged', async () => {
        let plans = await succeed(apisResource.getApiPlansRaw({ envId, orgId, api: createdApi.id, status: [PlanStatus.STAGING] }));
        expect(plans).toHaveLength(1);
        expect(plans[0].name).toBe('my new plan name');
        expect(plans[0].description).toBe('my old plan description');
        expect(plans[0].validation).toBe(PlanValidationType.AUTO);
        expect(plans[0].security).toBe(PlanSecurityType.APIKEY);
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: createdApi.id });
      });
    });

    describe('Update API with missing plans from already existing API', () => {
      // existing API contains 2 plans
      const expectedApiId = '3719057e-f218-3f28-a90f-10313fced230';

      const fakePlan1 = PlansFaker.aPlan({ name: 'test plan 1' });
      const fakePlan2 = PlansFaker.aPlan({ name: 'test plan 2' });
      const fakeApi = ApisFaker.apiImport({ id: '492eb123-a635-40a7-9438-fde72f11837e', plans: [fakePlan1, fakePlan2] });

      // update API contains 1 other plan
      const updateFakePlan = PlansFaker.aPlan({ name: 'test plan 3' });
      const updatedFakeApi = ApisFaker.apiImport({ id: expectedApiId, plans: [updateFakePlan] });

      test('should create the API', async () => {
        await succeed(apisResource.importApiDefinitionRaw({ envId, orgId, body: fakeApi }));
      });

      test('should update the API', async () => {
        await succeed(apisResource.updateApiWithDefinitionRaw({ envId, orgId, api: expectedApiId, body: updatedFakeApi }));
      });

      test('should get the API plan, containing only the plan that was in the update', async () => {
        let plans = await succeed(apisResource.getApiPlansRaw({ envId, orgId, api: expectedApiId, status: [PlanStatus.STAGING] }));
        expect(plans).toHaveLength(1);
        expect(plans[0].name).toBe('test plan 3');
      });

      afterAll(async () => {
        await apisResource.deleteApi({ envId, orgId, api: expectedApiId });
      });
    });
  });
});
