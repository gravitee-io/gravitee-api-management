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
import { Button, Input } from '@gravitee/graphene-core';
import { useEffect, useMemo, useState } from 'react';

import { deletePortalTenantMember } from '../storage/portal-tenant-members.storage';
import type { PortalTenant, PortalTenantMember } from '../types/portal-tenant.types';
import { AddTenantUsersDialog } from './AddTenantUsersDialog';
import { InviteTenantUserDialog } from './InviteTenantUserDialog';
import { PendingInvitationsTable } from './PendingInvitationsTable';
import { getInvitationsByTenantId } from '../../consumer-auth/storage/portal-invitations.storage';
import type { PortalInvitation } from '../../consumer-auth/types/consumer-auth.types';

interface TenantUsersTabProps {
    readonly tenant: PortalTenant;
    readonly portalName: string;
    readonly members: PortalTenantMember[];
    readonly onMembersChange: (members: PortalTenantMember[]) => void;
}

export function TenantUsersTab({ tenant, portalName, members, onMembersChange }: TenantUsersTabProps) {
    const [query, setQuery] = useState('');
    const [addDialogOpen, setAddDialogOpen] = useState(false);
    const [inviteDialogOpen, setInviteDialogOpen] = useState(false);
    const [invitations, setInvitations] = useState<PortalInvitation[]>([]);

    useEffect(() => {
        void getInvitationsByTenantId(tenant.id).then(setInvitations);
    }, [tenant.id]);

    const filteredMembers = useMemo(() => {
        const normalized = query.trim().toLowerCase();
        if (!normalized) {
            return members;
        }

        return members.filter(
            member =>
                member.displayName.toLowerCase().includes(normalized)
                || member.email.toLowerCase().includes(normalized),
        );
    }, [members, query]);

    const existingMemberUserIds = useMemo(() => new Set(members.map(member => member.userId)), [members]);

    const handleRemove = async (memberId: string) => {
        await deletePortalTenantMember(memberId);
        onMembersChange(members.filter(member => member.id !== memberId));
    };

    return (
        <div className="space-y-4">
            <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                    <h2 className="text-lg font-semibold">Users</h2>
                    <p className="text-sm text-muted-foreground">
                        Users in this tenant share access to the same APIs and applications.
                    </p>
                </div>
                <div className="flex flex-wrap gap-2">
                    <Button variant="outline" onClick={() => setInviteDialogOpen(true)}>
                        Invite user
                    </Button>
                    <Button onClick={() => setAddDialogOpen(true)}>+ Add users</Button>
                </div>
            </div>

            <Input
                placeholder="Search users…"
                value={query}
                onChange={event => setQuery(event.target.value)}
                aria-label="Search users"
                className="max-w-md"
            />

            <div className="overflow-hidden rounded-lg border">
                <table className="w-full text-sm">
                    <thead className="bg-muted/40 text-left">
                        <tr>
                            <th className="px-4 py-3 font-medium">User</th>
                            <th className="px-4 py-3 font-medium">Role</th>
                            <th className="px-4 py-3 font-medium" />
                        </tr>
                    </thead>
                    <tbody>
                        {filteredMembers.length === 0 ? (
                            <tr>
                                <td colSpan={3} className="px-4 py-8 text-center text-muted-foreground">
                                    No users in this tenant yet.
                                </td>
                            </tr>
                        ) : (
                            filteredMembers.map(member => (
                                <tr key={member.id} className="border-t">
                                    <td className="px-4 py-3">
                                        <div className="font-medium">{member.displayName}</div>
                                        <div className="text-muted-foreground">{member.email}</div>
                                    </td>
                                    <td className="px-4 py-3 capitalize">
                                        {member.role === 'admin' ? 'Tenant admin' : 'Member'}
                                    </td>
                                    <td className="px-4 py-3 text-right">
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            onClick={() => void handleRemove(member.id)}
                                        >
                                            Remove
                                        </Button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            <PendingInvitationsTable portalId={tenant.portalId} invitations={invitations} />

            <AddTenantUsersDialog
                tenant={tenant}
                existingMemberUserIds={existingMemberUserIds}
                open={addDialogOpen}
                onOpenChange={setAddDialogOpen}
                onAdded={added => onMembersChange([...members, ...added])}
            />

            <InviteTenantUserDialog
                tenant={tenant}
                portalName={portalName}
                open={inviteDialogOpen}
                onOpenChange={setInviteDialogOpen}
                onInvited={invitation => setInvitations(current => [invitation, ...current])}
            />
        </div>
    );
}
