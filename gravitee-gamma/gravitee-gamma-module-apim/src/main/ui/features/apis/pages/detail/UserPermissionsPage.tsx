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
    Badge,
    Button,
    Card,
    Checkbox,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Empty,
    EmptyContent,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Input,
    Label,
    ScrollArea,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Separator,
    Skeleton,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import {
    BellIcon,
    MoreHorizontalIcon,
    PencilIcon,
    PlusIcon,
    SearchIcon,
    ShieldCheckIcon,
    Trash2Icon,
    TriangleAlertIcon,
    UserCogIcon,
    UsersIcon,
    XIcon,
} from '@gravitee/graphene-core/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useDeferredValue, useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';

import { useApimRuntime } from '../../../../core/context/apimRuntimeContext';
import { apiMemberKeys, apiDetailKeys, groupKeys } from '../../../../utils/queryKeys';
import { useApiDetailContext } from '../../context/ApiDetailContext';
import { useApiGroupMembers } from '../../hooks/useApiGroupMembers';
import { useApiMembers } from '../../hooks/useApiMembers';
import { useApiRoles } from '../../hooks/useApiRoles';
import { useGroups } from '../../hooks/useGroups';
import {
    addApiMember,
    deleteApiMember,
    updateApiMember,
    transferApiOwnership,
    updateApiGroups,
    searchUsers,
    updateApiNotifications,
} from '../../services/members';
import type { Member, Group, SearchableUser, TransferOwnershipPayload } from '../../types/members.types';

// ── Avatar ────────────────────────────────────────────────────────────────────

function MemberAvatar({ name }: Readonly<{ name: string }>) {
    const initials = name
        .split(' ')
        .map(namePart => namePart[0] ?? '')
        .join('')
        .toUpperCase()
        .slice(0, 2);
    return (
        <div className="size-8 rounded-full bg-primary/10 flex items-center justify-center shrink-0">
            <span className="text-xs font-semibold text-primary">{initials || '?'}</span>
        </div>
    );
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function getApiRole(member: Member): string {
    return member.roles?.find(role => role.scope === 'API')?.name ?? '';
}

function isMemberPrimaryOwner(member: Member): boolean {
    return member.roles?.some(role => role.name === 'PRIMARY_OWNER') ?? false;
}

function isSameUser(userA: SearchableUser, userB: SearchableUser): boolean {
    if (userA.id != null && userB.id != null) return userA.id === userB.id;
    return userA.reference === userB.reference;
}

function RoleBadge({ roleName, isPO }: Readonly<{ roleName: string; isPO: boolean }>) {
    if (isPO) {
        return (
            <Badge className="gap-1 bg-primary/10 text-primary border-transparent font-normal">
                <ShieldCheckIcon className="size-3" aria-hidden="true" />
                Primary Owner
            </Badge>
        );
    }
    return (
        <Badge variant="secondary" className="font-normal">
            {roleName || '—'}
        </Badge>
    );
}

// ── Remove Confirmation Dialog ────────────────────────────────────────────────

function RemoveMemberDialog({
    member,
    isRemoving,
    onConfirm,
    onCancel,
}: Readonly<{ member: Member | null; isRemoving: boolean; onConfirm: () => void; onCancel: () => void }>) {
    return (
        <Dialog open={member !== null} onOpenChange={open => !open && onCancel()}>
            <DialogContent className="max-w-sm">
                <DialogHeader>
                    <DialogTitle>Remove API member</DialogTitle>
                    <DialogDescription>
                        Are you sure you want to remove <span className="font-semibold text-foreground">{member?.displayName}</span> from
                        this API? They will lose all access immediately.
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter>
                    <Button type="button" variant="outline" onClick={onCancel} disabled={isRemoving}>
                        Cancel
                    </Button>
                    <Button type="button" variant="destructive" onClick={onConfirm} disabled={isRemoving}>
                        Remove
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

// ── Add Members Dialog ────────────────────────────────────────────────────────

function AddMembersDialog({
    open,
    roles,
    existingMembers,
    onClose,
    onAdd,
    isAdding,
}: Readonly<{
    open: boolean;
    roles: string[];
    existingMembers: Member[];
    onClose: () => void;
    onAdd: (users: SearchableUser[], roleName: string) => void;
    isAdding: boolean;
}>) {
    const runtime = useApimRuntime();
    const [search, setSearch] = useState('');
    const [selectedUsers, setSelectedUsers] = useState<SearchableUser[]>([]);
    const [selectedRole, setSelectedRole] = useState(roles[0] ?? '');

    const deferredQuery = useDeferredValue(search);
    const { data: results, isFetching } = useQuery({
        queryKey: ['user-search', runtime.managementBaseURL, runtime.organizationId, deferredQuery],
        queryFn: () => searchUsers(runtime, deferredQuery),
        enabled: deferredQuery.trim().length >= 2,
        staleTime: 30_000,
    });

    // Reset all fields whenever the dialog opens (handles external close via mutation onSuccess)
    useEffect(() => {
        if (!open) return;
        setSearch('');
        setSelectedUsers([]);
        setSelectedRole(roles[0] ?? '');
    }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

    useEffect(() => {
        setSelectedRole(prev => (prev === '' && roles.length > 0 ? roles[0] : prev));
    }, [roles]);

    const filteredResults = useMemo(
        () =>
            (results ?? []).filter(
                result =>
                    !selectedUsers.some(selectedUser => isSameUser(selectedUser, result)) &&
                    !existingMembers.some(
                        existingMember => (result.id != null && existingMember.id === result.id) || existingMember.id === result.reference,
                    ),
            ),
        [results, selectedUsers, existingMembers],
    );

    const canSubmit = selectedUsers.length > 0 && !!selectedRole;

    function handleSelectUser(user: SearchableUser) {
        setSelectedUsers(prev => (prev.some(existingUser => isSameUser(existingUser, user)) ? prev : [...prev, user]));
        setSearch('');
    }

    function handleRemoveSelected(user: SearchableUser) {
        setSelectedUsers(prev => prev.filter(existingUser => !isSameUser(existingUser, user)));
    }

    function handleClose() {
        setSearch('');
        setSelectedUsers([]);
        setSelectedRole(roles[0] ?? '');
        onClose();
    }

    function handleAdd() {
        if (selectedUsers.length > 0 && selectedRole) onAdd(selectedUsers, selectedRole);
    }

    const addLabel = selectedUsers.length > 1 ? `Add ${selectedUsers.length} members` : 'Add member';

    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && handleClose()}>
            <DialogContent style={{ maxWidth: '480px', overflow: 'visible' }}>
                <DialogHeader>
                    <DialogTitle>Add Members</DialogTitle>
                    <DialogDescription>Search for users by name or email and add them to this API.</DialogDescription>
                </DialogHeader>

                <div className="space-y-6">
                    <div className="relative">
                        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                        <Input
                            style={{ paddingLeft: '2.5rem' }}
                            placeholder="Search a user by name or email…"
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                        />
                        {search.trim().length >= 2 && (
                            <div
                                className="absolute w-full top-full mt-1 rounded-lg border shadow-md overflow-hidden"
                                style={{ zIndex: 50, background: 'var(--color-background, #fff)' }}
                            >
                                {isFetching || search !== deferredQuery ? (
                                    <div className="p-3 space-y-2">
                                        <Skeleton className="h-10 rounded" />
                                        <Skeleton className="h-10 rounded" />
                                    </div>
                                ) : filteredResults.length === 0 ? (
                                    <p className="px-3 py-4 text-sm text-center text-muted-foreground">No users found.</p>
                                ) : (
                                    <ScrollArea style={{ maxHeight: '200px' }}>
                                        {filteredResults.map(user => (
                                            <button
                                                key={user.reference}
                                                type="button"
                                                onClick={() => handleSelectUser(user)}
                                                className="w-full flex items-center gap-3 px-3 py-2.5 text-left text-sm transition-colors hover:bg-muted/50"
                                            >
                                                <MemberAvatar name={user.displayName} />
                                                <div className="min-w-0">
                                                    <p className="font-medium truncate">{user.displayName}</p>
                                                    {user.email ? (
                                                        <p className="text-xs text-muted-foreground truncate">{user.email}</p>
                                                    ) : null}
                                                </div>
                                            </button>
                                        ))}
                                    </ScrollArea>
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
                                {selectedUsers.map(selectedUser => (
                                    <span
                                        key={selectedUser.id ?? selectedUser.reference}
                                        className="inline-flex items-center gap-1.5 rounded-md border px-2.5 py-1 text-sm font-medium bg-muted/40"
                                    >
                                        {selectedUser.displayName}
                                        <button
                                            type="button"
                                            onClick={() => handleRemoveSelected(selectedUser)}
                                            className="rounded-sm opacity-60 hover:opacity-100 transition-opacity"
                                            aria-label={`Remove ${selectedUser.displayName}`}
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
                        <Select value={selectedRole} onValueChange={setSelectedRole}>
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

                <DialogFooter>
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isAdding}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleAdd} disabled={!canSubmit || isAdding}>
                        <PlusIcon className="size-4" />
                        {addLabel}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

// ── Transfer Ownership Dialog ─────────────────────────────────────────────────

function TransferOwnershipDialog({
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
    const runtime = useApimRuntime();
    const [tab, setTab] = useState<'member' | 'user'>('member');
    const [selectedMemberId, setSelectedMemberId] = useState('');
    const [selectedRole, setSelectedRole] = useState(roles[0] ?? '');
    const [userSearch, setUserSearch] = useState('');
    const [selectedUser, setSelectedUser] = useState<SearchableUser | null>(null);

    const deferredQuery = useDeferredValue(userSearch);
    const { data: searchResults, isFetching: isSearching } = useQuery({
        queryKey: ['user-search-transfer', runtime.managementBaseURL, runtime.organizationId, deferredQuery],
        queryFn: () => searchUsers(runtime, deferredQuery),
        enabled: tab === 'user' && deferredQuery.trim().length >= 2,
        staleTime: 30_000,
    });

    // Reset all fields whenever the dialog opens (handles external close via mutation onSuccess)
    useEffect(() => {
        if (!open) return;
        setTab('member');
        setSelectedMemberId('');
        setSelectedRole(roles[0] ?? '');
        setUserSearch('');
        setSelectedUser(null);
    }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

    useEffect(() => {
        setSelectedRole(prev => (prev === '' && roles.length > 0 ? roles[0] : prev));
    }, [roles]);

    const nonOwnerMembers = useMemo(() => members.filter(member => !isMemberPrimaryOwner(member)), [members]);
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
        setSelectedRole(roles[0] ?? '');
        setUserSearch('');
        setSelectedUser(null);
        onClose();
    }

    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && handleClose()}>
            <DialogContent style={{ maxWidth: '560px', overflow: 'visible' }}>
                <DialogHeader>
                    <DialogTitle>Transfer ownership</DialogTitle>
                    <DialogDescription>Transfer ownership and grant primary access to your API to another user.</DialogDescription>
                </DialogHeader>

                <div className="space-y-8">
                    <div className="flex rounded-lg border overflow-hidden">
                        <button
                            type="button"
                            onClick={() => {
                                setTab('member');
                                setSelectedUser(null);
                                setUserSearch('');
                            }}
                            className="flex-1 px-4 py-2.5 text-sm font-semibold transition-colors"
                            style={
                                tab === 'member'
                                    ? { backgroundColor: 'var(--color-primary, #c2410c)', color: '#fff' }
                                    : { color: 'var(--color-muted-foreground, #6b7280)' }
                            }
                        >
                            API Member
                        </button>
                        <button
                            type="button"
                            onClick={() => {
                                setTab('user');
                                setSelectedMemberId('');
                            }}
                            className="flex-1 px-4 py-2.5 text-sm font-semibold transition-colors border-l"
                            style={
                                tab === 'user'
                                    ? { backgroundColor: 'var(--color-primary, #c2410c)', color: '#fff' }
                                    : { color: 'var(--color-muted-foreground, #6b7280)' }
                            }
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
                                    {nonOwnerMembers.map(member => (
                                        <SelectItem key={member.id} value={member.id}>
                                            {member.displayName}
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
                                        style={{ paddingLeft: '2.5rem' }}
                                        placeholder="Type at least 2 characters…"
                                        value={userSearch}
                                        onChange={e => setUserSearch(e.target.value)}
                                    />
                                    {userSearch.trim().length >= 2 && (
                                        <div
                                            className="absolute w-full top-full mt-1 rounded-lg border shadow-md overflow-hidden"
                                            style={{ zIndex: 50, background: 'var(--color-background, #fff)' }}
                                        >
                                            {isSearching || userSearch !== deferredQuery ? (
                                                <div className="p-3 space-y-2">
                                                    <Skeleton className="h-10 rounded" />
                                                    <Skeleton className="h-10 rounded" />
                                                </div>
                                            ) : (searchResults ?? []).length === 0 ? (
                                                <p className="px-3 py-4 text-sm text-center text-muted-foreground">No users found.</p>
                                            ) : (
                                                <ScrollArea style={{ maxHeight: '200px' }}>
                                                    {(searchResults ?? []).map(searchUser => (
                                                        <button
                                                            key={searchUser.reference}
                                                            type="button"
                                                            onClick={() => {
                                                                setSelectedUser(searchUser);
                                                                setUserSearch('');
                                                            }}
                                                            className="w-full flex items-center gap-3 px-3 py-2.5 text-left text-sm transition-colors hover:bg-muted/50"
                                                        >
                                                            <MemberAvatar name={searchUser.displayName} />
                                                            <div className="min-w-0">
                                                                <p className="font-medium truncate">{searchUser.displayName}</p>
                                                                {searchUser.email ? (
                                                                    <p className="text-xs text-muted-foreground truncate">
                                                                        {searchUser.email}
                                                                    </p>
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
                        <Select value={selectedRole} onValueChange={setSelectedRole}>
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

                    <div
                        className="flex items-start gap-2 rounded-lg border p-3"
                        style={{ backgroundColor: 'rgba(255,237,213,0.6)', borderColor: 'rgba(249,115,22,0.35)' }}
                    >
                        <TriangleAlertIcon className="size-4 shrink-0" style={{ color: 'rgb(234,88,12)', marginTop: '2px' }} />
                        <div className="text-sm leading-snug" style={{ color: 'rgb(154,52,18)' }}>
                            <p className="font-semibold">This action is irreversible</p>
                            <p className="mt-1">
                                The current Primary Owner will be reassigned to the{' '}
                                <span className="font-semibold">{selectedRole || '—'}</span> role. This cannot be undone without
                                transferring ownership again.
                            </p>
                        </div>
                    </div>
                </div>

                <DialogFooter>
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isTransferring}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleSubmit} disabled={!canSubmit || isTransferring}>
                        Transfer
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

// ── Manage Groups Dialog ──────────────────────────────────────────────────────

function ManageGroupsDialog({
    open,
    allGroups,
    currentGroupIds,
    onClose,
    onSave,
    isSaving,
}: Readonly<{
    open: boolean;
    allGroups: Group[];
    currentGroupIds: string[];
    onClose: () => void;
    onSave: (groupIds: string[]) => void;
    isSaving: boolean;
}>) {
    const [selected, setSelected] = useState<Set<string>>(() => new Set(currentGroupIds));
    const [search, setSearch] = useState('');

    const handleOpen = useCallback(
        (isOpen: boolean) => {
            if (isOpen) setSelected(new Set(currentGroupIds));
            else {
                setSearch('');
                onClose();
            }
        },
        [currentGroupIds, onClose],
    );

    const filtered = useMemo(() => {
        const query = search.trim().toLowerCase();
        return query ? allGroups.filter(group => group.name.toLowerCase().includes(query)) : allGroups;
    }, [allGroups, search]);

    function toggle(id: string) {
        setSelected(prev => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    }

    function handleClose() {
        setSearch('');
        onClose();
    }

    return (
        <Dialog open={open} onOpenChange={handleOpen}>
            <DialogContent className="max-w-md">
                <DialogHeader>
                    <DialogTitle>Manage groups</DialogTitle>
                    <DialogDescription>Select the groups that should have access to this API.</DialogDescription>
                </DialogHeader>

                <div className="space-y-4">
                    <div className="relative">
                        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                        <Input
                            style={{ paddingLeft: '2.5rem' }}
                            placeholder="Search groups…"
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                        />
                    </div>

                    <ScrollArea style={{ maxHeight: '320px' }}>
                        {filtered.length === 0 ? (
                            <p className="p-3 text-sm text-muted-foreground">No groups match your search.</p>
                        ) : (
                            <div className="space-y-1">
                                {filtered.map(group => {
                                    const isAssociated = currentGroupIds.includes(group.id);
                                    const isChecked = selected.has(group.id);
                                    return (
                                        <div
                                            key={group.id}
                                            className="flex items-center gap-3 rounded-lg px-3 py-2 hover:bg-muted/50 cursor-pointer"
                                            onClick={() => toggle(group.id)}
                                        >
                                            <Checkbox
                                                id={`group-${group.id}`}
                                                checked={isChecked}
                                                onClick={e => e.stopPropagation()}
                                                onCheckedChange={() => toggle(group.id)}
                                            />
                                            <label htmlFor={`group-${group.id}`} className="flex-1 text-sm cursor-pointer select-none">
                                                {group.name}
                                            </label>
                                            {isAssociated ? (
                                                <Badge variant="secondary" className="text-xs">
                                                    Associated
                                                </Badge>
                                            ) : null}
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </ScrollArea>

                    <p className="text-xs text-muted-foreground">
                        {selected.size} group{selected.size !== 1 ? 's' : ''} selected
                    </p>
                </div>

                <DialogFooter>
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isSaving}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={() => onSave([...selected])} disabled={isSaving}>
                        Save
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

// ── Direct members table ──────────────────────────────────────────────────────

type EditState = { memberId: string; role: string } | null;

function DirectMembersTable({
    members,
    roles,
    editState,
    onStartEdit,
    onRoleChange,
    onSaveRole,
    onCancelEdit,
    onRemove,
    isSaving,
    isRemoving,
}: Readonly<{
    members: Member[];
    roles: string[];
    editState: EditState;
    onStartEdit: (member: Member) => void;
    onRoleChange: (role: string) => void;
    onSaveRole: () => void;
    onCancelEdit: () => void;
    onRemove: (member: Member) => void;
    isSaving: boolean;
    isRemoving: boolean;
}>) {
    return (
        <div className="rounded-lg border overflow-hidden">
            {/*
             * table-fixed + percentage widths distribute columns proportionally:
             * Name ~60 % | Role ~32 % | Actions ~8 % (≈48 px in typical viewport)
             * This keeps Role visually centered rather than pushed to the far edge.
             */}
            <Table className="table-fixed w-full">
                <TableHeader>
                    <TableRow>
                        <TableHead>Name</TableHead>
                        <TableHead style={{ width: '32%' }}>Role</TableHead>
                        <TableHead style={{ width: '48px' }} />
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {members.map(member => {
                        const isPO = isMemberPrimaryOwner(member);
                        const isEditing = editState?.memberId === member.id;
                        const currentRole = getApiRole(member);

                        return (
                            <TableRow key={member.id} className={isEditing ? 'bg-muted/30' : undefined}>
                                <TableCell>
                                    <div className="flex items-center gap-3">
                                        <MemberAvatar name={member.displayName ?? ''} />
                                        <span className="text-sm font-medium">{member.displayName}</span>
                                    </div>
                                </TableCell>

                                {/* Role cell — edit controls share the same column space */}
                                <TableCell style={{ width: '32%' }}>
                                    {isEditing ? (
                                        <div className="flex items-center gap-1.5">
                                            <Select value={editState.role} onValueChange={onRoleChange}>
                                                <SelectTrigger className="h-8 flex-1 min-w-0">
                                                    <SelectValue />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    {roles.map(role => (
                                                        <SelectItem key={role} value={role}>
                                                            {role}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                            <Button
                                                type="button"
                                                size="sm"
                                                className="h-8 shrink-0"
                                                onClick={onSaveRole}
                                                disabled={isSaving || !editState.role}
                                            >
                                                Save
                                            </Button>
                                            <Button
                                                type="button"
                                                size="sm"
                                                variant="ghost"
                                                className="size-8 p-0 shrink-0"
                                                onClick={onCancelEdit}
                                                aria-label="Cancel edit"
                                            >
                                                <XIcon className="size-4" aria-hidden="true" />
                                            </Button>
                                        </div>
                                    ) : (
                                        <RoleBadge roleName={currentRole} isPO={isPO} />
                                    )}
                                </TableCell>

                                {/* Actions cell — always present; width fixed to prevent layout shift */}
                                <TableCell className="text-right" style={{ width: '48px' }}>
                                    {!isPO && !isEditing ? (
                                        <DropdownMenu>
                                            <DropdownMenuTrigger asChild>
                                                <Button
                                                    type="button"
                                                    variant="ghost"
                                                    size="icon"
                                                    className="size-8"
                                                    aria-label={`Actions for ${member.displayName}`}
                                                >
                                                    <MoreHorizontalIcon className="size-4" />
                                                </Button>
                                            </DropdownMenuTrigger>
                                            <DropdownMenuContent align="end" style={{ minWidth: '160px' }}>
                                                <DropdownMenuItem onClick={() => onStartEdit(member)}>
                                                    <PencilIcon className="size-4" />
                                                    Edit role
                                                </DropdownMenuItem>
                                                <DropdownMenuSeparator />
                                                <DropdownMenuItem
                                                    className="text-destructive focus:text-destructive"
                                                    onClick={() => onRemove(member)}
                                                    disabled={isRemoving}
                                                >
                                                    <Trash2Icon className="size-4" />
                                                    Remove member
                                                </DropdownMenuItem>
                                            </DropdownMenuContent>
                                        </DropdownMenu>
                                    ) : null}
                                </TableCell>
                            </TableRow>
                        );
                    })}
                </TableBody>
            </Table>
        </div>
    );
}

// ── Group members section ─────────────────────────────────────────────────────

function GroupMembersSection({
    groupName,
    members,
}: Readonly<{
    groupName: string;
    members: Array<{ id: string; displayName: string; roles: Record<string, string> }>;
}>) {
    const count = members.length;
    return (
        <Card className="overflow-hidden">
            <div className="flex items-center gap-3 px-4 py-3 border-b bg-muted/30">
                <UsersIcon className="size-4 text-muted-foreground shrink-0" aria-hidden="true" />
                <span className="text-sm font-semibold">{groupName}</span>
                <Badge variant="secondary" className="text-xs tabular-nums">
                    {count} inherited {count === 1 ? 'member' : 'members'}
                </Badge>
            </div>
            <p className="px-4 py-2 text-xs text-muted-foreground border-b">
                Members inherited from this group. Permissions are read-only and managed at the group level.
            </p>
            <Table>
                <TableHeader>
                    <TableRow>
                        <TableHead>Name</TableHead>
                        <TableHead style={{ width: '176px' }}>Role</TableHead>
                    </TableRow>
                </TableHeader>
                <TableBody>
                    {members.map(groupMember => (
                        <TableRow key={groupMember.id}>
                            <TableCell>
                                <div className="flex items-center gap-3">
                                    <MemberAvatar name={groupMember.displayName ?? ''} />
                                    <span className="text-sm font-medium">{groupMember.displayName}</span>
                                </div>
                            </TableCell>
                            <TableCell>
                                <Badge variant="secondary" className="font-normal">
                                    {groupMember.roles?.['API'] ?? '—'}
                                </Badge>
                            </TableCell>
                        </TableRow>
                    ))}
                </TableBody>
            </Table>
        </Card>
    );
}

// ── Page ──────────────────────────────────────────────────────────────────────

export function UserPermissionsPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const runtime = useApimRuntime();
    const queryClient = useQueryClient();
    const { api } = useApiDetailContext();

    const [editState, setEditState] = useState<EditState>(null);
    const [memberToRemove, setMemberToRemove] = useState<Member | null>(null);
    const [addMembersOpen, setAddMembersOpen] = useState(false);
    const [transferOpen, setTransferOpen] = useState(false);
    const [manageGroupsOpen, setManageGroupsOpen] = useState(false);

    const { data: membersData, isLoading: membersLoading } = useApiMembers(apiId);
    const { data: groupMembersMap, isLoading: groupsLoading } = useApiGroupMembers(apiId);
    const { data: rolesData } = useApiRoles();
    const { data: groupsData } = useGroups();

    const members = membersData?.data ?? [];
    const roleNames = (rolesData ?? []).filter(role => role.name !== 'PRIMARY_OWNER').map(role => role.name);
    const groupEntries = Object.entries(groupMembersMap ?? {}).filter(([, groupMembers]) => groupMembers.length > 0);
    const allGroups = groupsData?.data ?? [];
    const currentGroupIds = api?.groups ?? [];

    // ── Mutations ──────────────────────────────────────────────────────────────

    const addMutation = useMutation({
        mutationFn: ({ users, roleName }: { users: SearchableUser[]; roleName: string }) =>
            Promise.all(
                users.map(user =>
                    addApiMember(runtime, apiId!, {
                        userId: user.id ?? undefined,
                        externalReference: user.reference,
                        roleName,
                    }),
                ),
            ),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiMemberKeys.list(runtime, apiId!) });
            setAddMembersOpen(false);
        },
    });

    const updateMutation = useMutation({
        mutationFn: ({ memberId, roleName }: { memberId: string; roleName: string }) =>
            updateApiMember(runtime, apiId!, memberId, roleName),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiMemberKeys.list(runtime, apiId!) });
            setEditState(null);
        },
    });

    const deleteMutation = useMutation({
        mutationFn: (memberId: string) => deleteApiMember(runtime, apiId!, memberId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiMemberKeys.list(runtime, apiId!) });
            setMemberToRemove(null);
        },
    });

    const transferMutation = useMutation({
        mutationFn: (payload: Parameters<typeof transferApiOwnership>[2]) => transferApiOwnership(runtime, apiId!, payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiMemberKeys.list(runtime, apiId!) });
            queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(runtime, apiId!) });
            setTransferOpen(false);
        },
    });

    const groupsMutation = useMutation({
        mutationFn: (groupIds: string[]) => updateApiGroups(runtime, apiId!, groupIds),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiMemberKeys.groups(runtime, apiId!) });
            queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(runtime, apiId!) });
            queryClient.invalidateQueries({ queryKey: groupKeys.list(runtime) });
            setManageGroupsOpen(false);
        },
    });

    const notificationMutation = useMutation({
        mutationFn: (disable: boolean) => updateApiNotifications(runtime, apiId!, disable),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(runtime, apiId!) });
        },
    });

    // ── Handlers ──────────────────────────────────────────────────────────────

    const handleStartEdit = useCallback((member: Member) => {
        setEditState({ memberId: member.id, role: getApiRole(member) });
    }, []);

    const handleRoleChange = useCallback((role: string) => {
        setEditState(prev => (prev ? { ...prev, role } : null));
    }, []);

    const handleSaveRole = useCallback(() => {
        if (editState) updateMutation.mutate({ memberId: editState.memberId, roleName: editState.role });
    }, [editState, updateMutation.mutate]);

    const handleCancelEdit = useCallback(() => setEditState(null), []);

    const handleRemoveRequest = useCallback((member: Member) => setMemberToRemove(member), []);
    const handleRemoveConfirm = useCallback(() => {
        if (memberToRemove) deleteMutation.mutate(memberToRemove.id);
    }, [memberToRemove, deleteMutation.mutate]);
    const handleRemoveCancel = useCallback(() => setMemberToRemove(null), []);

    const notificationsEnabled = !(api?.disableMembershipNotifications ?? false);
    const handleNotificationToggle = useCallback(
        (checked: boolean) => notificationMutation.mutate(!checked),
        [notificationMutation.mutate],
    );

    const handleAddMembersClose = useCallback(() => setAddMembersOpen(false), []);
    const handleAddMembers = useCallback(
        (users: SearchableUser[], roleName: string) => addMutation.mutate({ users, roleName }),
        [addMutation.mutate],
    );
    const handleTransferClose = useCallback(() => setTransferOpen(false), []);
    const handleTransfer = useCallback((payload: TransferOwnershipPayload) => transferMutation.mutate(payload), [transferMutation.mutate]);
    const handleManageGroupsClose = useCallback(() => setManageGroupsOpen(false), []);
    const handleSaveGroups = useCallback((groupIds: string[]) => groupsMutation.mutate(groupIds), [groupsMutation.mutate]);

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold tracking-tight">User Permissions</h1>
                    <p className="text-sm text-muted-foreground">
                        Manage who can interact with your API through the API Management console.
                    </p>
                </div>
                <div className="flex shrink-0 flex-wrap items-center gap-2">
                    <div className="flex items-center gap-2 rounded-lg border px-3 py-2">
                        <BellIcon className="size-4 text-muted-foreground" aria-hidden="true" />
                        <span className="text-sm text-muted-foreground">Notifications</span>
                        <Switch
                            checked={notificationsEnabled}
                            onCheckedChange={handleNotificationToggle}
                            disabled={notificationMutation.isPending || !api}
                            aria-label="Toggle membership notifications"
                        />
                    </div>
                    <Button type="button" variant="outline" size="sm" onClick={() => setTransferOpen(true)}>
                        <UserCogIcon className="size-4" aria-hidden="true" />
                        Transfer ownership
                    </Button>
                    <Button type="button" variant="outline" size="sm" onClick={() => setManageGroupsOpen(true)}>
                        <UsersIcon className="size-4" aria-hidden="true" />
                        Manage groups
                    </Button>
                    <Button type="button" size="sm" onClick={() => setAddMembersOpen(true)}>
                        <PlusIcon className="size-4" aria-hidden="true" />
                        Add members
                    </Button>
                </div>
            </div>

            <section className="space-y-3">
                <div className="space-y-1">
                    <div className="flex items-center gap-2">
                        <h2 className="text-base font-semibold">Direct Members</h2>
                        {!membersLoading ? (
                            <Badge variant="secondary" className="text-xs tabular-nums">
                                {members.length}
                            </Badge>
                        ) : null}
                    </div>
                    <p className="text-sm text-muted-foreground">
                        Users with direct access to this API. You can change their roles or remove them.
                    </p>
                </div>

                {membersLoading ? (
                    <div className="space-y-2">
                        {Array.from({ length: 3 }).map((_, i) => (
                            <Skeleton key={i} className="h-12 rounded-lg" />
                        ))}
                    </div>
                ) : members.length === 0 ? (
                    <Card>
                        <Empty>
                            <EmptyHeader>
                                <EmptyTitle>No direct members</EmptyTitle>
                                <EmptyDescription>Add members to grant direct access to this API.</EmptyDescription>
                            </EmptyHeader>
                            <EmptyContent />
                        </Empty>
                    </Card>
                ) : (
                    <DirectMembersTable
                        members={members}
                        roles={roleNames}
                        editState={editState}
                        onStartEdit={handleStartEdit}
                        onRoleChange={handleRoleChange}
                        onSaveRole={handleSaveRole}
                        onCancelEdit={handleCancelEdit}
                        onRemove={handleRemoveRequest}
                        isSaving={updateMutation.isPending}
                        isRemoving={deleteMutation.isPending}
                    />
                )}
            </section>

            <Separator />

            <section className="space-y-3">
                <div className="space-y-1">
                    <h2 className="text-base font-semibold">Group Inherited Members</h2>
                    <p className="text-sm text-muted-foreground">
                        These members have access through their group membership. Their roles are managed at the group level.
                    </p>
                </div>

                {groupsLoading ? (
                    <div className="space-y-2">
                        {Array.from({ length: 2 }).map((_, i) => (
                            <Skeleton key={i} className="h-24 rounded-lg" />
                        ))}
                    </div>
                ) : groupEntries.length === 0 ? (
                    <Card>
                        <Empty>
                            <EmptyHeader>
                                <EmptyTitle>No group members</EmptyTitle>
                                <EmptyDescription>Add groups to this API to grant inherited access to their members.</EmptyDescription>
                            </EmptyHeader>
                            <EmptyContent />
                        </Empty>
                    </Card>
                ) : (
                    <div className="space-y-4">
                        {groupEntries.map(([groupName, groupMembers]) => (
                            <GroupMembersSection key={groupName} groupName={groupName} members={groupMembers} />
                        ))}
                    </div>
                )}
            </section>

            <RemoveMemberDialog
                member={memberToRemove}
                isRemoving={deleteMutation.isPending}
                onConfirm={handleRemoveConfirm}
                onCancel={handleRemoveCancel}
            />

            <AddMembersDialog
                open={addMembersOpen}
                roles={roleNames}
                existingMembers={members}
                onClose={handleAddMembersClose}
                onAdd={handleAddMembers}
                isAdding={addMutation.isPending}
            />

            <TransferOwnershipDialog
                open={transferOpen}
                members={members}
                roles={roleNames}
                onClose={handleTransferClose}
                onTransfer={handleTransfer}
                isTransferring={transferMutation.isPending}
            />

            <ManageGroupsDialog
                open={manageGroupsOpen}
                allGroups={allGroups}
                currentGroupIds={currentGroupIds}
                onClose={handleManageGroupsClose}
                onSave={handleSaveGroups}
                isSaving={groupsMutation.isPending}
            />
        </div>
    );
}
