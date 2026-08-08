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
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Sheet,
    SheetContent,
    SheetDescription,
    SheetFooter,
    SheetHeader,
    SheetTitle,
} from '@gravitee/graphene-core';
import { InfoIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useMemo, useState } from 'react';

import type { GroupMember, GroupMembershipPayload, GroupMembershipRole, GroupRole } from '../types/group';

// Radix Select's controlled `value` can't be an empty string, so unset state is represented by this
// sentinel internally — but classic's edit-member-dialog.component.html has no "None" mat-option (an
// unset scope just renders the select blank until a real role is picked), so it isn't rendered below.
const NO_ROLE_VALUE = '__none__';
const PRIMARY_OWNER = 'PRIMARY_OWNER';
const OWNER = 'OWNER';

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

function RoleSelect({
    label,
    roles,
    value,
    onChange,
    disabled,
    disabledOptionNames,
    hint,
}: Readonly<{
    label: string;
    roles: GroupRole[];
    value: string;
    onChange: (value: string) => void;
    disabled?: boolean;
    disabledOptionNames?: Set<string>;
    hint?: string;
}>) {
    return (
        <div className="space-y-1.5">
            <Label className="text-sm text-muted-foreground">{label}</Label>
            <Select value={value || NO_ROLE_VALUE} onValueChange={v => onChange(v === NO_ROLE_VALUE ? '' : v)} disabled={disabled}>
                <SelectTrigger className="w-full">
                    <SelectValue />
                </SelectTrigger>
                <SelectContent>
                    {roles.map(role => (
                        <SelectItem key={role.name} value={role.name} disabled={disabledOptionNames?.has(role.name)}>
                            {role.name}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
            {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
        </div>
    );
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

    // Classic mirror (edit-member-dialog.component.ts's onChange()): any further role edit invalidates
    // a previously-picked successor, forcing the operator to reconfirm the transfer.
    useEffect(() => {
        setSelectedSuccessorId(null);
    }, [apiRole, apiProductRole]);

    const successorCandidates = useMemo(
        () => members.filter(m => m.id !== member?.id).sort((a, b) => a.displayName.localeCompare(b.displayName)),
        [members, member],
    );

    if (!member) return null;

    const isApiPrimaryOwner = member.roles?.API === PRIMARY_OWNER;
    const isApiProductPrimaryOwner = member.roles?.API_PRODUCT === PRIMARY_OWNER;

    const isApiUpgrade = apiRole === PRIMARY_OWNER && !isApiPrimaryOwner;
    const isApiProductUpgrade = apiProductRole === PRIMARY_OWNER && !isApiProductPrimaryOwner;
    const existingApiOwner = isApiUpgrade ? members.find(m => m.id !== member.id && m.roles?.API === PRIMARY_OWNER) : undefined;
    const existingApiProductOwner = isApiProductUpgrade
        ? members.find(m => m.id !== member.id && m.roles?.API_PRODUCT === PRIMARY_OWNER)
        : undefined;
    const sameOutgoingOwner = Boolean(existingApiOwner && existingApiProductOwner && existingApiOwner.id === existingApiProductOwner.id);

    // member is currently the primary owner of a scope and is being moved off it — classic requires
    // picking a successor before the change can be saved (edit-member-dialog.component.ts's
    // isRoleDowngrade()/downgradedMember).
    const isApiDowngrade = isApiPrimaryOwner && apiRole !== PRIMARY_OWNER;
    const isApiProductDowngrade = isApiProductPrimaryOwner && apiProductRole !== PRIMARY_OWNER;
    const needsSuccessor = isApiDowngrade || isApiProductDowngrade;

    const selectedSuccessor = selectedSuccessorId ? (successorCandidates.find(m => m.id === selectedSuccessorId) ?? null) : null;

    // Mirrors edit-member-dialog.component.ts's buildUpgradeMessage() wording exactly.
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

    // Mirrors edit-member-dialog.component.ts's downgrade branch of buildOwnershipTransferMessage() —
    // only shown once a successor is actually picked, same as classic.
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
        addOverride(existingApiOwner, 'API', OWNER);
        addOverride(existingApiProductOwner, 'API_PRODUCT', OWNER);
        // Downgrade side: promote the picked successor for the scope(s) member is stepping down from.
        if (isApiDowngrade) addOverride(selectedSuccessor ?? undefined, 'API', PRIMARY_OWNER);
        if (isApiProductDowngrade) addOverride(selectedSuccessor ?? undefined, 'API_PRODUCT', PRIMARY_OWNER);

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
                        <RoleSelect label="API" roles={apiRoles} value={apiRole} onChange={setApiRole} />
                        <RoleSelect label="API product" roles={apiProductRoles} value={apiProductRole} onChange={setApiProductRole} />
                        <RoleSelect label="Application" roles={applicationRoles} value={applicationRole} onChange={setApplicationRole} />
                        <RoleSelect label="Integration" roles={integrationRoles} value={integrationRole} onChange={setIntegrationRole} />
                        <RoleSelect label="Cluster" roles={clusterRoles} value={clusterRole} onChange={setClusterRole} />
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
