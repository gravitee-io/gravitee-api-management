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
import type { ConditionSelector, Flow, FlowExecution, HttpMethod, HttpSelector, Selector, Step } from '@gravitee/graphene-policy-studio';

import type { PlatformFlow, PlatformFlowMode, PlatformFlowStep } from '../types/platformPolicies';

/** The organization flow schema always carries a path operator; a flow saved without one would never match. */
const DEFAULT_PATH_OPERATOR = { path: '/', operator: 'STARTS_WITH' } as const;

function toStudioStep(step: PlatformFlowStep): Step {
    return {
        policy: step.policy,
        name: step.name,
        description: step.description,
        enabled: step.enabled,
        configuration: step.configuration,
        condition: step.condition,
    };
}

function toPlatformStep(step: Step): PlatformFlowStep {
    return {
        policy: step.policy,
        name: step.name,
        description: step.description,
        enabled: step.enabled,
        configuration: step.configuration,
        condition: step.condition,
    };
}

function findSelector<T extends Selector>(flow: Flow, type: T['type']): T | undefined {
    return flow.selectors?.find((selector): selector is T => selector.type === type);
}

/** Converts a stored platform flow to the flow shape the Policy Studio renders. */
export function toStudioFlow(flow: PlatformFlow): Flow {
    const selectors: Selector[] = [];
    const pathOperator = flow['path-operator'];

    if (pathOperator) {
        selectors.push({
            type: 'HTTP',
            path: pathOperator.path ?? DEFAULT_PATH_OPERATOR.path,
            pathOperator: pathOperator.operator ?? DEFAULT_PATH_OPERATOR.operator,
            methods: flow.methods as readonly HttpMethod[] | undefined,
        });
    }

    if (flow.condition) {
        selectors.push({ type: 'CONDITION', condition: flow.condition });
    }

    return {
        id: flow.id,
        name: flow.name,
        enabled: flow.enabled ?? true,
        selectors,
        request: flow.pre?.map(toStudioStep),
        response: flow.post?.map(toStudioStep),
        tags: flow.consumers?.filter(consumer => consumer.consumerType === 'TAG').map(consumer => consumer.consumerId),
    };
}

/** Converts a Policy Studio flow back to the shape the organization entity stores. */
export function toPlatformFlow(flow: Flow): PlatformFlow {
    const httpSelector = findSelector<HttpSelector>(flow, 'HTTP');
    const conditionSelector = findSelector<ConditionSelector>(flow, 'CONDITION');
    const methods = httpSelector?.methods ?? [];

    return {
        id: flow.id,
        name: flow.name,
        enabled: flow.enabled ?? true,
        'path-operator': {
            path: httpSelector?.path ?? DEFAULT_PATH_OPERATOR.path,
            operator: httpSelector?.pathOperator ?? DEFAULT_PATH_OPERATOR.operator,
        },
        methods: [...methods],
        condition: conditionSelector?.condition,
        pre: (flow.request ?? []).map(toPlatformStep),
        post: (flow.response ?? []).map(toPlatformStep),
        consumers: (flow.tags ?? []).map(tag => ({ consumerType: 'TAG', consumerId: tag })),
    };
}

export function toStudioFlows(flows: readonly PlatformFlow[]): Flow[] {
    return flows.map(toStudioFlow);
}

export function toPlatformFlows(flows: readonly Flow[]): PlatformFlow[] {
    return flows.map(toPlatformFlow);
}

/** The organization entity holds a `flowMode`; the studio reads a `FlowExecution`. */
export function toFlowExecution(flowMode: PlatformFlowMode | undefined): FlowExecution {
    return { mode: flowMode ?? 'DEFAULT' };
}

export function toFlowMode(flowExecution: FlowExecution): PlatformFlowMode {
    return flowExecution.mode === 'BEST_MATCH' ? 'BEST_MATCH' : 'DEFAULT';
}
