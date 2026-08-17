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
import { EyeIcon, KubernetesIcon, LayersIcon, MoreHorizontalIcon, PencilIcon, SearchIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';
import { Link } from 'react-router-dom';

import { SharedPolicyGroupStatusBadge } from './SharedPolicyGroupStatusBadge';
import type { ColCell, ColHeader } from '../../applications/utils/dataTableTypes';
import { TABLE_PAGE_SIZE_OPTIONS } from '../../applications/utils/paginationConstants';
import type { TableSortingState } from '../../applications/utils/tableSort';
import { toReadableApiType, toReadableFlowPhase, type SharedPolicyGroup } from '../types/sharedPolicyGroup';
import { isKubernetesOrigin } from '../utils/sharedPolicyGroupPermissions';

function buildColumns({
    canEdit,
    canDelete,
    onView,
    onDelete,
}: {
    canEdit: boolean;
    canDelete: boolean;
    onView: (sharedPolicyGroup: SharedPolicyGroup) => void;
    onDelete: (sharedPolicyGroup: SharedPolicyGroup) => void;
}): DataTableProps<SharedPolicyGroup>['columns'] {
    return [
        {
            id: 'name',
            accessorKey: 'name',
            header: ({ column }: ColHeader<SharedPolicyGroup>) => <DataTableColumnHeader column={column} title="Name" />,
            cell: ({ row }: ColCell<SharedPolicyGroup>) => (
                <div className="flex flex-col items-start gap-1 text-left">
                    <Button asChild variant="link" className="h-auto p-0 text-left text-sm font-medium text-foreground hover:underline">
                        <Link to={row.original.id}>{row.original.name}</Link>
                    </Button>
                    {row.original.description && <span className="text-xs text-muted-foreground">{row.original.description}</span>}
                </div>
            ),
        },
        {
            id: 'status',
            accessorKey: 'lifecycleState',
            enableSorting: false,
            header: ({ column }: ColHeader<SharedPolicyGroup>) => <DataTableColumnHeader column={column} title="Status" />,
            cell: ({ row }: ColCell<SharedPolicyGroup>) => <SharedPolicyGroupStatusBadge lifecycleState={row.original.lifecycleState} />,
        },
        {
            id: 'apiType',
            accessorKey: 'apiType',
            enableSorting: false,
            header: ({ column }: ColHeader<SharedPolicyGroup>) => <DataTableColumnHeader column={column} title="API Type" />,
            cell: ({ row }: ColCell<SharedPolicyGroup>) => <span className="text-sm">{toReadableApiType(row.original.apiType)}</span>,
        },
        {
            id: 'phase',
            accessorKey: 'phase',
            header: ({ column }: ColHeader<SharedPolicyGroup>) => <DataTableColumnHeader column={column} title="Phase" />,
            cell: ({ row }: ColCell<SharedPolicyGroup>) => <span className="text-sm">{toReadableFlowPhase(row.original.phase)}</span>,
        },
        {
            id: 'updatedAt',
            accessorFn: (row: SharedPolicyGroup) => row.updatedAt ?? '',
            header: ({ column }: ColHeader<SharedPolicyGroup>) => <DataTableColumnHeader column={column} title="Last updated" />,
            cell: ({ row }: ColCell<SharedPolicyGroup>) =>
                row.original.updatedAt ? (
                    <DateCell value={new Date(row.original.updatedAt)} format="absolute" />
                ) : (
                    <span className="text-sm text-muted-foreground">—</span>
                ),
        },
        {
            id: 'deployedAt',
            accessorFn: (row: SharedPolicyGroup) => row.deployedAt ?? '',
            header: ({ column }: ColHeader<SharedPolicyGroup>) => <DataTableColumnHeader column={column} title="Last deployed" />,
            cell: ({ row }: ColCell<SharedPolicyGroup>) =>
                row.original.deployedAt ? (
                    <DateCell value={new Date(row.original.deployedAt)} format="absolute" />
                ) : (
                    <span className="text-sm text-muted-foreground">—</span>
                ),
        },
        {
            id: 'actions',
            header: () => <span className="sr-only">Actions</span>,
            size: 100,
            enableSorting: false,
            enableHiding: false,
            cell: ({ row }: ColCell<SharedPolicyGroup>) => {
                const sharedPolicyGroup = row.original;
                const readOnlyRow = !canEdit || isKubernetesOrigin(sharedPolicyGroup);
                const showDelete = !readOnlyRow && canDelete;
                return (
                    <div className="flex items-center justify-end gap-1">
                        {isKubernetesOrigin(sharedPolicyGroup) && <KubernetesIcon className="size-4 text-muted-foreground" aria-hidden />}
                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon" className="size-8" aria-label={`${sharedPolicyGroup.name} actions`}>
                                    <MoreHorizontalIcon className="size-4" aria-hidden />
                                </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                                {readOnlyRow ? (
                                    <DropdownMenuItem onSelect={() => onView(sharedPolicyGroup)}>
                                        <EyeIcon className="size-4 mr-2" aria-hidden />
                                        View
                                    </DropdownMenuItem>
                                ) : (
                                    <DropdownMenuItem onSelect={() => onView(sharedPolicyGroup)}>
                                        <PencilIcon className="size-4 mr-2" aria-hidden />
                                        Edit
                                    </DropdownMenuItem>
                                )}
                                {showDelete && (
                                    <>
                                        <DropdownMenuSeparator />
                                        <DropdownMenuItem variant="destructive" onSelect={() => onDelete(sharedPolicyGroup)}>
                                            <Trash2Icon className="size-4 mr-2" aria-hidden />
                                            Delete
                                        </DropdownMenuItem>
                                    </>
                                )}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                );
            },
        },
    ];
}

interface SharedPolicyGroupsTableProps {
    readonly sharedPolicyGroups: SharedPolicyGroup[];
    readonly totalCount: number;
    readonly loading: boolean;
    readonly isFirstUse: boolean;
    readonly search: string;
    readonly page: number;
    readonly pageSize: number;
    readonly sorting: TableSortingState;
    readonly canEdit: boolean;
    readonly canDelete: boolean;
    readonly onSearchChange: (value: string) => void;
    readonly onPageChange: (page: number) => void;
    readonly onPageSizeChange: (size: number) => void;
    readonly onSortingChange: (updater: TableSortingState | ((previous: TableSortingState) => TableSortingState)) => void;
    readonly onView: (sharedPolicyGroup: SharedPolicyGroup) => void;
    readonly onDelete: (sharedPolicyGroup: SharedPolicyGroup) => void;
    readonly onCreateSharedPolicyGroup?: () => void;
}

export function SharedPolicyGroupsTable({
    sharedPolicyGroups,
    totalCount,
    loading,
    isFirstUse,
    search,
    page,
    pageSize,
    sorting,
    canEdit,
    canDelete,
    onSearchChange,
    onPageChange,
    onPageSizeChange,
    onSortingChange,
    onView,
    onDelete,
    onCreateSharedPolicyGroup,
}: SharedPolicyGroupsTableProps) {
    const columns = useMemo(() => buildColumns({ canEdit, canDelete, onView, onDelete }), [canEdit, canDelete, onView, onDelete]);

    if (isFirstUse) {
        return (
            <div className="rounded-lg border">
                <DataTableEmptyState
                    variant="first-use"
                    icon={<LayersIcon className="size-8" aria-hidden />}
                    title="No Shared Policy Groups"
                    description="Shared Policy Groups let you create reusable policy bundles and apply them across multiple API flows."
                    primaryAction={
                        onCreateSharedPolicyGroup ? <Button onClick={onCreateSharedPolicyGroup}>Add Shared Policy Group</Button> : undefined
                    }
                />
            </div>
        );
    }

    return (
        <DataTable
            aria-label="Shared Policy Groups"
            columns={columns}
            data={sharedPolicyGroups}
            loading={loading}
            skeletonCount={pageSize}
            serverSide
            sorting={sorting}
            onSortingChange={onSortingChange}
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
                    title="No Shared Policy Group matches your search"
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
                        <InputGroupInput
                            placeholder="Search by name or description…"
                            value={search}
                            onChange={e => onSearchChange(e.target.value)}
                        />
                    </InputGroup>
                </div>
            }
        />
    );
}
