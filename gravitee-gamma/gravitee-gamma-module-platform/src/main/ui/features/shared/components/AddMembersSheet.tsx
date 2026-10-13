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
import { PlusIcon, SearchIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useQuery } from '@tanstack/react-query';
import { useDeferredValue, useEffect, useMemo, useState } from 'react';

import { MemberAvatar } from './MemberAvatar';
import { searchUsers } from '../../../shared/services/userSearch';
import type { SearchableUser } from '../../../shared/types/userSearch';
import { organizationUserSearchKeys } from '../../../shared/utils/queryKeys';
import { isSameUser } from '../../../shared/utils/userSearch';
import { AddMembersFailedError } from '../utils/addMembersResult';

function isExistingMember(existingMembers: ReadonlyArray<{ id?: string }>, user: SearchableUser): boolean {
    return existingMembers.some(m => (user.id !== null && user.id !== undefined && m.id === user.id) || m.id === user.reference);
}

export function AddMembersSheet({
    open,
    description,
    roles,
    defaultRole,
    existingMembers,
    onClose,
    onAdd,
    isAdding,
}: Readonly<{
    open: boolean;
    description: string;
    roles: string[];
    defaultRole?: string;
    existingMembers: ReadonlyArray<{ id?: string }>;
    onClose: () => void;
    onAdd: (users: SearchableUser[], roleName: string) => Promise<void>;
    isAdding: boolean;
}>) {
    const [search, setSearch] = useState('');
    const [selectedUsers, setSelectedUsers] = useState<SearchableUser[]>([]);
    const [selectedRoleOverride, setSelectedRoleOverride] = useState<string | null>(null);
    const selectedRole = selectedRoleOverride ?? defaultRole ?? roles[0] ?? '';

    const resetForm = () => {
        setSearch('');
        setSelectedUsers([]);
        setSelectedRoleOverride(null);
    };

    useEffect(() => {
        if (!open) {
            resetForm();
        }
    }, [open]);

    const deferredQuery = useDeferredValue(search);
    const { data: results, isFetching } = useQuery({
        queryKey: organizationUserSearchKeys.search(deferredQuery),
        queryFn: () => searchUsers(deferredQuery),
        enabled: deferredQuery.trim().length >= 2,
        staleTime: 30_000,
    });

    const filteredResults = useMemo(
        () =>
            (results ?? []).filter(result => !selectedUsers.some(u => isSameUser(u, result)) && !isExistingMember(existingMembers, result)),
        [results, selectedUsers, existingMembers],
    );

    const usersToAdd = useMemo(
        () => selectedUsers.filter(user => !isExistingMember(existingMembers, user)),
        [selectedUsers, existingMembers],
    );

    const canSubmit = usersToAdd.length > 0 && !!selectedRole;

    function handleSelectUser(user: SearchableUser) {
        setSelectedUsers(prev => (prev.some(u => isSameUser(u, user)) ? prev : [...prev, user]));
        setSearch('');
    }

    async function handleAdd() {
        try {
            await onAdd(usersToAdd, selectedRole);
        } catch (error) {
            if (error instanceof AddMembersFailedError) {
                setSelectedUsers(error.failedUsers);
            }
        }
    }

    function handleClose() {
        onClose();
    }

    const addLabel = usersToAdd.length > 1 ? `Add ${usersToAdd.length} members` : 'Add member';

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && handleClose()}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Add Members</SheetTitle>
                    <SheetDescription>{description}</SheetDescription>
                </SheetHeader>

                <ScrollArea className="min-h-0 flex-1">
                    <div className="space-y-6 px-4 pb-4">
                        <div className="space-y-1">
                            <div className="relative">
                                <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                                <Input
                                    className="pl-10"
                                    placeholder="Search a user by name or email…"
                                    value={search}
                                    onChange={e => setSearch(e.target.value)}
                                />
                            </div>
                            {search.trim().length >= 2 && (
                                <div className="rounded-lg border shadow-md bg-background overflow-hidden">
                                    {isFetching || search !== deferredQuery ? (
                                        <div className="p-3 space-y-2">
                                            <Skeleton className="h-10 rounded" />
                                            <Skeleton className="h-10 rounded" />
                                        </div>
                                    ) : filteredResults.length === 0 ? (
                                        <p className="px-3 py-4 text-sm text-center text-muted-foreground">No users found.</p>
                                    ) : (
                                        <div className="max-h-48 overflow-y-auto">
                                            {filteredResults.map(user => (
                                                <Button
                                                    key={user.reference}
                                                    type="button"
                                                    variant="ghost"
                                                    className="h-auto w-full justify-start gap-3 rounded-none px-3 py-2.5 font-normal hover:bg-muted/50"
                                                    onClick={() => handleSelectUser(user)}
                                                >
                                                    <MemberAvatar name={user.displayName} />
                                                    <div className="min-w-0 text-left">
                                                        <p className="font-medium truncate">{user.displayName}</p>
                                                        {user.email ? (
                                                            <p className="text-xs text-muted-foreground truncate">{user.email}</p>
                                                        ) : null}
                                                    </div>
                                                </Button>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>

                        {usersToAdd.length > 0 && (
                            <div className="space-y-2">
                                <p className="text-sm text-muted-foreground">
                                    {usersToAdd.length} {usersToAdd.length === 1 ? 'user' : 'users'} selected
                                </p>
                                <div className="flex flex-wrap gap-2">
                                    {usersToAdd.map(u => (
                                        <span
                                            key={u.id ?? u.reference}
                                            className="inline-flex items-center gap-1.5 rounded-md border px-2.5 py-1 text-sm font-medium bg-muted/40"
                                        >
                                            {u.displayName}
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon-xs"
                                                className="shrink-0 opacity-60 hover:opacity-100"
                                                onClick={() => setSelectedUsers(prev => prev.filter(x => !isSameUser(x, u)))}
                                                aria-label={`Remove ${u.displayName}`}
                                            >
                                                <XIcon className="size-3" aria-hidden />
                                            </Button>
                                        </span>
                                    ))}
                                </div>
                            </div>
                        )}

                        <div className="space-y-2">
                            <Label className="text-sm font-medium">Role for new members</Label>
                            <Select value={selectedRole} onValueChange={setSelectedRoleOverride}>
                                <SelectTrigger className="w-full">
                                    <SelectValue placeholder="Select a role…" />
                                </SelectTrigger>
                                <SelectContent>
                                    {roles.map(role => (
                                        <SelectItem key={role} value={role}>
                                            {role}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    </div>
                </ScrollArea>

                <SheetFooter className="shrink-0 flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isAdding}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={() => void handleAdd()} disabled={!canSubmit || isAdding}>
                        <PlusIcon className="size-4" />
                        {addLabel}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
