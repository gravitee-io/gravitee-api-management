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
import { ApiError, authzCoreApiClient } from './authz-api-client';
import type {
    AmSyncStartResponse,
    AmSyncStatusResponse,
    EngineSchemaJson,
    EntityResponse,
    PagedResponse,
    PolicyRequest,
    PolicyResponse,
    PolicyStatus,
    PolicyType,
    SchemaResponse,
    SchemaValidation,
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

export type EntityKindFilter = 'PRINCIPAL' | 'RESOURCE';

export interface EntityListParams extends PaginationParams {
    readonly kind?: EntityKindFilter;
    readonly source?: string;
    readonly entityIdPrefix?: string;
    readonly excludeEntityIdPrefix?: string;
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
    if (params?.kind !== undefined) q.set('kind', params.kind);
    if (params?.source !== undefined) q.set('source', params.source);
    if (params?.entityIdPrefix !== undefined) q.set('entityIdPrefix', params.entityIdPrefix);
    if (params?.excludeEntityIdPrefix !== undefined) q.set('excludeEntityIdPrefix', params.excludeEntityIdPrefix);
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

    updateSchema: async (environmentId: string, schemaText: string): Promise<SchemaResponse> => {
        const c = await authzCoreApiClient.put<CanonicalSchema>(corePath(environmentId, '/schema'), { schema: schemaText });
        return { environmentId, schemaText: c.schema ?? '', updatedAt: null };
    },

    deleteSchema: (environmentId: string): Promise<void> => authzCoreApiClient.delete<void>(corePath(environmentId, '/schema')),

    getParsedSchema: (environmentId: string): Promise<EngineSchemaJson> =>
        authzCoreApiClient.get<EngineSchemaJson>(corePath(environmentId, '/schema/parsed')),

    validateSchema: (environmentId: string, schemaText: string): Promise<SchemaValidation> =>
        authzCoreApiClient.post<SchemaValidation>(corePath(environmentId, '/schema/validate'), { schema: schemaText }),

    listPolicies: async (environmentId: string, params?: PolicyListParams): Promise<PagedResponse<PolicyResponse>> => {
        // The canonical backend has no concept of the UI 'type' (MCP/LLM/API/…);
        // it derives only from the entityId prefix on the client. Filtering a single
        // backend page client-side silently drops policies that live on other pages
        // (their backend total/order doesn't align with the type), so a freshly
        // created policy can vanish from its service list.
        //
        // Until the backend grows an entityId-prefix filter, fetch the full set
        // (capped at MAX_PER_PAGE), filter by type, then paginate locally so the
        // count and rows stay consistent. Status stays a server-side filter.
        if (params?.type !== undefined) {
            const page = params.page ?? 1;
            const perPage = params.perPage ?? DEFAULT_PER_PAGE;
            const fetchPath =
                corePath(environmentId, '/policies') + policyListQuery({ status: params.status, page: 1, perPage: MAX_PER_PAGE });
            const response = await authzCoreApiClient.get<CanonicalPagedResponse<CanonicalPolicy>>(fetchPath);
            const filtered = response.data.map(adaptPolicyResponse).filter(p => p.type === params.type);
            const start = (page - 1) * perPage;
            return {
                data: filtered.slice(start, start + perPage),
                total: filtered.length,
                page,
                perPage,
            };
        }
        const path = corePath(environmentId, '/policies') + policyListQuery(params);
        const response = await authzCoreApiClient.get<CanonicalPagedResponse<CanonicalPolicy>>(path);
        return {
            data: response.data.map(adaptPolicyResponse),
            total: response.total,
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

    getEntity: async (environmentId: string, entityId: string): Promise<EntityResponse | null> => {
        try {
            const entity = await authzCoreApiClient.get<CanonicalEntity>(
                corePath(environmentId, `/entities/${encodeURIComponent(entityId)}`),
            );
            return adaptEntityResponse(entity);
        } catch (err) {
            if (err instanceof ApiError && err.status === 404) return null;
            throw err;
        }
    },

    createEntity: async (environmentId: string, request: CreateEntityRequest): Promise<EntityResponse> => {
        const created = await authzCoreApiClient.post<CanonicalEntity>(corePath(environmentId, '/entities'), request);
        return adaptEntityResponse(created);
    },

    // Update is a full replace of attributes + parents (entityId/kind/source are immutable).
    // Callers must send the complete attributes map, not a partial patch.
    updateEntity: async (environmentId: string, entityId: string, request: UpdateEntityRequest): Promise<EntityResponse> => {
        const updated = await authzCoreApiClient.put<CanonicalEntity>(
            corePath(environmentId, `/entities/${encodeURIComponent(entityId)}`),
            request,
        );
        return adaptEntityResponse(updated);
    },

    deleteEntity: (environmentId: string, entityId: string): Promise<void> =>
        authzCoreApiClient.delete<void>(corePath(environmentId, `/entities/${encodeURIComponent(entityId)}`)),

    startUserSync: (environmentId: string): Promise<AmSyncStartResponse> =>
        authzCoreApiClient.post<AmSyncStartResponse>(corePath(environmentId, '/users/sync')),

    getUserSyncStatus: (environmentId: string): Promise<AmSyncStatusResponse> =>
        authzCoreApiClient.get<AmSyncStatusResponse>(corePath(environmentId, '/users/sync')),
};

export interface CreateEntityRequest {
    readonly entityId: string;
    readonly kind: 'PRINCIPAL' | 'RESOURCE';
    readonly entityType?: string;
    readonly attributes: Record<string, unknown>;
    readonly parents: readonly string[];
    readonly source: string;
}

export interface UpdateEntityRequest {
    readonly attributes: Record<string, unknown>;
    readonly parents: readonly string[];
}
