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
    Button,
    Checkbox,
    Input,
    Label,
    ScrollArea,
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
    Skeleton,
} from '@gravitee/graphene-core';
import { SearchIcon } from '@gravitee/graphene-core/icons';
import { useQuery } from '@tanstack/react-query';
import { useDeferredValue, useEffect, useMemo, useState } from 'react';

import { MemberAvatar } from './MemberAvatar';
import { searchUsers } from '../services/groups';
import type { GroupMember, GroupMembershipPayload, GroupMembershipRole, GroupRole, SearchableUser } from '../types/group';
import { groupKeys } from '../utils/queryKeys';

// Radix Select's controlled `value` can't be an empty string, so unset state is represented by this
// sentinel internally — but classic's add-members-dialog.component.html has no "None" mat-option (every
// default role control always starts from a real value, see initializeForm()), so it isn't rendered as a
// selectable option below.
const NO_ROLE_VALUE = '__none__';
const PRIMARY_OWNER = 'PRIMARY_OWNER';

function isSameUser(a: SearchableUser, b: SearchableUser): boolean {
    if (a.id !== null && a.id !== undefined && b.id !== null && b.id !== undefined) return a.id === b.id;
    return a.reference === b.reference;
}

function RoleSelect({
    label,
    roles,
    value,
    onChange,
    disabledOptionNames,
}: Readonly<{
    label: string;
    roles: GroupRole[];
    value: string;
    onChange: (value: string) => void;
    disabledOptionNames?: Set<string>;
}>) {
    return (
        <div className="space-y-1.5">
            <Label className="text-sm text-muted-foreground">{label}</Label>
            <Select value={value || NO_ROLE_VALUE} onValueChange={v => onChange(v === NO_ROLE_VALUE ? '' : v)}>
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
        </div>
    );
}

export function GroupAddMembersSheet({
    open,
    groupName,
    groupRoles,
    members,
    apiRoles,
    applicationRoles,
    apiProductRoles,
    integrationRoles,
    clusterRoles,
    onClose,
    onSubmit,
    isSaving,
}: Readonly<{
    open: boolean;
    groupName: string;
    /** The group's own configured default roles (`GroupEntity.roles`) — pre-fills API/API product/Application
     *  below, mirroring classic AddMembersDialogComponent's `group.roles['API'] ?? 'USER'` fallback. */
    groupRoles: Record<string, string> | undefined;
    members: GroupMember[];
    apiRoles: GroupRole[];
    applicationRoles: GroupRole[];
    apiProductRoles: GroupRole[];
    integrationRoles: GroupRole[];
    clusterRoles: GroupRole[];
    onClose: () => void;
    onSubmit: (memberships: GroupMembershipPayload[]) => void;
    isSaving: boolean;
}>) {
    const [search, setSearch] = useState('');
    const [selected, setSelected] = useState<SearchableUser[]>([]);
    const [apiRole, setApiRole] = useState('');
    const [apiProductRole, setApiProductRole] = useState('');
    const [applicationRole, setApplicationRole] = useState('');
    const [integrationRole, setIntegrationRole] = useState('');
    const [clusterRole, setClusterRole] = useState('');

    useEffect(() => {
        if (open) {
            setSearch('');
            setSelected([]);
            setApiRole(groupRoles?.API ?? 'USER');
            setApiProductRole(groupRoles?.API_PRODUCT ?? 'USER');
            setApplicationRole(groupRoles?.APPLICATION ?? 'USER');
            // Classic AddMembersDialogComponent hardcodes these two to 'USER' regardless of group config —
            // groups don't have configurable defaults for Integration/Cluster (only API/API product/Application do).
            setIntegrationRole('USER');
            setClusterRole('USER');
        }
    }, [open, groupRoles]);

    // A scope can only have one primary owner — if one already exists, new members can't be granted it
    // here (mirrors classic AddMembersDialogComponent's isPrimaryOwnerDisabled/isApiProductPrimaryOwnerDisabled).
    const apiPrimaryOwnerExists = members.some(m => m.roles?.API === PRIMARY_OWNER);
    const apiProductPrimaryOwnerExists = members.some(m => m.roles?.API_PRODUCT === PRIMARY_OWNER);

    const deferredQuery = useDeferredValue(search);
    const { data: results, isFetching } = useQuery({
        queryKey: groupKeys.userSearch(deferredQuery),
        queryFn: () => searchUsers(deferredQuery),
        enabled: deferredQuery.trim().length >= 2,
        staleTime: 30_000,
    });

    const existingMemberIds = useMemo(() => members.map(m => m.id), [members]);
    const candidates = useMemo(
        () => (results ?? []).filter(u => !existingMemberIds.includes(u.id ?? u.reference)),
        [results, existingMemberIds],
    );

    function toggle(user: SearchableUser) {
        setSelected(prev => (prev.some(u => isSameUser(u, user)) ? prev.filter(u => !isSameUser(u, user)) : [...prev, user]));
    }

    function handleClose() {
        onClose();
    }

    function buildRoles(): GroupMembershipRole[] {
        const roles: GroupMembershipRole[] = [];
        if (apiRole) roles.push({ scope: 'API', name: apiRole });
        if (apiProductRole) roles.push({ scope: 'API_PRODUCT', name: apiProductRole });
        if (applicationRole) roles.push({ scope: 'APPLICATION', name: applicationRole });
        if (integrationRole) roles.push({ scope: 'INTEGRATION', name: integrationRole });
        if (clusterRole) roles.push({ scope: 'CLUSTER', name: clusterRole });
        return roles;
    }

    function handleSubmit() {
        const roles = buildRoles();
        onSubmit(selected.map(user => ({ id: user.id ?? user.reference, reference: user.reference, roles })));
    }

    const canSubmit = selected.length > 0;
    const submitLabel = selected.length > 1 ? `Add ${selected.length} members` : 'Add member';

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && handleClose()}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Add members</SheetTitle>
                    <SheetDescription>Search platform users and assign roles for membership in {groupName}.</SheetDescription>
                </SheetHeader>

                <ScrollArea className="min-h-0 flex-1">
                    <div className="space-y-6 px-4 pb-4">
                        <div className="space-y-3">
                            <Label className="text-sm font-medium">Default roles for selected users</Label>
                            <div className="grid grid-cols-2 gap-4">
                                <RoleSelect
                                    label="API"
                                    roles={apiRoles}
                                    value={apiRole}
                                    onChange={setApiRole}
                                    disabledOptionNames={apiPrimaryOwnerExists ? new Set([PRIMARY_OWNER]) : undefined}
                                />
                                <RoleSelect
                                    label="API product"
                                    roles={apiProductRoles}
                                    value={apiProductRole}
                                    onChange={setApiProductRole}
                                    disabledOptionNames={apiProductPrimaryOwnerExists ? new Set([PRIMARY_OWNER]) : undefined}
                                />
                                <RoleSelect label="Application" roles={applicationRoles} value={applicationRole} onChange={setApplicationRole} />
                                <RoleSelect label="Integration" roles={integrationRoles} value={integrationRole} onChange={setIntegrationRole} />
                                <RoleSelect label="Cluster" roles={clusterRoles} value={clusterRole} onChange={setClusterRole} />
                            </div>
                        </div>

                        <div className="space-y-2">
                            <Label className="text-sm font-medium">Search users</Label>
                            <div className="relative">
                                <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                                <Input
                                    className="pl-10"
                                    placeholder="Search by name or email…"
                                    value={search}
                                    onChange={e => setSearch(e.target.value)}
                                />
                            </div>

                            {deferredQuery.trim().length < 2 ? (
                                <p className="px-1 py-2 text-sm text-muted-foreground">Type at least 2 characters to search for users.</p>
                            ) : isFetching || search !== deferredQuery ? (
                                <div className="space-y-2 p-1">
                                    <Skeleton className="h-10 rounded" />
                                    <Skeleton className="h-10 rounded" />
                                </div>
                            ) : candidates.length === 0 ? (
                                <p className="px-1 py-2 text-sm text-muted-foreground">No users found.</p>
                            ) : (
                                <div className="rounded-lg border">
                                    {candidates.map(user => {
                                        const isChecked = selected.some(u => isSameUser(u, user));
                                        const inputId = `add-member-${user.id ?? user.reference}`;
                                        return (
                                            <label
                                                key={user.id ?? user.reference}
                                                htmlFor={inputId}
                                                className="flex items-center gap-3 border-b px-3 py-2.5 last:border-b-0 hover:bg-muted/50 cursor-pointer"
                                            >
                                                <Checkbox id={inputId} checked={isChecked} onCheckedChange={() => toggle(user)} />
                                                <MemberAvatar name={user.displayName} />
                                                <div className="min-w-0 flex-1">
                                                    <p className="text-sm font-medium truncate">{user.displayName}</p>
                                                    {user.email ? (
                                                        <p className="text-xs text-muted-foreground truncate">{user.email}</p>
                                                    ) : null}
                                                </div>
                                            </label>
                                        );
                                    })}
                                </div>
                            )}
                        </div>

                        {selected.length > 0 && (
                            <p className="text-xs text-muted-foreground">
                                {selected.length} user{selected.length !== 1 ? 's' : ''} selected
                            </p>
                        )}
                    </div>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleSubmit} disabled={!canSubmit || isSaving}>
                        {isSaving ? 'Adding…' : submitLabel}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
