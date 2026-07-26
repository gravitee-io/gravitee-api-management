/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
    Checkbox,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
} from '@gravitee/graphene-core';
import { useEffect, useMemo, useState } from 'react';

import { DUMMY_PORTAL_USERS } from '../../tenants/storage/dummy-portal-users';
import type { PortalTenantMember, PortalUser } from '../../tenants/types/portal-tenant.types';

interface AddGroupMembersDialogProps {
    readonly open: boolean;
    readonly groupName: string;
    readonly tenantName: string;
    readonly tenantMembers: readonly PortalTenantMember[];
    readonly existingUserIds: ReadonlySet<string>;
    readonly onOpenChange: (open: boolean) => void;
    readonly onAdd: (users: readonly PortalUser[]) => void;
}

export function AddGroupMembersDialog({
    open,
    groupName,
    tenantName,
    tenantMembers,
    existingUserIds,
    onOpenChange,
    onAdd,
}: AddGroupMembersDialogProps) {
    const [query, setQuery] = useState('');
    const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);

    useEffect(() => {
        if (open) {
            setQuery('');
            setSelectedUserIds([]);
        }
    }, [open]);

    /** Tenant members come first; the remaining directory users get enrolled on the fly. */
    const candidates = useMemo(() => {
        const tenantUserIds = new Set(tenantMembers.map(member => member.userId));
        const fromTenant: (PortalUser & { inTenant: boolean })[] = tenantMembers.map(member => ({
            id: member.userId,
            displayName: member.displayName,
            email: member.email,
            inTenant: true,
        }));
        const fromDirectory = DUMMY_PORTAL_USERS.filter(user => !tenantUserIds.has(user.id)).map(user => ({
            ...user,
            inTenant: false,
        }));

        return [...fromTenant, ...fromDirectory].filter(user => !existingUserIds.has(user.id));
    }, [existingUserIds, tenantMembers]);

    const filtered = useMemo(() => {
        const normalized = query.trim().toLowerCase();
        if (!normalized) {
            return candidates;
        }

        return candidates.filter(
            user =>
                user.displayName.toLowerCase().includes(normalized)
                || user.email.toLowerCase().includes(normalized),
        );
    }, [candidates, query]);

    const toggleUser = (userId: string, checked: boolean) => {
        setSelectedUserIds(current =>
            checked ? [...current, userId] : current.filter(id => id !== userId),
        );
    };

    const handleSubmit = () => {
        const selected = candidates.filter(user => selectedUserIds.includes(user.id));
        onAdd(selected.map(({ id, displayName, email }) => ({ id, displayName, email })));
        onOpenChange(false);
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="flex max-h-[min(90vh,44rem)] flex-col overflow-hidden">
                <DialogHeader>
                    <DialogTitle>Add members to {groupName}</DialogTitle>
                    <DialogDescription>
                        Pick users from the {tenantName} directory. Users outside the tenant are enrolled when
                        added.
                    </DialogDescription>
                </DialogHeader>

                <Input
                    placeholder="Search users…"
                    aria-label="Search users"
                    value={query}
                    onChange={event => setQuery(event.target.value)}
                />

                <div className="min-h-0 flex-1 space-y-1 overflow-y-auto py-1">
                    {filtered.length === 0 ? (
                        <p className="py-6 text-center text-sm text-muted-foreground">
                            Every matching user is already a member.
                        </p>
                    ) : (
                        filtered.map(user => (
                            <label
                                key={user.id}
                                className="flex cursor-pointer items-center gap-3 rounded-md px-2 py-2 text-sm hover:bg-muted/50"
                            >
                                <Checkbox
                                    checked={selectedUserIds.includes(user.id)}
                                    onCheckedChange={checked => toggleUser(user.id, checked === true)}
                                />
                                <span className="min-w-0 flex-1">
                                    <span className="block truncate font-medium">{user.displayName}</span>
                                    <span className="block truncate text-muted-foreground">{user.email}</span>
                                </span>
                                {!user.inTenant && <Badge variant="outline">New to tenant</Badge>}
                            </label>
                        ))
                    )}
                </div>

                <DialogFooter className="sm:justify-end">
                    <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                        Cancel
                    </Button>
                    <Button type="button" disabled={selectedUserIds.length === 0} onClick={handleSubmit}>
                        Add {selectedUserIds.length > 0 ? selectedUserIds.length : ''} member
                        {selectedUserIds.length === 1 ? '' : 's'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
