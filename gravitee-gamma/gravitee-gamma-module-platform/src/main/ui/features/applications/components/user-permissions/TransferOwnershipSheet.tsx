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
import { searchUsers } from '../../services/applicationMembers';
import type { ApplicationTransferOwnershipPayload, ApplicationUiMember, SearchableUser } from '../../types/applicationMembers.types';
import { applicationMemberKeys } from '../../utils/queryKeys';

/** Matches AddMembersSheet search dropdown cap (~12rem). */
const USER_SEARCH_RESULTS_MAX_HEIGHT_CLASS = 'max-h-48';

export function TransferOwnershipSheet({
    open,
    members,
    roles,
    onClose,
    onTransfer,
    isTransferring,
}: Readonly<{
    open: boolean;
    members: ApplicationUiMember[];
    roles: string[];
    onClose: () => void;
    onTransfer: (payload: ApplicationTransferOwnershipPayload) => void;
    isTransferring: boolean;
}>) {
    const [tab, setTab] = useState<'member' | 'user'>('member');
    const [selectedMemberId, setSelectedMemberId] = useState('');
    const [selectedRoleOverride, setSelectedRoleOverride] = useState<string | null>(null);
    const selectedRole = selectedRoleOverride ?? roles[0] ?? '';
    const [userSearch, setUserSearch] = useState('');
    const [selectedUser, setSelectedUser] = useState<SearchableUser | null>(null);

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
        queryKey: applicationMemberKeys.userSearchTransfer(deferredQuery),
        queryFn: () => searchUsers(deferredQuery),
        enabled: tab === 'user' && deferredQuery.trim().length >= 2,
        staleTime: 30_000,
    });

    const nonOwnerMembers = useMemo(() => members.filter(m => !isMemberPrimaryOwner(m)), [members]);
    const canSubmit = selectedRole && (tab === 'member' ? !!selectedMemberId : !!selectedUser);

    function handleSubmit() {
        if (!canSubmit) return;
        if (tab === 'member') {
            onTransfer({ id: selectedMemberId, role: selectedRole });
        } else if (selectedUser) {
            onTransfer({
                id: selectedUser.id ?? undefined,
                reference: selectedUser.reference,
                role: selectedRole,
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
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Transfer ownership</SheetTitle>
                    <SheetDescription>Transfer primary ownership of this application to another user.</SheetDescription>
                </SheetHeader>

                <div className="flex min-h-0 flex-1 flex-col gap-8 overflow-y-auto px-4">
                    <div className="flex rounded-lg border overflow-hidden">
                        <Button
                            type="button"
                            variant={tab === 'member' ? 'default' : 'ghost'}
                            className={cn(
                                'h-auto flex-1 rounded-none px-4 py-2.5 text-sm font-semibold',
                                tab !== 'member' && 'text-muted-foreground',
                            )}
                            onClick={() => {
                                setTab('member');
                                setSelectedUser(null);
                                setUserSearch('');
                            }}
                        >
                            Application member
                        </Button>
                        <Button
                            type="button"
                            variant={tab === 'user' ? 'default' : 'ghost'}
                            className={cn(
                                'h-auto flex-1 rounded-none border-l px-4 py-2.5 text-sm font-semibold',
                                tab !== 'user' && 'text-muted-foreground',
                            )}
                            onClick={() => {
                                setTab('user');
                                setSelectedMemberId('');
                            }}
                        >
                            Other user
                        </Button>
                    </div>

                    {tab === 'member' ? (
                        <div className="space-y-2">
                            <Label className="text-sm font-medium">Select application member</Label>
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
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="icon"
                                        className="size-8 shrink-0 opacity-60 hover:opacity-100"
                                        onClick={() => setSelectedUser(null)}
                                        aria-label={`Remove ${selectedUser.displayName}`}
                                    >
                                        <XIcon className="size-4" aria-hidden />
                                    </Button>
                                </div>
                            ) : (
                                <div className="relative">
                                    <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                                    <Input
                                        className="pl-10"
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
                                                <div
                                                    className={`${USER_SEARCH_RESULTS_MAX_HEIGHT_CLASS} overflow-y-auto overscroll-y-contain`}
                                                    data-testid="transfer-ownership-user-search-results"
                                                >
                                                    {(searchResults ?? []).map(u => (
                                                        <Button
                                                            key={u.reference}
                                                            type="button"
                                                            variant="ghost"
                                                            className="h-auto w-full justify-start gap-3 rounded-none px-3 py-2.5 font-normal hover:bg-muted/50"
                                                            onClick={() => {
                                                                setSelectedUser(u);
                                                                setUserSearch('');
                                                            }}
                                                        >
                                                            <MemberAvatar name={u.displayName} />
                                                            <div className="min-w-0 text-left">
                                                                <p className="font-medium truncate">{u.displayName}</p>
                                                                {u.email ? (
                                                                    <p className="text-xs text-muted-foreground truncate">{u.email}</p>
                                                                ) : null}
                                                            </div>
                                                        </Button>
                                                    ))}
                                                </div>
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
