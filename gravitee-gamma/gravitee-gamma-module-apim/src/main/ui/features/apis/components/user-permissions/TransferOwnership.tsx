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
    cn,
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
import { SearchIcon, TriangleAlertIcon, XIcon } from '@gravitee/graphene-core/icons';
import { useQuery } from '@tanstack/react-query';
import { useDeferredValue, useMemo, useState } from 'react';

import { MemberAvatar } from './MemberAvatar';
import { isMemberPrimaryOwner } from './memberHelpers';
import { searchUsers } from '../../services/members';
import type { Member, SearchableUser, TransferOwnershipPayload } from '../../types/members.types';

export function TransferOwnership({
    open,
    members,
    roles,
    onClose,
    onTransfer,
    isTransferring,
}: Readonly<{
    open: boolean;
    members: Member[];
    roles: string[];
    onClose: () => void;
    onTransfer: (payload: TransferOwnershipPayload) => void;
    isTransferring: boolean;
}>) {
    const [tab, setTab] = useState<'member' | 'user'>('member');
    const [selectedMemberId, setSelectedMemberId] = useState('');
    const [selectedRoleOverride, setSelectedRoleOverride] = useState<string | null>(null);
    const selectedRole = selectedRoleOverride ?? roles[0] ?? '';
    const [userSearch, setUserSearch] = useState('');
    const [selectedUser, setSelectedUser] = useState<SearchableUser | null>(null);

    // Reset dialog state each time it opens (setState-during-render pattern).
    const [prevOpen, setPrevOpen] = useState(open);
    if (prevOpen !== open) {
        setPrevOpen(open);
        if (open) {
            setTab('member');
            setSelectedMemberId('');
            setSelectedRoleOverride(null);
            setUserSearch('');
            setSelectedUser(null);
        }
    }

    const deferredQuery = useDeferredValue(userSearch);
    const { data: searchResults, isFetching: isSearching } = useQuery({
        queryKey: ['user-search-transfer', deferredQuery],
        queryFn: () => searchUsers(deferredQuery),
        enabled: tab === 'user' && deferredQuery.trim().length >= 2,
    });

    const nonOwnerMembers = useMemo(() => members.filter(m => !isMemberPrimaryOwner(m)), [members]);
    const canSubmit = selectedRole && (tab === 'member' ? !!selectedMemberId : !!selectedUser);

    function handleSubmit() {
        if (!canSubmit) return;
        if (tab === 'member') {
            onTransfer({ userId: selectedMemberId, userType: 'USER', poRole: selectedRole });
        } else if (selectedUser) {
            onTransfer({
                userId: selectedUser.id ?? undefined,
                userReference: selectedUser.reference,
                userType: 'USER',
                poRole: selectedRole,
            });
        }
    }

    function handleClose() {
        setTab('member');
        setSelectedMemberId('');
        setSelectedRoleOverride(null);
        setUserSearch('');
        setSelectedUser(null);
        onClose();
    }

    return (
        <Sheet open={open} onOpenChange={isOpen => !isOpen && handleClose()}>
            <SheetContent side="right" style={{ maxWidth: '32rem' }}>
                <SheetHeader>
                    <SheetTitle>Transfer ownership</SheetTitle>
                    <SheetDescription>Transfer primary ownership of this API to another user.</SheetDescription>
                </SheetHeader>

                <div className="flex-1 space-y-8 overflow-y-auto px-4">
                    <div className="flex rounded-lg border overflow-hidden">
                        <button
                            type="button"
                            onClick={() => {
                                setTab('member');
                                setSelectedUser(null);
                                setUserSearch('');
                            }}
                            className={cn(
                                'flex-1 px-4 py-2.5 text-sm font-semibold transition-colors',
                                tab === 'member' ? 'bg-primary text-primary-foreground' : 'text-muted-foreground',
                            )}
                        >
                            API Member
                        </button>
                        <button
                            type="button"
                            onClick={() => {
                                setTab('user');
                                setSelectedMemberId('');
                            }}
                            className={cn(
                                'flex-1 px-4 py-2.5 text-sm font-semibold transition-colors border-l',
                                tab === 'user' ? 'bg-primary text-primary-foreground' : 'text-muted-foreground',
                            )}
                        >
                            Other User
                        </button>
                    </div>

                    {tab === 'member' ? (
                        <div className="space-y-2">
                            <Label className="text-sm font-medium">Select API member</Label>
                            <Select value={selectedMemberId} onValueChange={setSelectedMemberId}>
                                <SelectTrigger className="w-full">
                                    <SelectValue placeholder="Choose a member…" />
                                </SelectTrigger>
                                <SelectContent>
                                    {nonOwnerMembers.map(m => (
                                        <SelectItem key={m.id} value={m.id}>
                                            {m.displayName}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>
                    ) : (
                        <div className="space-y-2">
                            <Label className="text-sm font-medium">Search by name or email</Label>
                            {selectedUser ? (
                                <div className="flex items-center gap-3 rounded-lg border px-3 py-2.5">
                                    <MemberAvatar name={selectedUser.displayName} />
                                    <div className="flex-1 min-w-0">
                                        <p className="text-sm font-medium truncate">{selectedUser.displayName}</p>
                                        {selectedUser.email ? (
                                            <p className="text-xs text-muted-foreground truncate">{selectedUser.email}</p>
                                        ) : null}
                                    </div>
                                    <button
                                        type="button"
                                        onClick={() => setSelectedUser(null)}
                                        className="rounded-sm opacity-60 hover:opacity-100 transition-opacity shrink-0"
                                        aria-label={`Remove ${selectedUser.displayName}`}
                                    >
                                        <XIcon className="size-4" />
                                    </button>
                                </div>
                            ) : (
                                <div className="relative">
                                    <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                                    <Input
                                        className="pl-9"
                                        placeholder="Type at least 2 characters…"
                                        value={userSearch}
                                        onChange={e => setUserSearch(e.target.value)}
                                    />
                                    {userSearch.trim().length >= 2 && (
                                        <div className="absolute z-50 w-full top-full mt-1 rounded-lg border shadow-md overflow-hidden bg-background">
                                            {isSearching || userSearch !== deferredQuery ? (
                                                <div className="p-3 space-y-2">
                                                    <Skeleton className="h-10 rounded" />
                                                    <Skeleton className="h-10 rounded" />
                                                </div>
                                            ) : (searchResults ?? []).length === 0 ? (
                                                <p className="px-3 py-4 text-sm text-center text-muted-foreground">No users found.</p>
                                            ) : (
                                                <ScrollArea className="max-h-48">
                                                    {(searchResults ?? []).map(u => (
                                                        <button
                                                            key={u.reference}
                                                            type="button"
                                                            onClick={() => {
                                                                setSelectedUser(u);
                                                                setUserSearch('');
                                                            }}
                                                            className="w-full flex items-center gap-3 px-3 py-2.5 text-left text-sm transition-colors hover:bg-muted/50"
                                                        >
                                                            <MemberAvatar name={u.displayName} />
                                                            <div className="min-w-0">
                                                                <p className="font-medium truncate">{u.displayName}</p>
                                                                {u.email ? (
                                                                    <p className="text-xs text-muted-foreground truncate">{u.email}</p>
                                                                ) : null}
                                                            </div>
                                                        </button>
                                                    ))}
                                                </ScrollArea>
                                            )}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    )}

                    <div className="space-y-2">
                        <Label className="text-sm font-medium">New role for current Primary Owner</Label>
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

                    <div className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/10 p-3">
                        <TriangleAlertIcon className="size-4 shrink-0 mt-0.5 text-destructive" />
                        <div className="text-sm leading-snug text-destructive">
                            <p className="font-semibold">This action is irreversible</p>
                            <p className="mt-1">
                                The current Primary Owner will be reassigned to the{' '}
                                <span className="font-semibold">{selectedRole || '—'}</span> role.
                            </p>
                        </div>
                    </div>
                </div>

                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isTransferring}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleSubmit} disabled={!canSubmit || isTransferring}>
                        Transfer
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
