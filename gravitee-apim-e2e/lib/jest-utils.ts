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
import { expect } from '@jest/globals';
import * as faker from 'faker';
import { APIsApi, ImportSwaggerApiRequest } from '@management-apis/APIsApi';
import { forManagementAsAdminUser, forManagementAsApiUser } from '@client-conf/*';
import { PlansFaker } from '@management-fakers/PlansFaker';
import { UpdateApiEntityFromJSON } from '@management-models/UpdateApiEntity';
import { APIPlansApi } from '@management-apis/APIPlansApi';
import { LifecycleAction } from '@management-models/LifecycleAction';
import { ApiEntity } from '@management-models/ApiEntity';

export async function fail(promise, expectedStatus: number, expectedMessage?: string) {
  try {
    await promise;
    throw new Error(`The test didn't fail as expected!`);
  } catch (error) {
    expect(error.status).toEqual(expectedStatus);
    if (expectedMessage != null) {
      const { message } = await error.json();
      expect(message).toEqual(expectedMessage);
    }
  }
}

export async function createAndStartApiFromSwagger(
  requestParameters: ImportSwaggerApiRequest,
  externalEndpoint = 'https://api.gravitee.io/echo',
) {
  const { orgId, envId } = requestParameters;
  const apisResourceApiUser = new APIsApi(forManagementAsApiUser());
  const apiPlansResource = new APIPlansApi(forManagementAsApiUser());
  let api = await apisResourceApiUser.importSwaggerApi(requestParameters);

  const name = `swagger_${faker.datatype.number()}`;
  api.name = name;
  api.proxy.virtual_hosts = [{ path: `/${name}` }];
  api.proxy.groups[0].endpoints.forEach((_value, index) => {
    api.proxy.groups[0].endpoints[index].target = externalEndpoint;
  });
  const updateApiEntity = UpdateApiEntityFromJSON(api);
  api = await apisResourceApiUser.updateApi({ orgId, envId, api: api.id, updateApiEntity });

  const newPlanEntity = PlansFaker.newPlan();
  const plan = await apiPlansResource.createApiPlan({ orgId, envId, api: api.id, newPlanEntity });
  await apiPlansResource.publishApiPlan({ orgId, envId, api: api.id, plan: plan.id });
  api = await apisResourceApiUser.deployApi({ orgId, envId, api: api.id });
  await apisResourceApiUser.doApiLifecycleAction({ orgId, envId, api: api.id, action: LifecycleAction.START });
  return api;
}

export async function teardownApi(orgId: string, envId: string, api: ApiEntity) {
  const apiPlansResource = new APIPlansApi(forManagementAsAdminUser());
  const apisResource = new APIsApi(forManagementAsApiUser());
  console.log(`----- Removing API "${api.name}" -----`);
  console.log(`Number of plans: ${api.plans.length}`);

  if (api.plans.length > 0) {
    await Promise.all(api.plans.map((plan) => apiPlansResource.closeApiPlan({ orgId, envId, api: api.id, plan: plan.id })));
  }

  await apisResource.doApiLifecycleAction({ orgId, envId, api: api.id, action: LifecycleAction.STOP });
  await apisResource.deleteApi({ orgId, envId, api: api.id });
}
