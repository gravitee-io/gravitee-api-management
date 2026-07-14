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
    Button,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
} from '@gravitee/graphene-core';
import { useEffect, useMemo, useState } from 'react';

import { getMemberByUserId } from '../storage/portal-tenant-members.storage';
import { getPortalTenant } from '../storage/portal-tenants.storage';
import { DUMMY_PORTAL_USERS } from '../storage/dummy-portal-users';
import { createTenantMemberId } from '../utils/tenant-hrid';
import type { PortalTenant, PortalTenantMember, PortalTenantMemberRole } from '../types/portal-tenant.types';
import { savePortalTenantMember } from '../storage/portal-tenant-members.storage';

interface AddTenantUsersDialogProps {
    readonly tenant: PortalTenant;
    readonly existingMemberUserIds: ReadonlySet<string>;
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onAdded: (members: PortalTenantMember[]) => void;
}

interface UserRowState {
    userId: string;
    displayName: string;
    email: string;
    disabled: boolean;
    disabledReason?: string;
}

export function AddTenantUsersDialog({
    tenant,
    existingMemberUserIds,
    open,
    onOpenChange,
    onAdded,
}: AddTenantUsersDialogProps) {
    const [query, setQuery] = useState('');
    const [selectedUserIds, setSelectedUserIds] = useState<Set<string>>(new Set());
    const [role, setRole] = useState<PortalTenantMemberRole>('member');
    const [userRows, setUserRows] = useState<UserRowState[]>([]);
    const [isPending, setIsPending] = useState(false);

    useEffect(() => {
        if (!open) {
            setQuery('');
            setSelectedUserIds(new Set());
            setRole('member');
            return;
        }

        void (async () => {
            const rows: UserRowState[] = [];

            for (const user of DUMMY_PORTAL_USERS) {
                if (existingMemberUserIds.has(user.id)) {
                    continue;
                }

                const existingMembership = await getMemberByUserId(user.id);
                if (existingMembership) {
                    const otherTenant = await getPortalTenant(existingMembership.tenantId);
                    rows.push({
                        userId: user.id,
                        displayName: user.displayName,
                        email: user.email,
                        disabled: true,
                        disabledReason: otherTenant ? `Already in ${otherTenant.name}` : 'Already assigned to another tenant',
                    });
                } else {
                    rows.push({
                        userId: user.id,
                        displayName: user.displayName,
                        email: user.email,
                        disabled: false,
                    });
                }
            }

            setUserRows(rows);
        })();
    }, [open, existingMemberUserIds]);

    const filteredRows = useMemo(() => {
        const normalized = query.trim().toLowerCase();
        if (!normalized) {
            return userRows;
        }

        return userRows.filter(
            row =>
                row.displayName.toLowerCase().includes(normalized)
                || row.email.toLowerCase().includes(normalized),
        );
    }, [query, userRows]);

    const toggleUser = (userId: string, disabled: boolean) => {
        if (disabled) {
            return;
        }

        setSelectedUserIds(previous => {
            const next = new Set(previous);
            if (next.has(userId)) {
                next.delete(userId);
            } else {
                next.add(userId);
            }
            return next;
        });
    };

    const handleAdd = async () => {
        setIsPending(true);
        try {
            const newMembers: PortalTenantMember[] = [];

            for (const userId of selectedUserIds) {
                const user = DUMMY_PORTAL_USERS.find(candidate => candidate.id === userId);
                if (!user) {
                    continue;
                }

                const member: PortalTenantMember = {
                    id: createTenantMemberId(),
                    tenantId: tenant.id,
                    userId: user.id,
                    displayName: user.displayName,
                    email: user.email,
                    role,
                };
                await savePortalTenantMember(member);
                newMembers.push(member);
            }

            onAdded(newMembers);
            onOpenChange(false);
        } finally {
            setIsPending(false);
        }
    };

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent style={{ width: 'min(92vw, 32rem)' }}>
                <DialogHeader>
                    <DialogTitle>Add users to {tenant.name}</DialogTitle>
                    <DialogDescription>
                        Users can belong to only one tenant at a time.
                    </DialogDescription>
                </DialogHeader>

                <div className="space-y-4 py-2">
                    <Input
                        placeholder="Search users…"
                        value={query}
                        onChange={event => setQuery(event.target.value)}
                        aria-label="Search users"
                    />

                    <div className="max-h-56 space-y-1 overflow-y-auto rounded-md border p-2">
                        {filteredRows.length === 0 ? (
                            <p className="px-2 py-4 text-center text-sm text-muted-foreground">No users available</p>
                        ) : (
                            filteredRows.map(row => (
                                <label
                                    key={row.userId}
                                    className={`flex cursor-pointer items-start gap-3 rounded-md px-2 py-2 ${
                                        row.disabled ? 'cursor-not-allowed opacity-50' : 'hover:bg-muted/50'
                                    }`}
                                    title={row.disabledReason}
                                >
                                    <input
                                        type="checkbox"
                                        className="mt-1"
                                        checked={selectedUserIds.has(row.userId)}
                                        disabled={row.disabled || isPending}
                                        onChange={() => toggleUser(row.userId, row.disabled)}
                                    />
                                    <span>
                                        <span className="block text-sm font-medium">{row.displayName}</span>
                                        <span className="block text-xs text-muted-foreground">{row.email}</span>
                                        {row.disabledReason && (
                                            <span className="block text-xs text-muted-foreground">{row.disabledReason}</span>
                                        )}
                                    </span>
                                </label>
                            ))
                        )}
                    </div>

                    <fieldset className="space-y-2">
                        <legend className="text-sm font-medium">Assign role</legend>
                        <div className="flex gap-4 text-sm">
                            <label className="flex items-center gap-2">
                                <input
                                    type="radio"
                                    name="tenant-role"
                                    checked={role === 'member'}
                                    onChange={() => setRole('member')}
                                />
                                Member
                            </label>
                            <label className="flex items-center gap-2">
                                <input
                                    type="radio"
                                    name="tenant-role"
                                    checked={role === 'admin'}
                                    onChange={() => setRole('admin')}
                                />
                                Tenant admin
                            </label>
                        </div>
                    </fieldset>
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isPending}>
                        Cancel
                    </Button>
                    <Button disabled={selectedUserIds.size === 0 || isPending} onClick={() => void handleAdd()}>
                        {isPending ? 'Adding…' : `Add (${selectedUserIds.size})`}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
