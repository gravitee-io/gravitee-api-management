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
    Input,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';

import { AddGroupMembersDialog } from './AddGroupMembersDialog';
import { notify } from '../../../shared/notify/notify';
import type { PortalTenantMember, PortalUser } from '../../tenants/types/portal-tenant.types';
import type { PortalGroupMemberRole, PortalGroupMemberView } from '../types/permissions.types';

interface GroupMembersTabProps {
    readonly groupName: string;
    readonly tenantName: string;
    readonly members: readonly PortalGroupMemberView[];
    readonly tenantMembers: readonly PortalTenantMember[];
    readonly readOnly: boolean;
    readonly onAddMembers: (users: readonly PortalUser[]) => Promise<void>;
    readonly onRoleChange: (memberId: string, role: PortalGroupMemberRole) => Promise<void>;
    readonly onRemove: (memberId: string) => Promise<void>;
}

export function GroupMembersTab({
    groupName,
    tenantName,
    members,
    tenantMembers,
    readOnly,
    onAddMembers,
    onRoleChange,
    onRemove,
}: GroupMembersTabProps) {
    const [query, setQuery] = useState('');
    const [addOpen, setAddOpen] = useState(false);

    const filtered = useMemo(() => {
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

    const existingUserIds = useMemo(() => new Set(members.map(member => member.userId)), [members]);

    return (
        <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
                <Input
                    placeholder="Search members…"
                    aria-label="Search members"
                    value={query}
                    onChange={event => setQuery(event.target.value)}
                    className="max-w-xs"
                />
                <Button type="button" disabled={readOnly} onClick={() => setAddOpen(true)}>
                    <PlusIcon className="size-4" aria-hidden />
                    Add members
                </Button>
            </div>

            <div className="overflow-hidden rounded-lg border">
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>Member</TableHead>
                            <TableHead className="w-48">Role</TableHead>
                            <TableHead className="w-24" />
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {filtered.length === 0 ? (
                            <TableRow>
                                <TableCell colSpan={3} className="py-8 text-center text-muted-foreground">
                                    No members in this group yet.
                                </TableCell>
                            </TableRow>
                        ) : (
                            filtered.map(member => (
                                <TableRow key={member.id}>
                                    <TableCell>
                                        <span className="block font-medium">{member.displayName}</span>
                                        <span className="block text-muted-foreground">{member.email}</span>
                                    </TableCell>
                                    <TableCell>
                                        <Select
                                            value={member.role}
                                            disabled={readOnly}
                                            onValueChange={value => {
                                                void onRoleChange(
                                                    member.id,
                                                    value as PortalGroupMemberRole,
                                                ).then(() => notify.success('Member role updated.'));
                                            }}
                                        >
                                            <SelectTrigger
                                                className="h-8"
                                                aria-label={`Role of ${member.displayName}`}
                                            >
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem value="member">Member</SelectItem>
                                                <SelectItem value="admin">Group admin</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </TableCell>
                                    <TableCell className="text-right">
                                        <Button
                                            type="button"
                                            variant="ghost"
                                            size="sm"
                                            disabled={readOnly}
                                            onClick={() => {
                                                void onRemove(member.id).then(() =>
                                                    notify.success('Member removed from group.'),
                                                );
                                            }}
                                        >
                                            Remove
                                        </Button>
                                    </TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
            </div>

            <AddGroupMembersDialog
                open={addOpen}
                groupName={groupName}
                tenantName={tenantName}
                tenantMembers={tenantMembers}
                existingUserIds={existingUserIds}
                onOpenChange={setAddOpen}
                onAdd={users => {
                    void onAddMembers(users).then(() =>
                        notify.success(`${users.length} member${users.length === 1 ? '' : 's'} added.`),
                    );
                }}
            />
        </div>
    );
}
