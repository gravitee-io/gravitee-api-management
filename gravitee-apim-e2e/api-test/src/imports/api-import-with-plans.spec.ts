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
import { APIsApi } from '@management-apis/APIsApi';
import { forManagementAsAdminUser } from '@client-conf/*';
import { ApisFaker } from '@management-fakers/ApisFaker';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { ApiEntity } from '@management-models/ApiEntity';
import { succeed } from '@lib/jest-utils';
import faker from '@faker-js/faker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apisResourceAsAdminUser = new APIsApi(forManagementAsAdminUser());

const apiId = 'c4ddaa66-4646-4fca-a80b-284aa7407941';
const expectedApiId = '7ab9bd67-5540-396b-91ca-91479994fdd6';
const fakePlan1 = PlansFaker.plan({ name: faker.lorem.words(2), description: faker.lorem.sentence(10) });
const fakePlan2 = PlansFaker.plan({ name: faker.lorem.words(2), description: faker.lorem.sentence(10) });
const fakeApi = ApisFaker.apiImport({ id: apiId, plans: [fakePlan1, fakePlan2] });

let api: ApiEntity;
let planId1: string;
let planId2: string;

describe('API - Imports with plans', () => {
  beforeAll(async () => {
    api = await apisResourceAsAdminUser.importApiDefinition({
      envId,
      orgId,
      body: fakeApi,
    });
    expect(api).toBeTruthy();
    expect(api.plans).toHaveLength(2);
    expect(api.plans[0].id).toBeTruthy();
    expect(api.plans[1].id).toBeTruthy();
    planId1 = api.plans.find((p) => p.name === fakePlan1.name).id;
    planId2 = api.plans.find((p) => p.name === fakePlan2.name).id;
  });

  test('should get plan1 with correct data', async () => {
    const plan = await succeed(apisResourceAsAdminUser.getApiPlanRaw({ orgId, envId, api: expectedApiId, plan: planId1 }));
    expect(plan).toBeTruthy();
    expect(plan.id).toStrictEqual(planId1);
    expect(plan.name).toStrictEqual(fakePlan1.name);
    expect(plan.description).toStrictEqual(fakePlan1.description);
    expect(plan.validation).toStrictEqual(fakePlan1.validation);
    expect(plan.security).toStrictEqual(fakePlan1.security);
    expect(plan.type).toStrictEqual(fakePlan1.type);
    expect(plan.status).toStrictEqual(fakePlan1.status);
    expect(plan.order).toStrictEqual(fakePlan1.order);
  });

  test('should get plan2 with correct data', async () => {
    const plan = await succeed(apisResourceAsAdminUser.getApiPlanRaw({ orgId, envId, api: expectedApiId, plan: planId2 }));
    expect(plan).toBeTruthy();
    expect(plan.id).toStrictEqual(planId2);
    expect(plan.name).toStrictEqual(fakePlan2.name);
    expect(plan.description).toStrictEqual(fakePlan2.description);
    expect(plan.validation).toStrictEqual(fakePlan2.validation);
    expect(plan.security).toStrictEqual(fakePlan2.security);
    expect(plan.type).toStrictEqual(fakePlan2.type);
    expect(plan.status).toStrictEqual(fakePlan2.status);
    expect(plan.order).toStrictEqual(fakePlan2.order);
  });

  afterAll(async () => {
    await apisResourceAsAdminUser.deleteApi({
      envId,
      orgId,
      api: expectedApiId,
    });
  });
});
