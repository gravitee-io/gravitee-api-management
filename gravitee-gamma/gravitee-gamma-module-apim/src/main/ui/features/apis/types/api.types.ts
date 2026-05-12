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

/** Minimal CreateApiV4 shape for proxy creation (matches Console wizard output). */
export type PathV4 = {
    path: string;
    host?: string;
    overrideAccess?: boolean;
};

export type EntrypointV4 = {
    type: string;
    configuration?: unknown;
    qos?: string;
};

export type ListenerV4 = {
    type: string;
    entrypoints: EntrypointV4[];
    paths?: PathV4[];
    hosts?: string[];
    host?: string;
    port?: number;
};

export type EndpointV4 = {
    name: string;
    type: string;
    weight: number;
    inheritConfiguration: boolean;
    configuration?: unknown;
};

export type EndpointGroupV4 = {
    name?: string;
    type: string;
    sharedConfiguration?: unknown;
    endpoints: EndpointV4[];
};

export type CreateApiV4Payload = {
    definitionVersion: 'V4';
    name: string;
    apiVersion: string;
    description: string;
    type: 'PROXY';
    allowedInApiProducts?: boolean;
    listeners: ListenerV4[];
    endpointGroups: EndpointGroupV4[];
};

export type PlanSecurityType = 'KEY_LESS' | 'API_KEY' | 'JWT' | 'OAUTH2' | 'MTLS';

export type CreatePlanV4Payload = {
    definitionVersion: 'V4';
    name: string;
    description?: string;
    mode: 'STANDARD' | 'PUSH';
    security?: {
        type: PlanSecurityType;
        configuration?: Record<string, unknown>;
    };
    validation?: 'MANUAL' | 'AUTO';
};

export type ApiV4Dto = {
    id: string;
    name?: string;
    apiVersion?: string;
};

export type ApiDetailDto = {
    id: string;
    name: string;
    description?: string;
    apiVersion?: string;
    state?: 'STARTED' | 'STOPPED' | 'CLOSED';
    type?: 'PROXY' | 'MESSAGE';
    definitionVersion?: 'V4' | 'V4_NATIVE';
    tags?: string[];
    groups?: string[];
    disableMembershipNotifications?: boolean;
};

export type PlanV4Dto = {
    id: string;
    name?: string;
};

export type VerifyPathResponse = {
    ok: boolean;
    reason?: string;
};

export type ConnectorPluginDto = {
    id: string;
    name: string;
    supportedApiType?: string;
    supportedListenerType?: string;
    deployed?: boolean;
};

export type ProxyConnectorBootstrap = {
    entrypoint: ConnectorPluginDto;
    endpoint: ConnectorPluginDto;
};

export type OrgTag = {
    id: string;
    name: string;
    description?: string;
    restricted_groups?: string[];
};

export type ApiEvent = {
    id: string;
    type: string;
    payload?: string;
    createdAt: string;
    initiator: { id: string; displayName: string };
    properties: {
        DEPLOYMENT_NUMBER?: string;
        DEPLOYMENT_LABEL?: string;
        USER?: string;
    };
};

export type ApiEventsPage = {
    data: ApiEvent[];
    pagination: {
        page: number;
        perPage: number;
        pageCount: number;
        pageItemsCount: number;
        totalCount: number;
    };
};
