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
    Integration,
    IntegrationDeletedFederatedApisResponse,
    IntegrationFederatedApisResponse,
    IntegrationIngestionResponse,
    IntegrationPreview,
} from '../types/integration';

export async function getIntegration(environmentId: string, integrationId: string): Promise<Integration> {
    return apimFetchJsonV2<Integration>(environmentId, `/integrations/${encodeURIComponent(integrationId)}`);
}

export async function deleteIntegration(environmentId: string, integrationId: string): Promise<void> {
    await apimFetchJsonV2<void>(environmentId, `/integrations/${encodeURIComponent(integrationId)}`, { method: 'DELETE' });
}

export async function listFederatedApis(
    environmentId: string,
    integrationId: string,
    page: number,
    perPage: number,
): Promise<IntegrationFederatedApisResponse> {
    return apimFetchJsonV2<IntegrationFederatedApisResponse>(
        environmentId,
        `/integrations/${encodeURIComponent(integrationId)}/apis?page=${page}&perPage=${perPage}`,
    );
}

export async function previewIntegration(environmentId: string, integrationId: string): Promise<IntegrationPreview> {
    return apimFetchJsonV2<IntegrationPreview>(environmentId, `/integrations/${encodeURIComponent(integrationId)}/_preview`);
}

export async function ingestIntegration(
    environmentId: string,
    integrationId: string,
    apiIds: string[] = [],
): Promise<IntegrationIngestionResponse> {
    return apimFetchJsonV2<IntegrationIngestionResponse>(environmentId, `/integrations/${encodeURIComponent(integrationId)}/_ingest`, {
        method: 'POST',
        body: JSON.stringify({ apiIds }),
    });
}

export async function hasFederatedApis(environmentId: string, integrationId: string): Promise<boolean> {
    const response = await listFederatedApis(environmentId, integrationId, 1, 1);
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
