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

export interface AmEnvironment {
    id: string;
    name?: string;
}

export interface AmDomain {
    id: string;
    name?: string;
    hrid?: string;
}

export interface AmConnectionView {
    baseUrl: string;
    hasAccessToken: boolean;
    defaultDomainId?: string | null;
    defaultDomainHrid?: string | null;
    gatewayUrl?: string | null;
}

export interface AmConnectionRequest {
    baseUrl: string;
    serviceAccountAccessToken?: string | null;
    defaultDomainId?: string | null;
    defaultDomainHrid?: string | null;
    gatewayUrl?: string | null;
}

export interface AmGatewayEntrypoint {
    id: string;
    name?: string;
    url: string;
    defaultEntrypoint: boolean;
}

export interface AmConnectionTestResult {
    ok: boolean;
    status?: number;
    message?: string;
}
