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
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, ShieldCheckIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { MemberAvatar } from './MemberAvatar';
import { formatRoleLabel, getApplicationRole, isMemberPrimaryOwner } from './memberHelpers';
import type { ApplicationUiMember } from '../../types/applicationMembers.types';
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
    getRoleName,
    onEditRole,
    onRemove,
}: {
    showActionsMenu: boolean;
    canEditRole: boolean;
    canRemoveMember: boolean;
    isRemoving: boolean;
    getRoleName?: (member: ApplicationUiMember) => string;
    onEditRole: (member: ApplicationUiMember) => void;
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
                const isPO = isMemberPrimaryOwner(row.original);
                const currentRole = getRoleName ? getRoleName(row.original) : getApplicationRole(row.original);
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
                return (
                    <div className="flex justify-end">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" className="size-8" aria-label="Member actions">
                                    <MoreHorizontalIcon className="size-4" aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                {canEditRole && !isPO ? (
                                    <DropdownMenuItem onSelect={() => onEditRole(member)}>
                                        <PencilIcon className="size-4 mr-2" aria-hidden />
                                        Edit role
                                    </DropdownMenuItem>
                                ) : null}
                                {canRemoveMember && !isPO ? (
                                    <>
                                        {canEditRole ? <DropdownMenuSeparator /> : null}
                                        <DropdownMenuItem
                                            className="text-destructive focus:text-destructive"
                                            disabled={isRemoving}
                                            onSelect={() => onRemove(member)}
                                        >
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
    onEditRole,
    onRemove,
    isRemoving,
    getRoleName,
    canManageMembers = true,
    canEditRole = true,
    canRemoveMember = true,
}: Readonly<{
    members: ApplicationUiMember[];
    onEditRole: (member: ApplicationUiMember) => void;
    onRemove: (member: ApplicationUiMember) => void;
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
                getRoleName,
                onEditRole,
                onRemove,
            }),
        [showActionsMenu, canEditRole, canRemoveMember, isRemoving, getRoleName, onEditRole, onRemove],
    );

    return <DataTable columns={columns} data={members} emptyMessage="No members found." />;
}
