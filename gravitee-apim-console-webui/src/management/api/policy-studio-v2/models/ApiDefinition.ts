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
import { Definition } from './Definition';

import { ApiPlan, ApiProperty, ApiResource } from '../../../../entities/api';
import { Services } from '../../../../entities/services';
import { ApiV2, FlowV2, HttpMethod, PlanV2 } from '../../../../entities/management-api-v2';
import { Flow } from '../../../../entities/flow/flow';

export interface ApiDefinition extends Definition {
  resources: ApiResource[];
  plans?: ApiPlan[];
  properties: ApiProperty[];
  services: Services;
}

// Adapt ApiV2 to ApiDefinition for the policy studio
export function toApiDefinition(api: ApiV2): ApiDefinition {
  const toExecutionMode = {
    V3: 'v3',
    V4_EMULATION_ENGINE: 'v4-emulation-engine',
  } as const;

  const toOrigin = {
    MANAGEMENT: 'management',
    KUBERNETES: 'kubernetes',
  } as const;

  return {
    id: api.id,
    name: api.name,
    flows: api.flows.map(flow => toApiFlowDefinition(flow)),
    flow_mode: api.flowMode,
    resources: api.resources,
    version: api.apiVersion,
    properties: api.properties,
    services: api.services,
    execution_mode: toExecutionMode[api.executionMode],
    origin: toOrigin[api.definitionContext.origin],
  };
}

// Adapt ApiV2 flow to ApiDefinition flow
const toApiFlowDefinition = (flow: FlowV2): Flow => ({
  id: flow.id,
  name: flow.name,
  'path-operator': flow.pathOperator,
  pre: flow.pre,
  post: flow.post,
  enabled: flow.enabled,
  methods: flow.methods,
  condition: flow.condition,
  consumers: flow.consumers,
});

// Adapt ApiV2 plan to ApiDefinition plan
export const toApiPlansDefinition = (plans: PlanV2[]): ApiDefinition['plans'] => {
  return plans.map(plan => ({
    id: plan.id,
    name: plan.name,
    security: plan.security.type,
    securityDefinition: JSON.stringify(plan.security.configuration),
    paths: plan.paths,
    api: plan.apiId,
    tags: plan.tags,
    selectionRule: plan.selectionRule,
    order: plan.order,
    status: plan.status,
    flows: plan.flows.map(flow => toApiFlowDefinition(flow)),
  }));
};

// Adapt ApiDefinition to ApiV2 only for properties edited in the policy studio
export const toApiV2 = (apiDefinition: ApiDefinition, api: ApiV2): ApiV2 => {
  const toExecutionMode = {
    v3: 'V3',
    'v4-emulation-engine': 'V4_EMULATION_ENGINE',
  } as const;

  return {
    ...api,
    flows: apiDefinition.flows.map(flow => toApiFlowV2(flow)),
    flowMode: apiDefinition.flow_mode,
    executionMode: toExecutionMode[apiDefinition.execution_mode],
  };
};

const toApiFlowV2 = (flow: Flow): FlowV2 => ({
  id: flow.id,
  name: flow.name,
  pathOperator: flow['path-operator'],
  pre: flow.pre,
  post: flow.post,
  enabled: flow.enabled,
  methods: flow.methods as HttpMethod[],
  condition: flow.condition,
  consumers: flow.consumers,
});

// Adapt ApiDefinition plan to ApiV2 plan only for properties edited in the policy studio
export const toApiPlanV2 = (plan: ApiDefinition['plans'][number], planV2: PlanV2): PlanV2 => ({
  ...planV2,
  flows: plan.flows.map(flow => toApiFlowV2(flow)),
});
