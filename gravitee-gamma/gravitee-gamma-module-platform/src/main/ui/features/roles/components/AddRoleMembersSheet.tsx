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
    ScrollArea,
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
import { useEffect, useState } from 'react';

import { STANDARD_SHEET_WIDTH } from '../../../shared/layout/sheetLayout';
import { searchUsers } from '../../../shared/services/userSearch';
import type { SearchableUser } from '../../../shared/types/userSearch';
import { isSameUser } from '../../../shared/utils/userSearch';
import type { RoleMembershipListItem } from '../types/role';
import { ROLE_SEARCH_DEBOUNCE_MS } from '../utils/paginationConstants';
import { roleKeys } from '../utils/queryKeys';

export function AddRoleMembersSheet({
    open,
    existingMembers,
    onClose,
    onAdd,
    isAdding,
}: Readonly<{
    open: boolean;
    existingMembers: readonly RoleMembershipListItem[];
    onClose: () => void;
    onAdd: (users: SearchableUser[]) => void;
    isAdding: boolean;
}>) {
    const [search, setSearch] = useState('');
    const [debouncedQuery, setDebouncedQuery] = useState('');
    const [selectedUsers, setSelectedUsers] = useState<SearchableUser[]>([]);

    useEffect(() => {
        if (!open) return;
        const timer = setTimeout(() => setDebouncedQuery(search), ROLE_SEARCH_DEBOUNCE_MS);
        return () => clearTimeout(timer);
    }, [open, search]);

    const { data: results, isFetching } = useQuery({
        queryKey: roleKeys.userSearch(debouncedQuery),
        queryFn: () => searchUsers(debouncedQuery),
        enabled: debouncedQuery.trim().length > 0,
        staleTime: 30_000,
    });

    const filteredResults = (results ?? []).filter(
        result =>
            !selectedUsers.some(user => isSameUser(user, result)) &&
            !existingMembers.some(member => member.id === result.id || member.id === result.reference),
    );

    const canSubmit = selectedUsers.length > 0 && !isAdding;

    function handleSelectUser(user: SearchableUser) {
        setSelectedUsers(prev => (prev.some(u => isSameUser(u, user)) ? prev : [...prev, user]));
        setSearch('');
        setDebouncedQuery('');
    }

    function handleClose() {
        if (!isAdding) onClose();
    }

    const addLabel = selectedUsers.length > 1 ? `Add ${selectedUsers.length} members` : 'Add a member';

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && handleClose()}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: STANDARD_SHEET_WIDTH }}>
                <SheetHeader>
                    <SheetTitle>Add Members</SheetTitle>
                    <SheetDescription>Search for users by name or email and add them to this role.</SheetDescription>
                </SheetHeader>

                <ScrollArea className="min-h-0 flex-1">
                    <div className="space-y-6 px-4 pb-4">
                        <div className="space-y-1">
                            <div className="relative">
                                <SearchIcon className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                                <Input
                                    className="pl-10"
                                    placeholder="Search a user by name or email…"
                                    value={search}
                                    onChange={event => setSearch(event.target.value)}
                                    disabled={isAdding}
                                />
                            </div>
                            {search.trim().length > 0 && (
                                <div className="overflow-hidden rounded-lg border bg-background shadow-md">
                                    {isFetching || search !== debouncedQuery ? (
                                        <div className="space-y-2 p-3">
                                            <Skeleton className="h-10 rounded" />
                                            <Skeleton className="h-10 rounded" />
                                        </div>
                                    ) : filteredResults.length === 0 ? (
                                        <p className="px-3 py-4 text-center text-sm text-muted-foreground">No users found.</p>
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
                                                    <div className="min-w-0 text-left">
                                                        <p className="truncate font-medium">{user.displayName}</p>
                                                        {user.email ? (
                                                            <p className="truncate text-xs text-muted-foreground">{user.email}</p>
                                                        ) : null}
                                                    </div>
                                                </Button>
                                            ))}
                                        </div>
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
                                    {selectedUsers.map(user => (
                                        <span
                                            key={user.id ?? user.reference}
                                            className="inline-flex items-center gap-1.5 rounded-md border bg-muted/40 px-2.5 py-1 text-sm font-medium"
                                        >
                                            {user.displayName}
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon-xs"
                                                className="shrink-0 opacity-60 hover:opacity-100"
                                                onClick={() => setSelectedUsers(prev => prev.filter(u => !isSameUser(u, user)))}
                                                aria-label={`Remove ${user.displayName}`}
                                                disabled={isAdding}
                                            >
                                                <XIcon className="size-3" aria-hidden />
                                            </Button>
                                        </span>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>
                </ScrollArea>

                <SheetFooter className="flex-row shrink-0 justify-end border-t">
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isAdding}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={() => onAdd(selectedUsers)} disabled={!canSubmit}>
                        <PlusIcon className="size-4" aria-hidden />
                        {addLabel}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
