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
import type { Integration, IntegrationDeletedFederatedApisResponse, IntegrationFederatedApisResponse } from '../types/integration';
import { isA2aIntegration } from '../utils/integrationKind';

export async function getIntegration(environmentId: string, integrationId: string): Promise<Integration> {
    const integration = await apimFetchJsonV2<Integration>(environmentId, `/integrations/${encodeURIComponent(integrationId)}`);
    if (!isA2aIntegration(integration)) return integration;
    const { agentStatus: _agentStatus, pendingJob: _pendingJob, ...a2aIntegration } = integration;
    return a2aIntegration;
}

export async function deleteIntegration(environmentId: string, integrationId: string): Promise<void> {
    await apimFetchJsonV2<void>(environmentId, `/integrations/${encodeURIComponent(integrationId)}`, { method: 'DELETE' });
}

export async function hasFederatedApis(environmentId: string, integrationId: string): Promise<boolean> {
    const response = await apimFetchJsonV2<IntegrationFederatedApisResponse>(
        environmentId,
        `/integrations/${encodeURIComponent(integrationId)}/apis?page=1&perPage=1`,
    );
    return response.data.length > 0;
}

export async function deleteFederatedApis(environmentId: string, integrationId: string): Promise<IntegrationDeletedFederatedApisResponse> {
    return apimFetchJsonV2<IntegrationDeletedFederatedApisResponse>(
        environmentId,
        `/integrations/${encodeURIComponent(integrationId)}/apis`,
        {
            method: 'DELETE',
        },
    );
}
