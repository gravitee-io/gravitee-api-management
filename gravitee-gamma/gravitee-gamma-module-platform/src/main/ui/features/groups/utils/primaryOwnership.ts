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

import { buildMembershipRoles, type MemberRoleSelections, type RoleField } from './memberRoles';
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

export const PRIMARY_OWNER_SCOPES = ['API', 'APPLICATION', 'API_PRODUCT', 'INTEGRATION', 'CLUSTER'] as const;
export type PrimaryOwnerScope = (typeof PRIMARY_OWNER_SCOPES)[number];

export const PRIMARY_OWNER_ROLE_FIELD: Record<PrimaryOwnerScope, RoleField> = {
    API: 'apiRole',
    APPLICATION: 'applicationRole',
    API_PRODUCT: 'apiProductRole',
    INTEGRATION: 'integrationRole',
    CLUSTER: 'clusterRole',
};

/** Fail closed: until the mode is known and is not USER, PRIMARY_OWNER must stay unavailable. */
export function isPrimaryOwnerUnavailable(mode: string | undefined): boolean {
    return mode === undefined || mode.toUpperCase() === PRIMARY_OWNER_MODE_USER;
}

function roleUpdatePhrase(role: string | undefined): string {
    return role ? ` as ${role.toLowerCase().replace(/_/g, ' ')}` : '';
}

function compoundApiProductSentence(fromName: string, toName: string, updatedPhrase: string): string {
    return `${fromName} is the API and API Product primary owner. Primary ownership will be transferred to ${toName} and ${fromName} will be updated${updatedPhrase}.`;
}

function scopedTransferSentence(fromName: string, scopes: string[], toName: string, updatedPhrase: string): string {
    const labels = joinScopeLabels(scopes);
    return `${fromName} is the ${labels} primary owner. The ${labels} primary ownership will be transferred to ${toName} and ${fromName} will be updated${updatedPhrase}.`;
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
    existingOwners: Partial<Record<PrimaryOwnerScope, GroupMember>>;
    promotionScopes: PrimaryOwnerScope[];
    downgradeScopes: PrimaryOwnerScope[];
    needsSuccessor: boolean;
};

export function analyzeEditOwnershipTransfer(
    member: GroupMember,
    members: GroupMember[],
    selections: Pick<MemberRoleSelections, RoleField>,
): EditOwnershipTransfer {
    const existingOwners: Partial<Record<PrimaryOwnerScope, GroupMember>> = {};
    const promotionScopes: PrimaryOwnerScope[] = [];
    const downgradeScopes: PrimaryOwnerScope[] = [];

    for (const scope of PRIMARY_OWNER_SCOPES) {
        const selectedRole = selections[PRIMARY_OWNER_ROLE_FIELD[scope]];
        const wasOwner = member.roles?.[scope] === PRIMARY_OWNER_ROLE;
        if (selectedRole === PRIMARY_OWNER_ROLE && !wasOwner) {
            promotionScopes.push(scope);
            const existing = members.find(m => m.id !== member.id && m.roles?.[scope] === PRIMARY_OWNER_ROLE);
            if (existing) existingOwners[scope] = existing;
        }
        if (wasOwner && selectedRole !== PRIMARY_OWNER_ROLE) {
            downgradeScopes.push(scope);
        }
    }

    return {
        existingOwners,
        promotionScopes,
        downgradeScopes,
        needsSuccessor: downgradeScopes.length > 0,
    };
}

export function buildEditOwnershipTransferMessage(
    member: GroupMember,
    transfer: EditOwnershipTransfer,
    selectedSuccessor: GroupMember | null,
    selections?: Pick<MemberRoleSelections, RoleField>,
): string | null {
    const parts: string[] = [];
    const isApiDowngrade = transfer.downgradeScopes.includes('API');
    const isApiProductDowngrade = transfer.downgradeScopes.includes('API_PRODUCT');

    if (selectedSuccessor) {
        if (isApiDowngrade && isApiProductDowngrade) {
            const apiDesc = roleUpdatePhrase(selections?.apiRole);
            const apiProductDesc = roleUpdatePhrase(selections?.apiProductRole);
            const roleDesc =
                selections?.apiRole && selections?.apiProductRole && selections.apiRole === selections.apiProductRole
                    ? apiDesc
                    : `${apiDesc} (API) and${apiProductDesc} (API Product)`;
            parts.push(compoundApiProductSentence(member.displayName, selectedSuccessor.displayName, roleDesc));
        } else {
            if (isApiDowngrade) {
                parts.push(
                    scopedTransferSentence(
                        member.displayName,
                        ['API'],
                        selectedSuccessor.displayName,
                        roleUpdatePhrase(selections?.apiRole),
                    ),
                );
            }
            if (isApiProductDowngrade) {
                parts.push(
                    scopedTransferSentence(
                        member.displayName,
                        ['API_PRODUCT'],
                        selectedSuccessor.displayName,
                        roleUpdatePhrase(selections?.apiProductRole),
                    ),
                );
            }
        }

        const otherDowngrades = transfer.downgradeScopes.filter(scope => scope !== 'API' && scope !== 'API_PRODUCT');
        if (otherDowngrades.length === 1) {
            parts.push(
                scopedTransferSentence(
                    member.displayName,
                    otherDowngrades,
                    selectedSuccessor.displayName,
                    roleUpdatePhrase(selections?.[PRIMARY_OWNER_ROLE_FIELD[otherDowngrades[0]]]),
                ),
            );
        } else if (otherDowngrades.length > 1) {
            parts.push(scopedTransferSentence(member.displayName, otherDowngrades, selectedSuccessor.displayName, ''));
        }
    }

    const existingOwners = transfer.existingOwners;
    const sameOutgoingApiOwner = Boolean(
        existingOwners.API && existingOwners.API_PRODUCT && existingOwners.API.id === existingOwners.API_PRODUCT.id,
    );
    if (sameOutgoingApiOwner && existingOwners.API) {
        parts.push(compoundApiProductSentence(existingOwners.API.displayName, member.displayName, ' as owner'));
    }

    for (const scope of PRIMARY_OWNER_SCOPES) {
        if (sameOutgoingApiOwner && (scope === 'API' || scope === 'API_PRODUCT')) continue;
        const owner = existingOwners[scope];
        if (!owner) continue;
        parts.push(scopedTransferSentence(owner.displayName, [scope], member.displayName, ' as owner'));
    }

    const freshPromotionScopes = transfer.promotionScopes.filter(scope => !existingOwners[scope]);
    if (freshPromotionScopes.length > 0) {
        parts.push(`${member.displayName} will become the ${joinScopeLabels(freshPromotionScopes)} primary owner of this group.`);
    }

    return parts.length > 0 ? parts.join(' ') : null;
}

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

    for (const scope of PRIMARY_OWNER_SCOPES) {
        addOverride(transfer.existingOwners[scope], scope, OWNER_ROLE);
        if (transfer.downgradeScopes.includes(scope)) {
            addOverride(selectedSuccessor ?? undefined, scope, PRIMARY_OWNER_ROLE);
        }
    }

    return [
        ...Array.from(otherOverrides.values()).map(({ member: m, overrides }) => membershipFromMember(m, overrides)),
        { id: member.id, roles: buildMembershipRoles(selections) },
    ];
}
