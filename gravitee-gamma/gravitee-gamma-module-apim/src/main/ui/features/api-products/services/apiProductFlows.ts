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
import type { ApiProductFlows, ApiProductPlanFlows, UpdateApiProductFlowsRequest } from '../types/apiProduct';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

const flowsPath = (productId: string) => `/api-products/${encodeURIComponent(productId)}/flows`;

export async function getApiProductFlows(environmentId: string, productId: string): Promise<ApiProductFlows> {
    return apimFetchJsonV2<ApiProductFlows>(environmentId, flowsPath(productId));
}

export async function updateApiProductFlows(
    environmentId: string,
    productId: string,
    request: UpdateApiProductFlowsRequest,
): Promise<void> {
    await apimFetchJsonV2(environmentId, flowsPath(productId), {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify(request),
    });
}

export async function updateApiProductPlanFlows(environmentId: string, productId: string, plans: ApiProductPlanFlows[]): Promise<void> {
    await apimFetchJsonV2(environmentId, `/api-products/${encodeURIComponent(productId)}/plans/_flows`, {
        method: 'PUT',
        headers: JSON_HEADERS,
        body: JSON.stringify(plans),
    });
}
