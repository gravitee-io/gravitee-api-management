/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { ConnectorInfo, Flow, Selector, Step } from '@gravitee/graphene-policy-studio';

import type { FlowExecution, FlowV2, StepV2 } from '../types/policyStudio';

/**
 * Converts a V2 flow step (`pre`/`post`) to the V4 step shape expected by the policy studio.
 */
function mapStepV2ToV4(step: StepV2): Step {
    return {
        policy: step.policy,
        name: step.name,
        description: step.description,
        enabled: step.enabled,
        configuration: step.configuration,
        condition: step.condition,
    };
}

/**
 * Converts a V2 flow to the V4 Flow shape expected by `@gravitee/graphene-policy-studio`.
 *
 * V2 uses `pathOperator` + `methods` + `pre`/`post`.
 * V4 uses `selectors` + `request`/`response`.
 */
export function mapFlowV2ToV4(flow: FlowV2): Flow {
    const selectors: Selector[] = [];

    if (flow.pathOperator) {
        selectors.push({
            type: 'HTTP',
            path: flow.pathOperator.path,
            pathOperator: flow.pathOperator.operator,
            methods: flow.methods,
        } as Selector);
    }

    if (flow.condition) {
        selectors.push({
            type: 'CONDITION',
            condition: flow.condition,
        } as Selector);
    }

    return {
        id: flow.id,
        name: flow.name,
        enabled: flow.enabled ?? true,
        selectors: selectors.length > 0 ? selectors : undefined,
        request: flow.pre?.map(mapStepV2ToV4),
        response: flow.post?.map(mapStepV2ToV4),
    };
}

/**
 * Converts an array of V2 flows to V4 flows.
 */
export function mapFlowsV2ToV4(flows: FlowV2[]): Flow[] {
    return flows.map(mapFlowV2ToV4);
}

/**
 * Converts V2 `flowMode` to V4 `FlowExecution`.
 */
export function mapFlowModeToExecution(flowMode?: 'DEFAULT' | 'BEST_MATCH'): FlowExecution {
    return { mode: flowMode ?? 'DEFAULT' };
}

/**
 * Provides synthetic connector info for V2 APIs (which have no listeners/endpointGroups).
 * V2 PROXY APIs always operate in HTTP request/response mode.
 */
export const V2_DEFAULT_ENTRYPOINTS_INFO: readonly ConnectorInfo[] = [
    {
        type: 'http-proxy',
        name: 'HTTP Proxy',
        icon: 'gio:language',
        supportedModes: ['REQUEST_RESPONSE'] as ConnectorInfo['supportedModes'],
    },
];

export const V2_DEFAULT_ENDPOINTS_INFO: readonly ConnectorInfo[] = [
    {
        type: 'http-proxy',
        name: 'HTTP Proxy',
        icon: 'gio:language',
        supportedModes: ['REQUEST_RESPONSE'] as ConnectorInfo['supportedModes'],
    },
];
