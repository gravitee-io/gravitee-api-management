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

import type { Analytics } from './analytics';
import type { PlanSecurity } from './plan';

export interface PathToVerify {
    path: string;
    host?: string;
}

export interface VerifyApiPathResponse {
    ok: boolean;
    reason?: string;
}

export interface VerifyApiHostsResponse {
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

export interface Cors {
    enabled?: boolean;
    allowOrigin?: string[];
    allowMethods?: string[];
    allowHeaders?: string[];
    exposeHeaders?: string[];
    allowCredentials?: boolean;
    maxAge?: number;
    runPolicies?: boolean;
}

export interface HttpListener {
    type: 'HTTP';
    paths?: HttpPath[];
    hosts?: VirtualHost[];
    entrypoints?: { type: string }[];
    cors?: Cors;
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

export type DuplicateFilteredField = 'GROUPS' | 'MEMBERS' | 'PAGES' | 'PLANS';

export interface DuplicateApiOptions {
    contextPath?: string;
    host?: string;
    version: string;
    filteredFields?: DuplicateFilteredField[];
}
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
    picture?: string | null;
    _links?: {
        pictureUrl?: string;
        backgroundUrl?: string;
    };
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

export interface ApiHttpEndpoint {
    name: string;
    type: string;
    configuration?: { target?: string };
}

export interface ApiEndpointGroup {
    name: string;
    type: string;
    endpoints?: ApiHttpEndpoint[];
}

// ─── Rich endpoint types (used by the Endpoints feature) ─────────────────────

export type LoadBalancerType = 'ROUND_ROBIN' | 'RANDOM' | 'WEIGHTED_RANDOM' | 'WEIGHTED_ROUND_ROBIN';

export interface EndpointGroupProxy {
    enabled?: boolean;
    useSystemProxy?: boolean;
    host?: string;
    port?: number;
    username?: string;
    password?: string;
    type?: string;
}

export interface EndpointGroupHttp {
    keepAlive?: boolean;
    keepAliveTimeout?: number;
    readTimeout?: number;
    idleTimeout?: number;
    connectTimeout?: number;
    maxConcurrentConnections?: number;
    useCompression?: boolean;
    propagateClientAcceptEncoding?: boolean;
    propagateClientHost?: boolean;
    pipelining?: boolean;
    followRedirects?: boolean;
    version?: 'HTTP_1_1' | 'HTTP_2';
    clearTextUpgrade?: boolean;
    http2MultiplexingLimit?: number;
    http2ConnectionWindowSize?: number;
    http2StreamWindowSize?: number;
    http2MaxFrameSize?: number;
}

export interface EndpointGroupSsl {
    hostnameVerifier?: boolean;
    trustAll?: boolean;
    /** Legacy / UI-only; not sent on save (V4 ssl schema uses trustStore/keyStore). */
    clientAuthentication?: 'NONE' | 'REQUIRED' | 'OPTIONAL';
    trustStore?: Record<string, unknown>;
    keyStore?: Record<string, unknown>;
}

export interface EndpointGroupHeader {
    name: string;
    value: string;
}

export interface EndpointGroupSharedConfiguration {
    proxy?: EndpointGroupProxy;
    http?: EndpointGroupHttp;
    ssl?: EndpointGroupSsl;
    headers?: EndpointGroupHeader[];
}

export interface HealthCheckHeader {
    name: string;
    value: string;
}

export interface HealthCheckConfiguration {
    schedule?: string;
    method?: string;
    target?: string;
    overrideEndpointPath?: boolean;
    headers?: HealthCheckHeader[];
    assertion?: string;
    successThreshold?: number;
    failureThreshold?: number;
}

export interface HealthCheckService {
    enabled?: boolean;
    type?: string;
    configuration?: HealthCheckConfiguration;
    overrideConfiguration?: boolean;
}

export interface EndpointServices {
    healthCheck?: HealthCheckService;
}

export interface EndpointDto {
    name: string;
    type: string;
    weight?: number;
    backup?: boolean;
    inheritConfiguration?: boolean;
    configuration?: { target?: string; [key: string]: unknown };
    tenants?: string[];
    sharedConfigurationOverride?: Record<string, unknown>;
    services?: EndpointServices;
}

export interface EndpointGroupServices {
    healthCheck?: HealthCheckService;
}

export interface EndpointGroupDto {
    name: string;
    type: string;
    loadBalancer?: { type: LoadBalancerType };
    sharedConfiguration?: EndpointGroupSharedConfiguration;
    endpoints?: EndpointDto[];
    services?: EndpointGroupServices;
}

export interface Failover {
    enabled?: boolean;
    maxRetries?: number;
    slowCallDuration?: number;
    openStateDuration?: number;
    maxFailures?: number;
    perSubscription?: boolean;
    failureCondition?: string;
    forceNextEndpointOnFailure?: boolean;
}

/** Lightweight shape used by the API detail view. */
export interface ApiDetailDto {
    id: string;
    name: string;
    description?: string;
    state?: ApiState;
    deploymentState?: ApiDeploymentState;
    type?: ApiType;
    apiVersion?: string;
    definitionVersion?: 'V4' | 'V4_NATIVE';
    lifecycleState?: ApiLifecycleState;
    visibility?: ApiVisibility;
    tags?: string[];
    labels?: string[];
    categories?: string[];
    groups?: string[];
    allowedInApiProducts?: boolean;
    disableMembershipNotifications?: boolean;
    primaryOwner?: { id?: string; displayName?: string; email?: string };
    createdAt?: string;
    updatedAt?: string;
    /** Always null in GET response — use _links.pictureUrl to display the image. */
    picture?: string | null;
    /** Always null in GET response — use _links.backgroundUrl to display the image. */
    background?: string | null;
    _links?: {
        pictureUrl?: string;
        backgroundUrl?: string;
    };
    properties?: Property[];

    services?: { dynamicProperty?: DynamicPropertyConfig };
    analytics?: Analytics;
    listeners?: HttpListener[];
    endpointGroups?: EndpointGroupDto[];
    failover?: Failover;
    allowMultiJwtOauth2Subscriptions?: boolean;
    /** Populated for APIs synced from an external source (e.g. Kubernetes operator). */
    definitionContext?: {
        origin?: 'MANAGEMENT' | 'KUBERNETES';
        syncFrom?: string;
    };
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

export interface EnvCategory {
    id: string;
    key: string;
    name: string;
    description?: string;
    hidden?: boolean;
}

// ─── Properties types ─────────────────────────────────────────────────────────

export interface Property {
    key: string;
    value: string;
    encrypted?: boolean;
    encryptable?: boolean;
    dynamic?: boolean;
}

export type DynamicPropertyProvider = 'HTTP' | 'CUSTOM';

export interface DynamicPropertyHttpClientOptions {
    connectTimeout?: number;
    readTimeout?: number;
    keepAliveTimeout?: number;
    idleTimeout?: number;
    maxConcurrentConnections?: number;
    keepAlive?: boolean;
    pipelining?: boolean;
    useCompression?: boolean;
    propagateClientAcceptEncoding?: boolean;
    followRedirects?: boolean;
}

export interface DynamicPropertyHttpProxyOptions {
    enabled?: boolean;
    useSystemProxy?: boolean;
    type?: string;
    host?: string;
    port?: number;
    username?: string;
    password?: string;
}

export interface DynamicPropertySslStore {
    type: 'PEM' | 'JKS' | 'PKCS12';
    path?: string;
    password?: string;
    alias?: string;
    /** PEM cert path */
    certPath?: string;
    /** PEM key path */
    keyPath?: string;
    keyPassword?: string;
    content?: string;
    certContent?: string;
    keyContent?: string;
}

export interface DynamicPropertySslOptions {
    hostnameVerifier?: boolean;
    trustAll?: boolean;
    trustStore?: DynamicPropertySslStore | null;
    keyStore?: DynamicPropertySslStore | null;
}

export interface DynamicPropertyHttpConfiguration {
    method?: string;
    url?: string;
    headers?: { name: string; value: string }[];
    body?: string;
    specification?: string;
    useSystemProxy?: boolean;
    httpClientOptions?: DynamicPropertyHttpClientOptions;
    httpProxyOptions?: DynamicPropertyHttpProxyOptions;
    sslOptions?: DynamicPropertySslOptions;
}

export interface DynamicPropertyConfig {
    enabled: boolean;
    schedule?: string;
    provider?: DynamicPropertyProvider;
    configuration?: DynamicPropertyHttpConfiguration;
}

// ─── Tenant ───────────────────────────────────────────────────────────────────

export interface Tenant {
    id: string;
    key: string;
    name: string;
    description?: string;
}

// ─── Entrypoints types ───────────────────────────────────────────────────────

export interface ExposedEntrypoint {
    value: string;
}
