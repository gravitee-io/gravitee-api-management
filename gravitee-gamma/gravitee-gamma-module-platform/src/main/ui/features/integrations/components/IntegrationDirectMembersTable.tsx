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
import { createContext, useContext, useMemo } from 'react';

import type { ColCell } from '../../../shared/utils/dataTableTypes';
import { MemberAvatar } from '../../shared/components';
import { NON_SORTABLE_COLUMN } from '../../shared/utils/dataTableHeaders';
import { PRIMARY_OWNER_ROLE } from '../../shared/utils/memberRoles';
import type { IntegrationMember, IntegrationMemberEditState } from '../types/integrationMembers';

const INTEGRATION_SCOPE = 'INTEGRATION';

function isPrimaryOwner(member: IntegrationMember): boolean {
    return member.roles?.some(role => role.name === PRIMARY_OWNER_ROLE) ?? false;
}

function integrationRoleName(member: IntegrationMember): string | undefined {
    return member.roles?.find(role => role.scope === INTEGRATION_SCOPE)?.name ?? member.roles?.[0]?.name;
}

function RoleBadge({ member }: Readonly<{ member: IntegrationMember }>) {
    if (isPrimaryOwner(member)) {
        return (
            <Badge variant="default" className="gap-1 font-normal">
                <ShieldCheckIcon className="size-3" aria-hidden="true" />
                Primary Owner
            </Badge>
        );
    }
    const roleName = integrationRoleName(member);
    return (
        <Badge variant="secondary" className="font-normal">
            {roleName ?? '—'}
        </Badge>
    );
}

type EditedMember = NonNullable<IntegrationMemberEditState>;

export interface RoleEditing {
    roles: string[];
    editState: IntegrationMemberEditState;
    isSaving: boolean;
    onStartEdit: (editedMember: EditedMember) => void;
    onRoleChange: (role: string) => void;
    onSaveRole: () => void;
    onCancelEdit: () => void;
}

type RemoveMember = (member: IntegrationMember) => void;

interface MemberActions {
    roleEditing?: RoleEditing;
    onRemoveMember?: RemoveMember;
}

// Cells read member actions from context: a column rebuilt on every state change would remount its cells and close an open menu.
const MemberActionsContext = createContext<MemberActions>({});

function isBeingEdited(member: IntegrationMember, editState: IntegrationMemberEditState): editState is EditedMember {
    return editState !== null && editState.memberId === member.id;
}

function RoleEditor({
    member,
    editState,
    roles,
    isSaving,
    onRoleChange,
    onSaveRole,
    onCancelEdit,
}: Readonly<{ member: IntegrationMember; editState: EditedMember } & Omit<RoleEditing, 'editState' | 'onStartEdit'>>) {
    return (
        <div className="flex items-center gap-1.5">
            <Select value={editState.role} onValueChange={onRoleChange}>
                <SelectTrigger className="h-8 flex-1 min-w-0">
                    <SelectValue />
                </SelectTrigger>
                <SelectContent>
                    {roles.map(roleName => (
                        <SelectItem key={roleName} value={roleName}>
                            {roleName}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
            <Button
                type="button"
                size="sm"
                className="h-8 shrink-0"
                onClick={onSaveRole}
                disabled={isSaving || editState.role === integrationRoleName(member)}
            >
                Save
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

function MemberActionsMenu({
    member,
    onStartEdit,
    onRemoveMember,
}: Readonly<{ member: IntegrationMember; onStartEdit?: (editedMember: EditedMember) => void; onRemoveMember?: RemoveMember }>) {
    return (
        <div className="flex justify-end">
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="size-8" aria-label="Member actions">
                        <MoreHorizontalIcon className="size-4" aria-hidden />
                    </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                    {onStartEdit ? (
                        <DropdownMenuItem onSelect={() => onStartEdit({ memberId: member.id, role: integrationRoleName(member) ?? '' })}>
                            <PencilIcon className="size-4 mr-2" aria-hidden />
                            Edit role
                        </DropdownMenuItem>
                    ) : null}
                    {onRemoveMember ? (
                        <>
                            {onStartEdit ? <DropdownMenuSeparator /> : null}
                            <DropdownMenuItem variant="destructive" onSelect={() => onRemoveMember(member)}>
                                <Trash2Icon className="size-4 mr-2" aria-hidden />
                                Remove member
                            </DropdownMenuItem>
                        </>
                    ) : null}
                </DropdownMenuContent>
            </DropdownMenu>
        </div>
    );
}

function RoleCell({ member }: Readonly<{ member: IntegrationMember }>) {
    const { roleEditing } = useContext(MemberActionsContext);
    if (roleEditing && isBeingEdited(member, roleEditing.editState)) {
        return <RoleEditor member={member} {...roleEditing} editState={roleEditing.editState} />;
    }
    return <RoleBadge member={member} />;
}

function ActionsCell({ member }: Readonly<{ member: IntegrationMember }>) {
    const { roleEditing, onRemoveMember } = useContext(MemberActionsContext);
    const hasNoAction = !roleEditing && !onRemoveMember;
    if (hasNoAction || isPrimaryOwner(member) || (roleEditing && isBeingEdited(member, roleEditing.editState))) {
        return null;
    }
    return <MemberActionsMenu member={member} onStartEdit={roleEditing?.onStartEdit} onRemoveMember={onRemoveMember} />;
}

function buildColumns(showActions: boolean): DataTableProps<IntegrationMember>['columns'] {
    const columns: DataTableProps<IntegrationMember>['columns'] = [
        {
            id: 'name',
            accessorFn: (row: IntegrationMember) => row.displayName ?? '',
            header: 'Name',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<IntegrationMember>) => (
                <div className="flex items-center gap-3">
                    <MemberAvatar name={row.original.displayName ?? ''} />
                    <span className="text-sm font-medium">{row.original.displayName}</span>
                </div>
            ),
        },
        {
            id: 'role',
            header: 'Role',
            ...NON_SORTABLE_COLUMN,
            cell: ({ row }: ColCell<IntegrationMember>) => <RoleCell member={row.original} />,
        },
    ];

    if (showActions) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            cell: ({ row }: ColCell<IntegrationMember>) => <ActionsCell member={row.original} />,
            enableSorting: false,
            enableHiding: false,
        });
    }

    return columns;
}

export function IntegrationDirectMembersTable({
    members,
    roleEditing,
    onRemoveMember,
}: Readonly<{ members: IntegrationMember[]; roleEditing?: RoleEditing; onRemoveMember?: RemoveMember }>) {
    const showActions = roleEditing !== undefined || onRemoveMember !== undefined;
    const columns = useMemo(() => buildColumns(showActions), [showActions]);
    const memberActions = useMemo(() => ({ roleEditing, onRemoveMember }), [roleEditing, onRemoveMember]);
    return (
        <MemberActionsContext.Provider value={memberActions}>
            <DataTable columns={columns} data={members} />
        </MemberActionsContext.Provider>
    );
}
