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
    DataTable,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, ShieldCheckIcon, Trash2Icon, XIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { MemberAvatar } from './MemberAvatar';
import { formatRoleLabel, getApplicationRole, isMemberPrimaryOwner } from './memberHelpers';
import type { ApplicationUiMember, EditState } from '../../types/applicationMembers.types';
import { NON_SORTABLE_COLUMN } from '../../utils/dataTableHeaders';
import type { ColCell } from '../../utils/dataTableTypes';

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
            {roleName ? formatRoleLabel(roleName) : '—'}
        </Badge>
    );
}

function buildColumns({
    showActionsMenu,
    canEditRole,
    canRemoveMember,
    isRemoving,
    isSaving,
    roles,
    editState,
    getRoleName,
    onStartEdit,
    onRoleChange,
    onSaveRole,
    onCancelEdit,
    onRemove,
}: {
    showActionsMenu: boolean;
    canEditRole: boolean;
    canRemoveMember: boolean;
    isRemoving: boolean;
    isSaving: boolean;
    roles: string[];
    editState: EditState;
    getRoleName?: (member: ApplicationUiMember) => string;
    onStartEdit: (member: ApplicationUiMember) => void;
    onRoleChange: (role: string) => void;
    onSaveRole: () => void;
    onCancelEdit: () => void;
    onRemove: (member: ApplicationUiMember) => void;
}): DataTableProps<ApplicationUiMember>['columns'] {
    const columns: DataTableProps<ApplicationUiMember>['columns'] = [
        {
            id: 'name',
            accessorFn: (row: ApplicationUiMember) => row.displayName ?? '',
            header: 'Name',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<ApplicationUiMember>) => (
                <div className="flex items-center gap-3">
                    <MemberAvatar name={row.original.displayName ?? ''} />
                    <span className="text-sm font-medium">{row.original.displayName}</span>
                </div>
            ),
        },
        {
            id: 'role',
            accessorFn: (row: ApplicationUiMember) => {
                const roleName = getRoleName ? getRoleName(row) : getApplicationRole(row);
                return isMemberPrimaryOwner(row) ? 'Primary Owner' : roleName;
            },
            header: 'Role',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<ApplicationUiMember>) => {
                const member = row.original;
                const isPO = isMemberPrimaryOwner(member);
                const currentRole = getRoleName ? getRoleName(member) : getApplicationRole(member);
                const isEditing = editState?.memberId === member.id;

                if (isEditing && editState) {
                    return (
                        <div className="flex items-center gap-1.5">
                            <Select value={editState.role} onValueChange={onRoleChange}>
                                <SelectTrigger className="h-8 flex-1 min-w-0">
                                    <SelectValue />
                                </SelectTrigger>
                                <SelectContent>
                                    {roles.map(roleName => (
                                        <SelectItem key={roleName} value={roleName}>
                                            {formatRoleLabel(roleName)}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                            <Button
                                type="button"
                                size="sm"
                                className="h-8 shrink-0"
                                onClick={onSaveRole}
                                disabled={isSaving || !editState.role || editState.role === currentRole}
                            >
                                {isSaving ? 'Saving…' : 'Save'}
                            </Button>
                            <Button
                                type="button"
                                size="sm"
                                variant="ghost"
                                className="size-8 p-0 shrink-0"
                                onClick={onCancelEdit}
                                disabled={isSaving}
                                aria-label="Cancel edit"
                            >
                                <XIcon className="size-4" aria-hidden="true" />
                            </Button>
                        </div>
                    );
                }

                return <RoleBadge roleName={currentRole} isPO={isPO} />;
            },
        },
    ];

    if (showActionsMenu) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            cell: ({ row }: ColCell<ApplicationUiMember>) => {
                const member = row.original;
                const isPO = isMemberPrimaryOwner(member);
                const isEditing = editState?.memberId === member.id;
                if (isPO || isEditing) {
                    return null;
                }
                return (
                    <div className="flex justify-end">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" className="size-8" aria-label="Member actions">
                                    <MoreHorizontalIcon className="size-4" aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                {canEditRole ? (
                                    <DropdownMenuItem onSelect={() => onStartEdit(member)}>
                                        <PencilIcon className="size-4 mr-2" aria-hidden />
                                        Edit role
                                    </DropdownMenuItem>
                                ) : null}
                                {canRemoveMember ? (
                                    <>
                                        {canEditRole ? <DropdownMenuSeparator /> : null}
                                        <DropdownMenuItem variant="destructive" disabled={isRemoving} onSelect={() => onRemove(member)}>
                                            <Trash2Icon className="size-4 mr-2" aria-hidden />
                                            Remove member
                                        </DropdownMenuItem>
                                    </>
                                ) : null}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                );
            },
            enableSorting: false,
            enableHiding: false,
        });
    }

    return columns;
}

export function DirectMembersTable({
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
    getRoleName,
    canManageMembers = true,
    canEditRole = true,
    canRemoveMember = true,
}: Readonly<{
    members: ApplicationUiMember[];
    roles: string[];
    editState: EditState;
    onStartEdit: (member: ApplicationUiMember) => void;
    onRoleChange: (role: string) => void;
    onSaveRole: () => void;
    onCancelEdit: () => void;
    onRemove: (member: ApplicationUiMember) => void;
    isSaving: boolean;
    isRemoving: boolean;
    getRoleName?: (member: ApplicationUiMember) => string;
    canManageMembers?: boolean;
    canEditRole?: boolean;
    canRemoveMember?: boolean;
}>) {
    const showActionsMenu = canManageMembers && (canEditRole || canRemoveMember);
    const columns = useMemo(
        () =>
            buildColumns({
                showActionsMenu,
                canEditRole,
                canRemoveMember,
                isRemoving,
                isSaving,
                roles,
                editState,
                getRoleName,
                onStartEdit,
                onRoleChange,
                onSaveRole,
                onCancelEdit,
                onRemove,
            }),
        [
            showActionsMenu,
            canEditRole,
            canRemoveMember,
            isRemoving,
            isSaving,
            roles,
            editState,
            getRoleName,
            onStartEdit,
            onRoleChange,
            onSaveRole,
            onCancelEdit,
            onRemove,
        ],
    );

    return <DataTable columns={columns} data={members} emptyMessage="No members found." />;
}
