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
import { GatewayTestData, loadTestDataForSetup } from '../lib/test-api';
import { forManagementAsApiUser } from '@gravitee/utils/configuration';
import { APIsApi } from '@gravitee/management-webclient-sdk/src/lib/apis/APIsApi';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { ExecutionMode, LifecycleAction, PlanStatus, UpdateApiEntityFromJSON } from '@gravitee/management-webclient-sdk/src/lib/models';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';

const orgId = 'DEFAULT';
const envId = 'DEFAULT';

const apiManagementApiAsApiUser = new APIsApi(forManagementAsApiUser());

export async function init(): Promise<GatewayTestData> {
  const newPlanEntity = PlansFaker.newPlan({ status: PlanStatus.PUBLISHED });
  const api = await apiManagementApiAsApiUser.createApi({
    orgId,
    envId,
    newApiEntity: ApisFaker.newApi({
      endpoint: process.env.API_ENDPOINT_URL,
      gravitee: '2.0.0',
    }),
  });
  if (api && api.id) {
    const plan = await apiManagementApiAsApiUser.createApiPlan({ orgId, envId, api: api.id, newPlanEntity });
    api.proxy.groups[0].ssl = { trustAll: true };
    api.proxy.groups[0].endpoints[0].inherit = true;
    api.execution_mode = process.env.API_EXECUTION_MODE === ExecutionMode.JUPITER ? ExecutionMode.JUPITER : ExecutionMode.V3;

    await apiManagementApiAsApiUser.updateApi({
      orgId,
      envId,
      updateApiEntity: UpdateApiEntityFromJSON({ ...api }),
      api: api.id,
    });

    await apiManagementApiAsApiUser.doApiLifecycleActionRaw({
      orgId,
      envId,
      api: api.id,
      action: LifecycleAction.START,
    });

    return { api, plan, waitGateway: { contextPath: api.context_path } };
  }

  throw new Error('Cannot create api');
}

export async function tearDown() {
  const data: GatewayTestData = await loadTestDataForSetup();
  const { api, plan } = data;
  await apiManagementApiAsApiUser.doApiLifecycleAction({ orgId, envId, api: api.id, action: LifecycleAction.STOP });
  await apiManagementApiAsApiUser.deleteApiPlan({ orgId, envId, api: api.id, plan: plan.id });
  await apiManagementApiAsApiUser.deleteApi({ orgId, envId, api: api.id });
  return `'API[${api.id}] deleted !`;
}
