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
import { updateApplication, type UpdateApplicationPayload } from './applicationDetail';
import { apimFetchJsonOrg, apimFetchJsonV1Env, apimFetchJsonV2 } from '../../../shared/api/apimClient';
import type { ApplicationListItem } from '../types/application';
import type {
    ApplicationMemberEntity,
    ApplicationRole,
    ApplicationTransferOwnershipPayload,
    ApplicationUiMember,
    EnvironmentGroup,
    GroupMember,
    GroupsPagedResponse,
    SearchableUser,
} from '../types/applicationMembers.types';
import { mapApplicationMemberToUiMember } from '../utils/applicationMemberMapper';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export async function listApplicationMembers(environmentId: string, applicationId: string): Promise<ApplicationUiMember[]> {
    const members = await apimFetchJsonV1Env<ApplicationMemberEntity[]>(
        environmentId,
        `/applications/${encodeURIComponent(applicationId)}/members`,
    );
    return members.map(mapApplicationMemberToUiMember);
}

export async function addApplicationMember(
    environmentId: string,
    applicationId: string,
    payload: { id: string; role: string },
): Promise<ApplicationUiMember> {
    const created = await apimFetchJsonV1Env<ApplicationMemberEntity | undefined>(
        environmentId,
        `/applications/${encodeURIComponent(applicationId)}/members`,
        {
            method: 'POST',
            headers: JSON_HEADERS,
            body: JSON.stringify(payload),
        },
    );
    return mapApplicationMemberToUiMember(created ?? { id: payload.id, displayName: payload.id, role: payload.role });
}

export async function updateApplicationMember(
    environmentId: string,
    applicationId: string,
    member: ApplicationMemberEntity,
): Promise<ApplicationUiMember> {
    const updated = await apimFetchJsonV1Env<ApplicationMemberEntity | undefined>(
        environmentId,
        `/applications/${encodeURIComponent(applicationId)}/members`,
        {
            method: 'POST',
            headers: JSON_HEADERS,
            body: JSON.stringify(member),
        },
    );
    return mapApplicationMemberToUiMember(updated ?? member);
}

export async function deleteApplicationMember(environmentId: string, applicationId: string, userId: string): Promise<void> {
    await apimFetchJsonV1Env<void>(
        environmentId,
        `/applications/${encodeURIComponent(applicationId)}/members?user=${encodeURIComponent(userId)}`,
        { method: 'DELETE' },
    );
}

export async function transferApplicationOwnership(
    environmentId: string,
    applicationId: string,
    payload: ApplicationTransferOwnershipPayload,
): Promise<ApplicationTransferOwnershipPayload> {
    return apimFetchJsonV1Env<ApplicationTransferOwnershipPayload>(
        environmentId,
        `/applications/${encodeURIComponent(applicationId)}/members/transfer_ownership`,
        {
            method: 'POST',
            headers: JSON_HEADERS,
            body: JSON.stringify(payload),
        },
    );
}

export async function listApplicationRoles(): Promise<ApplicationRole[]> {
    return apimFetchJsonOrg<ApplicationRole[]>('/configuration/rolescopes/APPLICATION/roles');
}

export async function listEnvironmentGroups(environmentId: string): Promise<GroupsPagedResponse> {
    return apimFetchJsonV2<GroupsPagedResponse>(environmentId, '/groups?page=1&perPage=9999');
}

/** Resolves groups linked to an application by id (console GroupV2Service.listById). */
export async function searchEnvironmentGroupsByIds(environmentId: string, ids: string[]): Promise<EnvironmentGroup[]> {
    if (ids.length === 0) {
        return [];
    }
    const response = await apimFetchJsonV2<GroupsPagedResponse>(environmentId, `/groups/_search?page=1&perPage=${ids.length}`, {
        method: 'POST',
        headers: JSON_HEADERS,
        body: JSON.stringify({ ids }),
    });
    return response.data ?? [];
}

export async function getGroupMembers(environmentId: string, groupId: string): Promise<GroupMember[]> {
    const response = await apimFetchJsonV2<{
        data?: Array<{ id?: string; displayName?: string; roles?: Array<{ name?: string; scope?: string }> }>;
    }>(environmentId, `/groups/${encodeURIComponent(groupId)}/members?page=1&perPage=100`);
    return (response.data ?? []).map(m => ({
        id: m.id ?? '',
        displayName: m.displayName ?? '',
        roles: Object.fromEntries((m.roles ?? []).map(r => [r.scope ?? '', r.name ?? ''])),
    }));
}

export async function searchUsers(query: string): Promise<SearchableUser[]> {
    return apimFetchJsonOrg<SearchableUser[]>(`/search/users?q=${encodeURIComponent(query)}`);
}

export async function updateApplicationGroups(
    environmentId: string,
    application: ApplicationListItem,
    groupIds: string[],
): Promise<ApplicationListItem> {
    return updateApplication(environmentId, application, buildApplicationUpdatePayload(application, { groups: groupIds }));
}

export async function updateApplicationMembershipNotifications(
    environmentId: string,
    application: ApplicationListItem,
    disableMembershipNotifications: boolean,
): Promise<ApplicationListItem> {
    return updateApplication(
        environmentId,
        application,
        buildApplicationUpdatePayload(application, { disable_membership_notifications: disableMembershipNotifications }),
    );
}

function buildApplicationUpdatePayload(
    application: ApplicationListItem,
    overrides: Partial<UpdateApplicationPayload>,
): UpdateApplicationPayload {
    return {
        name: application.name,
        description: application.description ?? '',
        domain: application.domain,
        groups: application.groups,
        settings: application.settings,
        disable_membership_notifications: application.disable_membership_notifications,
        api_key_mode: application.api_key_mode,
        ...overrides,
    };
}
