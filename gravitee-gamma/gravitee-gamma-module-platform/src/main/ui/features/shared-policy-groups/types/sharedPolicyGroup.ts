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

/** Mirrors classic Console's `ApiType` (v4 API definition). */
export type ApiType = 'MESSAGE' | 'A2A_PROXY' | 'LLM_PROXY' | 'MCP_PROXY' | 'PROXY' | 'NATIVE';

/** Mirrors classic Console's `FlowPhase`. */
export type FlowPhase = 'REQUEST' | 'RESPONSE' | 'ENTRYPOINT_CONNECT' | 'INTERACT' | 'PUBLISH' | 'SUBSCRIBE';

export function toReadableFlowPhase(phase: FlowPhase): string {
    switch (phase) {
        case 'REQUEST':
            return 'Request';
        case 'RESPONSE':
            return 'Response';
        case 'PUBLISH':
            return 'Publish';
        case 'SUBSCRIBE':
            return 'Subscribe';
        case 'ENTRYPOINT_CONNECT':
            return 'Entrypoint connect';
        case 'INTERACT':
            return 'Interact';
    }
}

export function toReadableApiType(apiType: ApiType): string {
    switch (apiType) {
        case 'PROXY':
            return 'Proxy';
        case 'MESSAGE':
            return 'Message';
        case 'A2A_PROXY':
            return 'A2A Proxy';
        case 'LLM_PROXY':
            return 'LLM Proxy';
        case 'MCP_PROXY':
            return 'MCP Proxy';
        case 'NATIVE':
            return 'Native';
    }
}

export interface OriginContext {
    origin: 'MANAGEMENT' | 'KUBERNETES' | 'INTEGRATION';
}

/**
 * Policy steps on a shared policy group.
 * Opaque until Policy Studio (FOUND-19); passed through unchanged on metadata update.
 */
export type SharedPolicyGroupStep = Record<string, unknown>;

/** v2 `SharedPolicyGroup` (GET/POST .../v2/environments/{envId}/shared-policy-groups...). */
export interface SharedPolicyGroup {
    id: string;
    crossId?: string;
    name: string;
    description?: string;
    prerequisiteMessage?: string;
    lifecycleState?: 'DEPLOYED' | 'UNDEPLOYED' | 'PENDING';
    /** Present on history snapshots (`GET .../{id}/histories`). */
    version?: number;
    apiType: ApiType;
    phase: FlowPhase;
    steps?: SharedPolicyGroupStep[];
    deployedAt?: string;
    createdAt?: string;
    updatedAt?: string;
    originContext?: OriginContext;
}

/**
 * Classic Console `SharedPolicyGroupHistoriesSortByParam`.
 * @see shared-policy-groups.service.ts `listHistories`
 */
export type SharedPolicyGroupHistoriesSortByParam =
    | 'version'
    | '-version'
    | 'updatedAt'
    | '-updatedAt'
    | 'deployedAt'
    | '-deployedAt';

/** v2 `CreateSharedPolicyGroup` (POST .../shared-policy-groups). */
export interface CreateSharedPolicyGroupPayload {
    name: string;
    description?: string;
    prerequisiteMessage?: string;
    apiType: ApiType;
    phase: FlowPhase;
}

/** v2 `UpdateSharedPolicyGroup` (PUT .../shared-policy-groups/{id}). */
export interface UpdateSharedPolicyGroupPayload {
    name: string;
    description?: string;
    prerequisiteMessage?: string;
    /** Must be sent to avoid clearing policies — mirrors classic Console metadata edit. */
    steps: SharedPolicyGroupStep[];
}

interface SharedPolicyGroupsPagination {
    page: number;
    perPage: number;
    pageCount: number;
    pageItemsCount: number;
    totalCount: number;
}

/** v2 `PagedResult<SharedPolicyGroup>` (GET .../shared-policy-groups). */
export interface SharedPolicyGroupsPagedResponse {
    data: SharedPolicyGroup[];
    pagination: SharedPolicyGroupsPagination;
}
