/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { PORTAL_GROUP_MEMBERS_STORE_NAME, runTransaction } from '../../portals/storage/db';
import { getMembersByTenantId } from '../../tenants/storage/portal-tenant-members.storage';
import type { PortalGroupMember, PortalGroupMemberRole, PortalGroupMemberView } from '../types/permissions.types';

export interface PortalGroupMemberInput {
    groupId: string;
    tenantId: string;
    memberId: string;
    role?: PortalGroupMemberRole;
}

function createGroupMemberId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return `group-member-${crypto.randomUUID()}`;
    }
    return `group-member-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

export async function getGroupMembers(groupId: string): Promise<PortalGroupMember[]> {
    return runTransaction<PortalGroupMember[]>(PORTAL_GROUP_MEMBERS_STORE_NAME, 'readonly', store => {
        const index = store.index('groupId');
        return index.getAll(groupId);
    });
}

export async function getGroupMembersByTenantId(tenantId: string): Promise<PortalGroupMember[]> {
    return runTransaction<PortalGroupMember[]>(PORTAL_GROUP_MEMBERS_STORE_NAME, 'readonly', store => {
        const index = store.index('tenantId');
        return index.getAll(tenantId);
    });
}

/** Joins group membership rows with the tenant directory so the UI has names and emails. */
export async function getGroupMemberViews(
    groupId: string,
    tenantId: string,
): Promise<PortalGroupMemberView[]> {
    const [groupMembers, tenantMembers] = await Promise.all([
        getGroupMembers(groupId),
        getMembersByTenantId(tenantId),
    ]);
    const tenantMemberById = new Map(tenantMembers.map(member => [member.id, member]));

    return groupMembers
        .flatMap(groupMember => {
            const tenantMember = tenantMemberById.get(groupMember.memberId);
            if (!tenantMember) {
                return [];
            }

            return [
                {
                    ...groupMember,
                    userId: tenantMember.userId,
                    displayName: tenantMember.displayName,
                    email: tenantMember.email,
                },
            ];
        })
        .sort((a, b) => a.displayName.localeCompare(b.displayName));
}

export async function savePortalGroupMember(member: PortalGroupMember): Promise<void> {
    await runTransaction(PORTAL_GROUP_MEMBERS_STORE_NAME, 'readwrite', store => store.put(member));
}

export async function addPortalGroupMember(input: PortalGroupMemberInput): Promise<PortalGroupMember> {
    const member: PortalGroupMember = {
        id: createGroupMemberId(),
        groupId: input.groupId,
        tenantId: input.tenantId,
        memberId: input.memberId,
        role: input.role ?? 'member',
    };
    await savePortalGroupMember(member);
    return member;
}

export async function setPortalGroupMemberRole(
    id: string,
    role: PortalGroupMemberRole,
): Promise<PortalGroupMember | undefined> {
    const existing = await runTransaction<PortalGroupMember | undefined>(
        PORTAL_GROUP_MEMBERS_STORE_NAME,
        'readonly',
        store => store.get(id),
    );
    if (!existing) {
        return undefined;
    }

    const updated: PortalGroupMember = { ...existing, role };
    await savePortalGroupMember(updated);
    return updated;
}

export async function deletePortalGroupMember(id: string): Promise<void> {
    await runTransaction(PORTAL_GROUP_MEMBERS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function deleteMembersForGroup(groupId: string): Promise<void> {
    const members = await getGroupMembers(groupId);
    await Promise.all(members.map(member => deletePortalGroupMember(member.id)));
}
