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
export type ApiDetails = {
    name: string;
    version: string;
    description: string;
};

export type VirtualHost = {
    host: string;
    path: string;
    overrideAccess: boolean;
};

export type ProxyConfig = {
    contextPath: string;
    targetUrl: string;
    enableVirtualHosts: boolean;
    virtualHosts: VirtualHost[];
};

export type SecurityConfig =
    | { type: 'keyless' }
    | { type: 'api-key'; planName: string }
    | { type: 'jwt'; planName: string; signature: string; jwksResolver: string; resolverParam: string }
    | { type: 'oauth2'; planName: string; resource: string }
    | { type: 'mtls'; planName: string };

export type ApiCreationState = {
    details: ApiDetails;
    proxy: ProxyConfig;
    security: SecurityConfig;
    deployImmediately: boolean;
};

export type ApiCreationMode = 'scratch' | 'template';
