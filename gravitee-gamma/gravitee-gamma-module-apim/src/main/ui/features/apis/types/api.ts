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

export interface PathToVerify {
    path: string;
    host?: string;
}

export interface VerifyApiPathResponse {
    ok: boolean;
    reason?: string;
}

export interface HttpPath {
    path: string;
    overrideAccess?: boolean;
}

export interface VirtualHost {
    host: string;
    path: string;
    overrideAccess?: boolean;
}

export interface HttpListener {
    type: 'HTTP';
    paths?: HttpPath[];
    hosts?: VirtualHost[];
    entrypoints?: { type: string }[];
}

export interface HttpEndpoint {
    name: string;
    type: 'http-proxy';
    weight: number;
    inheritConfiguration: boolean;
    configuration: { target: string };
}

export interface HttpEndpointGroup {
    name: string;
    type: 'http-proxy';
    sharedConfiguration: Record<string, unknown>;
    endpoints: HttpEndpoint[];
}

export type PlanSecurityType = 'KEY_LESS' | 'API_KEY' | 'JWT' | 'OAUTH2' | 'MTLS';

export interface PlanSecurity {
    type: PlanSecurityType;
    configuration?: Record<string, unknown>;
}

export interface CreateApiProxyRequest {
    name: string;
    apiVersion: string;
    description: string;
    type: 'PROXY';
    definitionVersion: 'V4';
    allowedInApiProducts: boolean;
    listeners: HttpListener[];
    endpointGroups: HttpEndpointGroup[];
}

export interface ApiProxyCreated {
    id: string;
    name: string;
}

export interface CreateApiPlanRequest {
    name: string;
    security: PlanSecurity;
    definitionVersion: 'V4';
    mode: 'STANDARD';
}

export interface ApiPlanCreated {
    id: string;
}

// ─── API list & detail shared types ──────────────────────────────────────────

export type ApiState = 'CLOSED' | 'INITIALIZED' | 'STARTED' | 'STOPPED' | 'STOPPING';
export type ApiDeploymentState = 'NEED_REDEPLOY' | 'DEPLOYED';
export type ApiLifecycleState = 'ARCHIVED' | 'CREATED' | 'DEPRECATED' | 'PUBLISHED' | 'UNPUBLISHED';
export type ApiVisibility = 'PUBLIC' | 'PRIVATE';
export type ApiType = 'PROXY' | 'MESSAGE' | 'NATIVE';

export interface ApiListListener {
    type: string;
    paths?: { path: string; host?: string }[];
    hosts?: VirtualHost[];
    host?: string;
    port?: number;
}

export interface ApiListItem {
    id: string;
    name: string;
    apiVersion: string;
    description?: string;
    type: ApiType;
    definitionVersion: 'V4' | 'V2';
    state?: ApiState;
    deploymentState?: ApiDeploymentState;
    lifecycleState?: ApiLifecycleState;
    visibility?: ApiVisibility;
    listeners?: ApiListListener[];
    primaryOwner?: { id?: string; displayName?: string; email?: string };
}

export interface ApiListPagination {
    page: number;
    perPage: number;
    pageCount: number;
    totalCount: number;
}

export interface ApiListResponse {
    data: ApiListItem[];
    pagination: ApiListPagination;
}

export interface ApiSearchQuery {
    query?: string;
    statuses?: string[];
    published?: string[];
    visibilities?: string[];
    apiTypes?: string[];
}

/** Lightweight shape used by the API detail view. */
export interface ApiDetailDto {
    id: string;
    name: string;
    description?: string;
    state?: ApiState;
    type?: ApiType;
    apiVersion?: string;
    definitionVersion?: 'V4' | 'V4_NATIVE';
    tags?: string[];
    groups?: string[];
    disableMembershipNotifications?: boolean;
}

export interface ApiEvent {
    id: string;
    createdAt: string;
    payload: string;
    initiator: { id: string; displayName: string };
    properties: {
        DEPLOYMENT_NUMBER?: string;
        DEPLOYMENT_LABEL?: string;
        API_ID?: string;
    };
}

export interface ApiEventsPage {
    data: ApiEvent[];
    pagination: {
        page: number;
        perPage: number;
        totalCount: number;
        pageCount: number;
        pageItemsCount: number;
    };
}

export interface OrgShardingTag {
    id: string;
    name: string;
    description?: string;
}
