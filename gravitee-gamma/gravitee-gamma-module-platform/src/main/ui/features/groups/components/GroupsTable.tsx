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
    DataTableColumnHeader,
    DataTableEmptyState,
    DateCell,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
    InputGroup,
    InputGroupAddon,
    InputGroupInput,
    type DataTableProps,
} from '@gravitee/graphene-core';
import { MoreHorizontalIcon, PencilIcon, SearchIcon, Trash2Icon, UsersRoundIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { Group } from '../types/group';
import { isPrimaryOwnerGroup } from '../utils/groupPermissions';

function hasEventRule(group: Group, event: 'API_CREATE' | 'APPLICATION_CREATE' | 'API_PRODUCT_CREATE'): boolean {
    return (group.event_rules ?? []).some(rule => rule.event === event);
}

function buildColumns({
    canEdit,
    canDelete,
    onEdit,
    onDelete,
}: {
    canEdit: boolean;
    canDelete: boolean;
    onEdit: (group: Group) => void;
    onDelete: (group: Group) => void;
}): DataTableProps<Group>['columns'] {
    const columns: DataTableProps<Group>['columns'] = [
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<Group>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<Group>) => (
                <div className="flex flex-wrap items-center gap-2">
                    <span className="text-sm font-medium">{row.original.name}</span>
                    {isPrimaryOwnerGroup(row.original) && (
                        <Badge variant="default" className="text-xs font-normal">
                            Primary owner
                        </Badge>
                    )}
                    {hasEventRule(row.original, 'API_CREATE') && (
                        <Badge variant="default" className="text-xs font-normal">
                            Auto APIs
                        </Badge>
                    )}
                    {hasEventRule(row.original, 'API_PRODUCT_CREATE') && (
                        <Badge variant="default" className="text-xs font-normal">
                            Auto API Products
                        </Badge>
                    )}
                    {hasEventRule(row.original, 'APPLICATION_CREATE') && (
                        <Badge variant="default" className="text-xs font-normal">
                            Auto Applications
                        </Badge>
                    )}
                </div>
            ),
        },
        {
            id: 'created',
            accessorFn: (row: Group) => row.created_at ?? 0,
            header: ({ column }: ColHeader<Group>) => <DataTableColumnHeader column={column} title="Created" />,
            cell: ({ row }: ColCell<Group>) =>
                row.original.created_at ? (
                    <DateCell value={new Date(row.original.created_at)} format="absolute" />
                ) : (
                    <span className="text-sm text-muted-foreground">—</span>
                ),
        },
    ];

    if (canEdit || canDelete) {
        columns.push({
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 56,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<Group>) => {
                // Mirrors classic Console's group.component.ts: a group with manageable === false can't be
                // edited/deleted by the current user even if they hold the global environment-group-u/d
                // permission, and a primary-owner group can never be deleted (backend rejects with
                // StillPrimaryOwnerException regardless). Absent manageable (undefined) means the backend
                // didn't send the flag — default to allowed.
                const manageable = row.original.manageable ?? true;
                const canEditRow = canEdit && manageable;
                const canDeleteRow = canDelete && manageable && !isPrimaryOwnerGroup(row.original);
                if (!canEditRow && !canDeleteRow) return null;
                return (
                    <div className="flex justify-end">
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" className="size-8" aria-label="Group actions">
                                    <MoreHorizontalIcon className="size-4" aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                {canEditRow && (
                                    <DropdownMenuItem onSelect={() => onEdit(row.original)}>
                                        <PencilIcon className="size-4 mr-2" aria-hidden />
                                        Edit
                                    </DropdownMenuItem>
                                )}
                                {canEditRow && canDeleteRow && <DropdownMenuSeparator />}
                                {canDeleteRow && (
                                    <DropdownMenuItem variant="destructive" onSelect={() => onDelete(row.original)}>
                                        <Trash2Icon className="size-4 mr-2" aria-hidden />
                                        Delete
                                    </DropdownMenuItem>
                                )}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                );
            },
        });
    }

    return columns;
}

interface GroupsTableProps {
    readonly groups: Group[];
    readonly totalCount: number;
    readonly loading: boolean;
    readonly isFirstUse: boolean;
    readonly search: string;
    readonly page: number;
    readonly pageSize: number;
    readonly canEdit: boolean;
    readonly canDelete: boolean;
    readonly onSearchChange: (value: string) => void;
    readonly onPageChange: (page: number) => void;
    readonly onPageSizeChange: (size: number) => void;
    readonly onEdit: (group: Group) => void;
    readonly onDelete: (group: Group) => void;
    readonly onCreateGroup?: () => void;
}

export function GroupsTable({
    groups,
    totalCount,
    loading,
    isFirstUse,
    search,
    page,
    pageSize,
    canEdit,
    canDelete,
    onSearchChange,
    onPageChange,
    onPageSizeChange,
    onEdit,
    onDelete,
    onCreateGroup,
}: GroupsTableProps) {
    const columns = useMemo(() => buildColumns({ canEdit, canDelete, onEdit, onDelete }), [canEdit, canDelete, onEdit, onDelete]);

    if (isFirstUse) {
        return (
            <div className="rounded-lg border">
                <DataTableEmptyState
                    variant="first-use"
                    icon={<UsersRoundIcon className="size-8" aria-hidden />}
                    title="No groups"
                    description="Create groups to organize users and assign shared permissions."
                    primaryAction={onCreateGroup ? <Button onClick={onCreateGroup}>Create Group</Button> : undefined}
                />
            </div>
        );
    }

    return (
        <DataTable
            aria-label="Groups"
            columns={columns}
            data={groups}
            loading={loading}
            skeletonCount={pageSize}
            serverSide
            pagination={{
                page,
                pageSize,
                totalCount,
                pageSizeOptions: [...TABLE_PAGE_SIZE_OPTIONS],
                onPageChange,
                onPageSizeChange: size => {
                    onPageSizeChange(size);
                    onPageChange(1);
                },
            }}
            emptyMessage={
                <DataTableEmptyState
                    variant="no-results"
                    icon={<SearchIcon className="size-8" aria-hidden />}
                    title="No groups match your search"
                    description="Try adjusting your search terms."
                    action={
                        <Button
                            size="sm"
                            variant="outline"
                            onClick={() => {
                                onSearchChange('');
                                onPageChange(1);
                            }}
                        >
                            Clear search
                        </Button>
                    }
                />
            }
            toolbar={
                <div className="w-64">
                    <InputGroup>
                        <InputGroupAddon align="inline-start">
                            <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                        </InputGroupAddon>
                        <InputGroupInput placeholder="Search by name…" value={search} onChange={e => onSearchChange(e.target.value)} />
                    </InputGroup>
                </div>
            }
        />
    );
}
