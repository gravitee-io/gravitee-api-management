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
    IngestedApisResponse,
    IngestionScope,
    Integration,
    IntegrationDeletedFederatedApisResponse,
    IntegrationFederatedApisResponse,
    IntegrationIngestionResponse,
    IntegrationPreview,
} from '../types/integration';

export async function getIntegration(environmentId: string, integrationId: string): Promise<Integration> {
    return apimFetchJsonV2<Integration>(environmentId, `/integrations/${encodeURIComponent(integrationId)}`);
}

export async function previewIntegration(environmentId: string, integrationId: string): Promise<IntegrationPreview> {
    return apimFetchJsonV2<IntegrationPreview>(environmentId, `/integrations/${encodeURIComponent(integrationId)}/_preview`);
}

export async function ingestIntegrationApis(
    environmentId: string,
    integrationId: string,
    scope: IngestionScope,
): Promise<IntegrationIngestionResponse> {
    if (scope.kind === 'SELECTED' && scope.apiIds.length === 0) {
        throw new Error('At least one API must be selected for ingestion.');
    }
    const apiIds = scope.kind === 'ALL' ? [] : scope.apiIds;
    return apimFetchJsonV2<IntegrationIngestionResponse>(environmentId, `/integrations/${encodeURIComponent(integrationId)}/_ingest`, {
        method: 'POST',
        body: JSON.stringify({ apiIds }),
    });
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

export async function listIngestedApis(
    environmentId: string,
    integrationId: string,
    params: { page: number; perPage: number },
): Promise<IngestedApisResponse> {
    const searchParams = new URLSearchParams();
    searchParams.set('page', String(params.page));
    searchParams.set('perPage', String(params.perPage));
    return apimFetchJsonV2<IngestedApisResponse>(
        environmentId,
        `/integrations/${encodeURIComponent(integrationId)}/apis?${searchParams.toString()}`,
    );
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
