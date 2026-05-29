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
import { deriveServiceType } from '../entity-kind-registry';
import { deriveTargetEntityId } from '../policy-entity-refs';
import { authzCoreApiClient } from './authz-api-client';
import type {
    EntityResponse,
    PagedResponse,
    PolicyRequest,
    PolicyResponse,
    PolicyStatus,
    PolicyType,
    SchemaResponse,
} from './authz-api.types';

export const DEFAULT_PER_PAGE = 10;
export const MAX_PER_PAGE = 100;

export interface PaginationParams {
    readonly page?: number;
    readonly perPage?: number;
}

export interface PolicyListParams extends PaginationParams {
    readonly type?: PolicyType;
    readonly status?: PolicyStatus;
}

export interface EntityListParams extends PaginationParams {
    readonly entityIdPrefix?: string;
}

interface CanonicalEntity {
    readonly id: string;
    readonly entityId: string;
    readonly kind: 'PRINCIPAL' | 'RESOURCE';
    readonly attributes: Record<string, unknown>;
    readonly parents: readonly string[];
    readonly source: string;
    readonly environmentId: string;
    readonly createdAt: string;
    readonly updatedAt: string;
}

interface CanonicalPolicy {
    readonly id: string;
    readonly name: string;
    readonly kind: 'GLOBAL' | 'RESOURCE';
    readonly entityId: string | null;
    readonly policyText: string;
    readonly status: PolicyStatus;
    readonly environmentId: string;
    readonly createdAt: string;
    readonly updatedAt: string;
}

interface CanonicalSchema {
    readonly schema: string;
}

interface CanonicalPagedResponse<T> {
    readonly data: readonly T[];
    readonly total: number;
    readonly page: number;
    readonly perPage: number;
}

interface CanonicalPolicyRequest {
    readonly name: string;
    readonly kind: 'GLOBAL' | 'RESOURCE';
    readonly entityId: string | null;
    readonly policyText: string;
}

interface CanonicalUpdatePolicyRequest {
    readonly name: string;
    readonly policyText: string;
}

// Authz is mounted at /gamma/organizations/{org}/environments/{env}/modules/authz
// by GammaModulesResource (SPI). The client base URL already covers the
// `/organizations/{org}` prefix, so this builder fills in the env scope and
// the module mount.
function corePath(environmentId: string, suffix: string): string {
    return `/environments/${encodeURIComponent(environmentId)}/modules/authz${suffix}`;
}

function adaptEntityResponse(c: CanonicalEntity): EntityResponse {
    // Surface the canonical root-level source as a `_source` attribute so the
    // entity-adapter (which already speaks _source) doesn't need to know about
    // the canonical shape. An existing _source attribute wins.
    const attrs: Record<string, unknown> = { ...c.attributes };
    if (c.source && attrs._source === undefined) {
        attrs._source = c.source;
    }
    return {
        id: c.id,
        environmentId: c.environmentId,
        uid: c.entityId,
        attributes: attrs,
        parents: [...c.parents],
        createdAt: c.createdAt,
        updatedAt: c.updatedAt,
    };
}

function adaptPolicyResponse(c: CanonicalPolicy): PolicyResponse {
    const type = deriveServiceType(c.entityId);
    return {
        id: c.id,
        environmentId: c.environmentId,
        name: c.name,
        description: null,
        policyText: c.policyText,
        type,
        // We don't have a separate display label on the canonical side — the
        // target picker is fed by entities, so the entity adapter will fill in
        // a richer label later. For now the entityId doubles as the label.
        target: c.entityId ? { id: c.entityId, label: c.entityId } : null,
        status: c.status,
        createdAt: c.createdAt,
        updatedAt: c.updatedAt,
    };
}

function adaptCreatePolicyRequest(r: PolicyRequest): CanonicalPolicyRequest {
    // Prefer an explicitly picked target; otherwise derive the binding from the
    // first service-typed resource in the policy text. A policy referencing
    // `mcp.*`/`llm.*`/`api.*`/… binds as RESOURCE and surfaces on that service
    // page; one with only generic/custom resources stays GLOBAL → Custom.
    const entityId = r.target?.id ?? deriveTargetEntityId(r.policyText);
    const isGlobal = entityId === null;
    return {
        name: r.name,
        kind: isGlobal ? 'GLOBAL' : 'RESOURCE',
        entityId,
        policyText: r.policyText,
    };
}

function adaptUpdatePolicyRequest(r: PolicyRequest): CanonicalUpdatePolicyRequest {
    return {
        name: r.name,
        policyText: r.policyText,
    };
}

/**
 * Apply the requested status by calling the dedicated transition endpoint.
 * Returns the policy after the transition (canonical → UI shape).
 *
 * The canonical model has only two transition verbs: deploy → DEPLOYED,
 * disable → DISABLED. DRAFT is the implicit state after create/update.
 * We fire transitions optimistically: if the policy is already in the
 * requested state the backend short-circuits, so a redundant call is cheap.
 */
async function applyStatusTransition(
    environmentId: string,
    id: string,
    status: PolicyStatus | null | undefined,
): Promise<CanonicalPolicy | null> {
    if (status === 'DEPLOYED') {
        return authzCoreApiClient.post<CanonicalPolicy>(corePath(environmentId, `/policies/${encodeURIComponent(id)}/deploy`));
    }
    if (status === 'DISABLED') {
        return authzCoreApiClient.post<CanonicalPolicy>(corePath(environmentId, `/policies/${encodeURIComponent(id)}/disable`));
    }
    // DRAFT or undefined: leave whatever the base POST/PUT returned.
    return null;
}

function entityListQuery(params?: EntityListParams): string {
    const q = new URLSearchParams();
    if (params?.page !== undefined) q.set('page', String(params.page));
    if (params?.perPage !== undefined) q.set('perPage', String(params.perPage));
    if (params?.entityIdPrefix !== undefined) q.set('entityIdPrefix', params.entityIdPrefix);
    const qs = q.toString();
    return qs ? `?${qs}` : '';
}

function policyListQuery(params?: PolicyListParams): string {
    const q = new URLSearchParams();
    if (params?.page !== undefined) q.set('page', String(params.page));
    if (params?.perPage !== undefined) q.set('perPage', String(params.perPage));
    // status is the only UI-meaningful filter the backend can apply.
    // The UI 'type' (MCP/AGENT/…) is derived from the entityId prefix
    // on the client because the canonical model has no concept of it.
    if (params?.status !== undefined) q.set('status', params.status);
    const qs = q.toString();
    return qs ? `?${qs}` : '';
}

export const authzApiService = {
    getSchema: async (environmentId: string): Promise<SchemaResponse> => {
        const c = await authzCoreApiClient.get<CanonicalSchema>(corePath(environmentId, '/schema'));
        return {
            environmentId,
            schemaText: c.schema ?? '',
            updatedAt: null,
        };
    },

    listPolicies: async (environmentId: string, params?: PolicyListParams): Promise<PagedResponse<PolicyResponse>> => {
        const path = corePath(environmentId, '/policies') + policyListQuery(params);
        const response = await authzCoreApiClient.get<CanonicalPagedResponse<CanonicalPolicy>>(path);
        let mapped = response.data.map(adaptPolicyResponse);
        let total = response.total;
        if (params?.type !== undefined) {
            const before = mapped.length;
            mapped = mapped.filter(p => p.type === params.type);
            // total is approximate when we post-filter; downsize by what
            // we dropped on this page so the UI doesn't enable a "next"
            // for a page-load that already came back partial.
            total = Math.max(0, total - (before - mapped.length));
        }
        return {
            data: mapped,
            total,
            page: response.page,
            perPage: response.perPage,
        };
    },

    createPolicy: async (environmentId: string, request: PolicyRequest): Promise<PolicyResponse> => {
        const created = await authzCoreApiClient.post<CanonicalPolicy>(
            corePath(environmentId, '/policies'),
            adaptCreatePolicyRequest(request),
        );
        const afterTransition = await applyStatusTransition(environmentId, created.id, request.status ?? null);
        return adaptPolicyResponse(afterTransition ?? created);
    },

    updatePolicy: async (environmentId: string, id: string, request: PolicyRequest): Promise<PolicyResponse> => {
        const updated = await authzCoreApiClient.put<CanonicalPolicy>(
            corePath(environmentId, `/policies/${encodeURIComponent(id)}`),
            adaptUpdatePolicyRequest(request),
        );
        const afterTransition = await applyStatusTransition(environmentId, id, request.status ?? null);
        return adaptPolicyResponse(afterTransition ?? updated);
    },

    deletePolicy: (environmentId: string, id: string) =>
        authzCoreApiClient.delete<void>(corePath(environmentId, `/policies/${encodeURIComponent(id)}`)),

    listEntities: async (environmentId: string, params?: EntityListParams): Promise<PagedResponse<EntityResponse>> => {
        const path = corePath(environmentId, '/entities') + entityListQuery(params);
        const response = await authzCoreApiClient.get<CanonicalPagedResponse<CanonicalEntity>>(path);
        return {
            data: response.data.map(adaptEntityResponse),
            total: response.total,
            page: response.page,
            perPage: response.perPage,
        };
    },
};
