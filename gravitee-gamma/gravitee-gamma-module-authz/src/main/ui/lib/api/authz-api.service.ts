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
import { authzCoreApiClient, authzModuleApiClient } from './authz-api-client';
import type {
    EntityRequest,
    EntityResponse,
    PagedResponse,
    PolicyRequest,
    PolicyResponse,
    PolicyStatus,
    PolicyType,
    SchemaResponse,
    ScimConnectorRequest,
    ScimConnectorResponse,
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

// =============================================================================
// Adapter layer
// =============================================================================
//
// The canonical Gamma authz REST kernel (`/gamma/authz`) speaks a smaller,
// platform-shared domain (Entity{entityId,kind,source}, Policy{kind,entityId})
// than the richer shape the UI was originally built for (entity.uid:string,
// policy.type:'MCP'|'AGENT'|…|'CUSTOM', policy.target:{id,label}). Rather than
// fork the UI to the canonical shape — which would cascade into every page
// and component — we map both directions here and keep the UI contract stable.
//
// Mapping rules:
//   - PolicyKind:GLOBAL  ↔ UI type:'CUSTOM' (target = null)
//   - PolicyKind:RESOURCE ↔ UI type derived from `entityId` prefix:
//       "mcp.*"   → MCP
//       "agent.*" → AGENT
//       "llm.*"   → LLM
//       "api.*"   → API
//       "event.*" → EVENT
//       anything else (incl. legacy "User::alice" form) → CUSTOM
//   - Entity.entityId ↔ UI uid (string). UI entity-adapter does its own
//     `_kind` attribute → structured uid.type lookup, so we surface
//     `kind`/`source` as `_kind`/`_source` attributes when adapting from
//     canonical → UI, and strip them on the way back.
//   - Status transitions (DRAFT → DEPLOYED → DISABLED) live on dedicated
//     canonical endpoints (`/deploy`, `/disable`); UI submits them as part
//     of the policy payload, so we fan out after the base PUT/POST.
// =============================================================================

// ---- Canonical shapes (server) ---------------------------------------------

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

/**
 * Server-side paged response shape — same wire contract as the legacy UI
 * {@code PagedResponse<T>} so a single deserialise step yields the value
 * we hand back to the hook layer without further restructuring.
 */
interface CanonicalPagedResponse<T> {
    readonly data: readonly T[];
    readonly total: number;
    readonly page: number;
    readonly perPage: number;
}

/**
 * Normalises a list endpoint response to a paged shape regardless of whether
 * the deployed backend speaks the new {@code PagedResponseDto<T>} contract
 * or still returns a bare {@code List<T>}. The platform paging change ships
 * across multiple modules (authz-api, authz-core, authz-rest) and a rolling
 * restart can leave UI and rest-api temporarily out of sync; this guard
 * stops a stale rest-api from blowing up the list pages with
 * "Cannot read properties of undefined (reading 'map')".
 *
 * <p>When the legacy shape is detected we fabricate the paging envelope from
 * what we got: total = array length, page = requested page (or 1), perPage =
 * requested perPage (or the array length). The client paginates the resulting
 * slice locally — same behaviour as pre-migration.
 */
function adaptPagedListResponse<T>(
    raw: CanonicalPagedResponse<T> | readonly T[],
    requestedPage: number | undefined,
    requestedPerPage: number | undefined,
): CanonicalPagedResponse<T> {
    if (Array.isArray(raw)) {
        const items = raw as readonly T[];
        const page = requestedPage ?? 1;
        const perPage = requestedPerPage ?? items.length;
        const start = Math.max(0, (page - 1) * perPage);
        return {
            data: perPage > 0 ? items.slice(start, start + perPage) : items,
            total: items.length,
            page,
            perPage,
        };
    }
    return raw as CanonicalPagedResponse<T>;
}

interface CanonicalEntityRequest {
    readonly entityId: string;
    readonly kind: 'PRINCIPAL' | 'RESOURCE';
    readonly attributes: Record<string, unknown>;
    readonly parents: readonly string[];
    readonly source: string;
}

interface CanonicalUpdateEntityRequest {
    readonly attributes: Record<string, unknown>;
    readonly parents: readonly string[];
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

// ---- Path builders ----------------------------------------------------------

function corePath(environmentId: string, suffix: string): string {
    return `/environments/${encodeURIComponent(environmentId)}${suffix}`;
}

function modulePath(environmentId: string, suffix: string): string {
    return `/environments/${encodeURIComponent(environmentId)}${suffix}`;
}

// ---- Derivation helpers ----------------------------------------------------

export function deriveServiceType(entityId: string | null | undefined): PolicyType {
    if (!entityId) return 'CUSTOM';
    const prefix = entityId.split('.')[0]?.toLowerCase();
    switch (prefix) {
        case 'mcp':
            return 'MCP';
        case 'agent':
            return 'AGENT';
        case 'llm':
            return 'LLM';
        case 'api':
            return 'API';
        case 'event':
            return 'EVENT';
        default:
            return 'CUSTOM';
    }
}

/**
 * Heuristic UID-prefix → canonical EntityKind classifier used when adapting
 * a write request (UI → canonical). The canonical kind enum is just
 * {PRINCIPAL, RESOURCE}; everything user/group/service-account/agent-identity
 * shaped is principal, everything else is a resource.
 *
 * The `_kind` attribute (if present) wins over the prefix — that's the field
 * SCIM and entity-adapter already use to tag sub-kinds.
 */
function entityKindFromAttrs(uid: string, attrs: Record<string, unknown>): 'PRINCIPAL' | 'RESOURCE' {
    const explicit = typeof attrs._kind === 'string' ? attrs._kind.toLowerCase() : null;
    if (explicit) {
        if (
            explicit === 'user' ||
            explicit === 'group' ||
            explicit === 'serviceaccount' ||
            explicit === 'service-account' ||
            explicit === 'service_account' ||
            explicit === 'agentidentity' ||
            explicit === 'agent-identity' ||
            explicit === 'agent_identity'
        ) {
            return 'PRINCIPAL';
        }
        return 'RESOURCE';
    }
    const prefix = uid.split('.')[0]?.toLowerCase();
    if (prefix === 'user' || prefix === 'group' || prefix === 'serviceaccount' || prefix === 'agentidentity') {
        return 'PRINCIPAL';
    }
    // Legacy "Type::id" form (e.g. "User::alice") that pre-dates the dotted layout.
    const legacyType = uid.includes('::') ? uid.slice(0, uid.indexOf('::')).toLowerCase() : null;
    if (legacyType === 'user' || legacyType === 'group' || legacyType === 'serviceaccount' || legacyType === 'agentidentity') {
        return 'PRINCIPAL';
    }
    return 'RESOURCE';
}

// ---- Entity adapters --------------------------------------------------------

function adaptEntityResponse(c: CanonicalEntity): EntityResponse {
    // Surface canonical root-level kind/source as attributes so the existing
    // entity-adapter (which already speaks _kind / _source) doesn't have to
    // know we migrated. _source overrides nothing — if attributes already
    // carry it (SCIM does), the canonical root just confirms.
    const attrs: Record<string, unknown> = { ...c.attributes };
    if (c.source && attrs._source == null) {
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

function adaptCreateEntityRequest(r: EntityRequest): CanonicalEntityRequest {
    // Strip the meta keys we synthesize on the read side so they don't get
    // duplicated on the canonical record. _kind stays (it carries sub-kind
    // info the canonical enum can't express).
    const { _source, ...restAttrs } = r.attributes as Record<string, unknown>;
    const source = typeof _source === 'string' && _source ? _source : 'local';
    const kind = entityKindFromAttrs(r.uid, r.attributes);
    return {
        entityId: r.uid,
        kind,
        attributes: restAttrs,
        parents: r.parents,
        source,
    };
}

function adaptUpdateEntityRequest(r: EntityRequest): CanonicalUpdateEntityRequest {
    const { _source, ...restAttrs } = r.attributes as Record<string, unknown>;
    return {
        attributes: restAttrs,
        parents: r.parents,
    };
}

// ---- Policy adapters --------------------------------------------------------

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
    // Custom → GLOBAL (no target). Anything else → RESOURCE bound to the
    // picked target's entityId.
    const isGlobal = r.type === 'CUSTOM' || r.target == null;
    return {
        name: r.name,
        kind: isGlobal ? 'GLOBAL' : 'RESOURCE',
        entityId: isGlobal ? null : (r.target?.id ?? null),
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
async function applyStatusTransition(environmentId: string, id: string, status: PolicyStatus | null | undefined): Promise<CanonicalPolicy | null> {
    if (status === 'DEPLOYED') {
        return authzCoreApiClient.post<CanonicalPolicy>(corePath(environmentId, `/policies/${encodeURIComponent(id)}/deploy`));
    }
    if (status === 'DISABLED') {
        return authzCoreApiClient.post<CanonicalPolicy>(corePath(environmentId, `/policies/${encodeURIComponent(id)}/disable`));
    }
    // DRAFT or undefined: leave whatever the base POST/PUT returned.
    return null;
}

// ---- Query string helpers ---------------------------------------------------

function pagingQuery(params?: PaginationParams): string {
    const q = new URLSearchParams();
    if (params?.page !== undefined) q.set('page', String(params.page));
    if (params?.perPage !== undefined) q.set('perPage', String(params.perPage));
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

// =============================================================================
// Public service
// =============================================================================

export const authzApiService = {
    // ── Schema ──────────────────────────────────────────────────────────────
    //
    // Canonical returns { schema }. UI expects { environmentId, schemaText,
    // updatedAt }. environmentId is what the caller already knows; updatedAt
    // is informational only and not surfaced by the canonical endpoint, so we
    // null it out — pages that show "last edited X ago" gracefully render
    // nothing when it's null.
    getSchema: async (environmentId: string): Promise<SchemaResponse> => {
        const c = await authzCoreApiClient.get<CanonicalSchema>(corePath(environmentId, '/schema'));
        return {
            environmentId,
            schemaText: c.schema ?? '',
            updatedAt: null as unknown as string,
        };
    },

    // ── Policies ────────────────────────────────────────────────────────────
    //
    // Server-side paging through ?page/?perPage. Status is forwarded to the
    // backend as a query filter (?status=…); the UI `type` discriminator
    // (MCP/AGENT/…) has no canonical equivalent and is derived from the
    // entityId prefix client-side — for that filter we fetch the matching
    // page and post-filter what came back. That trades a tiny over-fetch
    // for keeping the canonical contract narrow.
    listPolicies: async (environmentId: string, params?: PolicyListParams): Promise<PagedResponse<PolicyResponse>> => {
        const path = corePath(environmentId, '/policies') + policyListQuery(params);
        const raw = await authzCoreApiClient.get<CanonicalPagedResponse<CanonicalPolicy> | readonly CanonicalPolicy[]>(path);
        const response = adaptPagedListResponse(raw, params?.page, params?.perPage);
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

    getPolicy: async (environmentId: string, id: string): Promise<PolicyResponse> => {
        const c = await authzCoreApiClient.get<CanonicalPolicy>(corePath(environmentId, `/policies/${encodeURIComponent(id)}`));
        return adaptPolicyResponse(c);
    },

    createPolicy: async (environmentId: string, request: PolicyRequest): Promise<PolicyResponse> => {
        const created = await authzCoreApiClient.post<CanonicalPolicy>(corePath(environmentId, '/policies'), adaptCreatePolicyRequest(request));
        const afterTransition = await applyStatusTransition(environmentId, created.id, request.status ?? null);
        return adaptPolicyResponse(afterTransition ?? created);
    },

    updatePolicy: async (environmentId: string, id: string, request: PolicyRequest): Promise<PolicyResponse> => {
        const updated = await authzCoreApiClient.put<CanonicalPolicy>(corePath(environmentId, `/policies/${encodeURIComponent(id)}`), adaptUpdatePolicyRequest(request));
        const afterTransition = await applyStatusTransition(environmentId, id, request.status ?? null);
        return adaptPolicyResponse(afterTransition ?? updated);
    },

    deletePolicy: (environmentId: string, id: string) => authzCoreApiClient.delete<void>(corePath(environmentId, `/policies/${encodeURIComponent(id)}`)),

    // ── Entities ────────────────────────────────────────────────────────────
    //
    // Server-side paging through ?page/?perPage. The backend slices in
    // Mongo (skip/limit + count) so a 100k-entity env doesn't drag the
    // whole result into the UI memory just to render 25 rows.
    listEntities: async (environmentId: string, params?: PaginationParams): Promise<PagedResponse<EntityResponse>> => {
        const path = corePath(environmentId, '/entities') + pagingQuery(params);
        const raw = await authzCoreApiClient.get<CanonicalPagedResponse<CanonicalEntity> | readonly CanonicalEntity[]>(path);
        const response = adaptPagedListResponse(raw, params?.page, params?.perPage);
        return {
            data: response.data.map(adaptEntityResponse),
            total: response.total,
            page: response.page,
            perPage: response.perPage,
        };
    },

    getEntity: async (environmentId: string, id: string): Promise<EntityResponse> => {
        const c = await authzCoreApiClient.get<CanonicalEntity>(corePath(environmentId, `/entities/${encodeURIComponent(id)}`));
        return adaptEntityResponse(c);
    },

    createEntity: async (environmentId: string, request: EntityRequest): Promise<EntityResponse> => {
        // Canonical POST is upsert (entityId in body). We always POST regardless
        // of whether the entityId already exists — the canonical engine handles
        // the race correctly.
        const c = await authzCoreApiClient.post<CanonicalEntity>(corePath(environmentId, '/entities'), adaptCreateEntityRequest(request));
        return adaptEntityResponse(c);
    },

    updateEntity: async (environmentId: string, id: string, request: EntityRequest): Promise<EntityResponse> => {
        const c = await authzCoreApiClient.put<CanonicalEntity>(corePath(environmentId, `/entities/${encodeURIComponent(id)}`), adaptUpdateEntityRequest(request));
        return adaptEntityResponse(c);
    },

    deleteEntity: (environmentId: string, id: string) =>
        // Canonical returns CascadeResponse — we ignore the body (UI doesn't
        // surface it today). When the cascade info needs to be shown, change
        // this signature to return the parsed CascadeResponse.
        authzCoreApiClient.delete<void>(corePath(environmentId, `/entities/${encodeURIComponent(id)}`)),

    // ── SCIM Connectors ────────────────────────────────────────────────────
    //
    // SCIM stays on the module-specific URL prefix — these endpoints belong
    // to this module's own UI, not the shared kernel. Shape is unchanged
    // because we kept the backing resources in module-authz.
    listScimConnectors: (environmentId: string) =>
        authzModuleApiClient.get<readonly ScimConnectorResponse[]>(modulePath(environmentId, '/scim-connectors')),

    createScimConnector: (environmentId: string, request: ScimConnectorRequest) =>
        authzModuleApiClient.post<ScimConnectorResponse>(modulePath(environmentId, '/scim-connectors'), request),

    updateScimConnector: (environmentId: string, id: string, request: ScimConnectorRequest) =>
        authzModuleApiClient.put<ScimConnectorResponse>(modulePath(environmentId, `/scim-connectors/${encodeURIComponent(id)}`), request),

    deleteScimConnector: (environmentId: string, id: string) =>
        authzModuleApiClient.delete<void>(modulePath(environmentId, `/scim-connectors/${encodeURIComponent(id)}`)),

    syncScimConnectorNow: (environmentId: string, id: string) =>
        authzModuleApiClient.post<ScimConnectorResponse>(modulePath(environmentId, `/scim-connectors/${encodeURIComponent(id)}/sync`)),
};
