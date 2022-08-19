'use strict';
Object.defineProperty(exports, '__esModule', { value: true });
exports.setup = void 0;
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
const APIsApi_1 = require('@management-apis/APIsApi');
const _1 = require('@client-conf/*');
const PlansFaker_1 = require('@management-fakers/PlansFaker');
const PlanStatus_1 = require('@management-models/PlanStatus');
const ApisFaker_1 = require('@management-fakers/ApisFaker');
const LifecycleAction_1 = require('@management-models/LifecycleAction');
const orgId = 'DEFAULT';
const envId = 'DEFAULT';
let createdApi;
let createdPlan;
async function setup() {
  console.log('setup...');
  const apiManagementApiAsApiUser = new APIsApi_1.APIsApi((0, _1.forManagementAsApiUser)());
  const newPlanEntity = PlansFaker_1.PlansFaker.newPlan({ status: PlanStatus_1.PlanStatus.PUBLISHED });
  createdApi = await apiManagementApiAsApiUser.createApi({ orgId, envId, newApiEntity: ApisFaker_1.ApisFaker.newApi() });
  createdPlan = await apiManagementApiAsApiUser.createApiPlan({ orgId, envId, api: createdApi.id, newPlanEntity });
  console.log('yeah...', createdApi.name, createdPlan.name);
  await apiManagementApiAsApiUser.doApiLifecycleActionRaw({
    orgId,
    envId,
    api: createdApi.id,
    action: LifecycleAction_1.LifecycleAction.START,
  });
  return 'Well done !';
}
exports.setup = setup;
setup().then(console.log).catch(console.error);
