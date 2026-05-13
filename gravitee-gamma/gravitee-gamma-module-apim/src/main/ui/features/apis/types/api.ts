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
