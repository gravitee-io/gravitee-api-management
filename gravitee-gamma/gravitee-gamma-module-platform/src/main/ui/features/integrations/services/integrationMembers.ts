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
import { apimFetchJsonOrg, apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { AddIntegrationMember, IntegrationMember, IntegrationMembersResponse, IntegrationRole } from '../types/integrationMembers';

export async function listIntegrationMembers(environmentId: string, integrationId: string): Promise<IntegrationMember[]> {
    // The endpoint pages 10 members by default.
    const response = await apimFetchJsonV2<IntegrationMembersResponse>(
        environmentId,
        `/integrations/${encodeURIComponent(integrationId)}/members?page=1&perPage=100`,
    );
    return response.data ?? [];
}

export async function addIntegrationMember(environmentId: string, integrationId: string, payload: AddIntegrationMember): Promise<void> {
    await apimFetchJsonV2<IntegrationMember>(environmentId, `/integrations/${encodeURIComponent(integrationId)}/members`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
    });
}

export async function updateIntegrationMemberRole(
    environmentId: string,
    integrationId: string,
    memberId: string,
    roleName: string,
): Promise<void> {
    await apimFetchJsonV2<IntegrationMember>(
        environmentId,
        `/integrations/${encodeURIComponent(integrationId)}/members/${encodeURIComponent(memberId)}`,
        {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ memberId, roleName }),
        },
    );
}

export async function removeIntegrationMember(environmentId: string, integrationId: string, memberId: string): Promise<void> {
    await apimFetchJsonV2<void>(
        environmentId,
        `/integrations/${encodeURIComponent(integrationId)}/members/${encodeURIComponent(memberId)}`,
        { method: 'DELETE' },
    );
}

export async function listIntegrationRoles(): Promise<IntegrationRole[]> {
    return apimFetchJsonOrg<IntegrationRole[]>('/configuration/rolescopes/INTEGRATION/roles');
}
