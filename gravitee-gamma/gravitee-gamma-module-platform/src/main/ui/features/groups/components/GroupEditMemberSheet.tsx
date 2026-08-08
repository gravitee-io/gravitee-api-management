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

import {
    Alert,
    AlertDescription,
    Button,
    Checkbox,
    Combobox,
    ComboboxContent,
    ComboboxEmpty,
    ComboboxInput,
    ComboboxItem,
    ComboboxList,
    Label,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import { GroupRoleSelect } from './GroupRoleSelect';
import type { GroupMember, GroupMembershipPayload, GroupMembershipRole, GroupRole } from '../types/group';
import { OWNER_ROLE, PRIMARY_OWNER_ROLE } from '../types/group';
import { isRoleLocked } from '../utils/groupPermissions';

/** Rebuilds a full membership payload for `member`, preserving every role they currently hold and
 *  overlaying `overrides` on top — the backend treats the submitted roles as the complete set for a
 *  member, so omitting an existing scope (e.g. GROUP/ADMIN) would silently revoke it. */
function membershipFromMember(member: GroupMember, overrides: Record<string, string>): GroupMembershipPayload {
    const merged = { ...(member.roles ?? {}), ...overrides };
    const roles: GroupMembershipRole[] = Object.entries(merged)
        .filter((entry): entry is [string, string] => Boolean(entry[1]))
        .map(([scope, name]) => ({ scope: scope as GroupMembershipRole['scope'], name }));
    return { id: member.id, roles };
}

export function GroupEditMemberSheet({
    open,
    groupName,
    member,
    members,
    apiRoles,
    applicationRoles,
    apiProductRoles,
    integrationRoles,
    clusterRoles,
    lockApiRole,
    lockApiProductRole,
    lockApplicationRole,
    canOverrideLocks,
    groupAllowsGroupAdmin,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    groupName: string;
    member: GroupMember | undefined;
    members: GroupMember[];
    apiRoles: GroupRole[];
    applicationRoles: GroupRole[];
    apiProductRoles: GroupRole[];
    integrationRoles: GroupRole[];
    clusterRoles: GroupRole[];
    lockApiRole: boolean;
    lockApiProductRole: boolean;
    lockApplicationRole: boolean;
    canOverrideLocks: boolean;
    groupAllowsGroupAdmin: boolean;
    onClose: () => void;
    onSubmit: (memberships: GroupMembershipPayload[]) => void;
    isSaving: boolean;
}>) {
    const [apiRole, setApiRole] = useState('');
    const [apiProductRole, setApiProductRole] = useState('');
    const [applicationRole, setApplicationRole] = useState('');
    const [integrationRole, setIntegrationRole] = useState('');
    const [clusterRole, setClusterRole] = useState('');
    const [groupAdmin, setGroupAdmin] = useState(false);
    const [selectedSuccessorId, setSelectedSuccessorId] = useState<string | null>(null);

    useEffect(() => {
        if (open && member) {
            setApiRole(member.roles?.API ?? '');
            setApiProductRole(member.roles?.API_PRODUCT ?? '');
            setApplicationRole(member.roles?.APPLICATION ?? '');
            setIntegrationRole(member.roles?.INTEGRATION ?? '');
            setClusterRole(member.roles?.CLUSTER ?? '');
            setGroupAdmin(member.roles?.GROUP === 'ADMIN');
            setSelectedSuccessorId(null);
        }
    }, [open, member]);

    useEffect(() => {
        setSelectedSuccessorId(null);
    }, [apiRole, apiProductRole]);

    const successorCandidates = useMemo(
        () => members.filter(m => m.id !== member?.id).sort((a, b) => a.displayName.localeCompare(b.displayName)),
        [members, member],
    );

    if (!member) return null;

    const apiRoleDisabled = isRoleLocked(lockApiRole, canOverrideLocks);
    const apiProductRoleDisabled = isRoleLocked(lockApiProductRole, canOverrideLocks);
    const applicationRoleDisabled = isRoleLocked(lockApplicationRole, canOverrideLocks);
    const integrationRoleDisabled = !canOverrideLocks;
    const clusterRoleDisabled = !canOverrideLocks;

    const isApiPrimaryOwner = member.roles?.API === PRIMARY_OWNER_ROLE;
    const isApiProductPrimaryOwner = member.roles?.API_PRODUCT === PRIMARY_OWNER_ROLE;

    const isApiUpgrade = apiRole === PRIMARY_OWNER_ROLE && !isApiPrimaryOwner;
    const isApiProductUpgrade = apiProductRole === PRIMARY_OWNER_ROLE && !isApiProductPrimaryOwner;
    const existingApiOwner = isApiUpgrade ? members.find(m => m.id !== member.id && m.roles?.API === PRIMARY_OWNER_ROLE) : undefined;
    const existingApiProductOwner = isApiProductUpgrade
        ? members.find(m => m.id !== member.id && m.roles?.API_PRODUCT === PRIMARY_OWNER_ROLE)
        : undefined;
    const sameOutgoingOwner = Boolean(existingApiOwner && existingApiProductOwner && existingApiOwner.id === existingApiProductOwner.id);

    const isApiDowngrade = isApiPrimaryOwner && apiRole !== PRIMARY_OWNER_ROLE;
    const isApiProductDowngrade = isApiProductPrimaryOwner && apiProductRole !== PRIMARY_OWNER_ROLE;
    const needsSuccessor = isApiDowngrade || isApiProductDowngrade;

    const selectedSuccessor = selectedSuccessorId ? (successorCandidates.find(m => m.id === selectedSuccessorId) ?? null) : null;

    function buildUpgradeMessage(): string | null {
        if (sameOutgoingOwner) {
            return `${existingApiOwner!.displayName} is the API and API Product primary owner. Primary ownership will be transferred to ${member!.displayName} and ${existingApiOwner!.displayName} will be updated as owner.`;
        }
        const parts: string[] = [];
        if (existingApiOwner) {
            parts.push(
                `${existingApiOwner.displayName} is the API primary owner. The API primary ownership will be transferred to ${member!.displayName} and ${existingApiOwner.displayName} will be updated as owner.`,
            );
        }
        if (existingApiProductOwner) {
            parts.push(
                `${existingApiProductOwner.displayName} is the API Product primary owner. The API Product primary ownership will be transferred to ${member!.displayName} and ${existingApiProductOwner.displayName} will be updated as owner.`,
            );
        }
        return parts.length > 0 ? parts.join(' ') : null;
    }

    function buildDowngradeMessage(): string | null {
        if (!selectedSuccessor) return null;
        if (isApiDowngrade && isApiProductDowngrade) {
            return `${member!.displayName} is the API and API Product primary owner. Primary ownership will be transferred to ${selectedSuccessor.displayName} and ${member!.displayName} will be updated as owner.`;
        }
        if (isApiDowngrade) {
            return `${member!.displayName} is the API primary owner. The API primary ownership will be transferred to ${selectedSuccessor.displayName} and ${member!.displayName} will be updated as owner.`;
        }
        if (isApiProductDowngrade) {
            return `${member!.displayName} is the API Product primary owner. The API Product primary ownership will be transferred to ${selectedSuccessor.displayName} and ${member!.displayName} will be updated as owner.`;
        }
        return null;
    }

    const transferMessage = [buildDowngradeMessage(), buildUpgradeMessage()].filter(Boolean).join(' ') || null;
    const canSubmit = !needsSuccessor || Boolean(selectedSuccessor);

    function buildRoles(): GroupMembershipRole[] {
        const roles: GroupMembershipRole[] = [];
        if (apiRole) roles.push({ scope: 'API', name: apiRole });
        if (apiProductRole) roles.push({ scope: 'API_PRODUCT', name: apiProductRole });
        if (applicationRole) roles.push({ scope: 'APPLICATION', name: applicationRole });
        if (integrationRole) roles.push({ scope: 'INTEGRATION', name: integrationRole });
        if (clusterRole) roles.push({ scope: 'CLUSTER', name: clusterRole });
        if (groupAdmin) roles.push({ scope: 'GROUP', name: 'ADMIN' });
        return roles;
    }

    function handleSubmit() {
        if (!member) return;

        const otherOverrides = new Map<string, { member: GroupMember; overrides: Record<string, string> }>();
        function addOverride(target: GroupMember | undefined, scope: string, role: string) {
            if (!target) return;
            const entry = otherOverrides.get(target.id) ?? { member: target, overrides: {} };
            entry.overrides[scope] = role;
            otherOverrides.set(target.id, entry);
        }
        // Upgrade side: demote the owner member is displacing.
        addOverride(existingApiOwner, 'API', OWNER_ROLE);
        addOverride(existingApiProductOwner, 'API_PRODUCT', OWNER_ROLE);
        // Downgrade side: promote the picked successor for the scope(s) member is stepping down from.
        if (isApiDowngrade) addOverride(selectedSuccessor ?? undefined, 'API', PRIMARY_OWNER_ROLE);
        if (isApiProductDowngrade) addOverride(selectedSuccessor ?? undefined, 'API_PRODUCT', PRIMARY_OWNER_ROLE);

        const otherMemberships = Array.from(otherOverrides.values()).map(({ member: m, overrides }) => membershipFromMember(m, overrides));

        onSubmit([...otherMemberships, { id: member.id, roles: buildRoles() }]);
    }

    function handleClose() {
        onClose();
    }

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && handleClose()}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Edit roles</SheetTitle>
                    <SheetDescription>
                        Update {member.displayName}&rsquo;s roles in {groupName}.
                    </SheetDescription>
                </SheetHeader>

                <div className="space-y-6 px-4 pb-4">
                    <div className="grid grid-cols-2 gap-4">
                        <GroupRoleSelect label="API" roles={apiRoles} value={apiRole} onChange={setApiRole} disabled={apiRoleDisabled} />
                        <GroupRoleSelect
                            label="API product"
                            roles={apiProductRoles}
                            value={apiProductRole}
                            onChange={setApiProductRole}
                            disabled={apiProductRoleDisabled}
                        />
                        <GroupRoleSelect
                            label="Application"
                            roles={applicationRoles}
                            value={applicationRole}
                            onChange={setApplicationRole}
                            disabled={applicationRoleDisabled}
                        />
                        <GroupRoleSelect
                            label="Integration"
                            roles={integrationRoles}
                            value={integrationRole}
                            onChange={setIntegrationRole}
                            disabled={integrationRoleDisabled}
                        />
                        <GroupRoleSelect
                            label="Cluster"
                            roles={clusterRoles}
                            value={clusterRole}
                            onChange={setClusterRole}
                            disabled={clusterRoleDisabled}
                        />
                    </div>

                    <div className="space-y-1.5">
                        <label
                            htmlFor="edit-member-group-admin"
                            className="flex items-center gap-2.5 cursor-pointer aria-disabled:cursor-not-allowed aria-disabled:opacity-50"
                            aria-disabled={!groupAllowsGroupAdmin}
                        >
                            <Checkbox
                                id="edit-member-group-admin"
                                checked={groupAdmin}
                                disabled={!groupAllowsGroupAdmin}
                                onCheckedChange={checked => setGroupAdmin(checked === true)}
                            />
                            <span className="text-sm select-none">Group admin</span>
                        </label>
                        <p className="text-xs text-muted-foreground">
                            {groupAllowsGroupAdmin
                                ? 'Lets this member manage the group’s membership themselves.'
                                : 'Enable "Allow adding members via user search" on this group to grant group admin access.'}
                        </p>
                    </div>

                    {needsSuccessor && (
                        <div className="space-y-1.5">
                            <Label htmlFor="edit-member-successor" className="text-sm text-muted-foreground">
                                Search members
                            </Label>
                            <Combobox
                                items={successorCandidates}
                                value={selectedSuccessor}
                                onValueChange={(picked: GroupMember | null) => setSelectedSuccessorId(picked?.id ?? null)}
                                itemToStringLabel={(m: GroupMember) => m.displayName}
                            >
                                <ComboboxInput
                                    id="edit-member-successor"
                                    aria-label="Search members"
                                    placeholder="Search members…"
                                    showClear
                                />
                                <ComboboxContent>
                                    <ComboboxEmpty>No members found</ComboboxEmpty>
                                    <ComboboxList>
                                        {(m: GroupMember) => (
                                            <ComboboxItem key={m.id} value={m}>
                                                {m.displayName}
                                            </ComboboxItem>
                                        )}
                                    </ComboboxList>
                                </ComboboxContent>
                            </Combobox>
                            <p className="text-xs text-muted-foreground">Select a member to transfer primary ownership.</p>
                        </div>
                    )}

                    {transferMessage && (
                        <Alert variant="default">
                            <InfoIcon className="size-4" aria-hidden />
                            <AlertDescription>{transferMessage}</AlertDescription>
                        </Alert>
                    )}
                </div>

                <SheetFooter className="shrink-0 flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleSubmit} disabled={isSaving || !canSubmit}>
                        {isSaving ? 'Saving…' : 'Save'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
