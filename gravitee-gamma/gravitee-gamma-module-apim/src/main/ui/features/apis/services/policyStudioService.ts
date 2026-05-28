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
import { apimFetchJsonV2, apimFetchJsonV2Org } from '../../../shared/api/apimClient';
import type {
    ApiProtocolType,
    ConnectorPlugin,
    PolicyPlugin,
    PolicyStudioApiDetail,
    PolicyStudioPlanPage,
    SharedPolicyGroupPlugin,
} from '../types/policyStudio';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export async function listPolicies(): Promise<PolicyPlugin[]> {
    return apimFetchJsonV2Org<PolicyPlugin[]>('/plugins/policies');
}

export async function getPolicySchema(policyId: string, protocolType: ApiProtocolType): Promise<unknown> {
    return apimFetchJsonV2Org<unknown>(
        `/plugins/policies/${encodeURIComponent(policyId)}/schema?apiProtocolType=${encodeURIComponent(protocolType)}`,
    );
}

export async function getPolicyDocumentation(policyId: string, protocolType: ApiProtocolType): Promise<string> {
    return apimFetchJsonV2Org<string>(
        `/plugins/policies/${encodeURIComponent(policyId)}/documentation-ext?apiProtocolType=${encodeURIComponent(protocolType)}`,
    );
}

export async function listEntrypointPlugins(): Promise<ConnectorPlugin[]> {
    return apimFetchJsonV2Org<ConnectorPlugin[]>('/plugins/entrypoints');
}

export async function listEndpointPlugins(): Promise<ConnectorPlugin[]> {
    return apimFetchJsonV2Org<ConnectorPlugin[]>('/plugins/endpoints');
}

export async function listSharedPolicyGroupPlugins(envId: string): Promise<SharedPolicyGroupPlugin[]> {
    return apimFetchJsonV2<SharedPolicyGroupPlugin[]>(envId, '/shared-policy-groups/policy-plugins');
}

export async function getFullApiDetail(envId: string, apiId: string): Promise<PolicyStudioApiDetail> {
    return apimFetchJsonV2<PolicyStudioApiDetail>(envId, `/apis/${encodeURIComponent(apiId)}`);
}

export async function listPublishedPlans(envId: string, apiId: string): Promise<PolicyStudioPlanPage> {
    // Policy Studio renders all published plan flows at once; paginated loading is not supported yet.
    return apimFetchJsonV2<PolicyStudioPlanPage>(
        envId,
        `/apis/${encodeURIComponent(apiId)}/plans?statuses=PUBLISHED,DEPRECATED&page=1&perPage=9999`,
    );
}

export async function updateApi(envId: string, apiId: string, body: Record<string, unknown>): Promise<void> {
    await apimFetchJsonV2(envId, `/apis/${encodeURIComponent(apiId)}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify(body),
    });
}

export async function getAndUpdatePlanFlows(envId: string, apiId: string, planId: string, flows: unknown[]): Promise<void> {
    const plan = await apimFetchJsonV2<Record<string, unknown>>(
        envId,
        `/apis/${encodeURIComponent(apiId)}/plans/${encodeURIComponent(planId)}`,
    );
    await apimFetchJsonV2(envId, `/apis/${encodeURIComponent(apiId)}/plans/${encodeURIComponent(planId)}`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify({ ...plan, flows }),
    });
}
