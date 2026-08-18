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

import { buildMembershipRoles, type MemberRoleSelections } from './memberRoles';
import type { GroupMember, GroupMembershipPayload, GroupMembershipRole, GroupMemberRoleScope } from '../types/group';
import { OWNER_ROLE, PRIMARY_OWNER_ROLE } from '../types/group';

const SCOPE_LABELS: Readonly<Record<string, string>> = {
    API: 'API',
    APPLICATION: 'Application',
    API_PRODUCT: 'API Product',
    INTEGRATION: 'Integration',
    CLUSTER: 'Cluster',
};

const PRIMARY_OWNER_MODE_USER = 'USER';

export const PRIMARY_OWNER_SCOPES: GroupMemberRoleScope[] = ['API', 'APPLICATION', 'API_PRODUCT', 'INTEGRATION', 'CLUSTER'];

/** Fail closed: until the mode is known and is not USER, PRIMARY_OWNER must stay unavailable. */
export function isPrimaryOwnerUnavailable(mode: string | undefined): boolean {
    return mode === undefined || mode.toUpperCase() === PRIMARY_OWNER_MODE_USER;
}

function roleUpdatePhrase(role: string | undefined): string {
    return role ? ` as ${role.toLowerCase().replace(/_/g, ' ')}` : '';
}

export function membershipFromMember(member: GroupMember, overrides: Record<string, string> = {}): GroupMembershipPayload {
    const merged = { ...(member.roles ?? {}), ...overrides };
    const roles: GroupMembershipRole[] = Object.entries(merged)
        .filter((entry): entry is [string, string] => Boolean(entry[1]))
        .map(([scope, name]) => ({ scope: scope as GroupMembershipRole['scope'], name }));
    return { id: member.id, roles };
}

export function joinScopeLabels(scopes: string[]): string {
    const labels = scopes.map(scope => SCOPE_LABELS[scope] ?? scope);
    if (labels.length <= 1) return labels.join('');
    return `${labels.slice(0, -1).join(', ')} and ${labels[labels.length - 1]}`;
}

export function primaryOwnerScopesOf(member: GroupMember | undefined): GroupMemberRoleScope[] {
    return PRIMARY_OWNER_SCOPES.filter(scope => member?.roles?.[scope] === PRIMARY_OWNER_ROLE);
}

export type EditOwnershipTransfer = {
    existingApiOwner?: GroupMember;
    existingApiProductOwner?: GroupMember;
    isApiDowngrade: boolean;
    isApiProductDowngrade: boolean;
    needsSuccessor: boolean;
    sameOutgoingOwner: boolean;
};

export function analyzeEditOwnershipTransfer(
    member: GroupMember,
    members: GroupMember[],
    selections: Pick<MemberRoleSelections, 'apiRole' | 'apiProductRole'>,
): EditOwnershipTransfer {
    const isApiPrimaryOwner = member.roles?.API === PRIMARY_OWNER_ROLE;
    const isApiProductPrimaryOwner = member.roles?.API_PRODUCT === PRIMARY_OWNER_ROLE;
    const isApiUpgrade = selections.apiRole === PRIMARY_OWNER_ROLE && !isApiPrimaryOwner;
    const isApiProductUpgrade = selections.apiProductRole === PRIMARY_OWNER_ROLE && !isApiProductPrimaryOwner;

    const existingApiOwner = isApiUpgrade ? members.find(m => m.id !== member.id && m.roles?.API === PRIMARY_OWNER_ROLE) : undefined;
    const existingApiProductOwner = isApiProductUpgrade
        ? members.find(m => m.id !== member.id && m.roles?.API_PRODUCT === PRIMARY_OWNER_ROLE)
        : undefined;

    const isApiDowngrade = isApiPrimaryOwner && selections.apiRole !== PRIMARY_OWNER_ROLE;
    const isApiProductDowngrade = isApiProductPrimaryOwner && selections.apiProductRole !== PRIMARY_OWNER_ROLE;

    return {
        existingApiOwner,
        existingApiProductOwner,
        isApiDowngrade,
        isApiProductDowngrade,
        needsSuccessor: isApiDowngrade || isApiProductDowngrade,
        sameOutgoingOwner: Boolean(existingApiOwner && existingApiProductOwner && existingApiOwner.id === existingApiProductOwner.id),
    };
}

export function buildEditOwnershipTransferMessage(
    member: GroupMember,
    transfer: EditOwnershipTransfer,
    selectedSuccessor: GroupMember | null,
    selections?: Pick<MemberRoleSelections, 'apiRole' | 'apiProductRole'>,
): string | null {
    const parts: string[] = [];

    if (selectedSuccessor) {
        if (transfer.isApiDowngrade && transfer.isApiProductDowngrade) {
            const apiDesc = roleUpdatePhrase(selections?.apiRole);
            const apiProductDesc = roleUpdatePhrase(selections?.apiProductRole);
            const roleDesc =
                selections?.apiRole && selections?.apiProductRole && selections.apiRole === selections.apiProductRole
                    ? apiDesc
                    : `${apiDesc} (API) and${apiProductDesc} (API Product)`;
            parts.push(
                `${member.displayName} is the API and API Product primary owner. Primary ownership will be transferred to ${selectedSuccessor.displayName} and ${member.displayName} will be updated${roleDesc}.`,
            );
        } else if (transfer.isApiDowngrade) {
            parts.push(
                `${member.displayName} is the API primary owner. The API primary ownership will be transferred to ${selectedSuccessor.displayName} and ${member.displayName} will be updated${roleUpdatePhrase(selections?.apiRole)}.`,
            );
        } else if (transfer.isApiProductDowngrade) {
            parts.push(
                `${member.displayName} is the API Product primary owner. The API Product primary ownership will be transferred to ${selectedSuccessor.displayName} and ${member.displayName} will be updated${roleUpdatePhrase(selections?.apiProductRole)}.`,
            );
        }
    }

    if (transfer.sameOutgoingOwner && transfer.existingApiOwner) {
        parts.push(
            `${transfer.existingApiOwner.displayName} is the API and API Product primary owner. Primary ownership will be transferred to ${member.displayName} and ${transfer.existingApiOwner.displayName} will be updated as owner.`,
        );
    } else {
        if (transfer.existingApiOwner) {
            parts.push(
                `${transfer.existingApiOwner.displayName} is the API primary owner. The API primary ownership will be transferred to ${member.displayName} and ${transfer.existingApiOwner.displayName} will be updated as owner.`,
            );
        }
        if (transfer.existingApiProductOwner) {
            parts.push(
                `${transfer.existingApiProductOwner.displayName} is the API Product primary owner. The API Product primary ownership will be transferred to ${member.displayName} and ${transfer.existingApiProductOwner.displayName} will be updated as owner.`,
            );
        }
    }

    return parts.length > 0 ? parts.join(' ') : null;
}

/** Builds membership payloads for a member edit that involves primary-ownership changes. */
export function buildEditMembershipPayloads(
    member: GroupMember,
    selections: MemberRoleSelections,
    transfer: EditOwnershipTransfer,
    selectedSuccessor: GroupMember | null,
): GroupMembershipPayload[] {
    const otherOverrides = new Map<string, { member: GroupMember; overrides: Record<string, string> }>();

    function addOverride(target: GroupMember | undefined, scope: string, role: string) {
        if (!target) return;
        const entry = otherOverrides.get(target.id) ?? { member: target, overrides: {} };
        entry.overrides[scope] = role;
        otherOverrides.set(target.id, entry);
    }

    addOverride(transfer.existingApiOwner, 'API', OWNER_ROLE);
    addOverride(transfer.existingApiProductOwner, 'API_PRODUCT', OWNER_ROLE);
    if (transfer.isApiDowngrade) addOverride(selectedSuccessor ?? undefined, 'API', PRIMARY_OWNER_ROLE);
    if (transfer.isApiProductDowngrade) addOverride(selectedSuccessor ?? undefined, 'API_PRODUCT', PRIMARY_OWNER_ROLE);

    return [
        ...Array.from(otherOverrides.values()).map(({ member: m, overrides }) => membershipFromMember(m, overrides)),
        { id: member.id, roles: buildMembershipRoles(selections) },
    ];
}
