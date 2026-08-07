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
import { useEffect, useState } from 'react';

import type { GroupMember, GroupMembershipPayload, GroupMembershipRole, GroupRole } from '../types/group';

const NO_ROLE_VALUE = '__none__';
const PRIMARY_OWNER = 'PRIMARY_OWNER';

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
                    <SelectItem value={NO_ROLE_VALUE}>None</SelectItem>
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

    useEffect(() => {
        if (open && member) {
            setApiRole(member.roles?.API ?? '');
            setApiProductRole(member.roles?.API_PRODUCT ?? '');
            setApplicationRole(member.roles?.APPLICATION ?? '');
            setIntegrationRole(member.roles?.INTEGRATION ?? '');
            setClusterRole(member.roles?.CLUSTER ?? '');
            setGroupAdmin(member.roles?.GROUP === 'ADMIN');
        }
    }, [open, member]);

    if (!member) return null;

    // Downgrading *away* from primary owner still requires picking a successor, which this sheet doesn't
    // support yet — so the select stays locked whenever this member already holds it. Promoting someone
    // *to* primary owner while another member holds it is supported below: the option is always
    // selectable. The previous owner's own membership is deliberately left untouched — the backend hard-
    // blocks any request that explicitly changes a member away from PRIMARY_OWNER while the group still
    // owns APIs/API Products (GroupMembersResource "prevent changing from PRIMARY_OWNER if group owns
    // APIs" guard → StillPrimaryOwnerException), even when a replacement is promoted in the very same
    // request. Promoting this member alone is enough: GroupMembersResource#addGroupMember reassigns the
    // group's actual apiPrimaryOwner/apiProductPrimaryOwner pointer as a side effect of that promotion —
    // the previous owner's per-member role label just isn't clearable from here afterwards.
    const isApiPrimaryOwner = member.roles?.API === PRIMARY_OWNER;
    const isApiProductPrimaryOwner = member.roles?.API_PRODUCT === PRIMARY_OWNER;

    const isApiUpgrade = apiRole === PRIMARY_OWNER && !isApiPrimaryOwner;
    const isApiProductUpgrade = apiProductRole === PRIMARY_OWNER && !isApiProductPrimaryOwner;
    const existingApiOwner = isApiUpgrade ? members.find(m => m.id !== member.id && m.roles?.API === PRIMARY_OWNER) : undefined;
    const existingApiProductOwner = isApiProductUpgrade
        ? members.find(m => m.id !== member.id && m.roles?.API_PRODUCT === PRIMARY_OWNER)
        : undefined;

    // Doesn't claim the previous owner "will be updated as owner" (unlike classic's dialog) — the backend
    // won't let us clear their PRIMARY_OWNER label in the same request that promotes someone else (see the
    // isApiPrimaryOwner/isApiProductPrimaryOwner comment above), so their row keeps showing PRIMARY_OWNER
    // after this save even though the group's actual primary owner has moved to the newly promoted member.
    function buildTransferMessage(): string | null {
        if (existingApiOwner && existingApiProductOwner && existingApiOwner.id === existingApiProductOwner.id) {
            return `${existingApiOwner.displayName} is currently the API and API Product primary owner. Saving will transfer primary ownership to ${member!.displayName}.`;
        }
        const parts: string[] = [];
        if (existingApiOwner) {
            parts.push(
                `${existingApiOwner.displayName} is currently the API primary owner. Saving will transfer primary ownership to ${member!.displayName}.`,
            );
        }
        if (existingApiProductOwner) {
            parts.push(
                `${existingApiProductOwner.displayName} is currently the API Product primary owner. Saving will transfer primary ownership to ${member!.displayName}.`,
            );
        }
        return parts.length > 0 ? parts.join(' ') : null;
    }

    const transferMessage = buildTransferMessage();

    // The backend treats the submitted roles as the complete set for this member — any scope left out
    // here gets its existing role deleted (GroupMembersResource#deleteIfNewAndPreviousRoleNull). So every
    // scope the member currently holds a role in must stay represented unless the operator deliberately
    // cleared it to "None"; GROUP/ADMIN specifically is only ever pushed when checked, mirroring classic's
    // edit-member-dialog (unchecking it just omits the scope, relying on that same delete-when-omitted path).
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
        onSubmit([{ id: member.id, roles: buildRoles() }]);
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
                        <RoleSelect
                            label="API"
                            roles={apiRoles}
                            value={apiRole}
                            onChange={setApiRole}
                            disabled={isApiPrimaryOwner}
                            hint={isApiPrimaryOwner ? 'Primary ownership can’t be transferred from here yet.' : undefined}
                        />
                        <RoleSelect
                            label="API product"
                            roles={apiProductRoles}
                            value={apiProductRole}
                            onChange={setApiProductRole}
                            disabled={isApiProductPrimaryOwner}
                            hint={isApiProductPrimaryOwner ? 'Primary ownership can’t be transferred from here yet.' : undefined}
                        />
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
                    <Button type="button" onClick={handleSubmit} disabled={isSaving}>
                        {isSaving ? 'Saving…' : 'Save'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
