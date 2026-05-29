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
import { apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type {
    ApiPlanCreated,
    ApiProxyCreated,
    CreateApiPlanRequest,
    CreateApiProxyRequest,
    PathToVerify,
    VerifyApiHostsResponse,
    VerifyApiPathResponse,
} from '../types';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export async function verifyContextPath(environmentId: string, paths: PathToVerify[], apiId?: string): Promise<VerifyApiPathResponse> {
    return apimFetchJsonV2<VerifyApiPathResponse>(environmentId, `/apis/_verify/paths`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({ paths, ...(apiId ? { apiId } : {}) }),
    });
}

export async function verifyApiHosts(
    environmentId: string,
    listenerType: 'TCP',
    hosts: string[],
    apiId?: string,
): Promise<VerifyApiHostsResponse> {
    return apimFetchJsonV2<VerifyApiHostsResponse>(environmentId, `/apis/_verify/hosts`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({ listenerType, hosts, ...(apiId ? { apiId } : {}) }),
    });
}

export async function createApiProxy(environmentId: string, request: CreateApiProxyRequest): Promise<ApiProxyCreated> {
    return apimFetchJsonV2<ApiProxyCreated>(environmentId, `/apis`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(request),
    });
}

export async function createApiPlan(environmentId: string, apiId: string, plan: CreateApiPlanRequest): Promise<ApiPlanCreated> {
    return apimFetchJsonV2<ApiPlanCreated>(environmentId, `/apis/${apiId}/plans`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify(plan),
    });
}

export async function publishApiPlan(environmentId: string, apiId: string, planId: string): Promise<void> {
    return apimFetchJsonV2(environmentId, `/apis/${apiId}/plans/${planId}/_publish`, { method: 'POST' });
}

export async function startApiProxy(environmentId: string, apiId: string): Promise<void> {
    return apimFetchJsonV2(environmentId, `/apis/${apiId}/_start`, { method: 'POST' });
}
