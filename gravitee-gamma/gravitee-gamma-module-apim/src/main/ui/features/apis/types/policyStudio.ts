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
import type { ApiType } from './api';

// ─── Plugin catalog types ────────────────────────────────────────────────────

export interface PolicyPlugin {
    id: string;
    name: string;
    description?: string;
    icon?: string;
    category?: string;
    onRequest?: boolean;
    onResponse?: boolean;
    flowPhaseCompatibility?: Record<string, string[]>;
}

export interface ConnectorPlugin {
    id: string;
    name: string;
    icon?: string;
    supportedModes: string[];
}

export interface SharedPolicyGroupPlugin {
    id: string;
    name: string;
    description?: string;
    prerequisiteMessage?: string;
    policyId: string;
    phase: string;
    apiType: string;
}

// ─── API protocol type mapping ───────────────────────────────────────────────

export type ApiProtocolType = 'HTTP_PROXY' | 'HTTP_MESSAGE' | 'NATIVE_KAFKA' | 'MCP_PROXY' | 'LLM_PROXY' | 'A2A_PROXY';

export function getApiProtocolType(apiType: ApiType): ApiProtocolType {
    switch (apiType) {
        case 'PROXY':
            return 'HTTP_PROXY';
        case 'MESSAGE':
            return 'HTTP_MESSAGE';
        case 'NATIVE':
            return 'NATIVE_KAFKA';
        default:
            return 'HTTP_PROXY';
    }
}

// ─── V2 flow types (returned by Management API for V2 APIs) ─────────────────

export interface PathOperator {
    path: string;
    operator: 'EQUALS' | 'STARTS_WITH';
}

export interface StepV2 {
    name?: string;
    description?: string;
    enabled: boolean;
    policy: string;
    condition?: string;
    configuration?: Record<string, unknown>;
}

export interface FlowV2 {
    id?: string;
    name?: string;
    pathOperator?: PathOperator;
    pre?: StepV2[];
    post?: StepV2[];
    enabled?: boolean;
    methods?: string[];
    condition?: string;
    consumers?: { consumerId: string; consumerType: string }[];
}

// ─── Extended API detail with policy-studio fields ───────────────────────────

export interface FlowStep {
    policy: string;
    name?: string;
    description?: string;
    enabled: boolean;
    configuration: Record<string, unknown>;
    condition?: string;
}

export interface FlowSelector {
    type: string;
    path?: string;
    pathOperator?: string;
    methods?: string[];
    condition?: string;
    channel?: string;
    channelOperator?: string;
    operations?: string[];
    entrypoints?: string[];
}

export interface ApiFlow {
    name?: string;
    enabled: boolean;
    selectors?: FlowSelector[];
    request?: FlowStep[];
    response?: FlowStep[];
    subscribe?: FlowStep[];
    publish?: FlowStep[];
    connect?: FlowStep[];
    interact?: FlowStep[];
    tags?: string[];
}

export interface FlowExecution {
    mode: 'DEFAULT' | 'BEST_MATCH';
    matchRequired?: boolean;
}

export interface ApiListener {
    type: string;
    paths?: { path: string; host?: string }[];
    hosts?: { host: string; path: string }[];
    entrypoints?: { type: string; configuration?: Record<string, unknown> }[];
    cors?: Record<string, unknown>;
}

export interface ApiEndpoint {
    name: string;
    type: string;
    weight?: number;
    inheritConfiguration?: boolean;
    configuration?: Record<string, unknown>;
}

export interface ApiEndpointGroupFull {
    name: string;
    type: string;
    sharedConfiguration?: Record<string, unknown>;
    endpoints?: ApiEndpoint[];
}

export interface ApiResource {
    name: string;
    type: string;
    enabled: boolean;
    configuration?: Record<string, unknown>;
}

/**
 * Full API detail shape including policy-studio-relevant fields
 * that the backend returns but `ApiDetailDto` omits.
 * Supports both V4 (flows as ApiFlow[]) and V2 (flows as FlowV2[], flowMode).
 */
export interface PolicyStudioApiDetail {
    id: string;
    name: string;
    description?: string;
    type?: ApiType;
    apiVersion?: string;
    definitionVersion?: string;
    flows?: ApiFlow[] | FlowV2[];
    flowExecution?: FlowExecution;
    /** V2-only: flow execution mode stored as a simple string. */
    flowMode?: 'DEFAULT' | 'BEST_MATCH';
    listeners?: ApiListener[];
    endpointGroups?: ApiEndpointGroupFull[];
    resources?: ApiResource[];
    definitionContext?: {
        origin?: 'MANAGEMENT' | 'KUBERNETES';
        syncFrom?: string;
    };
}

// ─── Plans page for policy studio ────────────────────────────────────────────

export interface PolicyStudioPlan {
    id: string;
    name: string;
    flows?: ApiFlow[] | FlowV2[];
}

export interface PolicyStudioPlanPage {
    data: PolicyStudioPlan[];
    pagination: { totalCount: number };
}
