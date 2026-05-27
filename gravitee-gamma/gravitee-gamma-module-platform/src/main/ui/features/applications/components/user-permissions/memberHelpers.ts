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
import type { ApplicationUiMember, SearchableUser } from '../../types/applicationMembers.types';

const APPLICATION_SCOPE = 'APPLICATION';

export function formatRoleLabel(role: string): string {
    return role
        .split('_')
        .map(part => part.charAt(0) + part.slice(1).toLowerCase())
        .join(' ');
}

export function getApplicationRole(member: ApplicationUiMember): string {
    return member.roles?.find(role => role.scope === APPLICATION_SCOPE)?.name ?? member.roles?.[0]?.name ?? '';
}

export function isMemberPrimaryOwner(member: ApplicationUiMember): boolean {
    return member.roles?.some(role => role.name === 'PRIMARY_OWNER') ?? false;
}

export function isSameUser(userA: SearchableUser, userB: SearchableUser): boolean {
    if (userA.id !== null && userA.id !== undefined && userB.id !== null && userB.id !== undefined) return userA.id === userB.id;
    return userA.reference === userB.reference;
}

/** Group members carry roles keyed by scope (typically GROUP). */
export function getGroupMemberRole(member: { roles: Record<string, string> }): string {
    return member.roles.GROUP ?? member.roles.APPLICATION ?? Object.values(member.roles)[0] ?? '—';
}

function addMemberUserLabel(user: SearchableUser): string {
    return user.displayName || user.email || user.reference;
}

/** User-facing message when one or more bulk member adds fail (including partial success). */
export function formatAddMembersResultMessage(
    total: number,
    succeededCount: number,
    failed: ReadonlyArray<{ user: SearchableUser; reason: string }>,
): string {
    const failedDescriptions = failed.map(({ user, reason }) => {
        const label = addMemberUserLabel(user);
        return reason ? `${label} (${reason})` : label;
    });
    const failedList = failedDescriptions.join(', ');

    if (succeededCount === 0) {
        if (total === 1) {
            const only = failed[0];
            if (!only) {
                return 'Failed to add member.';
            }
            return only.reason ? `Failed to add member: ${only.reason}` : `Failed to add ${addMemberUserLabel(only.user)}.`;
        }
        return `Failed to add ${total} members: ${failedList}.`;
    }

    return `Added ${succeededCount} of ${total} members. Failed to add: ${failedList}.`;
}
