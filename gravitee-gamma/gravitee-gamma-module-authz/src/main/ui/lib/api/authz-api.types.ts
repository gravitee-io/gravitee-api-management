export type PolicyType = 'MCP' | 'AGENT' | 'LLM' | 'API' | 'EVENT' | 'CUSTOM';
export type PolicyStatus = 'DRAFT' | 'DEPLOYED' | 'DISABLED';

export interface PolicyTarget {
    readonly id: string;
    readonly label: string;
}

export interface PolicyResponse {
    readonly id: string;
    readonly environmentId: string;
    readonly name: string;
    readonly description: string | null;
    readonly policyText: string;
    readonly type: PolicyType;
    readonly target: PolicyTarget | null;
    readonly status: PolicyStatus;
    readonly createdAt: string;
    readonly updatedAt: string;
}

export interface PolicyRequest {
    readonly name: string;
    readonly description?: string | null;
    readonly policyText: string;
    readonly type: PolicyType;
    readonly target?: PolicyTarget | null;
    readonly status?: PolicyStatus | null;
}

export interface EntityResponse {
    readonly id: string;
    readonly environmentId: string;
    readonly uid: string;
    readonly attributes: Record<string, unknown>;
    readonly parents: string[];
    readonly createdAt: string;
    readonly updatedAt: string;
}

export interface EntityRequest {
    readonly uid: string;
    readonly attributes: Record<string, unknown>;
    readonly parents: string[];
}

export interface ScimConnectorRequest {
    readonly name: string;
    readonly url: string;
    readonly token: string;
    readonly importUsers: boolean;
    readonly importGroups: boolean;
}

export interface ScimConnectorResponse {
    readonly id: string;
    readonly environmentId: string;
    readonly name: string;
    readonly url: string;
    readonly importUsers: boolean;
    readonly importGroups: boolean;
    readonly lastSyncAt: string | number | null;
    readonly lastSyncStatus: string | null;
    readonly lastError: string | null;
    readonly lastUsersSynced: number;
    readonly lastGroupsSynced: number;
    readonly lastDeleted: number;
    readonly createdAt: string | number;
    readonly updatedAt: string | number;
}

export interface SchemaResponse {
    readonly environmentId: string;
    readonly schemaText: string;
    readonly updatedAt: string;
}

export interface PagedResponse<T> {
    readonly data: readonly T[];
    readonly total: number;
    readonly page: number;
    readonly perPage: number;
}

export interface ValidationErrorResponse {
    readonly message: string;
    readonly status: number;
    readonly errors: readonly string[];
}

// Note: StatsResponse and the /stats endpoint were dropped during the
// canonical /gamma/authz migration. The dashboard page that depended on it
// was retired with the duplicated REST layer in module-authz. Re-introduce
// when a canonical stats projection lands in authorization-api.

// CatalogServiceType is no longer the shape of a REST response — the /catalog
// endpoint was dropped. Pages still use this enum locally to classify entities
// by their entityId prefix; see authzApiService.deriveServiceType.
export type CatalogServiceType = 'MCP' | 'AGENT' | 'LLM' | 'API' | 'EVENT';

export interface CatalogSubResource {
    readonly id: string;
    readonly name: string;
    readonly description?: string;
    readonly kind: string;
}

export interface CatalogEntry {
    readonly id: string;
    readonly name: string;
    readonly description: string;
    readonly type: CatalogServiceType;
    readonly subResources: readonly CatalogSubResource[];
    readonly badges?: readonly string[];
}

