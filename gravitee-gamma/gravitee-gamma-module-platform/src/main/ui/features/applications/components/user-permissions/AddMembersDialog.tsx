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
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Skeleton,
} from '@gravitee/graphene-core';
import { PlusIcon, SearchIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useQuery } from '@tanstack/react-query';
import { useDeferredValue, useMemo, useState } from 'react';

import { MemberAvatar } from './MemberAvatar';
import { isSameUser } from './memberHelpers';
import { searchUsers } from '../../services/applicationMembers';
import type { ApplicationUiMember, SearchableUser } from '../../types/applicationMembers.types';
import { applicationMemberKeys } from '../../utils/queryKeys';

export function AddMembersDialog({
    open,
    roles,
    existingMembers,
    onClose,
    onAdd,
    isAdding,
}: Readonly<{
    open: boolean;
    roles: string[];
    existingMembers: ApplicationUiMember[];
    onClose: () => void;
    onAdd: (users: SearchableUser[], roleName: string) => void;
    isAdding: boolean;
}>) {
    const [search, setSearch] = useState('');
    const [selectedUsers, setSelectedUsers] = useState<SearchableUser[]>([]);
    const [selectedRoleOverride, setSelectedRoleOverride] = useState<string | null>(null);
    const selectedRole = selectedRoleOverride ?? roles[0] ?? '';

    // Reset dialog state each time it opens (setState-during-render pattern).
    const [prevOpen, setPrevOpen] = useState(open);
    if (prevOpen !== open) {
        setPrevOpen(open);
        setSearch('');
        setSelectedUsers([]);
        setSelectedRoleOverride(null);
    }

    const deferredQuery = useDeferredValue(search);
    const { data: results, isFetching } = useQuery({
        queryKey: applicationMemberKeys.userSearch(deferredQuery),
        queryFn: () => searchUsers(deferredQuery),
        enabled: deferredQuery.trim().length >= 2,
        staleTime: 30_000,
    });

    const filteredResults = useMemo(
        () =>
            (results ?? []).filter(
                result =>
                    !selectedUsers.some(u => isSameUser(u, result)) &&
                    !existingMembers.some(
                        m => (result.id !== null && result.id !== undefined && m.id === result.id) || m.id === result.reference,
                    ),
            ),
        [results, selectedUsers, existingMembers],
    );

    const canSubmit = selectedUsers.length > 0 && !!selectedRole;

    function handleSelectUser(user: SearchableUser) {
        setSelectedUsers(prev => (prev.some(u => isSameUser(u, user)) ? prev : [...prev, user]));
        setSearch('');
    }

    function handleClose() {
        setSearch('');
        setSelectedUsers([]);
        setSelectedRoleOverride(null);
        onClose();
    }

    const addLabel = selectedUsers.length > 1 ? `Add ${selectedUsers.length} members` : 'Add member';

    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && handleClose()}>
            <DialogContent style={{ maxWidth: '30rem' }}>
                <DialogHeader>
                    <DialogTitle>Add Members</DialogTitle>
                    <DialogDescription>Search for users by name or email and add them to this application.</DialogDescription>
                </DialogHeader>

                <div className="space-y-6">
                    <div className="space-y-1">
                        <div className="relative">
                            <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                            <Input
                                placeholder="Search a user by name or email…"
                                value={search}
                                onChange={e => setSearch(e.target.value)}
                                style={{ paddingLeft: '2.5rem' }}
                            />
                        </div>
                        {search.trim().length >= 2 && (
                            <div className="rounded-lg border shadow-md bg-background overflow-y-auto" style={{ maxHeight: '12rem' }}>
                                {isFetching || search !== deferredQuery ? (
                                    <div className="p-3 space-y-2">
                                        <Skeleton className="h-10 rounded" />
                                        <Skeleton className="h-10 rounded" />
                                    </div>
                                ) : filteredResults.length === 0 ? (
                                    <p className="px-3 py-4 text-sm text-center text-muted-foreground">No users found.</p>
                                ) : (
                                    filteredResults.map(user => (
                                        <button
                                            key={user.reference}
                                            type="button"
                                            onClick={() => handleSelectUser(user)}
                                            className="w-full flex items-center gap-3 px-3 py-2.5 text-left text-sm transition-colors hover:bg-muted/50"
                                        >
                                            <MemberAvatar name={user.displayName} />
                                            <div className="min-w-0">
                                                <p className="font-medium truncate">{user.displayName}</p>
                                                {user.email ? <p className="text-xs text-muted-foreground truncate">{user.email}</p> : null}
                                            </div>
                                        </button>
                                    ))
                                )}
                            </div>
                        )}
                    </div>

                    {selectedUsers.length > 0 && (
                        <div className="space-y-2">
                            <p className="text-sm text-muted-foreground">
                                {selectedUsers.length} {selectedUsers.length === 1 ? 'user' : 'users'} selected
                            </p>
                            <div className="flex flex-wrap gap-2">
                                {selectedUsers.map(u => (
                                    <span
                                        key={u.id ?? u.reference}
                                        className="inline-flex items-center gap-1.5 rounded-md border px-2.5 py-1 text-sm font-medium bg-muted/40"
                                    >
                                        {u.displayName}
                                        <button
                                            type="button"
                                            onClick={() => setSelectedUsers(prev => prev.filter(x => !isSameUser(x, u)))}
                                            className="rounded-sm opacity-60 hover:opacity-100 transition-opacity"
                                            aria-label={`Remove ${u.displayName}`}
                                        >
                                            <XIcon className="size-3" />
                                        </button>
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

                <DialogFooter className="border-t px-6 py-4 gap-2">
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isAdding}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={() => onAdd(selectedUsers, selectedRole)} disabled={!canSubmit || isAdding}>
                        <PlusIcon className="size-4" />
                        {addLabel}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
